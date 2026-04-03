from __future__ import annotations

from datetime import datetime, timezone
from threading import Lock
from typing import Dict, List, Optional
from uuid import uuid4

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


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
    ocrPages: List[OcrPageText] = []
    artifacts: Dict[str, object] = {}


class SubmitJobResponse(BaseModel):
    jobId: str
    status: str


app = FastAPI(title="comic-translate-worker", version="0.1.0")

_jobs: Dict[str, JobStatusResponse] = {}
_idempotency_map: Dict[str, str] = {}
_lock = Lock()


@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}


@app.post("/jobs", response_model=SubmitJobResponse)
def submit_job(request: SubmitJobRequest) -> SubmitJobResponse:
    if not request.pages:
        raise HTTPException(status_code=400, detail="pages must not be empty")

    with _lock:
        if request.idempotencyKey and request.idempotencyKey in _idempotency_map:
            existing_job_id = _idempotency_map[request.idempotencyKey]
            existing = _jobs[existing_job_id]
            return SubmitJobResponse(jobId=existing.jobId, status=existing.status)

        job_id = str(uuid4())
        source_lang = request.sourceLang or "auto"
        now_iso = datetime.now(timezone.utc).isoformat()

        ocr_pages = [
            OcrPageText(
                chapterId=request.chapterId,
                pageNumber=page.pageNumber,
                sourceLang=source_lang,
                ocrText=f"[stub-ocr {now_iso}] page {page.pageNumber}: {page.imageUrl}",
            )
            for page in request.pages
        ]

        artifacts = {
            "rawTextJsonUrl": None,
            "translatedTextJsonUrl": None,
            "renderedArchiveUrl": None,
            "worker": "comic-translate-worker-stub",
        }

        _jobs[job_id] = JobStatusResponse(
            jobId=job_id,
            status="SUCCEEDED",
            error=None,
            ocrPages=ocr_pages,
            artifacts=artifacts,
        )

        if request.idempotencyKey:
            _idempotency_map[request.idempotencyKey] = job_id

    return SubmitJobResponse(jobId=job_id, status="SUCCEEDED")


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
