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

## Upload storage
- URL path: `/uploads/**`
- Docker runtime folder: `/app/uploads`
- Compose volume: `uploads_data`

## Environment files
- Backend reads `backend/.env` via Spring `spring.config.import`
- Docker Compose also reads `backend/.env`
- `DB_URL` in `.env` is intended for local runs; Docker overrides it to use the `postgres` service host
