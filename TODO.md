# Android Gemini Replacement - Project Plan

## 🎯 Vision
A 100% local, privacy-first Android assistant that replaces Google Gemini/Assistant. It uses AICore (Gemini Nano) for reasoning and local SQLite for memory and personalization.

## 🛠 Feature List (Roadmap)
- [x] **Core Service Skeleton**: `VoiceInteractionService` implementation.
- [x] **Screen Awareness**: `AssistStructure` parsing to "see" active apps.
- [x] **Initial Project Structure**: Finalized for Android Studio compatibility.
- [x] **On-Device Reasoning**: Integration with MediaPipe GenAI SDK (LlmInference/Gemini Nano/Gemma).
- [x] **Personalization Engine**: Profile management (concise vs. verbose, etc.).
- [x] **UI Overlay**: Bottom-sheet assistant UI with **ClawControl v1.3.1** aesthetic.
- [x] **Ambient Themes**: Day/Night cycle and "Cozy Office" status indicators.
- [x] **Local Memory (RAG Skeleton)**: SQLite integration for facts and history.
- [x] **Bargain Hunter Table**: Specialized storage for detected prices/items.
- [x] **Manifest Integration**: Initialized separate project for OpenClaw cost orchestration.
- [ ] **Custom Wake Word Detection**: Implementation of `AlwaysOnHotwordDetector` with local fallback (Porcupine/openWakeWord).
- [ ] **Advanced RAG Engine**: Implement `sqlite-vec` for semantic search on local facts.
- [ ] **ClawControl Integration**: Research potential for bridging with the ClawControl UI/API for remote management.
- [ ] **Cross-Platform Portability**: Architect the core logic for iOS and OpenClaw Linux environments.
- [ ] **App Orchestration**: Intent-based control for Email and Calendar.

## 📝 To-Do List (Active)
- [x] **Step 1: Setup Gradle for MediaPipe LlmInference & SQLite** (Dependencies ready).
- [x] **Step 2: Implement `AICoreClient`** to handle local inference (Uses LlmInference and auto-downloads model).
- [x] **Step 3: Analyze ClawControl v1.3.1 for UI inspiration** (Wake-word dictation composer).
- [x] **Step 4: Create `MemoryManager`** using SQLite for local "facts" storage.
- [x] **Step 5: Build the Bargain Detection Logic** to trigger on currency symbols.
- [x] **Step 6: Create a basic Settings Activity** for user customization.
- [ ] **Step 7: Implement Voice-to-Text** for user queries in the overlay.
- [ ] **Step 8: Develop "Living Office" Visuals** (Integrating 2.5D assets for the assistant background).
