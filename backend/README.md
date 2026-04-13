# Comic Reader Backend - Sprint 1 Foundation

## Tech Stack
- Spring Boot 3.3.x
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Flyway migration
- Docker Compose (backend + postgres)

## Default accounts
- Admin:
  - email: `admin@comicreader.dev`
  - password: `admin123`
- User:
  - email: `user@comicreader.dev`
  - password: `user123`

## Run with Docker
1. Copy env file:
```bash
cp .env.example .env
```
2. Start services:
```bash
docker compose up --build
```
3. If Docker starts failing on a Flyway checksum mismatch after pulling schema changes, your
   existing `pg_data` volume is likely carrying an older migration history. For disposable local
   data, reset it with:
```bash
docker compose down -v --remove-orphans
docker compose up --build
```

### Low-RAM profile (8GB all-in-one)
Use the dedicated compose file:
```bash
docker compose -f docker-compose.low-ram.yml up --build
```

Optional local LLM sidecar (`ollama`) can be enabled with profile:
```bash
docker compose -f docker-compose.low-ram.yml --profile local-llm up --build
```

### Basic compose profiles (docker-compose.yml)
Use runtime flags/env vars instead of editing YAML each time.

- Core only (postgres + backend):
```bash
docker compose -f docker-compose.yml up --build
```

- OCR worker:
```bash
TRANSLATION_WORKER_ENABLED=true docker compose -f docker-compose.yml --profile ocr up --build
```

- TTS worker:
```bash
TTS_WORKER_ENABLED=true docker compose -f docker-compose.yml --profile tts up --build
```

- OCR + TTS:
```bash
TRANSLATION_WORKER_ENABLED=true TTS_WORKER_ENABLED=true docker compose -f docker-compose.yml --profile ocr --profile tts up --build
```

- Local LLM sidecar:
```bash
docker compose -f docker-compose.yml --profile local-llm up --build
```

#### What Local LLM is used for
- `local-llm` starts `ollama` for local translation provider flow.
- Use it when backend translate provider is set to local (or fallback to local).
- It is not required for OCR/TTS pipelines unless your translation config points to local model.

### OCR job persistence flow (current)
- `POST /api/v1/translation-jobs` creates `translation_jobs` row immediately.
- Worker returns incremental `ocrPages` while job is `RUNNING`.
- Backend scheduler syncs worker status every `TRANSLATION_WORKER_SYNC_INTERVAL_MS` (default `3000` ms) and upserts each OCR page into `chapter_page_ocr_texts`.
- When `tts-worker.enabled=true`, each newly persisted OCR page also triggers TTS generation for that page immediately (no need to wait full chapter OCR done).
- Audio playlist generation returns `503` while OCR pages are still incomplete for the chapter; retry later.

## Run local (without Docker)
1. Copy env file:
```bash
cp .env.example .env
```
2. Start PostgreSQL and create DB `comic_reader`
3. Adjust `.env` if your local DB settings differ.
4. Run:
```bash
./mvnw spring-boot:run
```

## API quick check
- Health: `GET /api/v1/health`
- Register: `POST /api/v1/auth/register`
- Login: `POST /api/v1/auth/login`
- Comics: `GET /api/v1/comics` (Bearer token)
- Admin create comic: `POST /api/v1/admin/comics` (ADMIN token)
- Admin upload pages: `POST /api/v1/admin/chapters/{chapterId}/pages/upload` (multipart `files[]`)
- Translation jobs submit: `POST /api/v1/translation-jobs`
- Translation jobs status: `GET /api/v1/translation-jobs/{jobId}`
- Translation jobs artifacts: `GET /api/v1/translation-jobs/{jobId}/artifacts`

## Upload storage
- URL path: `/uploads/**`
- Docker runtime folder: `/app/uploads`
- Compose volume: `uploads_data`

## Environment files
- Backend reads `backend/.env` via Spring `spring.config.import`
- Docker Compose also reads `backend/.env`
- `DB_URL` in `.env` is intended for local runs; Docker overrides it to use the `postgres` service host

- `docker compose up` reuses the named `pg_data` volume, so schema history survives rebuilds until you remove that volume

- `TRANSLATION_WORKER_BASE_URL` should point to the worker service (`http://comic-translate-worker:8090` in Docker)

