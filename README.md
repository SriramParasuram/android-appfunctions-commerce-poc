# Android AppFunctions Commerce POC

Experimental proof of concept demonstrating Android AppFunctions, conversational commerce orchestration, backend-owned session state, deterministic catalogue matching, device selection, and Android app handoff. All products and data are demo data. This repository is not affiliated with or representative of any production commercial system.

The Android app is a client of the companion backend repository `agentic-commerce-backend`. Shopping-session state lives in that backend. This app does not keep a local catalogue or session store.

## What it does

Three AppFunctions:

| Function | Role |
|----------|------|
| `shopForDevices` | Search and refine devices in the current commerce shopping session. |
| `selectDevice` | Select a device previously shown in the current shopping session. |
| `openSelectedDevice` | Open the Commerce Agent app for the device selected in the current shopping session. |

`openSelectedDevice` returns a `PendingIntent` that launches `MainActivity` with the shopping `sessionId`. The app then loads the session and catalogue from the backend and shows a Shop Phones screen. If a device is selected, that card is pinned to the top with an assistant-selection badge.

Application ID stays `com.example.myapplication`. Launcher label: **Commerce Agent**.

## Versions

| Component | Version |
|-----------|---------|
| AppFunctions | `androidx.appfunctions:appfunctions:1.0.0-alpha12` |
| AppFunctions compiler | `androidx.appfunctions:appfunctions-compiler:1.0.0-alpha12` |
| KSP | `2.2.10-2.0.2` |
| Kotlin | `2.2.10` |
| AGP | `9.4.1` |
| Gradle | `9.6.0` |
| OkHttp | `4.12.0` |
| compileSdk / targetSdk | `37` |
| minSdk | `36` |

Registered functions:

```text
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#shopForDevices
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#selectDevice
com.example.myapplication.appfunctions.BaseCommerceAppFunctionService#openSelectedDevice
```

## Run the backend first

The debug client calls the host through the Android emulator alias `10.0.2.2` (host loopback). Start the commerce backend on the machine that runs the emulator:

```bash
cd agentic-commerce-backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Health check:

```bash
curl -s http://127.0.0.1:8000/health
```

Cleartext HTTP to `10.0.2.2`, `localhost`, and `127.0.0.1` is allowed in the debug build only.

## Build and install

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Confirm the functions are indexed:

```bash
adb shell cmd app_function list-app-functions | grep com.example.myapplication
```

Launch the Shop Phones screen without a handoff:

```bash
adb shell am start -n com.example.myapplication/.MainActivity
```

## Notes

- Preference fields omitted on a later `shopForDevices` call keep their previous backend value. `clearPreferences` removes named fields, including `minPrice` and `maxPrice`.
- `selectDevice` does not launch the app. Use `openSelectedDevice` for that handoff.
- Session state is in-memory on the backend and is lost when that process restarts.
- AGP 9 does not automatically package KSP-generated AppFunction XML. `app/build.gradle.kts` copies those assets into the APK.
