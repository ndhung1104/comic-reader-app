# comic-translate-worker (stub)

This worker provides the v1 API contract for backend integration:

- `POST /jobs`
- `GET /jobs/{id}`
- `GET /jobs/{id}/artifacts`

Current behavior is an in-memory stub that returns `SUCCEEDED` immediately and emits OCR text per page.

## Why a stub in v1

The backend contract and OCR persistence flow can be integrated and validated now while the full `ogkalu2/comic-translate` pipeline is being adapted to headless server mode.

## Next step to use ogkalu2/comic-translate

1. Replace in-memory processing in `app/main.py` with pipeline invocation.
2. Keep response schema unchanged so backend does not need any contract update.
3. Preserve `idempotencyKey` handling in submit path.

## Run locally

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8090
```
