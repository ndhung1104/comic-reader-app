from __future__ import annotations

import base64
import io
import logging
import math
import os
import re
import threading
import wave
from pathlib import Path
from typing import Dict, List, Optional

import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

logger = logging.getLogger("uvicorn.error")

app = FastAPI(title="piper-tts-worker", version="0.1.0")

_voice_cache: Dict[str, object] = {}
_voice_errors: Dict[str, str] = {}
_state_lock = threading.Lock()
_engine_name = "fallback"
_warmup_state = "starting"
_warmup_error: Optional[str] = None

_default_sample_rate = int(os.environ.get("PIPER_SAMPLE_RATE", "22050"))
_strict_startup = os.environ.get("PIPER_STRICT_STARTUP", "false").lower() == "true"
_enable_real_model = os.environ.get("PIPER_ENABLE_MODEL", "true").lower() == "true"
_default_voice = os.environ.get("PIPER_DEFAULT_VOICE", "vi_VN-vais1000-medium").strip()
_model_dir = Path(os.environ.get("PIPER_MODEL_DIR", "/var/lib/piper/models")).resolve()
_voice_vi = os.environ.get("PIPER_VOICE_VI", "vi_VN-vais1000-medium").strip()
_voice_en = os.environ.get("PIPER_VOICE_EN", "en_US-lessac-medium").strip()
_voice_ja = os.environ.get("PIPER_VOICE_JA", _voice_en).strip()
_voice_ko = os.environ.get("PIPER_VOICE_KO", _voice_en).strip()
_fallback_voice = os.environ.get("PIPER_FALLBACK_VOICE", _voice_en).strip()


class TtsPageInput(BaseModel):
    pageNumber: int = Field(..., ge=1)
    text: str


class TtsSynthesizeBatchRequest(BaseModel):
    chapterId: str
    lang: str = "auto"
    voice: Optional[str] = None
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


@app.on_event("startup")
def startup() -> None:
    _model_dir.mkdir(parents=True, exist_ok=True)
    if not _enable_real_model:
        _set_state("degraded", "PIPER_ENABLE_MODEL=false")
        logger.warning("Piper real model disabled, fallback audio generator is active")
        return

    thread = threading.Thread(target=_warmup_runner, name="piper-warmup", daemon=True)
    thread.start()


@app.get("/health")
def health() -> dict:
    with _state_lock:
        state = _warmup_state
        warning = _warmup_error
        engine = _engine_name

    if _strict_startup and state != "ready":
        detail = warning or "Piper warmup is still running"
        raise HTTPException(status_code=503, detail=detail)

    response = {
        "status": "ok",
        "engine": engine,
        "warmupState": state,
        "defaultVoice": _default_voice,
        "voiceMap": {
            "vi": _voice_vi,
            "en": _voice_en,
            "ja": _voice_ja,
            "ko": _voice_ko,
            "fallback": _fallback_voice,
        },
    }
    if warning:
        response["warning"] = warning
    return response


@app.post("/tts/synthesize-batch", response_model=TtsSynthesizeBatchResponse)
def synthesize_batch(request: TtsSynthesizeBatchRequest) -> TtsSynthesizeBatchResponse:
    if not request.pages:
        raise HTTPException(status_code=400, detail="pages must not be empty")

    voice = _resolve_voice(request.voice, request.lang)
    logger.info("Resolved voice '%s' for lang '%s' (requestedVoice=%s)", voice, request.lang, request.voice)
    audio_pages: List[TtsAudioPage] = []

    for page in request.pages:
        if page.text is None:
            raise HTTPException(status_code=400, detail=f"text is required for page {page.pageNumber}")

        normalized = page.text.strip()
        if not normalized:
            wav_bytes, duration_ms, sample_rate = _fallback_silent_clip()
        else:
            wav_bytes, duration_ms, sample_rate = _synthesize_wav(
                text=normalized,
                voice=voice,
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


def _warmup_runner() -> None:
    try:
        voice = _resolve_voice(_default_voice, "auto")
        _ensure_voice_ready(voice)
        _set_state("ready", None)
        logger.info("Piper warmup completed with voice %s", voice)
    except Exception as exception:
        logger.exception("Piper warmup failed")
        _set_state("degraded", str(exception))
        if _strict_startup:
            raise


def _set_state(state: str, warning: Optional[str]) -> None:
    global _warmup_state
    global _warmup_error
    with _state_lock:
        _warmup_state = state
        _warmup_error = warning


def _synthesize_wav(text: str, voice: str, speed: float) -> tuple[bytes, int, int]:
    if _enable_real_model:
        try:
            piper_voice = _ensure_voice_ready(voice)
            wav_bytes = _synthesize_with_piper(piper_voice, text, speed)
            duration_ms, sample_rate = _wav_duration_and_rate(wav_bytes)
            return wav_bytes, duration_ms, sample_rate
        except Exception as exception:
            logger.warning("Piper synthesis failed for voice %s, fallback tone enabled: %s", voice, exception)
            if _strict_startup:
                raise HTTPException(status_code=503, detail=f"Piper unavailable: {exception}") from exception

    return _fallback_tone(text=text)


def _ensure_voice_ready(voice: str) -> object:
    global _engine_name
    with _state_lock:
        cached = _voice_cache.get(voice)
    if cached is not None:
        return cached

    _ensure_voice_assets(voice)
    try:
        loaded = _load_piper_voice(voice)
    except Exception as first_exception:
        logger.warning("Piper voice '%s' failed to load, forcing redownload: %s", voice, first_exception)
        _download_voice_assets(voice, force_redownload=True)
        try:
            loaded = _load_piper_voice(voice)
        except Exception as second_exception:
            _voice_errors[voice] = str(second_exception)
            raise RuntimeError(f"Cannot load Piper voice '{voice}': {second_exception}") from second_exception

    with _state_lock:
        _voice_cache[voice] = loaded
        _engine_name = "piper"
    return loaded


def _ensure_voice_assets(voice: str) -> None:
    model_path = _model_dir / f"{voice}.onnx"
    config_path = _model_dir / f"{voice}.onnx.json"
    if model_path.exists() and config_path.exists():
        return

    _download_voice_assets(voice, force_redownload=False)

    if not model_path.exists() or not config_path.exists():
        raise FileNotFoundError(f"Missing Piper model assets for voice '{voice}' in {_model_dir}")


def _download_voice_assets(voice: str, force_redownload: bool) -> None:
    try:
        from piper.download_voices import download_voice  # type: ignore

        logger.info("Downloading Piper voice assets for %s (force=%s)", voice, force_redownload)
        download_voice(voice=voice, download_dir=_model_dir, force_redownload=force_redownload)
    except Exception as exception:
        _voice_errors[voice] = str(exception)
        raise RuntimeError(
                f"Failed to download Piper voice '{voice}'. "
                "Provide a valid voice name like vi_VN-vais1000-medium."
        ) from exception


def _load_piper_voice(voice: str) -> object:
    from piper.voice import PiperVoice  # type: ignore

    model_path = _model_dir / f"{voice}.onnx"
    config_path = _model_dir / f"{voice}.onnx.json"
    return PiperVoice.load(model_path=model_path, config_path=config_path, use_cuda=False)


def _synthesize_with_piper(voice: object, text: str, speed: float) -> bytes:
    from piper.config import SynthesisConfig  # type: ignore

    text_chunks = _split_text(text)
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav_writer:
        syn_config = SynthesisConfig(length_scale=_speed_to_length_scale(speed))
        first_chunk = True
        for chunk in text_chunks:
            voice.synthesize_wav(  # type: ignore[attr-defined]
                    chunk,
                    wav_writer,
                    syn_config=syn_config,
                    set_wav_format=first_chunk
            )
            first_chunk = False
    return buffer.getvalue()


def _speed_to_length_scale(speed: float) -> float:
    if speed <= 0:
        return 1.0
    return max(0.4, min(2.2, 1.0 / speed))


def _split_text(text: str, max_chars: int = 260) -> List[str]:
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


def _canonicalize_voice(raw_voice: str) -> str:
    voice = (raw_voice or "").strip()
    matched = re.match(r"^([A-Za-z]+)_([A-Za-z]+)-(.+)-([^-]+)$", voice)
    if not matched:
        return voice
    return f"{matched.group(1).lower()}_{matched.group(2).upper()}-{matched.group(3)}-{matched.group(4)}"


def _resolve_voice(request_voice: Optional[str], lang: str) -> str:
    if request_voice is not None and request_voice.strip() and request_voice.strip().lower() != "auto":
        return _canonicalize_voice(request_voice)

    lang_voice = _voice_for_lang(lang)
    if lang_voice:
        return _canonicalize_voice(lang_voice)

    if _default_voice:
        return _canonicalize_voice(_default_voice)
    return _canonicalize_voice(_fallback_voice)


def _voice_for_lang(lang: str) -> str:
    normalized = _normalize_lang(lang)
    if _is_lang(normalized, "vi", "vietnamese"):
        return _voice_vi or _fallback_voice
    if _is_lang(normalized, "ko", "korean"):
        return _voice_ko or _fallback_voice
    if _is_lang(normalized, "ja", "japanese"):
        return _voice_ja or _fallback_voice
    if _is_lang(normalized, "en", "english"):
        return _voice_en or _fallback_voice
    return _fallback_voice


def _normalize_lang(lang: str) -> str:
    return (lang or "").strip().lower()


def _is_lang(normalized: str, short_code: str, long_name: str) -> bool:
    return normalized == short_code \
        or normalized.startswith(short_code + "-") \
        or normalized.startswith(short_code + "_") \
        or normalized == long_name


def _wav_duration_and_rate(wav_bytes: bytes) -> tuple[int, int]:
    with wave.open(io.BytesIO(wav_bytes), "rb") as wav_reader:
        sample_rate = wav_reader.getframerate()
        frame_count = wav_reader.getnframes()
    if sample_rate <= 0:
        return 0, _default_sample_rate
    duration_ms = int((frame_count / sample_rate) * 1000)
    return duration_ms, sample_rate


def _fallback_tone(text: str) -> tuple[bytes, int, int]:
    sample_rate = _default_sample_rate
    duration_s = max(0.3, min(len(text) / 24.0, 12.0))
    timeline = np.linspace(0, duration_s, int(sample_rate * duration_s), endpoint=False, dtype=np.float32)
    wave_1 = 0.18 * np.sin(2.0 * math.pi * 220.0 * timeline)
    wave_2 = 0.08 * np.sin(2.0 * math.pi * 330.0 * timeline)
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
