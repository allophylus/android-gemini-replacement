# Mate Assistant - Implemented Features

## 🚀 Core Infrastructure
- **System Integration**: `VoiceInteractionService` + `VoiceInteractionSessionService`. Registered as Default Digital Assistant App.
- **Screen Awareness**: `AssistStructure` parsing reads text/hierarchy of active apps.
- **Dual Inference Backend**: `AICoreClient` routes to MediaPipe (TFLite) or llama.cpp (GGUF) based on selected model.

## 🧠 AI Models
- **Model Selector**: Settings dropdown lists 6 models with vision badges (👁️) and engine labels.
- **MediaPipe Models**: Gemma 1.1 2B (default), Gemma 2B — verified free public downloads.
- **llama.cpp Models**: Moondream2 👁️, SmolVLM 500M 👁️, Qwen2-VL 2B 👁️, Phi-3.5 Mini.
- **Auto-Download**: Models download from HuggingFace on Wi-Fi. Cellular prompts user first.
- **Model Management**: Unload from RAM button, download selected model button.

## 🔧 Tool Use (LLM Actions)
- **App Launcher**: LLM emits `[LAUNCH:app name]` → fuzzy-matches via `PackageManager` → fires Intent.
- **Web Search**: LLM emits `[SEARCH:query]` → scrapes DuckDuckGo HTML → feeds results back for summarization.
- **URL Summarization**: Paste any URL → Jsoup extracts content → LLM summarizes → stored in memory.

## 🧠 Memory & Context
- **Encrypted Memory Vault**: AES-encrypted storage for name, DOB, family (via `EncryptedPrefsManager`).
- **Conversation Compaction**: After 5 exchanges, chat is auto-summarized to conserve the 1024-token context.
- **Long-Term Memory**: Bio data injected into every system prompt for persistent recall.

## 🎨 Persona Engine
- **Personality Types**: Helpful, Funny, Sad, Happy, Childish, Sarcastic, Professional.
- **Trait Sliders**: Verbosity, Formality, Warmth, Proactivity, Humor, Confidence (1-10).
- **Voice Gender**: Male/Female TTS selection linked to Android TTS engine.
- **Response Timing**: Generation time displayed on each response.

## 🏗️ Technical Stack
- **SDK**: Target SDK 34 (Android 14), Gradle 8.13
- **Inference**: MediaPipe GenAI SDK + llama.cpp (git submodule, CMake/NDK build)
- **Web**: Jsoup 1.17.2 for HTML scraping
- **Security**: AndroidX Security-Crypto 1.1.0-alpha06
- **Privacy**: 100% on-device. No cloud calls for reasoning or memory.

## 📂 Project Files
| File | Purpose |
|------|---------|
| `AICoreClient.kt` | Dual-backend routing, download, generation |
| `LlamaCppBackend.kt` | llama.cpp Kotlin/JNI wrapper |
| `llama-android.cpp` | C++ JNI bridge for llama.cpp |
| `InferenceBackend.java` | Backend abstraction interface |
| `ModelConfig.java` | Model catalog (URLs, backend, vision, sizes) |
| `ToolExecutor.java` | App launching + web search |
| `WebScraper.java` | Jsoup URL scraping |
| `PreferencesManager.java` | Persona + system prompt generation |
| `EncryptedPrefsManager.java` | AES-encrypted user bio storage |
| `VoiceManager.java` | TTS voice output |
| `MainActivity.java` | Settings UI + chat interface |
| `AssistantSession.java` | Voice assistant overlay UI |
| `AssistantService.java` | Android service entry point |
