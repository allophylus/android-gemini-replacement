# Mate Assistant

A privacy-first, on-device AI assistant for Android. Mate replaces the default Google Assistant with a fully local LLM that never sends your data to the cloud.

## Features
- **Dual Inference Backend** — MediaPipe (TFLite) for Google Gemma models + llama.cpp (GGUF) for Moondream2, SmolVLM, Qwen2-VL, Phi-3.5
- **Vision Models** — Moondream2, SmolVLM, and Qwen2-VL can describe images and read text in photos
- **Model Selector** — Choose from 6 compatible LLMs in Settings, with vision badges (👁️) and engine labels
- **Tool Use** — LLM can launch apps (`[LAUNCH:YouTube]`) and search the web (`[SEARCH:query]`) via DuckDuckGo
- **Web Scraping** — Send a URL to extract and summarize web content using Jsoup
- **Conversation Compaction** — Auto-summarizes chat after 5 exchanges to stay within token limits
- **Encrypted Memory Vault** — Securely stores your name, DOB, and family info using Android EncryptedSharedPreferences
- **Persona Engine** — Tune personality (Helpful/Funny/Sarcastic/etc.), verbosity, formality, humor, warmth via sliders
- **Voice Output** — TTS with male/female voice selection
- **Screen Awareness** — Reads the active app's UI via AssistStructure API
- **Response Timing** — Shows LLM generation time on each response

## Build & Run

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 34+
- Android NDK r26+ (for llama.cpp backend)
- CMake 3.22.1+

### Setup
```bash
# Clone with submodules (required for llama.cpp)
git clone --recursive https://github.com/YOUR_USERNAME/android-gemini-replacement.git

# If already cloned without --recursive:
git submodule update --init
```

### Build
1. Open in Android Studio
2. Sync Gradle (Gradle 8.13 configured)
3. Build & Run on device (arm64 only)

### Set as Default Assistant
1. Go to **Settings > Apps > Default Apps > Digital Assistant App**
2. Select **Mate Assistant**
3. Long-press home button or swipe from corners to trigger

## Architecture
```
AICoreClient.kt          ← Routing layer (selects backend per model)
├── LlmInference          ← MediaPipe TFLite (Gemma models)
├── LlamaCppBackend.kt    ← llama.cpp GGUF (via JNI)
│   └── llama-android.cpp ← C++ JNI bridge
├── ModelConfig.java      ← Model catalog with URLs, backend type, vision flags
├── ToolExecutor.java     ← App launching + DuckDuckGo web search
├── WebScraper.java       ← Jsoup URL content extraction
└── PreferencesManager.java ← System prompt generation + persona traits
```

## License
MIT
