This folder is where you (optionally) place model files that should be BUNDLED inside the APK.

DO NOT bundle the LLM here if it would push the APK over the 500 MB limit — instead sideload
it via adb (see README §3, "Method A: sideload (recommended)").

Two kinds of model live in the on-device pipeline:

1. Embedding model (small, ~25 MB) — REQUIRED for RAG.
   File name expected by EmbeddingManager.kt:
       universal_sentence_encoder_quantized.tflite
   Download: https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite
   (or a quantized variant). Place the file in THIS folder.

2. LLM (large, ~670 MB for TinyLlama-1.1B Q4_K_M) — REQUIRED for generation.
   Sideload via adb to filesDir/models/ (do NOT put it here, or the APK will exceed 500 MB):
       adb push TinyLlama-1.1B-Chat-v1.0.Q4_K_M.tflite /data/local/tmp/
   See README §3 for the full adb push command including run-as for non-rooted devices.

If you DO bundle a small LLM in assets, ModelLoader.kt will find it automatically; the
candidate file names are listed in ModelLoader.CANDIDATES.
