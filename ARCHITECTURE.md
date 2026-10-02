# Architecture — Android AppFunctions Commerce POC

> AI AGENT INSTRUCTION

This document is the canonical architectural context for this repository.

Read this file completely before exploring or modifying the project.

The AppFunctions integration in this repository is already working and has been validated.

Do not attempt to redesign, replace, simplify or rediscover the AppFunctions setup unless explicitly requested.

When using this repository as a reference for another Android application, reproduce the proven AppFunctions integration pattern first before adapting it to the target application's architecture.

Inspect only files relevant to the requested task.
Avoid broad repository exploration unless necessary.
Prefer the known-good implementation documented here.

---

## 1. Product concept

This is a generic public proof of concept. It is not a production commerce system. Product records are demo data.

```text
User
  ↓
System AI / Google AppFunctions Testing Agent
  ↓
Android AppFunctions
  ↓
Commerce Agent Android capability layer
  ↓
FastAPI backend
  ↓
LangGraph shopping session
  ↓
SQLite demo catalogue
```

That path supports:

```text
shop for products conversationally
refine preferences across turns
select a product
open the selected product in the Android application
```

The Android application id is `com.example.myapplication`. The launcher label is **Commerce Agent**.

Companion backend repository:

```text
https://github.com/SriramParasuram/agentic-commerce-backend
```

---

## 2. Architectural principle

The AI agent interprets intent. AppFunctions expose typed capabilities. The backend owns shopping state and business truth. Android presents the experience.

```text
Agent interprets.
AppFunctions expose.
Backend remembers.
Commerce validates.
Android presents.
PendingIntent hands off.
```

Android does not keep a shopping-session store. Each AppFunction call is stateless except for the `sessionId` string it sends to the backend.

---

## 3. Android technology stack

Values below are taken from `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and `app/build.gradle.kts`.

| Piece | Value in this repository |
| --- | --- |
| Kotlin | `2.2.10` |
| Kotlin Compose plugin | `org.jetbrains.kotlin.plugin.compose` at the same Kotlin version |
| Android Gradle Plugin | `9.4.1` |
| Gradle wrapper | `9.6.0` (`gradle-9.6.0-bin.zip`) |
| Java source/target | 11 |
| compileSdk | `release(37)` |
| targetSdk | `37` |
| minSdk | `36` |
| applicationId / namespace | `com.example.myapplication` |
| AndroidX AppFunctions runtime | `androidx.appfunctions:appfunctions:1.0.0-alpha12` |
| AndroidX AppFunctions compiler | `androidx.appfunctions:appfunctions-compiler:1.0.0-alpha12` (KSP) |
| KSP | `2.2.10-2.0.2` |
| OkHttp | `4.12.0` |
| JSON | Android platform `org.json` (`JSONObject` / `JSONArray`). No Moshi, Gson, kotlinx.serialization, or Retrofit. |
| Compose BOM | `2026.02.01` |
| Compose UI / UI Graphics / Material3 | versions from that BOM |
| Activity Compose | `1.8.0` |
| KSP argument | `appfunctions:aggregateAppFunctions=true` |

`appfunctions-service` is **not** a dependency. Do not add it. The working alpha12 integration uses `appfunctions` plus `appfunctions-compiler` only.

minSdk 36 is required. The AppFunction base class is annotated `@RequiresApi(36)`. On a working debug build, KSP also emitted the generated concrete service with `@RequiresApi(value = 36, api = 1)`. Generated sources are not committed. The `36` is the Android API level. The `api = 1` argument is the AppFunctions metadata API level emitted by the alpha12 compiler. Do not lower minSdk below 36 to "make it compile for older phones."

---

## 4. AppFunctions integration in this app

```text
AppFunctions service
      ↓
typed request
      ↓
CommerceApiClient
      ↓
FastAPI
      ↓
typed response
```

| Role | Path |
| --- | --- |
| Abstract AppFunction service (methods live here) | `app/src/main/java/com/example/myapplication/appfunctions/BaseCommerceAppFunctionService.kt` |
| Generated concrete service name | `CommerceAppFunctionService` (KSP; declared in the manifest, not hand-written) |
| Manifest registration | `app/src/main/AndroidManifest.xml` |
| App-level metadata | `app/src/main/res/xml/app_metadata.xml` |
| HTTP client | `app/src/main/java/com/example/myapplication/network/CommerceApiClient.kt` |
| Handoff extra keys | `app/src/main/java/com/example/myapplication/HandoffExtras.kt` |
| Activity that receives the handoff | `app/src/main/java/com/example/myapplication/MainActivity.kt` |
| Shop Phones presentation | `app/src/main/java/com/example/myapplication/ui/shop/ShopPhonesScreen.kt` |

`BaseCommerceAppFunctionService` is abstract and annotated:

```text
@AppFunctionServiceEntryPoint(
    serviceName = "CommerceAppFunctionService",
    appFunctionXmlFileName = "commerce_app_function_service",
)
```

KSP generates `CommerceAppFunctionService`, which the manifest registers as an exported service with permission `android.permission.BIND_APP_FUNCTION_SERVICE` and action `android.app.appfunctions.AppFunctionService`.

Manifest properties on that service:

```text
android.app.appfunctions.schema = app_functions_schema.xsd
android.app.appfunctions.v2 = commerce_app_function_service.xml
```

Application property:

```text
android.app.appfunctions.app_metadata = @xml/app_metadata
```

`app_functions_schema.xsd` comes from the AppFunctions library assets. `commerce_app_function_service.xml` is generated by KSP and must be packaged into the APK (see the workaround below). It is not a hand-written source file.

---

## 5. The three AppFunctions

Indexed function identifiers use the **abstract base class**, not the generated service name:

```text
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#shopForDevices
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#selectDevice
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#openSelectedDevice
```

That is what `adb shell cmd app_function list-app-functions` showed for the installed debug APK. Do not "fix" the IDs to `CommerceAppFunctionService#...`.

### shopForDevices

| | |
| --- | --- |
| Purpose | Search or refine devices in the current shopping session. |
| Request | `DeviceShoppingRequest` |
| Response | `DeviceShoppingResult` (`sessionId`, `summary`, `products`) |
| Backend | `POST /shopping/turn` |
| Handoff | None. Does not launch an Activity. |

`DeviceShoppingRequest` fields (`app/src/main/java/com/example/myapplication/appfunctions/DeviceShoppingRequest.kt`):

```text
sessionId
minPrice
maxPrice
operatingSystem
brand
color
storageGb
cameraPriority
batteryPriority
displayPriority
isFoldable
clearPreferences
```

Semantics:

```text
null / omitted preference field
→ CommerceApiClient does not send that field
→ backend keeps the previous LangGraph value

clearPreferences = ["fieldName"]
→ backend stores null for that field (forgets it)

same field present in both the delta and clearPreferences
→ the delta wins (backend: update.update(delta) after applying clears)
```

`sessionId == null` starts a new backend thread (`SHOP-<uuid>`). A later turn must send the returned `sessionId`.

`clearPreferences` names allowed by both the KDoc and `KNOWN_PREFERENCE_FIELDS` in the backend: `minPrice`, `maxPrice`, `operatingSystem`, `brand`, `color`, `storageGb`, `cameraPriority`, `batteryPriority`, `displayPriority`, `isFoldable`. An unknown name returns HTTP 400 `unknown_clearPreferences`.

If the merged state has `minPrice > maxPrice`, the backend returns HTTP 400 `invalid_price_range` and does not write that turn into the checkpoint.

Android does not own this state.

### selectDevice

| | |
| --- | --- |
| Purpose | Commit to one product previously returned by `shopForDevices`. |
| Request | `DeviceSelectionRequest` (`sessionId`, `productId`) |
| Response | `DeviceSelectionResult` (`sessionId`, `status`, `selectedProductId`, `selectedProduct`, `summary`) |
| Backend | `POST /shopping/select` |
| Handoff | None. Selection does not start `MainActivity`. |

Backend checks, in order (`run_select_device`):

1. Session thread exists, else HTTP 404 `session_not_found`.
2. `productId` is in that thread's `lastShownProductIds` (the top products from the last shop turn), else HTTP 400 `product_not_in_last_shown`.
3. The product still exists in the SQLite catalogue, else HTTP 404 `product_not_in_catalogue`.

On success it stores `selectedProductId` on the same LangGraph thread and returns `status = "SELECTED"`.

The agent must resolve "the second one" or a product name using the previous `products` list, then send that `productId`. The backend does not parse natural language.

### openSelectedDevice

| | |
| --- | --- |
| Purpose | Open the Commerce Agent app for the device already selected in the session. |
| Request | `OpenSelectedDeviceRequest` (`sessionId` only) |
| Response | `android.app.PendingIntent` directly. No serializable wrapper. |
| Backend | `GET /shopping/session/{sessionId}` |
| Handoff | `PendingIntent.getActivity` → `MainActivity` |

The agent does not send `productId`. `CommerceApiClient.getSessionHandoff` loads the backend session. The backend requires `selectedProductId` on the thread and a catalogue match. Missing selection is HTTP 400 `no_selected_product`.

The PendingIntent is built in `BaseCommerceAppFunctionService.openSelectedDevice`:

```text
Intent(context, MainActivity::class.java)
flags = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP
extras:
  com.example.myapplication.EXTRA_SESSION_ID
  com.example.myapplication.EXTRA_SELECTED_PRODUCT_ID
PendingIntent.getActivity(
  requestCode = sessionId.hashCode(),
  flags = FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE
)
```

`MainActivity` is `android:launchMode="singleTop"`.

- Cold start: `onCreate` reads the launching intent.
- Warm start: `onNewIntent` replaces the Activity intent and reloads.

Both paths call `loadShopPhones()`. The Activity then:

1. `GET /catalogue/devices`
2. If a session id extra is present, `GET /shopping/session/{sessionId}`
3. Uses the **backend** `selectedProductId` for highlighting and ordering

If the Intent extra and the backend disagree, the UI logs a warning and uses the backend value. The Intent only says which session to resume. It is not product truth.

---

## 6. KSP resource packaging workaround

Why this exists: AGP 9 built-in Kotlin does not automatically package resources/assets that KSP writes under `app/build/generated/ksp/<variant>/resources/assets`. A successful Kotlin compile is not proof that `commerce_app_function_service.xml` is inside the APK. Without that XML, AppFunctions do not register.

What KSP generates for the debug variant (not committed):

```text
app/build/generated/ksp/debug/kotlin/.../CommerceAppFunctionService.kt
app/build/generated/ksp/debug/kotlin/.../$BaseCommerceAppFunctionService_AppFunctionInventory.kt
app/build/generated/ksp/debug/kotlin/.../*Factory.kt
app/build/generated/ksp/debug/resources/assets/commerce_app_function_service.xml
```

The XML is the asset named by `appFunctionXmlFileName`.

What the build script does (`app/build.gradle.kts`):

1. `PackageAppFunctionAssetsTask` copies `*.xml` from the KSP assets directory into `app/build/generated/appFunctionAssets/<variant>/`.
2. Registered task name is `package${Variant}AppFunctionAssets`, so debug is `packageDebugAppFunctionAssets`.
3. That task `dependsOn("ksp${Variant}Kotlin")` (`kspDebugKotlin`).
4. `androidComponents.onVariants` adds the task output as a generated asset source:

```text
variant.sources.assets?.addGeneratedSourceDirectory(
    packageTask,
    PackageAppFunctionAssetsTask::outputDir,
)
```

Do not delete this task, replace `project.copy` patterns that break on this AGP, or assume a newer AGP will package KSP assets unless you have re-verified registration with `list-app-functions` after the change.

The manifest still has to point `android.app.appfunctions.v2` at `commerce_app_function_service.xml`. Packaging the file and declaring the property are both required.

---

## 7. Registration versus a green build

A successful `./gradlew :app:assembleDebug` only means the APK was produced. Registration is a separate check after install:

```bash
adb shell cmd app_function list-app-functions
```

Filter for this package. Expected identifiers:

```text
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#shopForDevices
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#selectDevice
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#openSelectedDevice
```

If those three IDs are absent, the usual cause in this project is the generated XML missing from the APK, not a wrong Kotlin signature.

---

## 8. Direct ADB invocation

Low-level check of registration, argument serialization, and execution:

```bash
FUNC='com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#shopForDevices'
adb shell cmd app_function execute-app-function \
  --package com.example.myapplication \
  --function "$FUNC" \
  --parameters '{"request":{"maxPrice":1000}}'
```

`shopForDevices` and `selectDevice` return JSON that the CLI prints.

`openSelectedDevice` returns a real `PendingIntent`. The CLI JSON serializer does not render that object. A successful call has been observed as `{}` while logcat still shows:

```text
CommerceAppFunctions: HANDOFF
CommerceAppFunctions: sessionId: SHOP-...
CommerceAppFunctions: selectedProductId: ...
```

Do not treat `{}` as proof that the PendingIntent was empty or that the function failed. Confirm with logcat, then confirm UI handoff by executing the PendingIntent (Testing Agent confirmation, below).

---

## 9. Google AppFunctions Testing Agent

Conversational testing uses Google's official project:

```text
https://github.com/android/appfunctions
```

That tree is **not** inside this public repository. During POC development it was cloned separately (locally under a `tools/android-appfunctions` directory on the development machine). Do not search this repo for `run_privileged.sh`.

Three different checks:

```text
ADB execute-app-function
→ plumbing: registration, JSON arguments, JSON results

Testing Agent manual/debug mode
→ invoke one indexed function with explicit parameters

Testing Agent retail/LLM mode
→ system-agent simulation: natural language to tool calls
```

Retail launch used against that separate clone, from its `agent` directory:

```bash
./run_privileged.sh \
  --build \
  --flavor retail \
  --api-key "$GEMINI_API_KEY"
```

The Gemini API key belongs to the Testing Agent process only. It is not a dependency of this app, not in `CommerceApiClient`, and must never be committed here.

### Observed Testing Agent behavior

These are observations from POC runs, not source code in this repository.

**Session continuity.** The agent passed the opaque `sessionId` from one tool result into the next call. Turn 1 `shopForDevices` returned `SHOP-...`. Turn 2 called `shopForDevices` with that same id. Turn 3 called `selectDevice` with that same id. No Android `DataStore`, `SharedPreferences`, or session singleton was required.

**Ordinal selection.** A phrase such as "I'll take the second one." was resolved by the model against the previous `products` list. It called `selectDevice` with that product's `productId`. The backend then checked `lastShownProductIds`.

**PendingIntent confirmation.** The retail Testing Agent did not auto-execute the returned `PendingIntent`. It stored the intent and showed a confirmation:

```text
I can perform this action for you. Should I proceed?
```

The button label in that build was `Yes, go ahead`. Confirming calls `PendingIntent.send()`, which launched `MainActivity` with the extras above. After a delay the button can show as expired (`Done`) because the in-memory intent was consumed or dropped. That is harness behavior. Do not replace `PendingIntent` with a deep link because the confirm step exists.

**Stale tool metadata.** After reinstalls, the Testing Agent sometimes kept an older function schema (missing a new parameter or an old description). Recovery that worked:

```text
start a fresh Agent Demo conversation
restart the Testing Agent process
refresh or re-toggle Connected Apps
reinstall and relaunch the commerce app if the schema on disk is stale
```

Do not redesign the application or the backend because the Testing Agent is temporarily using stale tool metadata.

---

## 10. Android process lifetime

AppFunction calls in this POC ran in the app process when the system bound `CommerceAppFunctionService`. That process id changed across turns while the same `SHOP-...` session continued.

Do not rely on any of the following for conversational continuity:

```text
singleton state
static state
Activity lifetime
Service lifetime
Android process lifetime
```

Correct chain:

```text
AppFunction call
      ↓
sessionId
      ↓
backend session
```

Do not add `SharedPreferences`, DataStore, or a local session singleton to remember shopping preferences or the selected product. The backend thread is the memory.

---

## 11. Backend

Repository: https://github.com/SriramParasuram/agentic-commerce-backend

```text
FastAPI
   ↓
LangGraph StateGraph
   ↓
InMemorySaver
   ↓
SQLite catalogue (seeded demo devices)
```

Pinned versions in that repo's `requirements.txt` include FastAPI `0.141.1`, Uvicorn `0.54.0`, Pydantic `2.13.5`, LangGraph `1.2.12`, langgraph-checkpoint `4.2.0`.

`sessionId` is the LangGraph `thread_id` (`config["configurable"]["thread_id"]`).

Endpoints used by this Android app:

| Method | Path | Caller |
| --- | --- | --- |
| `POST` | `/shopping/turn` | `shopForDevices` |
| `POST` | `/shopping/select` | `selectDevice` |
| `GET` | `/shopping/session/{sessionId}` | `openSelectedDevice` and Shop Phones restore |
| `GET` | `/catalogue/devices` | Shop Phones catalogue load |
| `GET` | `/health` | liveness only |

Conceptual thread state:

```text
preference fields (see shopForDevices)
lastShownProductIds   # top catalogue matches from the latest shop turn
selectedProductId     # set only by selectDevice
```

`InMemorySaver` is process-local. Restarting Uvicorn drops every shopping session. That is intentional for this POC. Do not add a database for session state unless a task explicitly asks for it.

There is no LLM inside the backend. The agent interprets language. The backend receives typed fields.

### Deterministic matching

Hard filters in `app/catalogue.py` (`hard_filter`), all case-insensitive where they are strings:

```text
minPrice          priceUsd >= minPrice
maxPrice          priceUsd <= maxPrice
operatingSystem
brand
color             must be in availableColors
storageGb         must be in availableStorageGb
isFoldable
```

Priority fields are not hard filters. They rank matches:

```text
cameraPriority  → cameraScore
batteryPriority → batteryScore
displayPriority → displayScore
```

Weights:

```text
HIGH = 3
MEDIUM = 2
LOW = 1
```

Score is `sum(deviceScore * weight)` for priorities that are set and recognized. If no priority is set, order is price ascending, then name. The HTTP response returns the top 5 (`TOP_N`). `matchCount` is the full hard-filter count. `lastShownProductIds` is those top product ids, which is what `selectDevice` validates against.

---

## 12. Android / backend networking

`CommerceApiClient.BASE_URL` is hardcoded:

```text
http://10.0.2.2:8000
```

`10.0.2.2` is the standard Android emulator alias for the host machine's loopback. The backend is started on the host at `127.0.0.1:8000`.

Debug-only cleartext is allowed for that host. Release does not attach this config.

| File | What it does |
| --- | --- |
| `app/src/debug/res/xml/network_security_config.xml` | `cleartextTrafficPermitted` for `10.0.2.2`, `localhost`, `127.0.0.1` |
| `app/src/debug/AndroidManifest.xml` | sets `android:networkSecurityConfig` on `<application>` |
| `app/src/main/AndroidManifest.xml` | `android.permission.INTERNET` |

Timeouts in the client: connect 10s, read 20s, write 10s. Failures become `AppFunctionAppUnknownException` so the AppFunctions runtime can surface them.

The current source does **not** call `adb reverse` and does **not** switch `BASE_URL` to `127.0.0.1` for a physical device. A physical device cannot use `10.0.2.2`. Changing the base URL for a device is an adaptation, not something this POC already does.

---

## 13. Shop Phones UI

`MainActivity` always shows `ShopPhonesScreen`. The screen is presentation. It does not decide what is selected.

Normal launch (no session extra):

```text
GET /catalogue/devices
→ render the catalogue
→ no assistant badge
```

Agent handoff:

```text
PendingIntent
→ sessionId extra
→ GET /shopping/session/{sessionId}
→ backend selectedProductId
→ GET /catalogue/devices
→ pin that product to the top of the list
→ badge: ✓ Selected with your assistant
```

Pinning is `MainActivity.orderForDisplay`. It does not rewrite backend catalogue order.

If catalogue load fails, the screen shows "We couldn't load phones." and Retry. If the session lookup fails but the catalogue succeeds, the catalogue still renders and a restore message is shown. The Activity does not crash.

There is no product-detail screen, cart, checkout, or image loader. Placeholders are drawn in Compose.

---

## 14. What to reuse in another Android app

Reusable:

```text
AppFunctions Gradle/KSP setup, including PackageAppFunctionAssetsTask
abstract @AppFunctionServiceEntryPoint + generated service name in the manifest
typed @AppFunctionSerializable request/response models
@AppFunction methods that return either a serializable result or PendingIntent
registration check via cmd app_function list-app-functions
Testing Agent workflow (separate clone, separate API key)
PendingIntent handoff with an explicit component and FLAG_IMMUTABLE
process-lifetime assumption: pass sessionId, do not keep session memory in the process
thin capability layer: one HTTP or one existing use case per function
```

POC-specific. Do not copy these into a large app just because they exist:

```text
demo Shop Phones UI
SQLite demo catalogue and score weights
hardcoded http://10.0.2.2:8000
demo product data
Commerce Agent launcher shell
```

When the target app already has domain code, the pattern is:

```text
Existing UI
     ↓
Domain UseCase
     ↓
Repository/API

AppFunction
     ↓
same Domain UseCase
     ↓
same Repository/API
```

AppFunctions are another typed entry point. They should not reimplement the use case.

---

## 15. Integration sequence for a large existing app

```text
STEP 1
Integrate AppFunctions dependencies, KSP, and the asset-packaging workaround only.

STEP 2
Add one minimal static AppFunction.

STEP 3
Build and install the target application.

STEP 4
Verify registration with adb shell cmd app_function list-app-functions.

STEP 5
Verify invocation with the Google Testing Agent.

STEP 6
Only after that, connect one AppFunction to one existing domain use case.

STEP 7
Only after that capability works, add multi-step flows such as shop → select → PendingIntent.
```

This separates AppFunctions/build-system failures from domain/business-logic failures.

---

## 16. Invariants

1. AppFunctions are thin typed capability adapters.
2. Business truth does not live in the AI agent.
3. Conversational state does not depend on Android process lifetime.
4. Backend `sessionId` is opaque to the agent. The agent stores and resends it. It does not parse it.
5. Omitted preference fields preserve backend state.
6. `clearPreferences` explicitly removes named backend state.
7. Product selection is backend-validated against `lastShownProductIds` and the catalogue.
8. UI handoff uses an explicit immutable `PendingIntent` to this app's own `MainActivity`. The agent does not supply a component, action, or URI.
9. The Testing Agent is development tooling, not production architecture.
10. Gemini API credentials are never shipped in this application.
11. Do not add an LLM to the backend unless a task explicitly requires it. Matching is deterministic.
12. Do not replace existing domain/business logic when integrating into a larger app.
13. Prove AppFunctions infrastructure before integrating complex features.
14. `selectDevice` does not launch the UI. `openSelectedDevice` does.
15. Do not trust a `productId` supplied only on the handoff Intent when the backend session disagrees.

---

## 17. Known-good files

Read these first. Do not scan the repository until one of them is insufficient.

| File | Why it matters |
| --- | --- |
| `ARCHITECTURE.md` | This document. |
| `app/build.gradle.kts` | Dependencies, SDK levels, KSP argument, `PackageAppFunctionAssetsTask`. |
| `gradle/libs.versions.toml` | Version catalog for AGP, Kotlin, KSP, AppFunctions, OkHttp, Compose. |
| `settings.gradle.kts` | Root project name and `:app`. |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.6.0. |
| `app/src/main/AndroidManifest.xml` | `CommerceAppFunctionService`, AppFunctions XML properties, `singleTop` activity. |
| `app/src/main/res/xml/app_metadata.xml` | Natural-language description of the three functions for the agent runtime. |
| `app/src/main/java/com/example/myapplication/appfunctions/BaseCommerceAppFunctionService.kt` | The three `@AppFunction` methods and PendingIntent construction. |
| `app/src/main/java/com/example/myapplication/appfunctions/DeviceShoppingRequest.kt` | Preference delta fields and clear semantics. |
| `app/src/main/java/com/example/myapplication/appfunctions/DeviceShoppingResult.kt` | `sessionId` + products returned to the agent. |
| `app/src/main/java/com/example/myapplication/appfunctions/DeviceResult.kt` | One catalogue product. |
| `app/src/main/java/com/example/myapplication/appfunctions/DeviceSelectionRequest.kt` | `sessionId` + `productId`. |
| `app/src/main/java/com/example/myapplication/appfunctions/DeviceSelectionResult.kt` | Selection confirmation. |
| `app/src/main/java/com/example/myapplication/appfunctions/OpenSelectedDeviceRequest.kt` | Session id only. |
| `app/src/main/java/com/example/myapplication/network/CommerceApiClient.kt` | OkHttp calls, `BASE_URL`, null-omitting JSON. |
| `app/src/main/java/com/example/myapplication/HandoffExtras.kt` | Intent extra key strings. |
| `app/src/main/java/com/example/myapplication/MainActivity.kt` | `onCreate` / `onNewIntent`, backend restore, display ordering. |
| `app/src/debug/AndroidManifest.xml` | Debug network security config overlay. |
| `app/src/debug/res/xml/network_security_config.xml` | Cleartext allowlist for emulator loopback. |
| `README.md` | Build, install, and emulator backend startup. |

Backend files to open only when the task is about matching, sessions, or HTTP contracts (in `agentic-commerce-backend`):

| File | Why it matters |
| --- | --- |
| `app/main.py` | Route table. |
| `app/models.py` | Preference model and `KNOWN_PREFERENCE_FIELDS`. |
| `app/shopping_graph.py` | Thread id, merge, clear, price-range rejection, select, handoff. |
| `app/catalogue.py` | Hard filters, weights, top 5. |

---

## Minimal Mental Model

If token budget is limited, remember only this:

```text
System Agent
    ↓
typed Android AppFunction
    ↓
existing application/domain capability
    ↓
backend/business system
```

For conversational Commerce:

```text
Agent
    ↓
shopForDevices
    ↓
backend session
    ↓
selectDevice
    ↓
openSelectedDevice
    ↓
PendingIntent
    ↓
Android UI
```

The Android process is transient.
The backend owns session state.
The agent owns language interpretation.
AppFunctions are the capability boundary.
