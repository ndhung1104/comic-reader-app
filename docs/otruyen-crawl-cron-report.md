# OTruyen Crawl & Weekly Sync — Design, Operation, and Troubleshooting

Date: 2026-04-20

This document describes the new OTruyen crawling mechanism implemented in the backend, the weekly cron job that runs it, how transactions and visibility are handled, how mobile pagination interacts with the backend, and operational runbook & troubleshooting steps.

## Summary

- Purpose: import comics from the external OTruyen API into our local `comics` and `chapters` tables, keep new comics synced weekly, and provide deterministic pagination to the mobile app.
- Primary features:
    - Full crawl and per-page import support (`syncComicsFromOTruyen`, `syncComicsFromOTruyenPage`).
    - Incremental weekly-only-new sync (`syncOnlyNewComics`) that stops when an already-imported comic is encountered.
    - Asynchronous detailed import of comic metadata and chapters (`OTruyenAsyncService.lazyLoadComicDetails`) to avoid blocking the scheduler.
    - Admin endpoint to trigger manual syncs: `POST /api/v1/admin/comics/sync?page={n}`.
    - Scheduler: `OTruyenScheduler.weeklyCrawl` driven by `otruyen.crawl.cron` property (default Sunday 03:00 UTC).

## Components & Responsibilities

- `OTruyenService`
    - `syncComicsFromOTruyen()` — triggers a full crawl (iterates pages until no items).
    - `syncComicsFromOTruyenPage(int page)` — imports a single page and returns `true` when the page contains items.
    - `syncOnlyNewComics()` — calls OTruyen "truyen-moi" endpoint and stops at first existing `slug`. For each new item it calls `saveBasicComic(...)` and triggers async detail import.
    - `saveBasicComic(...)` — annotated `@Transactional(propagation = Propagation.REQUIRES_NEW)` and calls `saveAndFlush` so the _shell_ is committed immediately and becomes visible to other threads / DB clients.

- `OTruyenAsyncService`
    - `@Async("taskExecutor") @Transactional void lazyLoadComicDetails(Long comicId, String slug)` — fetches the detail endpoint, updates `author`, `synopsis`, `coverUrl` when available, and imports chapters.
    - Runs in the `taskExecutor` configured in `AsyncConfig` (4–8 threads by default).

- `OTruyenScheduler`
    - Annotated with `@Scheduled(cron = "${otruyen.crawl.cron:0 0 3 * * SUN}" , zone = "${otruyen.crawl.zone:UTC}")` and `@Async("taskExecutor")`, so the scheduled method runs asynchronously on the task executor.

- `AdminComicController`
    - `POST /api/v1/admin/comics/sync?page={n}` — public for convenience (per `SecurityConfig`) and useful for manual tests.

## Data model & transaction visibility

- Tables: `comics` and `chapters` (see `ComicEntity` and `ChapterEntity`). `author` column in `comics` is NOT NULL — the crawler must set a non-null default when creating shells.
- To allow the async loader to see newly created shells immediately, `saveBasicComic(...)` uses `REQUIRES_NEW` and `saveAndFlush()` so the row is committed in its own transaction.
- Async detail import runs in separate transactions and inserts chapters; those will become visible when the async transaction commits.

## Why you might not see rows in pgAdmin immediately

- If an import runs inside a long uncommitted transaction, pgAdmin will not show rows until commit. The incremental flow uses `saveBasicComic` with `REQUIRES_NEW`, so those shells should appear immediately.
- If the job found no new comics it may have ended without inserting anything (look for `Found NEW comic` / `Saved comic` log lines).
- If the job threw an exception (e.g., NOT NULL violation for `author`) the insert could fail — check logs for stacktraces. (Note: a fix was applied to set default `author = "Unknown"` in `saveBasicComic`).

## How to run manually (runbook)

- Trigger a single-page import (safe test):

```bash
curl -v -X POST "http://localhost:8080/api/v1/admin/comics/sync?page=1"
```

- Trigger a full crawl manually (may be slow):

```bash
curl -v -X POST "http://localhost:8080/api/v1/admin/comics/sync"
```

- Temporarily run scheduler every minute for testing by setting the env before start (example — do not keep in production):

```bash
export OTRUYEN_CRAWL_CRON='0 * * * * *'   # every minute (second 0)
./mvnw spring-boot:run
```

## Useful log strings to monitor

- `Starting scheduled OTruyen weekly crawl`
- `Weekly Sync: Checking page` — shows page number being processed
- `Found NEW comic:` — new comic discovered
- `Saved comic (page ...)` — indicates save succeeded
- `Async: lazy loading details for comic` — async detail worker started
- `Lazy loaded X chapters for comic` — chapters imported
- `Failed lazy load`, `Failed to fetch detail`, or stack traces — check these for errors

Filter logs example:

```bash
./mvnw spring-boot:run |& grep -i "OTruyen\|Weekly Sync\|Found NEW\|Saved comic\|Async: lazy\|Failed lazy"
```

## SQL queries for quick inspection (pgAdmin / psql)

- Latest comics:

```sql
SELECT id, title, slug, cover_url, created_at, updated_at
FROM comics
ORDER BY created_at DESC
LIMIT 50;
```

- Chapter counts per comic:

```sql
SELECT c.id, c.title, c.slug, COUNT(ch.id) AS chapter_count
FROM comics c
LEFT JOIN chapters ch ON ch.comic_id = c.id
GROUP BY c.id
ORDER BY c.created_at DESC
LIMIT 50;
```

- Active DB sessions (long transactions, blocking):

```sql
SELECT pid, usename, application_name, client_addr, state, query_start, query
FROM pg_stat_activity
WHERE datname = 'comic_reader'
ORDER BY query_start DESC;
```

## Common errors & immediate fixes

- SQL NOT NULL violation (e.g., `author` is null): ensure `saveBasicComic` sets a default `author` before saving. This repository now sets `author = "Unknown"` for shells.
- RestTemplate network errors / remote API changes: look for stacktraces in logs and `Failed to fetch detail`. Add retries or backoff if transient errors occur.
- Scheduler fires but finds no new items: confirm OTruyen `truyen-moi` endpoint returns items and that `slug` checks map to your DB correctly.

## Production recommendations

- Add an overlap guard (prevent concurrent runs) — options:
    - Use ShedLock (DB-based locking) so only one instance runs the job at a time.
    - Use a simple DB `jobs` table and `SELECT FOR UPDATE` locking to coordinate runs.
- Make external calls resilient:
    - Use Spring Retry with exponential backoff for RestTemplate calls.
    - Respect remote API rate limits and add configurable delay between pages.
- Instrument metrics & alerts:
    - Add metrics counters (Prometheus) for `pagesProcessed`, `newComics`, `failedDetails`.
    - Alert when `failedDetails` or `restTemplate` errors exceed thresholds.
- Consider moving blocking chapter import to a separate long-running queue (e.g., Spring TaskExecutor backed by a persistent queue) for better control and retries.

## Option: wait for async detail imports before reporting job finished

If you want `syncOnlyNewComics()` to block until all async detail imports finish, change the async method to return a `CompletableFuture<Void>` and collect futures in `syncOnlyNewComics()`:

```java
// in OTruyenAsyncService
@Async("taskExecutor")
public CompletableFuture<Void> lazyLoadComicDetails(Long comicId, String slug) {
    // existing work
    return CompletableFuture.completedFuture(null);
}

// in OTruyenService.syncOnlyNewComics()
List<CompletableFuture<Void>> futures = new ArrayList<>();
futures.add(oTruyenAsyncService.lazyLoadComicDetails(savedComic.getId(), item.getSlug()));
// after scheduling all tasks
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

## Configuration (important properties)

- `application.yml` (defaults):

````yaml
otruyen:
    crawl:
        cron: ${OTRUYEN_CRAWL_CRON:0 0 3 * * SUN}
        zone: ${OTRUYEN_CRAWL_ZONE:UTC}
        enabled: ${OTRUYEN_CRAWL_ENABLED:true}

### Startup automatic sync

- New behavior: when the application starts and the `comics` table is empty, the application will automatically trigger an OTruyen sync in the background so the app can continue serving requests during the crawl. The startup invocation uses the full `syncComicsFromOTruyen()` flow and runs on the configured `taskExecutor`.
- Controlled by property `otruyen.crawl.startup-enabled` (default `true`). To disable this automatic startup behaviour set:
```yaml
otruyen:
    crawl:
        startup-enabled: false
````

- Implementation details: The `DatabaseInitializer` listens for `ApplicationReadyEvent`, checks `comicRepository.count()`, and when zero submits a background task to run `oTruyenService.syncComicsFromOTruyen()` on the `taskExecutor` so the crawl does not block normal traffic.
  Implication: the application will accept API traffic immediately while the initial crawl runs in the background. This provides faster availability at the cost of clients seeing partial results until the crawl completes.

```

- Local DB config is loaded from `backend/.env` (DB_URL/DB_USERNAME/DB_PASSWORD).

## Files touched by this change

- `backend/src/main/java/com/group09/ComicReader/comic/service/OTruyenService.java` — crawl logic, incremental-only-new method, `saveBasicComic`.
- `backend/src/main/java/com/group09/ComicReader/comic/service/OTruyenAsyncService.java` — async detail & chapter import.
- `backend/src/main/java/com/group09/ComicReader/comic/scheduler/OTruyenScheduler.java` — scheduled job wiring.
- `backend/src/main/java/com/group09/ComicReader/config/AsyncConfig.java` — task executor.
- `backend/src/main/resources/application.yml` — new `otruyen.crawl` properties.
- `mobile/` — pagination UI and data changes to consume paged API (separate mobile docs).

## Next steps / follow-ups

1. Run a manual `page=1` import and watch logs to validate inserts and async work.
2. Add basic unit/integration tests for `syncOnlyNewComics()` (mock RestTemplate responses).
3. Add an overlap guard before enabling high-frequency schedules in production.
4. Add metrics and alerts for production observability.

---

If you want, I can: (A) add the blocking `CompletableFuture` change so the scheduler waits for all async imports, (B) implement a simple ShedLock-based overlap guard, or (C) add Prometheus metrics and example alerts. Which should I do next?
```
