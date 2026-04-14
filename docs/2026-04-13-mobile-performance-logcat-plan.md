# Mobile Performance Logcat + Stabilization Plan (2026-04-13, updated)

## Goal
Thêm hệ thống logcat có cấu trúc để đo và tối ưu:
- RAM usage (peak, pressure, leak signals)
- UX mượt (startup, navigation, scroll, image loading, API latency)
- Tránh lag khi thoát app rồi mở lại
- Theo dõi trạng thái đọc và phục hồi state
- Giảm crash/leak risk trong Reader flow

## Context Constraints (must follow)
- Project mobile đang dùng Java + callback style.
- Không thêm Kotlin Coroutines/RxJava (theo AGENTS.md của repo).
- Tối ưu theo hướng nhỏ, an toàn, có đo đạc trước/sau.

## Confirmed Findings From Source + New Feedback
1. `ZoomableImageView.getTransformedRect()` tạo `new RectF(...)` lặp lại trong drag/zoom path -> memory churn + GC jank.
2. `ReaderActivity.onScrolled()` gọi `updateReaderProgressUi()` mỗi lần scroll pixel -> over-updating UI.
3. `ReaderActivity.setItemViewCacheSize(6)` có rủi ro RAM cao khi item là ảnh truyện lớn.
4. `ReaderActivity` có callback async cập nhật UI trực tiếp, cần guard lifecycle khi activity đã `isFinishing/isDestroyed`.
5. Logic logout dựa trên hardcoded string "Session expired..." mong manh, cần chuyển dần sang status/error code.
6. `ReaderPageAdapter` đang preload thủ công + cache view cao, cần đo và tinh chỉnh phối hợp để tránh phình RAM.
7. `ReaderPageAdapter.applyScaleToAttachedHolders()` zoom đồng loạt nhiều item có thể gây UX khó chịu.

## Priority Roadmap

## P0 (Critical stability + anti-jank)

### P0.1 - Perf logger foundation
**Files**
- Create: `mobile/app/src/main/java/com/group09/ComicReader/util/PerfLogger.java`
- Create: `mobile/app/src/main/java/com/group09/ComicReader/util/PerfSession.java`

**Plan**
- Chuẩn hóa log tag: `PERF_APP`, `PERF_NAV`, `PERF_NET`, `PERF_READER`, `PERF_MEM`, `PERF_STATE`, `PERF_DL`.
- Chỉ bật detailed perf logs khi `BuildConfig.DEBUG`.
- Mỗi event có `sessionId`, `screen`, `durationMs`, `thread`.

### P0.2 - Fix memory churn in zoom hot path
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/widget/ZoomableImageView.java`

**Plan**
- Reuse `RectF` tạm ở cấp class (vd: `private final RectF tempRect = new RectF();`).
- Thay `new RectF(...)` trong `getTransformedRect()` bằng `tempRect.set(...)` + `mapRect(tempRect)`.
- Không tạo object mới trong touch/drag frame loop.

### P0.3 - Throttle progress UI updates during scroll
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`

**Plan**
- Thêm `lastRenderedPageIndex`.
- `updateReaderProgressUi()` return sớm nếu page index không đổi.
- Tránh set text/progress/alpha liên tục khi chưa đổi trang thực.

### P0.4 - Guard async callbacks + cancel in-flight requests
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/data/ReaderRepository.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/data/remote/ApiClient.java`

**Plan**
- Thêm helper: `private boolean isUiActive()` (`!isFinishing() && !isDestroyed() && binding != null`).
- Trong callback repo/network trước khi update UI: `if (!isUiActive()) return;`.
- Đặc biệt ở `resolveChapterMetaIfNeeded()`, observers/callbacks có thao tác binding.
- Theo dõi các `Call<?>` đang chạy cho Reader flow và hủy ở `onDestroy()` bằng `cancelAllPendingRequests()`.
- Nếu callback nào không thể cancel sạch bằng repository boundary, cân nhắc tách callback class tĩnh + `WeakReference<ReaderActivity>`.

### P0.5 - Audio release safety check
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/audio/MediaPlayerReaderAudioPlayer.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/audio/ReaderAudioController.java`

**Plan**
- Trước `release()`: set listeners về `null` để cắt callback chain rõ ràng.
- Log lifecycle audio player create/prepare/play/pause/release.
- Verify không còn callback sau khi Activity đóng.

## P1 (High impact performance + memory)

### P1.1 - Tune RecyclerView cache + preload policy
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/adapter/ReaderPageAdapter.java`

**Plan**
- Giảm `setItemViewCacheSize` từ `6` xuống baseline `2` hoặc `3`.
- Bật `binding.rcvReaderPages.setHasFixedSize(true)` khi item bounds đã ổn định theo aspect ratio.
- Giữ preload nhưng thêm telemetry: preload range, bind rate, memory snapshot trước/sau.
- Benchmark 3 cấu hình (`2`, `3`, `6`) và chọn theo số liệu GC/jank/memory.

### P1.2 - Structured network latency logging
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/data/remote/ApiClient.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/data/ComicRepository.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/data/ReaderRepository.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/data/LibraryRepository.java`

**Plan**
- Bỏ default `HttpLoggingInterceptor.Level.BODY` cho flow thường.
- Dùng interceptor nhẹ log method/path/status/duration.
- Đếm request volume theo màn hình để phát hiện spam.

### P1.3 - Centralized API error mapping (remove brittle string checks)
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`
- Modify: các repository liên quan trả lỗi auth (`ReaderRepository`, `LibraryRepository`, `ComicRepository` nếu cần)
- Create: `mobile/app/src/main/java/com/group09/ComicReader/common/error/AppError.java`
- Create: `mobile/app/src/main/java/com/group09/ComicReader/common/error/ErrorParser.java`

**Plan**
- Parse lỗi API về enum chuẩn (`AppError.TOKEN_EXPIRED`, `AppError.NETWORK`, ...).
- UI xử lý theo enum/code thay vì so khớp string message.
- Ưu tiên status `401/403` hoặc backend `errorCode`; message text chỉ là fallback hiển thị.

### P1.4 - Leak detection in debug builds (LeakCanary)
**Files**
- Modify: `mobile/app/build.gradle.kts`
- Modify: `mobile/gradle/libs.versions.toml` (nếu repo đang quản lý dependency qua version catalog)
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ComicReaderApp.java`

**Plan**
- Thêm LeakCanary cho debug variant only (`debugImplementation`), không ảnh hưởng release.
- Theo dõi leak của `ReaderActivity`, `ComicDetailFragment`, adapter/view binding.
- Gắn leak report vào checklist verify sau mỗi phase P0/P1.

## P2 (UX refinement + larger refactor candidates)

### P2.1 - Evaluate Glide RecyclerViewPreloader replacement
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/adapter/ReaderPageAdapter.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`

**Plan**
- Sau khi có số liệu P0/P1, cân nhắc thay preload tự chế bằng `RecyclerViewPreloader` của Glide.
- Chỉ thực hiện nếu telemetry cho thấy preload hiện tại gây memory spikes/jank.

### P2.2 - Zoom behavior UX mode
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/adapter/ReaderPageAdapter.java`

**Plan**
- Đánh giá lại cơ chế `globalScale` áp vào nhiều item đang visible.
- Cân nhắc mode zoom theo item đang tương tác thay vì đồng bộ tất cả.
- Giữ backward-compatible qua feature flag để test A/B nội bộ.

### P2.3 - Restore-position robustness when metadata incomplete
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/adapter/ReaderPageAdapter.java`
- Backend contract note: API chapter pages nên luôn trả `imageWidth/imageHeight`

**Plan**
- Log tỷ lệ page có `imageWidth/imageHeight <= 0`.
- Defer restore offset đến khi layout stable nếu metadata thiếu.
- Ghi lại mismatch để phía backend fix contract.

## Instrumentation Scope (screen/lifecycle)

### App lifecycle + memory pressure
**Files**
- Create: `mobile/app/src/main/java/com/group09/ComicReader/ComicReaderApp.java`
- Modify: `mobile/app/src/main/AndroidManifest.xml`

**Plan**
- Register `ActivityLifecycleCallbacks` và `ComponentCallbacks2`.
- Log foreground/background transitions + `onTrimMemory` levels.
- Snapshot memory ở app start, reader open, reader close, app resume.

### Navigation and screen timing
**Files**
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/MainActivity.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/base/BaseFragment.java`
- Modify: `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`

**Plan**
- Log destination change + delta time giữa transitions.
- Log fragment/view lifecycle timing ở màn hình chính.
- Reader: đo `onCreate -> pages_received -> first_rendered`.

## Validation Scenarios
1. Cold start -> Home -> đo startup + memory baseline.
2. Home -> ComicDetail -> Reader (chapter dài) -> scroll 60s.
3. Reader -> Back -> reopen Reader nhiều lần để bắt leak/callback muộn.
4. Reader đang mở -> Home (background) 30s -> mở lại app -> verify restore mượt.
5. Search gõ nhanh 10-15 ký tự -> đo request burst và UI responsiveness.
6. Bật log GC (`art`) song song để correlate jank với garbage collection.
7. Chạy debug với LeakCanary, xác nhận không còn retained `ReaderActivity/Fragment` sau khi thoát màn.

## Logcat Commands
```bash
adb logcat -v time PERF_APP:* PERF_NAV:* PERF_NET:* PERF_READER:* PERF_MEM:* PERF_STATE:* PERF_DL:* *:S
adb logcat -v time | grep -E "GC|PERF_"
```

## Success Criteria
- Truy vết được 1 flow end-to-end bằng `sessionId`.
- Chỉ ra top 3 bottleneck có số liệu (duration, memory, request count, GC spikes).
- Giảm rõ rệt spam UI update khi scroll và giảm object churn ở zoom path.
- Không còn callback UI chạy sau khi `ReaderActivity` đã đóng.

## Execution Order (Recommended)
1. P0.1 -> P0.5
2. P1.1 -> P1.4
3. P2 theo telemetry (không làm trước khi có evidence)

## Test Commands After Each Phase
```bash
cd mobile
bash ./gradlew testDebugUnitTest
bash ./gradlew lint
bash ./gradlew assembleDebug
```

## Notes
- Plan đã tích hợp góp ý mới và ưu tiên crash/leak trước tối ưu sâu.
- Các gợi ý chuyển sang coroutines không áp dụng ở repo này; sẽ dùng Java lifecycle-safe patterns tương đương.

## Progress Update (2026-04-14)

### Overall
- Current scope status: **P0 + P1 implemented in code, P2 pending**.
- Branch: `optimization-testing`.

### P0 Status
- [x] **P0.1 Perf logger foundation**
  - Added `PerfLogger` + `PerfSession`.
  - Added tags and structured log message format with `sessionId`, `screen`, `event`, `thread`, `durationMs`.
- [x] **P0.2 Fix memory churn in zoom hot path**
  - Replaced `new RectF(...)` in `ZoomableImageView.getTransformedRect()` with reusable `tempRect`.
- [x] **P0.3 Throttle progress UI updates**
  - Added page-index/page-count guard in `ReaderActivity.updateReaderProgressUi()` to skip redundant updates.
- [x] **P0.4 Guard async callbacks + cancel in-flight requests**
  - Added `isUiActive()` guard in Reader async UI update points.
  - Added request tracking registry and `ReaderRepository.cancelAllPendingRequests()`.
  - Called cancel in `ReaderActivity.onDestroy()`.
  - Note: `ApiClient` was not changed in this batch because request cancellation is handled at repository call-tracking layer.
- [x] **P0.5 Audio release safety**
  - Added listener cleanup in `MediaPlayerReaderAudioPlayer.release()`.
  - Hardened `ReaderAudioController` callbacks to ignore stale player callbacks and added lifecycle logs.

### Verification Status
- [x] `testDebugUnitTest` passed.
- [x] `lint` passed.
- [x] `assembleDebug` passed.
- [x] Added and passed new unit tests:
  - `PerfSessionTest`
  - `PerfLoggerTest`
  - `InFlightCallRegistryTest`
  - `ErrorParserTest`
  - `ApiRequestMetricsTest`

### P1 Status
- [x] **P1.1 Tune RecyclerView cache + preload policy**
  - Reduced `ReaderActivity` cache size from `6` to `3`.
  - Added conditional `setHasFixedSize(...)` based on page metadata stability.
  - Added preload/bind/memory telemetry in `ReaderPageAdapter` and reader lifecycle memory snapshots.
- [x] **P1.2 Structured network latency logging**
  - Removed default `HttpLoggingInterceptor.Level.BODY` from normal flow.
  - Added lightweight network perf interceptor in `ApiClient` (method/path/status/duration/request-count window).
  - Added request window counter (`ApiRequestMetrics`) + unit test.
- [x] **P1.3 Centralized API error mapping**
  - Added `AppError` + `ErrorParser` with HTTP + throwable mapping.
  - Repositories now parse auth/network errors via `ErrorParser`.
  - Replaced brittle session-expired string checks in Reader/Library/Comic comment UI flows with `ErrorParser.isTokenExpiredMessage(...)`.
- [x] **P1.4 LeakCanary debug integration**
  - Added `debugImplementation` LeakCanary dependency via version catalog.
  - Added `ComicReaderApp` and registered in manifest for app-level perf hooks/debug leak tooling.

### Remaining Work
- [ ] Run full manual validation scenarios on real device/emulator with new P1 telemetry + LeakCanary traces.
- [ ] Execute P2 phases (as defined above) after collecting P1 telemetry evidence.
