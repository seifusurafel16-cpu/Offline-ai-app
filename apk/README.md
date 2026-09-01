# Prebuilt APK

This folder contains a prebuilt debug APK of the StudyMate app so the project can be
installed and inspected without building it.

## File

- `StudyMate-debug.apk` — debug build (signed with the debug key, `versionName 1.0`).

## Specs

- Package: `com.studymate.app`
- Size: ~89 MB (under the 500 MB limit)
- ABIs: arm64-v8a, armeabi-v7a, x86, x86_64
  - The MediaPipe LLM inference engine native library (`libllm_inference_engine_jni.so`)
    is **arm64-v8a only**, so the LLM Chat/RAG answer generation runs on real arm64
    devices. Text embedding and ML Kit OCR work on all listed ABIs.
- Min SDK 24 / Target SDK 34

## Crash-on-launch fix (this build)

An earlier build crashed immediately on launch ("StudyMate keeps stopping") on some
devices. This build fixes it by:

1. **Lazy initialization** — `Application.onCreate` no longer builds the Room database, the
   ML Kit text recognizer, the embedder, or the RAG service at process start. Every heavy
   component is created on first *use* (inside `Dispatchers.IO`), so app launch can't crash
   on a native-library / 3rd-party initializer failure.
2. **ML Kit recognizer deferred** — `TextRecognition.getClient(...)` now runs on first OCR
   pass, not in the `TextExtractor` constructor.
3. **Defensive guards** — `Application.onCreate`, the process-lifecycle unload observer,
   `enableEdgeToEdge()`, and `setContent {}` are each wrapped so a single failure degrades
   gracefully (the UI shows an error instead of crashing).
4. **Stripped telemetry** — Google `datatransport` (network telemetry) is excluded globally;
   the merged manifest keeps only what the bundled offline OCR actually needs.

## Offline verification

The merged manifest contains **no** `INTERNET` or `ACCESS_NETWORK_STATE` permissions:

```
$ANDROID_HOME/build-tools/34.0.0/aapt dump permissions StudyMate-debug.apk
package: com.studymate.app
uses-permission: name='android.permission.READ_MEDIA_IMAGES'
uses-permission: name='android.permission.READ_EXTERNAL_STORAGE' maxSdkVersion='32'
uses-permission: name='com.studymate.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```

## How to build it yourself

From the project root:

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

See the project root `README.md` for:
- How to add the embedding model (`app/src/main/assets/`).
- How to sideload the LLM model via `adb` (`filesDir/models/`).
- The full RAG pipeline and architecture.

## Note on models

This APK ships **without** the AI model weights (to stay under the 500 MB size limit and
keep the repository lightweight). After installing the APK, follow `README.md` section 3
to add the embedding model and sideload the quantized LLM — otherwise the app will show a
"model not found" message in the Chat / Study Assistant tabs.

## If it still crashes on your device

If the app still won't open after installing this build, please capture a crash log and
share it so we can pinpoint the device-specific cause:

```bash
adb logcat -d *:E AndroidRuntime:E | grep -i studymate
```

Then look for a `FATAL EXCEPTION` stack trace and include it in a bug report.
