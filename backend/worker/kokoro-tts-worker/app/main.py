from __future__ import annotations

import base64
import io
import logging
import math
import os
import re
import wave
from typing import List, Optional

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

logger = logging.getLogger("uvicorn.error")

app = FastAPI(title="kokoro-tts-worker", version="0.1.0")

_pipeline = None
_pipeline_error: Optional[str] = None
_engine_name = "fallback"
_default_sample_rate = int(os.environ.get("KOKORO_SAMPLE_RATE", "24000"))
_strict_startup = os.environ.get("KOKORO_STRICT_STARTUP", "false").lower() == "true"
_enable_real_model = os.environ.get("KOKORO_ENABLE_MODEL", "true").lower() == "true"


class TtsPageInput(BaseModel):
    pageNumber: int = Field(..., ge=1)
    text: str


class TtsSynthesizeBatchRequest(BaseModel):
    chapterId: str
    lang: str = "auto"
    voice: str = "af_heart"
    speed: float = Field(1.0, ge=0.5, le=2.0)
    pages: List[TtsPageInput]


class TtsAudioPage(BaseModel):
    pageNumber: int
    audioBase64: str
    durationMs: int
    sampleRateHz: int
    format: str = "wav"


class TtsSynthesizeBatchResponse(BaseModel):
    status: str
    error: Optional[str] = None
    audioPages: List[TtsAudioPage] = Field(default_factory=list)


def _load_pipeline() -> None:
    global _pipeline
    global _pipeline_error
    global _engine_name

    if not _enable_real_model:
        _pipeline = None
        _engine_name = "fallback"
        _pipeline_error = "KOKORO_ENABLE_MODEL=false"
        logger.warning("Kokoro real model disabled, fallback audio generator is active")
        return

    try:
        from kokoro import KPipeline  # type: ignore

        # Language code is decided per request call.
        _pipeline = KPipeline(lang_code="a")
        _engine_name = "kokoro"
        _pipeline_error = None
        logger.info("Kokoro pipeline initialized")
    except Exception as exception:
        _pipeline = None
        _engine_name = "fallback"
        _pipeline_error = str(exception)
        logger.warning("Kokoro pipeline unavailable, using fallback audio generator: %s", exception)
        if _strict_startup:
            raise


@app.on_event("startup")
def startup() -> None:
    _load_pipeline()


@app.get("/health")
def health() -> dict:
    if _pipeline is not None:
        return {"status": "ok", "engine": _engine_name}
    if _strict_startup:
        raise HTTPException(status_code=503, detail=f"Kokoro unavailable: {_pipeline_error}")
    return {"status": "ok", "engine": _engine_name, "warning": _pipeline_error}


@app.post("/tts/synthesize-batch", response_model=TtsSynthesizeBatchResponse)
def synthesize_batch(request: TtsSynthesizeBatchRequest) -> TtsSynthesizeBatchResponse:
    if not request.pages:
        raise HTTPException(status_code=400, detail="pages must not be empty")

    audio_pages: List[TtsAudioPage] = []
    for page in request.pages:
        if page.text is None:
            raise HTTPException(status_code=400, detail=f"text is required for page {page.pageNumber}")

        normalized = page.text.strip()
        if not normalized:
            # Return short silent clip for empty text to keep page index stable.
            wav_bytes, duration_ms, sample_rate = _fallback_silent_clip()
        else:
            wav_bytes, duration_ms, sample_rate = _synthesize_wav(
                text=normalized,
                lang=request.lang,
                voice=request.voice,
                speed=request.speed,
            )

        audio_pages.append(
            TtsAudioPage(
                pageNumber=page.pageNumber,
                audioBase64=base64.b64encode(wav_bytes).decode("ascii"),
                durationMs=duration_ms,
                sampleRateHz=sample_rate,
                format="wav",
            )
        )

    return TtsSynthesizeBatchResponse(status="SUCCEEDED", audioPages=audio_pages)


def _synthesize_wav(text: str, lang: str, voice: str, speed: float) -> tuple[bytes, int, int]:
    if _pipeline is not None:
        try:
            samples = _synthesize_with_kokoro(text=text, lang=lang, voice=voice, speed=speed)
            wav_bytes = _float_audio_to_wav_bytes(samples, _default_sample_rate)
            duration_ms = _duration_ms(samples, _default_sample_rate)
            return wav_bytes, duration_ms, _default_sample_rate
        except Exception as exception:
            logger.warning("Kokoro synthesis failed, falling back to tone: %s", exception)

    return _fallback_tone(text=text)


def _synthesize_with_kokoro(text: str, lang: str, voice: str, speed: float) -> np.ndarray:
    from kokoro import KPipeline  # type: ignore

    lang_code = _map_lang_code(lang)
    pipeline = KPipeline(lang_code=lang_code)
    chunks: List[np.ndarray] = []

    for segment in _split_text(text):
        generator = pipeline(segment, voice=voice, speed=speed, split_pattern=r"\n+")
        for _, _, audio in generator:
            audio_np = np.asarray(audio, dtype=np.float32)
            if audio_np.size == 0:
                continue
            chunks.append(audio_np)

    if not chunks:
        return np.zeros(int(0.2 * _default_sample_rate), dtype=np.float32)
    return np.concatenate(chunks)


def _split_text(text: str, max_chars: int = 220) -> List[str]:
    normalized = re.sub(r"\s+", " ", text).strip()
    if len(normalized) <= max_chars:
        return [normalized]

    parts: List[str] = []
    remaining = normalized
    while len(remaining) > max_chars:
        cut = remaining.rfind(" ", 0, max_chars)
        if cut <= 0:
            cut = max_chars
        parts.append(remaining[:cut].strip())
        remaining = remaining[cut:].strip()
    if remaining:
        parts.append(remaining)
    return parts


def _map_lang_code(lang: str) -> str:
    normalized = (lang or "").strip().lower()
    if normalized in {"en", "en-us", "en_us", "english"}:
        return "a"
    if normalized in {"en-gb", "en_uk"}:
        return "b"
    if normalized in {"vi", "vi-vn", "vietnamese"}:
        # Kokoro language support may vary by runtime package; keep closest default.
        return "a"
    if normalized in {"ja", "japanese"}:
        return "j"
    if normalized in {"zh", "zh-cn", "chinese"}:
        return "z"
    return "a"


def _fallback_tone(text: str) -> tuple[bytes, int, int]:
    sample_rate = _default_sample_rate
    duration_s = max(0.3, min(len(text) / 26.0, 12.0))
    t = np.linspace(0, duration_s, int(sample_rate * duration_s), endpoint=False, dtype=np.float32)
    wave_1 = 0.18 * np.sin(2.0 * math.pi * 220.0 * t)
    wave_2 = 0.08 * np.sin(2.0 * math.pi * 330.0 * t)
    audio = np.clip(wave_1 + wave_2, -0.9, 0.9)
    wav_bytes = _float_audio_to_wav_bytes(audio, sample_rate)
    duration_ms = _duration_ms(audio, sample_rate)
    return wav_bytes, duration_ms, sample_rate


def _fallback_silent_clip() -> tuple[bytes, int, int]:
    sample_rate = _default_sample_rate
    audio = np.zeros(int(0.2 * sample_rate), dtype=np.float32)
    wav_bytes = _float_audio_to_wav_bytes(audio, sample_rate)
    return wav_bytes, _duration_ms(audio, sample_rate), sample_rate


def _float_audio_to_wav_bytes(audio: np.ndarray, sample_rate: int) -> bytes:
    clipped = np.clip(audio, -1.0, 1.0)
    pcm = (clipped * 32767.0).astype(np.int16)
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(pcm.tobytes())
    return buffer.getvalue()


def _duration_ms(audio: np.ndarray, sample_rate: int) -> int:
    if sample_rate <= 0:
        return 0
    return int((audio.size / sample_rate) * 1000)
