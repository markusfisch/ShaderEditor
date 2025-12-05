# CODEX – ShaderEditor

This document captures how the app is structured today so you can change it confidently. Keep it close when you explore the code base.

---

## 1. What the app does
- ShaderEditor is an Android GLSL playground/live wallpaper. Users edit fragment shaders, preview them live, import textures/uniforms, and set any shader as a wallpaper (`README.md`).
- The whole product lives in a single Android application module under `app/`. Source is Java (language level 17) with Gradle Kotlin build scripts.
- The UI is a single-activity, multi-fragment setup with custom widgets for the editor, shader preview, uniform pickers, and texture tooling.
- Sensors, camera, microphone, battery status, wallpaper offsets, notification counts, etc., are exposed to shaders as uniforms via `ShaderRenderer`.

---

## 2. Build, run, and tooling
- Requirements: Android Studio Ladybug+, Android SDK 21–36, Java 17, and the Android NDK is not required. Gradle wrapper + Kotlin DSL (`build.gradle.kts`, `app/build.gradle.kts:1`).
- Build variants: `debug` and `release`; release is minified/resource-shrunk and expects signing env vars (`ANDROID_KEY_*`).
- View binding + `BuildConfig` generation are enabled. Compose is not used.
- Helpful commands:
  - `./gradlew assembleDebug` (or `make debug`) – build APK.
  - `./gradlew assembleRelease` and `./gradlew bundleRelease` – ship builds.
  - `./gradlew lintDebug`, `make lint`, `make infer`, `make avocado` – lint/static analysis/drawable checks.
  - `make install`, `make start`, `make uninstall` – adb helpers.
- Version catalog (`gradle/libs.versions.toml:1`) centralizes dependency versions (AppCompat 1.7, Material 1.12, Preference 1.2, CameraX 1.4, AGP 8.12).
- Fastlane metadata lives in `fastlane/` for Play/F-Droid; SVG assets are under `svg/`.

---

## 3. Repository layout (high signal paths)

| Path | Notes |
| --- | --- |
| `app/src/main/AndroidManifest.xml:1` | Permissions (camera, mic, legacy storage), wallpaper + notification services, exported activities. |
| `app/src/main/java/de/markusfisch/android/shadereditor/` | All runtime code. Key packages: `activity`, `fragment`, `widget`, `opengl`, `database`, `hardware`, `io`, `view`, `adapter`. |
| `app/src/main/res/` | Layouts, drawables, fonts, `res/raw/*.glsl` sample shaders, `res/xml/preferences.xml`. |
| `res/xml/backup_rules.xml`, `res/xml/extraction_rules.xml` | Backup configuration referenced by the manifest. |
| `fastlane/metadata/...` | Store descriptions/screenshots. Keep in sync for releases. |
| `CONTRIBUTING.md`, `FAQ.md`, `CHANGELOG.md` | Ground rules, docs referenced in-app. |

---

## 4. Runtime architecture

### 4.1 Application start
- `ShaderEditorApp` (`app/src/main/java/de/markusfisch/android/shadereditor/app/ShaderEditorApp.java:1`) is the `Application`. It initializes the singleton `Preferences`, prewarms `Database`, keeps a process-wide `UndoRedo.EditHistory`, turns on StrictMode in debug builds, and registers `BatteryLevelReceiver` (API ≥ 24) to drive low-power behavior.
- Splash flow: `SplashActivity` immediately launches `MainActivity` so the app initializes before the UI is drawn (`activity/SplashActivity.java:1`).

### 4.2 Main screen plumbing
- `MainActivity` (`activity/MainActivity.java:1`) hosts everything. It instantiates:
  - `EditorFragment` (code editing).
  - `ShaderViewManager` (GLSurfaceView preview + quality spinner).
  - `ShaderManager` (load/save shaders, handle `ACTION_SEND`/`ACTION_VIEW` intents, duplicate/delete, persist thumbnails).
  - `ShaderListManager` (ListView + background loader for saved shaders).
  - `UIManager` (toolbar buttons, drawer, toggling editor visibility, extra keys).
  - `MainMenuManager` (popup menu with editor actions, navigation to Add Uniform, samples, settings).
  - `NavigationManager` (previews, FAQ link, share intent).
  - `ExtraKeysManager` (soft keyboard helpers + completions strip).
- State flow: the editor notifies `ShaderManager` when text changes; `ShaderManager` updates `ShaderViewManager` and `ShaderListManager`; UI state (title/subtitle, show-errors pill) is adjusted via `UIManager`.

### 4.3 Editor stack
- `EditorFragment` (`fragment/EditorFragment.java:1`) wires the custom `ShaderEditor` widget to undo/redo, completion listeners, and preference-driven appearance (font, ligatures, line numbers, update delay).
- `ShaderEditor` (`widget/ShaderEditor.java:1`) extends `LineNumberEditText`. Responsibilities: syntax highlighting (`highlighter/*`), debounce compile requests, auto-insert braces, convert ShaderToy snippets, completion entry, lint error highlighting. Highlights/errors are recalculated on a worker thread (`TokenListUpdater`).
- `UndoRedo` (`view/UndoRedo.java:1`) tracks a shared, process-wide edit history so the editor can preserve undo stacks between fragment recreations (history object is kept in `ShaderEditorApp.editHistory`).
- `ExtraKeysManager` (`activity/managers/ExtraKeysManager.java:1`) shows tab/brace buttons and completion chips, auto-hiding when the IME is closed if preferences demand.
- Compile errors from the GL renderer are visualized by `ErrorListModal` and `ErrorAdapter` (`widget/ErrorListModal.java:1`, `adapter/ErrorAdapter.java:1`).

### 4.4 Shader preview + rendering
- `ShaderView` (`widget/ShaderView.java:1`) wraps a `GLSurfaceView`, sets the GLES client version, installs a custom EGL context factory (prefers GLES3 but falls back to GLES2), forwards touch events to the renderer, and scrubs non-ASCII characters before sending shader source to OpenGL.
- `ShaderRenderer` (`opengl/ShaderRenderer.java:1`) is the core engine:
  - Parses shader code for `sampler2D`, `samplerCube`, and `samplerExternalOES` uniforms, manages backbuffer FBOs, thumbnails, and resolution scaling via the "quality" multiplier.
  - Exposes dozens of uniforms (time, resolution, multi-touch, sensors, battery, day/night, notification counts/time, camera frames, mic amplitude, wallpaper offsets, etc.). Sensor listeners live in `hardware/*` (e.g., `AccelerometerListener`, `CameraListener`, `MicInputListener`, etc.) and are registered only when the shader actually references the relevant uniform.
  - Taps Android services: `NotificationService` for notification uniforms, `BatteryLevelReceiver` for power state, media volume via `AudioManager`, wallpaper offsets, etc.
  - Uses `TextureBinder`, `TextureParameters`, and `BackBufferParameters` to manage imported textures/backbuffer configuration.
  - Publishes FPS + compile info logs back to UI threads via `ShaderRenderer.OnRendererListener`.
- `ShaderViewManager` (`activity/managers/ShaderViewManager.java:1`) coordinates `ShaderView` lifecycle with the activity, exposes the quality spinner, and pushes renderer events to `MainActivity`.
- `PreviewActivity` (`activity/PreviewActivity.java:1`) runs shaders fullscreen when the user prefers manual runs. Its static `RenderStatus` object transports FPS/errors/thumbnail data back to `MainActivity`.

### 4.5 Wallpaper & services
- `ShaderWallpaperService` (`service/ShaderWallpaperService.java:1`) hosts a `GLSurfaceView` inside a live wallpaper engine. It listens for preference changes (`Preferences.WALLPAPER_SHADER`, `SAVE_BATTERY`), enforces low-power render modes when the receiver marks the battery low, and reuses `ShaderView`/`ShaderRenderer`.
- `NotificationService` (`service/NotificationService.java:1`) is a notification listener. Its static getters feed `ShaderRenderer` uniforms `notificationCount` and `lastNotificationTime`. It prompts the user to enable the listener when needed.
- `BatteryLevelReceiver` (`receiver/BatteryLevelReceiver.java:1`) toggles low-power mode and power-connected flags and updates wallpaper render modes.

---

## 5. Persistence & data layer
- `Database` (`database/Database.java:1`) is a singleton façade around `SQLiteOpenHelper` with schema version 5. It builds tables via `DataSource.buildSchema()` and exposes DAOs.
- `DataSource` (`database/DataSource.java:1`) bundles the DAOs and is retrieved everywhere via `Database.getInstance(context).getDataSource()`. Never cache raw `SQLiteDatabase` objects outside DAO scope.
- `ShaderDao` (`database/dao/ShaderDao.java:1`):
  - CRUD for shaders, thumbnails (PNG blobs), and quality multiplier.
  - `insertShaderFromResource` seeds new shaders from `res/raw/*.glsl`.
  - Column definitions live in `DatabaseContract`.
  - Names are optional; UI falls back to the last-modified timestamp (`DataRecords.ShaderInfo.getTitle()`).
- `TextureDao` (`database/dao/TextureDao.java:1`):
  - Stores user-imported 2D textures and cube maps (ratio differentiates them).
  - Saves both thumbnails (PNG) and full-resolution PNG matrices. Dimensions feed sampler defaults.
- `DataRecords` (`database/DataRecords.java:1`) holds immutable records used throughout UI/DAOs.
- Import/export:
  - `ImportExportAsFiles` (`io/ImportExportAsFiles.java:1`) reads/writes `.glsl` files under `Downloads/ShaderEditor`. Uses legacy storage APIs on Android < Q; guarded by runtime permissions from `PreferencesFragment`.
  - `DatabaseImporter` / `DatabaseExporter` support SAF-based SQLite exports/imports.
  - Texture import flows go through `BitmapEditor` and `TextureDao`.

---

## 6. Uniforms, textures, and samples
- Adding uniforms:
  - `AddUniformActivity` (`activity/AddUniformActivity.java:1`) hosts `UniformPagesFragment` (ViewPager with presets, 2D textures, cube maps). The activity registers ActivityResult launchers for picking/cropping images and returns the selected GLSL statement in the result bundle.
  - `UniformPresetPageFragment` (`fragment/UniformPresetPageFragment.java:1`) shows the built-in uniform list from `PresetUniformAdapter`. Sampler presets launch `TextureParametersFragment`.
  - `UniformSampler2dPageFragment` and `UniformSamplerCubePageFragment` (`fragment/UniformSampler2dPageFragment.java:1`, `fragment/UniformSamplerCubePageFragment.java:1`) list user textures via `TextureAdapter`, async-loaded through `TextureDao`.
  - `TextureViewFragment` / `TextureViewActivity` let users preview a texture, delete it, or insert the uniform statement for it.
  - `TextureParametersFragment` (`fragment/TextureParametersFragment.java:1`) formats sampler parameters (wrap, filter, backbuffer options) before returning the statement to `AddUniformActivity`.
- Texture creation:
  - `CropImageActivity` + `CropImageFragment` (`activity/CropImageActivity.java:1`, `fragment/CropImageFragment.java:1`) handle picking and cropping an image prior to import.
  - `Sampler2dPropertiesFragment`, `CubeMapActivity`, `CubeMapFragment`, and `TextureParametersView`/`BackBufferParametersView` (under `widget/`) finalize sampler metadata.
- Samples:
  - `LoadSampleActivity` with `LoadSampleFragment` (`activity/LoadSampleActivity.java:1`, `fragment/LoadSampleFragment.java:1`) surfaces sample shaders from `res/raw/sample_*.glsl` via `SamplesAdapter` (`adapter/SamplesAdapter.java:1`). `ShaderManager.loadSampleLauncher` inserts the sample into the DB and selects it.

---

## 7. Preferences and behavior flags
- Preference keys/constants live in `Preferences` (`preference/Preferences.java:1`). Important toggles:
  - Run mode (auto vs manual vs manual-with-preview activity).
  - Update delay/debounce for shader recompiles.
  - Editor styling (font, tab width, ligatures, extra keys, line numbers).
  - Auto-save, save-on-run, default shader template ID, default wallpaper shader, "show extra keys".
  - Sensor delay, hide native IME suggestions.
  - Import/export directory preferences (only used for labeling).
- `PreferencesFragment` (`fragment/PreferencesFragment.java:1`) renders `res/xml/preferences.xml`, requests runtime permissions for file I/O, and wires preference click listeners for import/export, database backup, etc. It also updates preference summaries (shader pickers via `ShaderListPreference`/`ShaderListPreferenceDialogFragment`) and triggers `BatteryLevelReceiver` when save-battery changes.
- Many runtime checks reference `ShaderEditorApp.preferences`. Always read/update through this singleton to keep UI/resume logic consistent.

---

## 8. Sensors, camera, audio, and permissions
- Sensors (accelerometer, gyroscope, gravity, rotation vector, magnetic field, light, linear acceleration, pressure, proximity) share a base `AbstractListener` (`hardware/AbstractListener.java:1`). `ShaderRenderer` registers/unregisters them based on uniforms in the shader source. Sensor delays honor the preference set in settings.
- Camera textures use `CameraX` (`hardware/CameraListener.java:1`) with `ProcessCameraProvider` and `SurfaceTexture`. Both front/back cameras are exposed, along with orientation matrices and addent vectors (`UNIFORM_CAMERA_ADDENT`, `UNIFORM_CAMERA_ORIENTATION`). The wallpaper engine acts as the `LifecycleOwner` when rendering live wallpapers.
- Microphone input (`hardware/MicInputListener.java:1`) samples amplitudes via `AudioRecord`. RECORD_AUDIO permission must already be granted (no custom request flow here).
- Notifications require the Notification Listener permission; `NotificationService.requirePermissions()` prompts with a dialog.
- Media volume uniforms query `AudioManager` each frame.
- Battery events rely on `BatteryLevelReceiver`, coupled with the `save_battery` preference to switch `GLSurfaceView` render modes.

---

## 9. Services, receivers, and background components
- `ShaderWallpaperService` – see §4.5.
- `NotificationService` – see §4.5.
- `BatteryLevelReceiver` – see §4.5.
- `ShaderWallpaperService` toggles the manifest-enabled state of `BatteryLevelReceiver` based on lifecycle to avoid wakeups when not running.
- `ShaderWallpaperService.ShaderWallpaperView` extends `ShaderView` but overrides `getHolder()` to render into the wallpaper surface.

---

## 10. Import/export & file handling
- File import/export only works on API < 29 because scoped storage removed direct SD access; for newer versions the preference entries are hidden (`PreferencesFragment.wireImportExport()`).
- Database import uses the Storage Access Framework with MIME types `application/x-sqlite3`, `application/vnd.sqlite3`, and `application/octet-stream`.
- Texture imports rely on SAF `ACTION_GET_CONTENT` flows and optionally cropping/resampling to avoid OOM (see `BitmapEditor.getBitmapFromUri()`).
- When exporting shaders, tabs can be converted to spaces based on `export_tabs` preference before sharing (handled inside `NavigationManager.shareShader()`).

---

## 11. Coding conventions & dependencies
- Follow `CONTRIBUTING.md`: tabs for indentation, EditorConfig is authoritative, align with Android Studio defaults otherwise. Keep commits focused (one feature per commit) and prefer squash merges.
- Most helpers live under `activity/managers/`; resist the urge to introduce new global singletons—prefer scoped manager classes or fragments.
- Shared state flows through the `ShaderManager`/`ShaderListManager`/`ShaderViewManager` triad. If you add new shader metadata, update all of them plus the DAO and UI surfaces.
- Rendering code runs on GL threads; do not touch UI widgets from inside `ShaderRenderer`. Use `queueEvent()` or the provided listener callbacks to hand results back to the main thread.
- Keep database interactions inside DAOs; they already open/close databases in `try-with-resources`. Long-running reads are dispatched to single-thread executors + main-thread handlers (follow the patterns in `ShaderListManager` and `UniformSampler2dPageFragment`).
- When adding uniforms or sensors, define new constants in `ShaderRenderer`, expose them in `PresetUniformAdapter`, and wire end-to-end (renderer uniform location, value assignment, UI copy).
- Build/res dependencies:
  - Material components for dialogs, tabs, snackbars.
  - `androidx.preference` for settings.
  - `CameraX` for live camera textures.
  - No Jetpack Compose/Navigation; fragments are added manually via helper methods in `AbstractSubsequentActivity`.

---

## 12. Debugging, logging, and testing
- The project doesn’t ship automated unit/instrumentation tests. Rely on manual testing, lint (`./gradlew lintDebug`), and static analyses (`make infer`).
- Debug builds enable StrictMode (thread + VM violations), so fix any disk/network access on the main thread before shipping.
- Compile/GLSL errors:
  - For live preview (`ShaderView`), errors are surfaced through the UI (Snackbar + `ErrorListModal`).
  - For manual runs (`PreviewActivity`), error info is stored in `PreviewActivity.renderStatus.infoLog`.
- Use `adb shell dumpsys meminfo ...` or `make meminfo` to inspect memory usage; `make glxinfo` dumps gfx stats.
- Wallpapers should be tested both with `save_battery` enabled/disabled and while toggling device charging state to verify render mode switching.
- Verify notification uniforms by granting the listener permission via the system dialog triggered in `NotificationService`.

---

## 13. Common change scenarios
- **Add a new sensor/uniform**: extend `ShaderRenderer` (new constant, uniform location lookup, value updates), register/unregister a new listener in `hardware/`, add it to `PresetUniformAdapter`, and document it in `FAQ.md`/UI strings. Consider its availability (API level) and gating preferences.
- **Add shader metadata** (e.g., tags): update `DatabaseContract`, bump DB version, add columns to `ShaderDao` CRUD, persist via `ShaderManager`, show via `ShaderAdapter`/layouts.
- **Add a new sample shader**: drop a `.glsl` file + thumbnail in `res/raw`/`res/drawable`, extend `SamplesAdapter` entry, add translation strings for name/rationale, and bump screenshots if needed.
- **Modify shader preview behavior**: touch `ShaderViewManager`, `ShaderRenderer`, and `MainActivity`. Remember manual-run flows rely on `PreviewActivity`, not the embedded GLSurfaceView.
- **Adjust import/export**: modify `ImportExportAsFiles` and the preference wiring; keep runtime permissions + scoped storage differences in mind.
- **Extend preferences**: add entries to `res/xml/preferences.xml`, define keys/defaults in `Preferences`, update `PreferencesFragment` summaries and any code paths referencing the new setting.
- **Release prep**: update `CHANGELOG.md`, `fastlane/metadata/**/changelogs`, bump `versionCode`/`versionName` in `app/build.gradle.kts`, run `make release` or `make bundle`, and refresh screenshots if UI changed.

---

## 14. Reference docs & assets
- `README.md` – feature overview, download links, support info.
- `FAQ.md` – answers referenced from settings (NavigationManager → FAQ).
- `PRIVACY.md` – sensors/camera/mic usage disclosure (needed for stores).
- `CONTRIBUTING.md` – workflow, formatting rules, EditorConfig hints.
- `fastlane/metadata/**` – store copy/screenshots; keep translations consistent with `res/values-*`.

---

## 15. Tips while editing
- When adding string resources/translations, always add them for all supported languages.
- Prefer `ShaderEditorApp.preferences` for all preference reads/writes to keep state centralized.
- Respect the async loading patterns: DAO operations should not touch the UI thread; use `Executors` + `Handler` the same way existing code does.
- Mind multi-window: `PreviewActivity` is marked `singleInstance` and may run adjacent to `MainActivity` if `RUN_MODE` is set accordingly.
- Keep GLSL sources ASCII-only to avoid compiler crashes (`ShaderView.removeNonAscii()` already enforces this).
- When touching layout/system bar behavior, update `SystemBarMetrics` to maintain consistent inset handling.
- For wallpaper-specific bugs, remember the engine uses `GLSurfaceView.RENDERMODE_WHEN_DIRTY` when on low battery; test both render modes.
- Always recycle/release bitmaps and textures (see `TextureViewFragment` and `BitmapEditor`) to avoid spikes.

With this map you should be able to navigate the project quickly, understand the impact of a change, and know where to plug new functionality in. Refer back whenever you need to chase a flow—from GLSL compilation, to sensor wiring, to persistence, to the UI. Happy hacking!
