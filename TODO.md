# Android Gemini Replacement - Project Plan

## 🎯 Vision
A 100% local, privacy-first Android assistant that replaces Google Gemini/Assistant. It uses AICore (Gemini Nano) for reasoning and local SQLite for memory and personalization.

## 🛠 Feature List (Roadmap)
- [x] **Core Service Skeleton**: `VoiceInteractionService` implementation.
- [x] **Screen Awareness**: `AssistStructure` parsing to "see" active apps.
- [x] **Initial Project Structure**: Finalized for Android Studio compatibility.
- [ ] **Custom Wake Word Detection**: Implementation of `AlwaysOnHotwordDetector` with local fallback (Porcupine/openWakeWord).
- [ ] **ClawControl Integration**: Research potential for bridging with the ClawControl UI/API for remote management.
- [ ] **On-Device Reasoning**: Integration with Google AI Edge SDK (AICore/Gemini Nano).
- [ ] **Local Memory (RAG)**: SQLite-vec integration for storing facts and preferences.
- [ ] **Bargain Hunter**: Price extraction logic and local price-history database.
- [ ] **App Orchestration**: Intent-based control for Email and Calendar.
- [ ] **Personalization Engine**: Profile management (concise vs. verbose, etc.).
- [ ] **UI Overlay**: Bottom-sheet assistant UI (similar to native Google Assistant).

## 📝 To-Do List (Active)
1. [ ] **Step 1: Setup Gradle for AI Edge SDK & SQLite** (Dependencies ready).
2. [ ] **Step 2: Implement `AICoreClient`** to handle local inference.
3. [ ] **Step 3: Analyze ClawControl v1.3.1 for UI inspiration** (Wake-word dictation composer).
4. [ ] **Step 4: Create `MemoryManager`** using SQLite for local "facts" storage.
5. [ ] **Step 5: Build the Bargain Detection Logic** to trigger on currency symbols.
6. [ ] **Step 6: Create a basic Settings Activity** for user customization.
