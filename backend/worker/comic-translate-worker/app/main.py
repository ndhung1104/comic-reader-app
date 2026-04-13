from __future__ import annotations

from datetime import datetime, timezone
import logging
import os
from pathlib import Path
from threading import Lock, Semaphore, Thread
from typing import Dict, List, Optional
from urllib.parse import unquote, urlparse
from uuid import uuid4
import time
from io import BytesIO

import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from .ocr_pipeline import OgkaluHeadlessOcr, UnsupportedImageFormatError


class PageInput(BaseModel):
    pageNumber: int = Field(..., ge=1)
    imageUrl: str


class SubmitJobRequest(BaseModel):
    idempotencyKey: Optional[str] = None
    chapterId: int
    comicId: Optional[int] = None
    sourceLang: str = "auto"
    targetLang: str = "vi"
    pages: List[PageInput]


class OcrPageText(BaseModel):
    chapterId: int
    pageNumber: int
    sourceLang: str
    ocrText: str


class JobStatusResponse(BaseModel):
    jobId: str
    status: str
    error: Optional[str] = None
    ocrPages: List[OcrPageText] = Field(default_factory=list)
    artifacts: Dict[str, object] = Field(default_factory=dict)


class SubmitJobResponse(BaseModel):
    jobId: str
    status: str


app = FastAPI(title="comic-translate-worker", version="0.1.0")
logger = logging.getLogger("uvicorn.error")

_jobs: Dict[str, JobStatusResponse] = {}
_idempotency_map: Dict[str, str] = {}
_lock = Lock()
_warmup_lock = Lock()
_warmup_started = False
_max_concurrent_jobs = max(1, int(os.environ.get("WORKER_MAX_CONCURRENT_JOBS", "1")))
_job_slots = Semaphore(_max_concurrent_jobs)
_model_root = Path(
    os.environ.get("OCR_MODEL_DIR")
    or os.environ.get("CT_MODEL_CACHE_DIR")
    or "/var/lib/comic-translate/models"
)
_ocr_pipeline = OgkaluHeadlessOcr(model_root=_model_root, device=os.environ.get("OCR_DEVICE", "cpu"))
UNSUPPORTED_IMAGE_FALLBACK_TEXT = "dinh dang anh khong ho tro"


def _start_warmup_if_needed() -> None:
    global _warmup_started

    if _ocr_pipeline.ready:
        return

    with _warmup_lock:
        if _ocr_pipeline.ready or _warmup_started:
            return
        _warmup_started = True

        def _warmup_runner() -> None:
            try:
                logger.info("Starting OCR warmup in background")
                _ocr_pipeline.ensure_ready()
                logger.info("OCR warmup completed")
            except Exception:
                logger.exception("OCR warmup failed")

        Thread(target=_warmup_runner, daemon=True).start()


def _download_image_bytes(image_url: str) -> bytes:
    parsed = urlparse(image_url)
    scheme = parsed.scheme.lower()

    if scheme in {"http", "https"}:
        connect_timeout = float(os.environ.get("WORKER_IMAGE_FETCH_CONNECT_TIMEOUT_SECONDS", "10"))
        read_timeout = float(os.environ.get("WORKER_IMAGE_FETCH_READ_TIMEOUT_SECONDS", "45"))
        max_retries = max(1, int(os.environ.get("WORKER_IMAGE_FETCH_MAX_RETRIES", "4")))
        backoff_seconds = float(os.environ.get("WORKER_IMAGE_FETCH_BACKOFF_SECONDS", "1.5"))
        user_agent = os.environ.get(
            "WORKER_IMAGE_FETCH_USER_AGENT",
            "Mozilla/5.0 (X11; Linux x86_64) comic-translate-worker/0.1",
        )
        headers = {
            "User-Agent": user_agent,
            "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
            "Connection": "close",
        }

        last_exception: Optional[Exception] = None
        for attempt in range(1, max_retries + 1):
            try:
                with requests.get(
                    image_url,
                    timeout=(connect_timeout, read_timeout),
                    stream=True,
                    headers=headers,
                ) as response:
                    response.raise_for_status()
                    output = BytesIO()
                    for chunk in response.iter_content(chunk_size=1024 * 1024):
                        if chunk:
                            output.write(chunk)
                    return output.getvalue()
            except Exception as exc:
                last_exception = exc
                logger.warning(
                    "Image fetch failed (%d/%d) for %s: %s",
                    attempt,
                    max_retries,
                    image_url,
                    exc,
                )
                if attempt < max_retries:
                    time.sleep(backoff_seconds * attempt)

        raise RuntimeError(f"Failed to fetch image after retries: {image_url}: {last_exception}")

    if scheme == "file":
        path = Path(unquote(parsed.path))
        if not path.exists():
            raise RuntimeError(f"Image file does not exist: {path}")
        return path.read_bytes()

    raise RuntimeError(f"Unsupported image URL scheme: {scheme or '(missing scheme)'}")


def _process_job(job_id: str, request: SubmitJobRequest) -> None:
    with _job_slots:
        source_lang = request.sourceLang or "auto"
        start_time = datetime.now(timezone.utc)
        sorted_pages = sorted(request.pages, key=lambda page: page.pageNumber)
        ocr_pages: List[OcrPageText] = []
        fallback_page_count = 0
        total_pages = len(sorted_pages)

        try:
            for index, page in enumerate(sorted_pages, start=1):
                logger.info("Processing OCR page %d/%d (job=%s, chapter=%s)", index, total_pages, job_id, request.chapterId)
                image_bytes = _download_image_bytes(page.imageUrl)
                try:
                    ocr_text = _ocr_pipeline.extract_page_text(image_bytes=image_bytes, source_lang=source_lang)
                except UnsupportedImageFormatError:
                    fallback_page_count += 1
                    ocr_text = UNSUPPORTED_IMAGE_FALLBACK_TEXT
                    logger.warning(
                        "Unsupported image format at page %s (job=%s, chapter=%s); using fallback OCR text",
                        page.pageNumber,
                        job_id,
                        request.chapterId,
                    )
                ocr_page = OcrPageText(
                    chapterId=request.chapterId,
                    pageNumber=page.pageNumber,
                    sourceLang=source_lang,
                    ocrText=ocr_text,
                )
                ocr_pages.append(ocr_page)
                with _lock:
                    current = _jobs.get(job_id)
                    if current is not None:
                        current.status = "RUNNING"
                        current.error = None
                        current.ocrPages = list(ocr_pages)
                        current.artifacts = {
                            "worker": "comic-translate-worker-ogkalu-headless",
                            "ocrProvider": "ogkalu-rtdetr+manga-ocr",
                            "pagesProcessed": len(ocr_pages),
                            "totalPages": total_pages,
                            "fallbackPages": fallback_page_count,
                        }
                logger.info("Completed OCR page %d/%d (job=%s)", index, total_pages, job_id)

            duration_ms = int((datetime.now(timezone.utc) - start_time).total_seconds() * 1000)
            artifacts = {
                "rawTextJsonUrl": None,
                "translatedTextJsonUrl": None,
                "renderedArchiveUrl": None,
                "worker": "comic-translate-worker-ogkalu-headless",
                "ocrProvider": "ogkalu-rtdetr+manga-ocr",
                "pagesProcessed": len(ocr_pages),
                "totalPages": total_pages,
                "fallbackPages": fallback_page_count,
                "processingMs": duration_ms,
            }
            result = JobStatusResponse(
                jobId=job_id,
                status="SUCCEEDED",
                error=None,
                ocrPages=ocr_pages,
                artifacts=artifacts,
            )
        except Exception as exception:
            duration_ms = int((datetime.now(timezone.utc) - start_time).total_seconds() * 1000)
            result = JobStatusResponse(
                jobId=job_id,
                status="FAILED",
                error=str(exception),
                ocrPages=ocr_pages,
                artifacts={
                    "worker": "comic-translate-worker-ogkalu-headless",
                    "ocrProvider": "ogkalu-rtdetr+manga-ocr",
                    "pagesProcessed": len(ocr_pages),
                    "totalPages": total_pages,
                    "fallbackPages": fallback_page_count,
                    "processingMs": duration_ms,
                },
            )

        with _lock:
            _jobs[job_id] = result


@app.on_event("startup")
def startup() -> None:
    _start_warmup_if_needed()


@app.get("/health")
def health() -> Dict[str, str]:
    if _ocr_pipeline.ready:
        return {"status": "ok"}
    _start_warmup_if_needed()
    error = _ocr_pipeline.last_error
    if error:
        raise HTTPException(status_code=503, detail=f"OCR warmup failed: {error}")
    raise HTTPException(status_code=503, detail="OCR models are warming up")


@app.post("/jobs", response_model=SubmitJobResponse)
def submit_job(request: SubmitJobRequest) -> SubmitJobResponse:
    if not request.pages:
        raise HTTPException(status_code=400, detail="pages must not be empty")

    max_pages = int(os.environ.get("WORKER_MAX_PAGES_PER_JOB", "200"))
    if len(request.pages) > max_pages:
        raise HTTPException(status_code=400, detail=f"pages exceeds max allowed: {max_pages}")

    with _lock:
        if request.idempotencyKey and request.idempotencyKey in _idempotency_map:
            existing_job_id = _idempotency_map[request.idempotencyKey]
            existing = _jobs[existing_job_id]
            return SubmitJobResponse(jobId=existing.jobId, status=existing.status)

        job_id = str(uuid4())
        _jobs[job_id] = JobStatusResponse(
            jobId=job_id,
            status="RUNNING",
            error=None,
            ocrPages=[],
            artifacts={
                "worker": "comic-translate-worker-ogkalu-headless",
                "ocrProvider": "ogkalu-rtdetr+manga-ocr",
                "pagesProcessed": 0,
                "totalPages": len(request.pages),
                "fallbackPages": 0,
            },
        )

        if request.idempotencyKey:
            _idempotency_map[request.idempotencyKey] = job_id

    Thread(target=_process_job, args=(job_id, request), daemon=True).start()
    return SubmitJobResponse(jobId=job_id, status="RUNNING")


@app.get("/jobs/{job_id}", response_model=JobStatusResponse)
def get_job(job_id: str) -> JobStatusResponse:
    job = _jobs.get(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="job not found")
    return job


@app.get("/jobs/{job_id}/artifacts", response_model=JobStatusResponse)
def get_job_artifacts(job_id: str) -> JobStatusResponse:
    job = _jobs.get(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="job not found")
    return job
