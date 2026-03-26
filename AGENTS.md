# AGENTS.md

Repository-wide guide for coding agents working in `Comic-reader`.

## Scope
- This repository has two main modules: `backend/` (Spring Boot) and `mobile/` (Android Java).
- Follow module-specific conventions when editing code.
- There are no Cursor rules in `.cursor/rules/` or `.cursorrules`.
- There are no Copilot rules in `.github/copilot-instructions.md`.
- `mobile/AGENTS.md` exists and contains extra Android-specific guidance; treat it as required when changing `mobile/`.

## Repo Layout
- `backend/`: Spring Boot 3.3 app, Maven wrapper, PostgreSQL/Flyway, JWT auth, H2-backed tests.
- `mobile/`: Android app, Gradle wrapper, Java 11, XML layouts, ViewBinding, Navigation Component.
- `api_test.py`: standalone API script; do not treat it as the primary test harness.

## General Agent Rules
- Prefer small, targeted edits; do not reformat unrelated files.
- Never commit `.env`, `local.properties`, secrets, tokens, or generated IDE files.
- Keep package and folder structure aligned with the existing domain split.
- Match existing naming before introducing new abstractions.
- When both backend and mobile are involved, update backend contracts first, then mobile models/repositories.

## Build And Test Commands

## Backend Commands (`backend/`)
- Run app locally: `bash ./mvnw spring-boot:run`
- Run full test suite: `bash ./mvnw test`
- Run a single test class: `bash ./mvnw -Dtest=AuthControllerTest test`
- Run a single test method: `bash ./mvnw -Dtest=AuthControllerTest#registerAndLoginShouldReturnJwtToken test`
- Build jar without tests: `bash ./mvnw -DskipTests package`
- Clean build: `bash ./mvnw clean verify`
- Show dependency or Maven environment info: `bash ./mvnw -version`

## Mobile Commands (`mobile/`)
- Build debug APK: `bash ./gradlew assembleDebug`
- Run unit tests: `bash ./gradlew testDebugUnitTest`
- Run one unit test class: `bash ./gradlew testDebugUnitTest --tests "com.group09.ComicReader.ExampleUnitTest"`
- Run one unit test method: `bash ./gradlew testDebugUnitTest --tests "com.group09.ComicReader.ExampleUnitTest.addition_isCorrect"`
- Run instrumentation tests on a connected device/emulator: `bash ./gradlew connectedDebugAndroidTest`
- Run one instrumentation test class: `bash ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.group09.ComicReader.ExampleInstrumentedTest`
- Run one instrumentation test method: `bash ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.group09.ComicReader.ExampleInstrumentedTest#useAppContext`
- Run Android lint: `bash ./gradlew lint`
- Build and check common tasks together: `bash ./gradlew assembleDebug testDebugUnitTest lint`

## Command Notes
- Run wrappers through `bash` if execute bits are missing.
- Backend uses Java 21 via Maven.
- Mobile is configured for Java 11 source/target, AGP 8.13.2, compile/target SDK 36.
- Mobile reads `mobile/.env` for `MOBILE_BASE_URL`; default is `http://10.0.2.2:8080/`.
- Backend reads `backend/.env`; copy from `backend/.env.example` for local work.
- If Android Gradle fails early with an SDK/JDK mismatch, verify the local Android SDK setup in `mobile/local.properties` and installed build tools.

## Backend Architecture
- Package layout is domain-first: `auth`, `comic`, `chapter`, `comment`, `wallet`, `common`, `config`.
- Typical flow is `controller -> service -> repository -> entity`.
- DTOs live beside their domain packages and are used at API boundaries.
- Repositories are Spring Data interfaces; business rules belong in services, not controllers.
- Global API error shaping goes through `common/exception/GlobalExceptionHandler`.

## Backend Style Guidelines
- Use constructor injection only; do not add field injection.
- Keep controllers thin: validate input, delegate to services, return `ResponseEntity`.
- Put business logic, authorization checks, and cross-entity rules in services.
- Throw domain exceptions like `NotFoundException` or `BadRequestException` instead of returning ad hoc error objects.
- Prefer `@Valid` request DTOs for input validation.
- Keep transaction boundaries on service methods with `@Transactional`.
- Reuse existing response DTOs instead of exposing JPA entities directly.
- Continue using Lombok where the package already uses it, especially entities.
- Use descriptive method names such as `getComic`, `createComic`, `findActiveByUserId`.
- Keep method visibility tight; make helpers `private` unless they are part of a service contract.

## Backend Imports, Formatting, And Types
- Keep imports explicit; avoid wildcard imports in production code.
- Existing code generally groups imports by project, third-party/framework, then Java; be consistent within the file.
- Use 4-space indentation and braces on the same line.
- Prefer `Long` for persisted IDs and `Integer` only when nullability matters in request/response models.
- Prefer collection interfaces (`List`, `Set`, `Map`) for fields and return types.
- Use `final` for dependencies and effectively immutable locals when it improves clarity.
- Use text blocks in tests for JSON payloads when they make requests easier to read.

## Backend Naming Conventions
- Classes: `PascalCase`.
- Methods and fields: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Request/response models end with `Request` or `Response`.
- JPA classes end with `Entity`.
- Spring Data interfaces end with `Repository`.
- Feature tests usually end with `Test` and sit under `backend/src/test/java/com/group09/ComicReader`.

## Backend Error Handling
- Preserve the existing JSON error shape from `GlobalExceptionHandler`: timestamp, status, error, and optional field map.
- Prefer meaningful messages that can be shown to the mobile app.
- Do not swallow exceptions silently.
- When validation fails, add field-level constraints instead of manual controller checks when possible.
- For not-found or invalid state paths, throw specific exceptions early.

## Backend Testing Guidance
- Existing tests use `@SpringBootTest`, `@AutoConfigureMockMvc`, and `@ActiveProfiles("test")`.
- Prefer `MockMvc` for controller integration tests.
- Use AssertJ and `jsonPath` for assertions.
- Generate unique emails or IDs in auth tests to avoid collisions.
- Keep tests deterministic and independent of local PostgreSQL by relying on the test profile.

## Mobile Architecture
- The app is mostly fragment-driven under `MainActivity`, using Navigation Component and Safe Args.
- Important exception: `ui/reader/ReaderActivity.java` exists and is a real second activity; do not assume strict single-activity architecture.
- Fragments typically extend `base/BaseFragment`.
- ViewBinding is enabled and should be used instead of `findViewById`.
- Current data flow is usually `Fragment -> ViewModel -> Repository -> Retrofit API`.
- Remote access lives under `data/remote`, local session storage under `data/local`.

## Mobile Style Guidelines
- Use Java, not Kotlin or Compose.
- Use XML layouts with Material components and `ConstraintLayout` as the default root where practical.
- Initialize fragment bindings in `onCreateView` and clear them in `onDestroyView`.
- Keep UI logic in fragments/activities; put data loading and state in `ViewModel` classes.
- Repositories should own Retrofit calls and callback translation.
- Follow existing callback-based async style; do not introduce coroutines or RxJava.
- Use `new ViewModelProvider(...)` and existing `Factory` classes when dependencies are required.
- Prefer guard clauses for null/empty cases.
- Keep user-visible strings in `strings.xml` when adding new UI copy.

## Mobile Imports, Formatting, And Types
- Keep imports explicit; avoid wildcard imports except in legacy generated/example tests.
- Existing files usually order imports as Android/AndroidX, third-party, app packages, then Java.
- Use 4-space indentation.
- Keep lines readable; wrap fluent chains and long method signatures across lines.
- Use `@NonNull` and `@Nullable` annotations where Android lifecycle nullability matters.
- Prefer immutable fields (`private final`) for adapters, repositories, and constants.
- Use boxed types only when null is meaningful; otherwise use primitives.

## Mobile Naming Conventions
- Packages are lower-case and feature-oriented: `ui.home`, `ui.login`, `data.remote`, `viewmodel`, `model`, `adapter`.
- Classes use suffixes like `Fragment`, `Activity`, `ViewModel`, `Repository`, `Adapter`, `Response`, `Request`.
- Resource files use snake_case: `fragment_home.xml`, `item_home_daily.xml`, `ic_arrow_back_24.xml`.
- View IDs follow the existing prefix pattern: `btn_`, `tv_`, `img_`, `rcv_`, `edt_`, `prg_`, `bnv_`, `fcv_`, `card_`.
- String resources use feature-prefixed keys such as `login_*`, `home_*`, `reader_*`, `comment_*`.

## Mobile Layout And Resource Rules
- Prefer `0dp` with constraints inside `ConstraintLayout` instead of `match_parent` for constrained children.
- Reuse `@dimen/*`, `@color/*`, and `@string/*`; avoid new hardcoded literals in XML.
- Continue using the existing dimension tokens like `spacing_lg`, `text_sm`, `radius_lg`.
- Prefer vector drawables for simple icons/shapes.
- Preserve content descriptions for important images and actions.

## Mobile Error Handling
- Repositories pass errors back through callbacks as user-readable strings.
- ViewModels expose error state via `LiveData`; fragments usually show it with `showToast(...)` or dedicated text views.
- Handle null API bodies and unsuccessful responses explicitly.
- For auth/session problems, follow the existing `SessionManager` flow and clear stale sessions when appropriate.
- Do not ignore asynchronous failure paths when adding new API calls.

## Mobile Testing Guidance
- Current test coverage is minimal; prefer adding focused unit tests for pure logic and backend-facing transformations.
- Use `app/src/test/java` for local JVM tests and `app/src/androidTest/java` for instrumentation tests.
- Keep new tests package-aligned with the classes under test.
- If a feature is tightly coupled to Android UI widgets, prefer instrumentation tests or extract logic into testable helpers.

## Environment And Safety
- Never overwrite real secrets in `backend/.env` or `mobile/.env`.
- Do not check in `mobile/local.properties`.
- Backend local runs expect PostgreSQL unless the test profile is active.
- Mobile emulator traffic is expected to hit the backend through `10.0.2.2`.

## Practical Defaults For Agents
- For backend changes, run at least the narrowest relevant Maven test.
- For mobile code changes, run at least `testDebugUnitTest`; add `lint` when XML/resources/UI code changes.
- If changing API contracts, update both backend DTOs/controllers and mobile models/repositories in the same task.
- Mention any unverified Android commands if local SDK issues block execution.
