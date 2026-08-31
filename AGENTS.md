# AGENTS.md — StudyMate (Offline AI Study Assistant)

Repository-specific memory for OpenHands agents working on this Android app.

## What this project is
A fully-offline Android study assistant (Kotlin + Jetpack Compose) that runs on 1 GB-RAM
devices. Two tabs: **Chat** (general LLM Q&A) and **Study Assistant** (RAG over uploaded
PDF/TXT). No internet permission is declared.

## Tech stack & key versions
- Kotlin 1.9.24, AGP 8.3.2, Gradle 8.7, JDK 17, Compose compiler ext 1.5.14, Compose BOM 2024.06.00
- compileSdk/targetSdk 34, minSdk 24
- Room 2.6.1 (KSP), ML Kit text-recognition 16.0.1, MediaPipe tasks-text + tasks-genai 0.10.14
- Package: `com.studymate.app`

## Architecture map
- `StudyMateApp` (Application) = manual DI + ProcessLifecycleObserver that **unloads the LLM
  and embedder on background** (the core OOM-prevention strategy for 1 GB RAM).
- `llm/LlmManager` — MediaPipe GenAI `LlmInference` (0.10.14, no-session API). Lazy-loaded, mutex-guarded;
  streaming via a per-query `StreamTarget` + `CompletableDeferred`.
- `rag/TextExtractor` — PdfRenderer (page→Bitmap→ML Kit OCR, one page at a time) or UTF-8 read.
- `rag/EmbeddingManager` — MediaPipe `TextEmbedder` (USE tflite from assets).
- `rag/TextChunker` — sentence-aware, 500-char chunks, 100-char overlap; recognizes Amharic `።`.
- `rag/VectorRetriever` — in-memory cosine (no native FAISS); chunks persisted in Room as BLOB.
- `data/` — Room DB `studymate.db`, `documents` + `chunks` tables, `VectorConverters` (FloatArray↔BLOB).

## MediaPipe API gotchas (verified against the ACTUAL resolved jars, not just docs)
> ⚠️ Version 0.10.14 of `tasks-genai` does **NOT** have the session-based API.
> The `LlmInferenceSession` class only exists in later versions. Inspect the jar with
> `javap` before writing code against API examples found online — many tutorials target
> newer versions.
- **LLM (0.10.14):** `LlmInference.createFromOptions(ctx, opts)` with `LlmInferenceOptions`
  builder: `setModelPath`, `setMaxTokens`, `setTemperature`, `setTopK`, `setRandomSeed`,
  `setResultListener(ProgressListener<String>)`, `setErrorListener(ErrorListener)`.
- `generateResponse(prompt): String` — synchronous full response (used for RAG).
- `generateResponseAsync(prompt): void` — fire-and-forget; tokens arrive via the
  `resultListener` registered at engine creation (NOT a per-call callback, NOT a returned
  future). Streaming is implemented in `LlmManager` by swapping an `AtomicReference<
  StreamTarget>` per query and completing a `CompletableDeferred` when `done==true`.
- `ProgressListener.run(OutputT, boolean)` — SAM, Kotlin lambda `{ partial, done -> }`.
- NO `setSystemPrompt` in 0.10.14 → system instruction is prepended to the prompt in
  `PromptBuilder`.
- **TextEmbedder (0.10.14):** `TextEmbedder.createFromOptions(ctx, opts)` where opts =
  `TextEmbedderOptions.builder().setBaseOptions(BaseOptions.builder().setModelAssetPath(p).build())
  .setL2Normalize(true).build()`. `embed(text): TextEmbedderResult` is **synchronous**.
  Chain: `embedder.embed(text).embeddingResult().embeddings().first().floatEmbedding()`
  (returns `FloatArray` directly — do NOT call `.toFloatArray()`).
- `Embedding`/`EmbeddingResult` live in `tasks-core` (`com.google.mediapipe.tasks.components.
  containers`), not `tasks-text`.
- Models load from a **filesystem path**, not assets — copy asset→filesDir/models/ first.
- **LLM native lib** (`libllm_inference_engine_jni.so`) ships **only for arm64-v8a**;
  the LLM won't run on x86 emulators. Text embedder + ML Kit OCR work on all ABIs.

## Offline enforcement
ML Kit / MediaPipe pull in `INTERNET` + `ACCESS_NETWORK_STATE` transitively. The manifest
strips them with `<uses-permission ... tools:node="remove"/>` so the merged APK has
zero network permissions. Verify with: `aapt dump permissions app-debug.apk`.

## Models (NOT committed — too large)
- Embedding model `universal_sentence_encoder_quantized.tflite` (~25 MB) → `app/src/main/assets/`.
- LLM (TinyLlama-1.1B-Chat Q4_K_M, ~670 MB) → **sideloaded via adb** to
  `filesDir/models/` (keeps APK < 500 MB). Candidate names in `ModelLoader.CANDIDATES`.
- See README §3 for exact download URLs + adb `run-as` commands.

## Build
The APK builds successfully in this sandbox (JDK 17 + Android cmdline-tools installed
under `~/tools` and `~/android-sdk`; `local.properties` points at the SDK).
- `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (~90 MB, < 500 MB).
- `gradle-wrapper.jar` is committed (downloaded from gradle/gradle v8.7.0 tag).
- Build host needs ~2 GB Gradle heap (set in `gradle.properties`).

## Conventions
- Single-activity + Compose; one ViewModel per tab (`ChatViewModel`, `StudyViewModel`).
- All heavy work on `Dispatchers.IO` via `withContext`; UI state as `StateFlow` collected with
  `collectAsStateWithLifecycle`.
- Comments only for non-obvious invariants (lazy load/unload, PFD lifetime, cosine retrieval choice).
