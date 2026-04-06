from __future__ import annotations

import hashlib
import logging
import os
import re
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path
import time
from typing import Callable, Dict, List, Optional, Tuple

import cv2
import jaconv
import numpy as np
import onnxruntime as ort
from PIL import Image
import pyclipper
import requests
from shapely.geometry import Polygon

logger = logging.getLogger("uvicorn.error")


@dataclass(frozen=True)
class ModelAsset:
    name: str
    base_url: str
    files: tuple[tuple[str, str], ...]
    relative_dir: str


DETECTOR_ASSET = ModelAsset(
    name="rtdetr-v2-onnx",
    base_url="https://huggingface.co/ogkalu/comic-text-and-bubble-detector/resolve/main/",
    files=(
        ("detector.onnx", "065744e91c0594ad8663aa8b870ce3fb27222942eded5a3cc388ce23421bd195"),
    ),
    relative_dir="detection",
)

MANGA_OCR_ASSET = ModelAsset(
    name="manga-ocr-base-onnx",
    base_url="https://huggingface.co/mayocream/manga-ocr-onnx/resolve/main/",
    files=(
        ("encoder_model.onnx", "15fa8155fe9bc1a7d25d9bb353debaa4def033d0174e907dbd2dd6d995def85f"),
        ("decoder_model.onnx", "ef7765261e9d1cdc34d89356986c2bbc2a082897f753a89605ae80fdfa61f5e8"),
        ("vocab.txt", "5cb5c5586d98a2f331d9f8828e4586479b0611bfba5d8c3b6dadffc84d6a36a3"),
    ),
    relative_dir="ocr/manga-ocr-base-onnx",
)


def _sha256(path: Path) -> str:
    hasher = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def _download_file(url: str, target_path: Path) -> None:
    target_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = target_path.with_suffix(target_path.suffix + ".part")
    timeout_seconds = float(os.environ.get("OCR_MODEL_DOWNLOAD_TIMEOUT_SECONDS", "120"))
    connect_timeout_seconds = float(os.environ.get("OCR_MODEL_CONNECT_TIMEOUT_SECONDS", "20"))
    max_retries = int(os.environ.get("OCR_MODEL_DOWNLOAD_MAX_RETRIES", "6"))
    backoff_seconds = float(os.environ.get("OCR_MODEL_DOWNLOAD_BACKOFF_SECONDS", "2"))
    chunk_size = 1024 * 1024

    hf_endpoint = os.environ.get("HF_ENDPOINT", "").strip().rstrip("/")
    download_url = url
    if hf_endpoint and download_url.startswith("https://huggingface.co/"):
        download_url = hf_endpoint + download_url[len("https://huggingface.co") :]

    logger.info("Downloading model file from %s", download_url)

    last_exception: Optional[Exception] = None
    for attempt in range(1, max_retries + 1):
        try:
            resume_from = temp_path.stat().st_size if temp_path.exists() else 0
            headers: Dict[str, str] = {}
            if resume_from > 0:
                headers["Range"] = f"bytes={resume_from}-"

            with requests.get(
                download_url,
                headers=headers,
                stream=True,
                timeout=(connect_timeout_seconds, timeout_seconds),
            ) as response:
                response.raise_for_status()

                # If server ignores range, restart from scratch.
                if resume_from > 0 and response.status_code == 200:
                    temp_path.unlink(missing_ok=True)
                    resume_from = 0

                mode = "ab" if resume_from > 0 else "wb"
                with temp_path.open(mode) as output:
                    for chunk in response.iter_content(chunk_size=chunk_size):
                        if chunk:
                            output.write(chunk)

            os.replace(temp_path, target_path)
            return
        except Exception as exc:
            last_exception = exc
            logger.warning(
                "Model download attempt %d/%d failed for %s: %s",
                attempt,
                max_retries,
                target_path.name,
                exc,
            )
            if attempt < max_retries:
                sleep_seconds = backoff_seconds * attempt
                time.sleep(sleep_seconds)

    raise RuntimeError(f"Failed to download model file {target_path.name}: {last_exception}")


def ensure_asset(model_root: Path, asset: ModelAsset) -> Path:
    asset_dir = model_root / asset.relative_dir
    asset_dir.mkdir(parents=True, exist_ok=True)

    for file_name, expected_sha in asset.files:
        target_path = asset_dir / file_name
        if not target_path.exists():
            _download_file(asset.base_url + file_name, target_path)
        actual_sha = _sha256(target_path)
        if actual_sha != expected_sha:
            logger.warning("Checksum mismatch for %s, re-downloading", target_path)
            target_path.unlink(missing_ok=True)
            _download_file(asset.base_url + file_name, target_path)
            actual_sha = _sha256(target_path)
            if actual_sha != expected_sha:
                raise RuntimeError(f"Checksum mismatch for {target_path.name}")

    return asset_dir


def ensure_downloaded_file(target_path: Path, url: str, expected_sha: Optional[str]) -> Path:
    target_path.parent.mkdir(parents=True, exist_ok=True)
    if not target_path.exists():
        _download_file(url, target_path)

    if expected_sha:
        actual_sha = _sha256(target_path)
        if actual_sha != expected_sha:
            logger.warning("Checksum mismatch for %s, re-downloading", target_path)
            target_path.unlink(missing_ok=True)
            _download_file(url, target_path)
            actual_sha = _sha256(target_path)
            if actual_sha != expected_sha:
                raise RuntimeError(f"Checksum mismatch for {target_path.name}")
    return target_path


def _providers_for_device(device: str) -> list:
    normalized = (device or "cpu").strip().lower()
    if normalized == "cpu":
        return ["CPUExecutionProvider"]

    available = ort.get_available_providers()
    configured: list = []
    if "CUDAExecutionProvider" in available and normalized in {"cuda", "gpu"}:
        configured.append("CUDAExecutionProvider")
    configured.append("CPUExecutionProvider")
    return configured


def _make_session(model_path: Path, device: str) -> ort.InferenceSession:
    options = ort.SessionOptions()
    options.log_severity_level = 3
    options.enable_mem_pattern = False
    options.enable_cpu_mem_arena = False
    # Keep ORT memory usage predictable on low-RAM hosts.
    options.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL
    options.intra_op_num_threads = int(os.environ.get("OCR_ORT_INTRA_OP_THREADS", "1"))
    options.inter_op_num_threads = int(os.environ.get("OCR_ORT_INTER_OP_THREADS", "1"))
    return ort.InferenceSession(str(model_path), sess_options=options, providers=_providers_for_device(device))


def _calculate_iou(rect1: np.ndarray, rect2: np.ndarray) -> float:
    x1 = max(rect1[0], rect2[0])
    y1 = max(rect1[1], rect2[1])
    x2 = min(rect1[2], rect2[2])
    y2 = min(rect1[3], rect2[3])
    intersection = max(0, x2 - x1) * max(0, y2 - y1)
    if intersection <= 0:
        return 0.0
    area1 = max(0, rect1[2] - rect1[0]) * max(0, rect1[3] - rect1[1])
    area2 = max(0, rect2[2] - rect2[0]) * max(0, rect2[3] - rect2[1])
    denominator = area1 + area2 - intersection
    if denominator <= 0:
        return 0.0
    return float(intersection / denominator)


def _contains(outer: np.ndarray, inner: np.ndarray) -> bool:
    return outer[0] <= inner[0] and outer[1] <= inner[1] and outer[2] >= inner[2] and outer[3] >= inner[3]


class ImageSlicer:
    def __init__(self) -> None:
        self.height_to_width_ratio_threshold = 3.5
        self.target_slice_ratio = 3.0
        self.overlap_height_ratio = 0.2
        self.merge_iou_threshold = 0.5
        self.merge_y_distance_ratio = 0.1

    def should_slice(self, image: np.ndarray) -> bool:
        h, w = image.shape[:2]
        return w > 0 and (h / float(w)) > self.height_to_width_ratio_threshold

    def _slice_params(self, image: np.ndarray) -> tuple[int, int, int]:
        h, w = image.shape[:2]
        slice_height = max(64, int(w * self.target_slice_ratio))
        effective = max(32, int(slice_height * (1.0 - self.overlap_height_ratio)))
        num_slices = max(1, int(np.ceil(h / float(effective))))
        return slice_height, effective, num_slices

    def _get_slice(self, image: np.ndarray, index: int, slice_height: int, effective: int) -> tuple[np.ndarray, int]:
        start_y = index * effective
        end_y = min(image.shape[0], start_y + slice_height)
        return image[start_y:end_y, :], start_y

    def _adjust_boxes(self, boxes: np.ndarray, offset_y: int) -> np.ndarray:
        if boxes.size == 0:
            return boxes
        out = boxes.copy()
        out[:, 1] += offset_y
        out[:, 3] += offset_y
        return out

    def _merge_boxes(self, boxes: np.ndarray, image_height: int) -> np.ndarray:
        if boxes.size == 0:
            return boxes
        merged: list[np.ndarray] = []
        y_threshold = max(4.0, image_height * self.merge_y_distance_ratio)
        for box in boxes:
            candidate = box.astype(np.float32)
            was_merged = False
            for idx, existing in enumerate(merged):
                iou = _calculate_iou(existing, candidate)
                y_distance = min(abs(existing[1] - candidate[3]), abs(existing[3] - candidate[1]))
                if iou >= self.merge_iou_threshold or y_distance <= y_threshold:
                    merged[idx] = np.array(
                        [
                            min(existing[0], candidate[0]),
                            min(existing[1], candidate[1]),
                            max(existing[2], candidate[2]),
                            max(existing[3], candidate[3]),
                        ],
                        dtype=np.float32,
                    )
                    was_merged = True
                    break
            if not was_merged:
                merged.append(candidate)
        return np.array(merged, dtype=np.int32)

    def process_slices(
        self, image: np.ndarray, detect_fn: Callable[[np.ndarray], tuple[np.ndarray, np.ndarray]]
    ) -> tuple[np.ndarray, np.ndarray]:
        if not self.should_slice(image):
            return detect_fn(image)

        slice_height, effective, num_slices = self._slice_params(image)
        all_bubbles: list[np.ndarray] = []
        all_texts: list[np.ndarray] = []

        for index in range(num_slices):
            slice_image, offset_y = self._get_slice(image, index, slice_height, effective)
            bubble_boxes, text_boxes = detect_fn(slice_image)
            if bubble_boxes.size > 0:
                all_bubbles.append(self._adjust_boxes(bubble_boxes, offset_y))
            if text_boxes.size > 0:
                all_texts.append(self._adjust_boxes(text_boxes, offset_y))

        bubble = np.vstack(all_bubbles) if all_bubbles else np.empty((0, 4), dtype=np.int32)
        text = np.vstack(all_texts) if all_texts else np.empty((0, 4), dtype=np.int32)
        return self._merge_boxes(bubble, image.shape[0]), self._merge_boxes(text, image.shape[0])


class RTDetrV2Detector:
    def __init__(self, model_root: Path, device: str = "cpu") -> None:
        self.model_root = model_root
        self.device = device
        self.session: Optional[ort.InferenceSession] = None
        self.slicer = ImageSlicer()
        self.confidence_threshold = 0.3

    def initialize(self) -> None:
        detector_dir = ensure_asset(self.model_root, DETECTOR_ASSET)
        self.session = _make_session(detector_dir / "detector.onnx", self.device)

    def _detect_single_image(self, image: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
        if self.session is None:
            raise RuntimeError("Detector session is not initialized")
        pil_image = Image.fromarray(image)
        resized = pil_image.resize((640, 640), Image.Resampling.BILINEAR)
        array = np.asarray(resized, dtype=np.float32) / 255.0
        array = np.transpose(array, (2, 0, 1))[None, ...]
        width, height = pil_image.size
        orig_size = np.array([[width, height]], dtype=np.int64)

        labels, boxes, scores = self.session.run(
            None,
            {"images": array, "orig_target_sizes": orig_size},
        )[:3]

        if labels.ndim == 2:
            labels = labels[0]
        if boxes.ndim == 3:
            boxes = boxes[0]
        if scores.ndim == 2:
            scores = scores[0]

        bubble: list[list[int]] = []
        text: list[list[int]] = []
        for label, box, score in zip(labels, boxes, scores):
            if float(score) < self.confidence_threshold:
                continue
            x1, y1, x2, y2 = [int(v) for v in box]
            if x2 <= x1 or y2 <= y1:
                continue
            if int(label) == 0:
                bubble.append([x1, y1, x2, y2])
            elif int(label) in {1, 2}:
                text.append([x1, y1, x2, y2])

        return (
            np.array(bubble, dtype=np.int32) if bubble else np.empty((0, 4), dtype=np.int32),
            np.array(text, dtype=np.int32) if text else np.empty((0, 4), dtype=np.int32),
        )

    def detect(self, image: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
        return self.slicer.process_slices(image, self._detect_single_image)


class MangaOcrOnnx:
    def __init__(self, model_root: Path, device: str = "cpu") -> None:
        self.model_root = model_root
        self.device = device
        self.encoder: Optional[ort.InferenceSession] = None
        self.decoder: Optional[ort.InferenceSession] = None
        self.vocab: list[str] = []
        self.encoder_image_input = ""
        self.encoder_output_name = ""
        self.decoder_token_input = ""
        self.decoder_encoder_input = ""

    def initialize(self) -> None:
        model_dir = ensure_asset(self.model_root, MANGA_OCR_ASSET)
        self.encoder = _make_session(model_dir / "encoder_model.onnx", self.device)
        self.decoder = _make_session(model_dir / "decoder_model.onnx", self.device)

        vocab_path = model_dir / "vocab.txt"
        self.vocab = vocab_path.read_text(encoding="utf-8").splitlines()

        self.encoder_image_input = self._find_input(self.encoder, ("image", "pixel_values", "input"))
        self.encoder_output_name = self.encoder.get_outputs()[0].name
        self.decoder_token_input = self._find_input(self.decoder, ("token_ids", "input_ids", "input"))
        self.decoder_encoder_input = self._find_input(
            self.decoder, ("encoder_hidden_states", "encoder_outputs", "encoder_last_hidden_state")
        )

    @staticmethod
    def _find_input(session: ort.InferenceSession, candidates: tuple[str, ...]) -> str:
        names = [input_item.name for input_item in session.get_inputs()]
        for candidate in candidates:
            for name in names:
                if candidate in name:
                    return name
        return names[0]

    @staticmethod
    def _preprocess(image: np.ndarray) -> np.ndarray:
        pil = Image.fromarray(image).convert("L").convert("RGB")
        pil = pil.resize((224, 224), resample=Image.Resampling.BILINEAR)
        array = np.asarray(pil, dtype=np.float32) / 255.0
        array = (array - 0.5) / 0.5
        array = np.transpose(array, (2, 0, 1))[None, ...]
        return array.astype(np.float32)

    def _decode_tokens(self, token_ids: list[int]) -> str:
        out = []
        for token_id in token_ids:
            if token_id < 5:
                continue
            if token_id < len(self.vocab):
                out.append(self.vocab[token_id])
        text = "".join(out)
        text = "".join(text.split())
        text = text.replace("\u2026", "...")
        text = re.sub(r"[\u30fb.]{2,}", lambda match: "." * len(match.group(0)), text)
        return jaconv.h2z(text, ascii=True, digit=True)

    def recognize(self, image: np.ndarray) -> str:
        if self.encoder is None or self.decoder is None:
            raise RuntimeError("Manga OCR sessions are not initialized")
        if image.size == 0:
            return ""

        encoded_input = self._preprocess(image)
        hidden = self.encoder.run(None, {self.encoder_image_input: encoded_input})[0]
        tokens = [2]

        for _ in range(300):
            logits = self.decoder.run(
                None,
                {
                    self.decoder_token_input: np.array([tokens], dtype=np.int64),
                    self.decoder_encoder_input: hidden,
                },
            )[0]
            next_token = int(np.argmax(logits[0, -1, :]))
            tokens.append(next_token)
            if next_token == 3:
                break

        return self._decode_tokens(tokens)


class PPOcrV5Onnx:
    DETECTOR_URL = "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/det/ch_PP-OCRv5_mobile_det.onnx"
    DETECTOR_SHA256 = "4d97c44a20d30a81aad087d6a396b08f786c4635742afc391f6621f5c6ae78ae"

    REC_CONFIGS: Dict[str, Dict[str, str]] = {
        "ch": {
            "onnx_name": "ch_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/rec/ch_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_sha256": "5825fc7ebf84ae7a412be049820b4d86d77620f204a041697b0494669b1742c5",
            "dict_name": "ppocrv5_dict.txt",
            "dict_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/paddle/PP-OCRv5/rec/ch_PP-OCRv5_rec_mobile_infer/ppocrv5_dict.txt",
        },
        "en": {
            "onnx_name": "en_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/rec/en_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_sha256": "c3461add59bb4323ecba96a492ab75e06dda42467c9e3d0c18db5d1d21924be8",
            "dict_name": "ppocrv5_en_dict.txt",
            "dict_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/paddle/PP-OCRv5/rec/en_PP-OCRv5_rec_mobile_infer/ppocrv5_en_dict.txt",
        },
        "ko": {
            "onnx_name": "korean_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/rec/korean_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_sha256": "cd6e2ea50f6943ca7271eb8c56a877a5a90720b7047fe9c41a2e541a25773c9b",
            "dict_name": "ppocrv5_korean_dict.txt",
            "dict_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/paddle/PP-OCRv5/rec/korean_PP-OCRv5_rec_mobile_infer/ppocrv5_korean_dict.txt",
        },
        "latin": {
            "onnx_name": "latin_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/rec/latin_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_sha256": "b20bd37c168a570f583afbc8cd7925603890efbcdc000a59e22c269d160b5f5a",
            "dict_name": "ppocrv5_latin_dict.txt",
            "dict_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/paddle/PP-OCRv5/rec/latin_PP-OCRv5_rec_mobile_infer/ppocrv5_latin_dict.txt",
        },
        "eslav": {
            "onnx_name": "eslav_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/onnx/PP-OCRv5/rec/eslav_PP-OCRv5_rec_mobile_infer.onnx",
            "onnx_sha256": "08705d6721849b1347d26187f15a5e362c431963a2a62bfff4feac578c489aab",
            "dict_name": "ppocrv5_eslav_dict.txt",
            "dict_url": "https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.4.0/paddle/PP-OCRv5/rec/eslav_PP-OCRv5_rec_mobile_infer/ppocrv5_eslav_dict.txt",
        },
    }

    def __init__(self, model_root: Path, device: str = "cpu") -> None:
        self.model_root = model_root
        self.device = device
        self.model_dir = model_root / "ocr" / "ppocr-v5-onnx"
        self.det_session: Optional[ort.InferenceSession] = None
        self.rec_sessions: Dict[str, ort.InferenceSession] = {}
        self.rec_vocabularies: Dict[str, List[str]] = {}

    @staticmethod
    def _map_rec_key(source_lang_code: str) -> str:
        code = (source_lang_code or "").strip().lower()
        if code.startswith("ko"):
            return "ko"
        if code.startswith("zh"):
            return "ch"
        if code.startswith("ru"):
            return "eslav"
        if code.startswith("en"):
            return "en"
        return "latin"

    @staticmethod
    def _is_no_space_language(source_lang_code: str) -> bool:
        code = (source_lang_code or "").strip().lower()
        return code.startswith("zh") or code.startswith("ja") or code.startswith("th")

    def initialize(self) -> None:
        if self.det_session is not None:
            return
        det_path = ensure_downloaded_file(
            self.model_dir / "ch_PP-OCRv5_mobile_det.onnx",
            self.DETECTOR_URL,
            self.DETECTOR_SHA256,
        )
        self.det_session = _make_session(det_path, self.device)

    def _ensure_recognizer(self, rec_key: str) -> tuple[ort.InferenceSession, List[str]]:
        if rec_key in self.rec_sessions and rec_key in self.rec_vocabularies:
            return self.rec_sessions[rec_key], self.rec_vocabularies[rec_key]

        config = self.REC_CONFIGS[rec_key]
        onnx_path = ensure_downloaded_file(
            self.model_dir / config["onnx_name"],
            config["onnx_url"],
            config["onnx_sha256"],
        )
        dict_path = ensure_downloaded_file(
            self.model_dir / config["dict_name"],
            config["dict_url"],
            None,
        )
        session = _make_session(onnx_path, self.device)
        vocab = dict_path.read_text(encoding="utf-8").splitlines()

        self.rec_sessions[rec_key] = session
        self.rec_vocabularies[rec_key] = vocab
        return session, vocab

    @staticmethod
    def _resize_keep_stride(image: np.ndarray, limit_side_len: int = 960, limit_type: str = "min") -> np.ndarray:
        h, w = image.shape[:2]
        if limit_type == "max":
            ratio = float(limit_side_len) / max(h, w) if max(h, w) > limit_side_len else 1.0
        else:
            ratio = float(limit_side_len) / min(h, w) if min(h, w) < limit_side_len else 1.0
        nh = max(32, int(round((h * ratio) / 32) * 32))
        nw = max(32, int(round((w * ratio) / 32) * 32))
        if nh == h and nw == w:
            return image
        return cv2.resize(image, (nw, nh), interpolation=cv2.INTER_LINEAR)

    @classmethod
    def _det_preprocess(cls, image: np.ndarray) -> np.ndarray:
        resized = cls._resize_keep_stride(image, limit_side_len=960, limit_type="min")
        array = resized.astype(np.float32) / 255.0
        array = (array - np.array((0.5, 0.5, 0.5), dtype=np.float32)) / np.array((0.5, 0.5, 0.5), dtype=np.float32)
        array = np.transpose(array, (2, 0, 1))
        return np.expand_dims(array, axis=0).astype(np.float32)

    @staticmethod
    def _min_box(contour: np.ndarray) -> tuple[np.ndarray, float]:
        rect = cv2.minAreaRect(contour)
        pts = cv2.boxPoints(rect).astype(np.float32)
        pts = sorted(pts.tolist(), key=lambda p: p[0])
        left = np.array(pts[:2], dtype=np.float32)
        right = np.array(pts[2:], dtype=np.float32)
        tl, bl = left[np.argsort(left[:, 1])]
        tr, br = right[np.argsort(right[:, 1])]
        box = np.array([tl, tr, br, bl], dtype=np.float32)
        return box, min(rect[1][0], rect[1][1])

    @staticmethod
    def _order_clockwise(points: np.ndarray) -> np.ndarray:
        xs = points[np.argsort(points[:, 0])]
        left = xs[:2]
        right = xs[2:]
        tl, bl = left[np.argsort(left[:, 1])]
        tr, br = right[np.argsort(right[:, 1])]
        return np.array([tl, tr, br, bl], dtype=np.float32)

    @staticmethod
    def _score_fast(probability_map: np.ndarray, box: np.ndarray) -> float:
        h, w = probability_map.shape
        xs = box[:, 0]
        ys = box[:, 1]
        xmin = int(np.clip(np.floor(xs.min()), 0, w - 1))
        xmax = int(np.clip(np.ceil(xs.max()), 0, w - 1))
        ymin = int(np.clip(np.floor(ys.min()), 0, h - 1))
        ymax = int(np.clip(np.ceil(ys.max()), 0, h - 1))

        mask = np.zeros((ymax - ymin + 1, xmax - xmin + 1), dtype=np.uint8)
        shifted = box.copy()
        shifted[:, 0] -= xmin
        shifted[:, 1] -= ymin
        cv2.fillPoly(mask, [shifted.astype(np.int32)], 1)
        roi = probability_map[ymin : ymax + 1, xmin : xmax + 1]
        return float(cv2.mean(roi, mask=mask)[0])

    @staticmethod
    def _unclip(box: np.ndarray, unclip_ratio: float = 2.0) -> np.ndarray:
        poly = Polygon(box)
        if poly.area <= 0 or poly.length <= 0:
            return box.reshape(-1, 1, 2).astype(np.float32)
        distance = poly.area * unclip_ratio / (poly.length + 1e-6)
        offset = pyclipper.PyclipperOffset()
        offset.AddPath(box.tolist(), pyclipper.JT_ROUND, pyclipper.ET_CLOSEDPOLYGON)
        expanded = offset.Execute(distance)
        if not expanded:
            return box.reshape(-1, 1, 2).astype(np.float32)
        return np.array(expanded[0], dtype=np.float32).reshape(-1, 1, 2)

    def _boxes_from_bitmap(
        self, probability_map: np.ndarray, bitmap: np.ndarray, dest_width: int, dest_height: int
    ) -> np.ndarray:
        contours, _ = cv2.findContours((bitmap * 255).astype(np.uint8), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
        max_candidates = min(len(contours), 1000)
        boxes: list[np.ndarray] = []
        h, w = bitmap.shape

        for index in range(max_candidates):
            contour = contours[index]
            box, short_side = self._min_box(contour)
            if short_side < 3:
                continue

            score = self._score_fast(probability_map, box.reshape(-1, 2))
            if score < 0.5:
                continue

            expanded = self._unclip(box)
            box, short_side = self._min_box(expanded)
            if short_side < 5:
                continue

            box[:, 0] = np.clip(np.round(box[:, 0] / w * dest_width), 0, dest_width - 1)
            box[:, 1] = np.clip(np.round(box[:, 1] / h * dest_height), 0, dest_height - 1)
            ordered = self._order_clockwise(box)
            width = int(np.linalg.norm(ordered[0] - ordered[1]))
            height = int(np.linalg.norm(ordered[0] - ordered[3]))
            if width <= 3 or height <= 3:
                continue
            boxes.append(ordered.astype(np.int32))

        return np.array(boxes, dtype=np.int32) if boxes else np.zeros((0, 4, 2), dtype=np.int32)

    def _det_infer(self, image: np.ndarray) -> np.ndarray:
        if self.det_session is None:
            raise RuntimeError("PPOCR detector session is not initialized")
        input_name = self.det_session.get_inputs()[0].name
        output_name = self.det_session.get_outputs()[0].name
        det_input = self._det_preprocess(image)
        pred = self.det_session.run([output_name], {input_name: det_input})[0]

        if pred.ndim != 4:
            return np.zeros((0, 4, 2), dtype=np.int32)
        probability_map = pred[0, 0]
        bitmap = (probability_map > 0.3).astype(np.uint8)
        bitmap = cv2.dilate(bitmap, np.ones((2, 2), dtype=np.uint8), iterations=1)
        return self._boxes_from_bitmap(probability_map, bitmap, image.shape[1], image.shape[0])

    @staticmethod
    def _crop_quad(image: np.ndarray, quad: np.ndarray) -> np.ndarray:
        points = quad.astype(np.float32)
        width = int(max(np.linalg.norm(points[0] - points[1]), np.linalg.norm(points[2] - points[3])))
        height = int(max(np.linalg.norm(points[0] - points[3]), np.linalg.norm(points[1] - points[2])))
        width = max(1, width)
        height = max(1, height)
        destination = np.array([[0, 0], [width, 0], [width, height], [0, height]], dtype=np.float32)
        matrix = cv2.getPerspectiveTransform(points, destination)
        crop = cv2.warpPerspective(image, matrix, (width, height))
        if height / float(width) >= 1.5:
            crop = np.rot90(crop)
        return crop

    @staticmethod
    def _rec_resize_norm(image: np.ndarray, image_shape: tuple[int, int, int], max_ratio: Optional[float]) -> np.ndarray:
        channels, target_h, target_w = image_shape
        h, w = image.shape[:2]
        ratio = w / float(max(1, h))
        max_ratio = max_ratio if max_ratio is not None else target_w / float(target_h)
        resized_w = min(target_w, int(np.ceil(target_h * ratio)))
        resized_w = max(1, resized_w)
        resized = cv2.resize(image, (resized_w, target_h), interpolation=cv2.INTER_LINEAR)
        array = resized.astype(np.float32) / 255.0
        array = np.transpose(array, (2, 0, 1))
        array = (array - 0.5) / 0.5
        out = np.zeros((channels, target_h, target_w), dtype=np.float32)
        out[:, :, :resized_w] = array
        return out

    @staticmethod
    def _decode_ctc(logits: np.ndarray, vocab: List[str]) -> tuple[List[str], List[float]]:
        if logits.ndim == 2:
            logits = logits[None, ...]
        if np.max(logits) > 1.0 or np.min(logits) < 0.0:
            exp = np.exp(logits - np.max(logits, axis=-1, keepdims=True))
            probs = exp / np.sum(exp, axis=-1, keepdims=True)
        else:
            probs = logits

        class_count = probs.shape[-1]
        if class_count == len(vocab) + 2:
            decode_vocab = [""] + vocab + [" "]
        elif class_count == len(vocab) + 1:
            decode_vocab = [""] + vocab
        elif class_count == len(vocab):
            pad = np.zeros((*probs.shape[:-1], 1), dtype=probs.dtype)
            probs = np.concatenate([pad, probs], axis=-1)
            decode_vocab = [""] + vocab
        else:
            keep = max(0, class_count - 1)
            decode_vocab = [""] + vocab[:keep]

        texts: list[str] = []
        confidences: list[float] = []
        for batch_index in range(probs.shape[0]):
            sequence = probs[batch_index]
            max_indices = sequence.argmax(axis=-1)
            last = -1
            chars: list[str] = []
            scores: list[float] = []
            for time_step, current in enumerate(max_indices):
                token = int(current)
                if token == 0 or token == last:
                    last = token
                    continue
                probability = float(sequence[time_step, token])
                if token < len(decode_vocab):
                    character = decode_vocab[token]
                    if character:
                        chars.append(character)
                        scores.append(probability)
                last = token
            texts.append("".join(chars))
            confidences.append(float(np.mean(scores)) if scores else 0.0)
        return texts, confidences

    def _rec_infer(self, crops: List[np.ndarray], rec_key: str) -> List[str]:
        if not crops:
            return []
        rec_session, vocab = self._ensure_recognizer(rec_key)
        input_name = rec_session.get_inputs()[0].name
        output_name = rec_session.get_outputs()[0].name
        image_shape = (3, 48, 320)
        ratios = [crop.shape[1] / float(max(1, crop.shape[0])) for crop in crops]
        order = np.argsort(ratios)
        texts = [""] * len(crops)
        batch_size = max(1, int(os.environ.get("PPOCR_REC_BATCH_SIZE", "4")))

        for offset in range(0, len(crops), batch_size):
            batch_indices = order[offset : offset + batch_size]
            max_ratio = max(ratios[int(idx)] for idx in batch_indices) if len(batch_indices) > 0 else None
            batch = [
                self._rec_resize_norm(crops[int(idx)], image_shape=image_shape, max_ratio=max_ratio)[None, ...]
                for idx in batch_indices
            ]
            batch_array = np.concatenate(batch, axis=0).astype(np.float32)
            logits = rec_session.run([output_name], {input_name: batch_array})[0]
            if logits.ndim == 3 and logits.shape[1] > logits.shape[2]:
                logits = np.transpose(logits, (0, 2, 1))
            decoded, _ = self._decode_ctc(logits, vocab)
            for local_index, text in zip(batch_indices, decoded):
                texts[int(local_index)] = text
        return texts

    @staticmethod
    def _sort_line_items(items: List[tuple[tuple[int, int, int, int], str]], source_lang_code: str) -> List[str]:
        code = (source_lang_code or "").strip().lower()
        if code.startswith(("zh", "ja")):
            ordered = sorted(items, key=lambda item: ((item[0][1] + item[0][3]) / 2.0, -((item[0][0] + item[0][2]) / 2.0)))
        else:
            ordered = sorted(items, key=lambda item: ((item[0][1] + item[0][3]) / 2.0, (item[0][0] + item[0][2]) / 2.0))
        return [text for _, text in ordered if text and text.strip()]

    def recognize(self, image: np.ndarray, source_lang_code: str) -> str:
        self.initialize()
        rec_key = self._map_rec_key(source_lang_code)
        polygons = self._det_infer(image)
        if polygons.size == 0:
            return ""

        crops = [self._crop_quad(image, polygon.astype(np.float32)) for polygon in polygons]
        texts = self._rec_infer(crops, rec_key)

        spans: list[tuple[tuple[int, int, int, int], str]] = []
        for polygon, text in zip(polygons, texts):
            if not text or not text.strip():
                continue
            xs = polygon[:, 0]
            ys = polygon[:, 1]
            bbox = (int(xs.min()), int(ys.min()), int(xs.max()), int(ys.max()))
            spans.append((bbox, text.strip()))

        ordered_texts = self._sort_line_items(spans, source_lang_code)
        if not ordered_texts:
            return ""
        if self._is_no_space_language(source_lang_code):
            return "".join(ordered_texts)
        return " ".join(ordered_texts)


class OgkaluHeadlessOcr:
    def __init__(self, model_root: Path, device: str = "cpu") -> None:
        self.model_root = model_root
        self.device = device
        self.detector = RTDetrV2Detector(model_root=model_root, device=device)
        self.manga_ocr = MangaOcrOnnx(model_root=model_root, device=device)
        self.ppocr = PPOcrV5Onnx(model_root=model_root, device=device)
        self.ready = False
        self.last_error: Optional[str] = None

    def ensure_ready(self) -> None:
        if self.ready:
            return
        try:
            self.detector.initialize()
            self.ready = True
            self.last_error = None
            logger.info("OCR pipeline initialized successfully (detector preloaded)")
        except Exception as exc:
            self.ready = False
            self.last_error = str(exc)
            raise

    @staticmethod
    def _load_rgb_image(image_bytes: bytes) -> np.ndarray:
        image = Image.open(BytesIO(image_bytes)).convert("RGB")
        max_side = max(512, int(os.environ.get("OCR_MAX_IMAGE_SIDE", "2048")))
        width, height = image.size
        largest_side = max(width, height)
        if largest_side > max_side:
            scale = max_side / float(largest_side)
            resized_width = max(1, int(round(width * scale)))
            resized_height = max(1, int(round(height * scale)))
            image = image.resize((resized_width, resized_height), resample=Image.Resampling.BILINEAR)
        return np.asarray(image, dtype=np.uint8)

    @staticmethod
    def _normalize_boxes(boxes: np.ndarray, image_shape: tuple[int, int, int]) -> np.ndarray:
        if boxes.size == 0:
            return np.empty((0, 4), dtype=np.int32)

        h, w = image_shape[:2]
        out: list[list[int]] = []
        for box in boxes:
            x1, y1, x2, y2 = [int(v) for v in box]
            x1 = max(0, min(x1, w))
            y1 = max(0, min(y1, h))
            x2 = max(0, min(x2, w))
            y2 = max(0, min(y2, h))
            if x2 - x1 > 5 and y2 - y1 > 5:
                out.append([x1, y1, x2, y2])
        return np.array(out, dtype=np.int32) if out else np.empty((0, 4), dtype=np.int32)

    @staticmethod
    def _expand_box(box: np.ndarray, image_shape: tuple[int, int, int], ratio: float = 0.05) -> np.ndarray:
        h, w = image_shape[:2]
        x1, y1, x2, y2 = [int(v) for v in box]
        bw = x2 - x1
        bh = y2 - y1
        dx = int(round(bw * ratio))
        dy = int(round(bh * ratio))
        return np.array(
            [
                max(0, x1 - dx),
                max(0, y1 - dy),
                min(w, x2 + dx),
                min(h, y2 + dy),
            ],
            dtype=np.int32,
        )

    def _match_regions(self, text_boxes: np.ndarray, bubble_boxes: np.ndarray, image_shape: tuple[int, int, int]) -> list[np.ndarray]:
        regions: list[np.ndarray] = []
        for text_box in text_boxes:
            selected = self._expand_box(text_box, image_shape)
            text_area = max(1, (selected[2] - selected[0]) * (selected[3] - selected[1]))
            for bubble_box in bubble_boxes:
                bubble_area = max(1, (bubble_box[2] - bubble_box[0]) * (bubble_box[3] - bubble_box[1]))
                if _contains(bubble_box, text_box) and bubble_area <= text_area * 8:
                    selected = bubble_box
                    break
                if _calculate_iou(bubble_box, text_box) >= 0.2 and bubble_area <= text_area * 6:
                    selected = bubble_box
                    break
            regions.append(selected.astype(np.int32))
        return regions

    @staticmethod
    def _sort_regions(regions: list[np.ndarray], source_lang: str) -> list[np.ndarray]:
        if source_lang.lower().startswith(("ja", "zh")):
            return sorted(regions, key=lambda box: (int(box[1]), -int(box[0])))
        return sorted(regions, key=lambda box: (int(box[1]), int(box[0])))

    @staticmethod
    def _prefer_ppocr(source_lang: str) -> bool:
        return not source_lang.strip().lower().startswith("ja")

    @staticmethod
    def _cross_engine_fallback_enabled() -> bool:
        value = os.environ.get("OCR_CROSS_ENGINE_FALLBACK", "false").strip().lower()
        return value in {"1", "true", "yes", "on"}

    @staticmethod
    def _clean_text(text: str) -> str:
        normalized = (text or "").strip()
        normalized = re.sub(r"[ \t]+", " ", normalized)
        return normalized

    def extract_page_text(self, image_bytes: bytes, source_lang: str) -> str:
        if not self.ready:
            self.ensure_ready()

        image = self._load_rgb_image(image_bytes)
        bubble_boxes, text_boxes = self.detector.detect(image)
        bubble_boxes = self._normalize_boxes(bubble_boxes, image.shape)
        text_boxes = self._normalize_boxes(text_boxes, image.shape)

        regions = self._match_regions(text_boxes, bubble_boxes, image.shape)
        if not regions:
            full = np.array([0, 0, image.shape[1], image.shape[0]], dtype=np.int32)
            regions = [full]

        ordered_regions = self._sort_regions(regions, source_lang)

        extracted_lines: list[str] = []
        for region in ordered_regions[:200]:
            x1, y1, x2, y2 = [int(v) for v in region]
            if x2 <= x1 or y2 <= y1:
                continue
            crop = image[y1:y2, x1:x2]
            if self._prefer_ppocr(source_lang):
                text = self._clean_text(self.ppocr.recognize(crop, source_lang))
            else:
                self.manga_ocr.initialize()
                text = self._clean_text(self.manga_ocr.recognize(crop))
            if text:
                extracted_lines.append(text)

        if not extracted_lines:
            if self._prefer_ppocr(source_lang):
                fallback_text = self._clean_text(self.ppocr.recognize(image, source_lang))
                if not fallback_text and self._cross_engine_fallback_enabled():
                    self.manga_ocr.initialize()
                    fallback_text = self._clean_text(self.manga_ocr.recognize(image))
            else:
                self.manga_ocr.initialize()
                fallback_text = self._clean_text(self.manga_ocr.recognize(image))
            if fallback_text:
                extracted_lines.append(fallback_text)

        return "\n".join(extracted_lines)
