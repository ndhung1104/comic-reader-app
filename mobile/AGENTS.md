# AI AGENT CODING GUIDELINES: ANDROID (JAVA)

**Role Definition:** You are an expert Android Java Developer. When generating, modifying, or refactoring code for this project, you MUST strictly adhere to the following architectural, stylistic, and structural rules.

## 1. ARCHITECTURE & UI FRAMEWORK
* **Architecture Pattern:** Single Activity Architecture.
* **Implementation:** The app consists of exactly ONE `MainActivity.java`. All other screens (Home, Reader, Profile, etc.) MUST be implemented as `Fragment` classes.
* **Navigation:** Use Jetpack Navigation Component (`nav_graph.xml`) for routing between Fragments.
* **UI Framework:** **CRITICAL:** Use traditional XML Layouts. DO NOT use Jetpack Compose under any circumstances. This is a pure Java project, and Compose is not supported.
* **View Binding:** Bắt buộc (Mandatory). Use `ViewBinding` for all UI interactions. DO NOT generate `findViewById` code.

## 2. LIBRARIES & TECH STACK
When adding implementations, assume and utilize the following dependencies:
* **UI Components:** Material Design 3 (`com.google.android.material:material`). Always use `MaterialButton`, `TextInputEditText`, `MaterialCardView`, etc., instead of legacy Android widgets.
* **Image Loading:** Glide.
* **Networking:** Retrofit2 + Gson Converter.
* **Local Database:** Room Database.
* **Asynchronous Operations:** Use standard Java `ExecutorService` or simple Callbacks. DO NOT generate RxJava or Kotlin Coroutines code.

## 3. NAMING CONVENTIONS
* **XML View IDs:** `[view_type]_[screen_name]_[action_or_purpose]`
  * *Example:* `btn_login_submit`, `tv_home_title`, `img_reader_page`, `rcv_comic_list`.
* **Resource Files:**
  * Layouts: `fragment_[feature_name].xml` or `item_[entity_name].xml`.
  * Drawables: `ic_[name].xml` (icons), `bg_[name].xml` (backgrounds). Strictly lowercase with underscores.
* **Java Classes:**
  * ViewModels: `[ScreenName]ViewModel` (e.g., `HomeViewModel`).
  * Repositories: `[Entity]Repository` (e.g., `ComicRepository`).

## 4. CODING STYLE & DATA FLOW (MVVM)
* **ViewBinding Lifecycle:** In Fragments, ViewBinding MUST be initialized in `onCreateView` and the binding instance MUST be set to `null` in `onDestroyView` to prevent memory leaks.
* **BaseFragment:** All newly generated Fragments MUST extend `BaseFragment` (assume it handles loading states, keyboard hiding, and toasts).
* **Strict MVVM Flow:** `Fragment` -> `ViewModel` -> `Repository`.
  * **CRITICAL:** Fragments MUST NOT contain business logic, database queries, or direct API calls.
  * Fragments only observe ViewModel data and handle UI click events.

## 5. LAYOUT & UI RULES
* **Root Layout:** Favor `ConstraintLayout`. Avoid deep nesting of `LinearLayout` or `RelativeLayout`.
* **The "0dp" Rule:** Inside a `ConstraintLayout`, DO NOT use `match_parent`. Use `android:layout_width="0dp"` (match_constraint) and set the appropriate start/end constraints.
* **Proportional Sizing:** Avoid hardcoding large margins/positions. Use `Guideline` with `layout_constraintGuide_percent` to ensure responsive layouts across different screen sizes.

## 6. RESOURCES & ASSETS
* **Measurement Units:** * Sizes, margins, padding: MUST use `dp`.
  * Text sizes: MUST use `sp` (for accessibility).
* **No Hardcoding:** DO NOT hardcode dimensions directly in XML (e.g., `android:layout_margin="16dp"` is invalid). Always reference `@dimen/...` (e.g., `android:layout_margin="@dimen/spacing_medium"`).
* **Images:** Use Vector Drawables (`.xml`) for single-color shapes, logos, and icons.

## 7. MULTI-SCREEN & TABLET SUPPORT
* **Resource Qualifiers:** DO NOT generate Java `if-else` blocks to check screen sizes. Rely on Android's resource folder mechanism (e.g., refer to values in `values/dimens.xml` vs `values-sw600dp/dimens.xml`).
* **Fragment-First:** Keep all UI logic isolated within Fragments so the `MainActivity` acts purely as a flexible container.

## 8. GIT WORKFLOW (For Agent Context)
If generating bash scripts, PR descriptions, or commit messages:
* **Branching format:** `feature/[developer_name]_[feature_description]`
* **Target:** Features merge into `develop`, not `main`.