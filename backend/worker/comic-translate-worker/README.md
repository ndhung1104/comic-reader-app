# comic-translate-worker (headless OCR)

This worker provides the v1 API contract for backend integration:

- `POST /jobs`
- `GET /jobs/{id}`
- `GET /jobs/{id}/artifacts`

Current behavior uses a real OCR flow adapted from `ogkalu2/comic-translate`:
- RT-DETR v2 ONNX for text/bubble detection
- OCR engine routing:
  - `ja` -> Manga OCR ONNX
  - non-`ja` -> PPOCRv5 ONNX (language-specific rec model)
- Per-page OCR output with the existing backend contract

The worker still keeps in-memory job state in v1, but OCR is no longer stub text.
On first boot it downloads OCR models, so `/health` can stay `503` for a few minutes until warmup completes.

## Environment variables

- `OCR_MODEL_DIR` (default: `/var/lib/comic-translate/models`)
- `OCR_DEVICE` (default: `cpu`)
- `WORKER_IMAGE_FETCH_TIMEOUT_SECONDS` (default: `30`)
- `WORKER_MAX_PAGES_PER_JOB` (default: `200`)
- `OCR_MODEL_CONNECT_TIMEOUT_SECONDS` (default: `20`)
- `OCR_MODEL_DOWNLOAD_TIMEOUT_SECONDS` (default: `120`)
- `OCR_MODEL_DOWNLOAD_MAX_RETRIES` (default: `6`)
- `OCR_MODEL_DOWNLOAD_BACKOFF_SECONDS` (default: `2`)
- `HF_ENDPOINT` (optional mirror for HuggingFace downloads)

## Run locally

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8090
```
