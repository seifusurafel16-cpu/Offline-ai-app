You are an expert Android Developer. Your task is to build a complete, fully functional, and optimized Android application from scratch. Follow these requirements EXACTLY.

### Project Overview:
The app is an educational study assistant designed to run completely offline on low-end Android devices. It must support two distinct modes of interaction.

### 📌 Strict Constraints (MUST BE OBSERVED):
1. **Offline:** The app MUST work 100% without an internet connection. No API calls to the cloud.
2. **App Size Limit:** The final APK size MUST NOT exceed 500 MB.
3. **Device Hardware Limit:** The app must run smoothly on a device with only **1 GB of RAM**.
4. **Tech Stack:** Use **Kotlin**, **Android Studio**, **Jetpack Compose** (for UI), **Google ML Kit** (for PDF/Text extraction), and **MediaPipe LLM Inference API** (or TensorFlow Lite) for the local model.
5. **No Custom Training:** Do NOT attempt to train a new AI model from scratch. You must use a pre-existing, small, quantized Large Language Model (LLM) (like TinyLlama 1.1B or Qwen2.5-1.5B, quantized to 4-bit) and embed it into the app. If the model download URL is needed, provide a clear instruction on where to get it and how to load it locally.

### 🎨 UI / UX Requirements:
The app must have a modern, clean interface with a bottom navigation bar containing two main tabs:

1. **Tab 1: "Chat" (Normal Mode):**
   - A standard chat interface (similar to WhatsApp/ChatGPT).
   - Users can type any general question.
   - The AI model answers using its general pre-trained knowledge.
   - Include a "Clear Chat" button.

2. **Tab 2: "Study Assistant" (RAG Mode):**
   - A button to upload/select a **PDF** or **TXT** file from the device's local storage.
   - Upon upload, the app must extract the text (using ML Kit), split it into chunks, and store it in a local **Vector Database** (e.g., SQLite-VSS or FAISS).
   - The user can click a "Read & Learn" button to process the file (Indexing phase).
   - After indexing, users can ask questions about the uploaded document. The app will retrieve the most relevant chunks and use the local LLM to generate an answer based specifically on that document.
   - Show a loading indicator when the file is being processed and when the AI is generating an answer.

### ⚙️ Technical Implementation Details:
- **Text Extraction:** Use Google ML Kit for text recognition from PDFs/Images.
- **Embedding & Vector DB:** Use an on-device embedding model to convert text chunks into vectors. Store them in an efficient mobile-friendly vector store.
- **LLM & Memory Management:** Because of the strict 1GB RAM limit, ensure the model is loaded lazily (only when needed), and unloaded when the app is backgrounded to prevent OutOfMemory (OOM) errors. Use 4-bit quantization.
- **Language Support:** The UI labels should be in English, but the model should be capable of understanding prompts in both Amharic and English.

### 📝 Deliverables & Step-by-step Instructions:
1. Create the complete Android project file structure.
2. Write all necessary files: `build.gradle`, `AndroidManifest.xml`, `MainActivity.kt`, ViewModels, and Data classes.
3. Implement the RAG pipeline (extract, split, embed, retrieve).
4. Implement the Chat UI and Chat logic.
5. Write clear comments in the code.
6. Provide a detailed `README.md` explaining how to download the specific quantized model files, place them in the assets folder, and build the APK.

**Strict Instruction:** Do not give me placeholder code or half-finished implementations. Provide the full, functional code for every component. If a step is too large, break it down logically and continue until the app is fully functional.
