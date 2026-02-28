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
- [ ] **Cross-Platform Portability**: Architect the core logic to be portable to OpenClaw platforms, other Android devices, and eventually iOS.
- [ ] **Advanced Persona Engine**: Add custom voices (paid feature), behaviors, and distinct personalities on top of the base LLM.
- [ ] **Profile Ingestion**: Implement import functionality for LinkedIn PDF, FOAF, and Google Takeout to build the initial user profile.
- [ ] **Local Memory (RAG) & Skills**: Expand SQLite integration to act as long-term memory and allow Mate to learn and use custom skills.
- [ ] **App Orchestration**: Intent-based control for Email and Calendar.
<<<<<<< Updated upstream
- [ ] **Personalization Engine**: Profile management (concise vs. verbose, etc.).
- [ ] **UI Overlay**: Bottom-sheet assistant UI (similar to native Google Assistant).
=======
- [ ] **UI Overlay**: Minimalist assistant UI that appears over other apps.
>>>>>>> Stashed changes

## 📝 To-Do List (Active)
1. [ ] **Step 1: Setup Gradle for AI Edge SDK & SQLite** (Dependencies ready).
2. [ ] **Step 2: Implement `AICoreClient`** to handle local inference.
3. [ ] **Step 3: Analyze ClawControl v1.3.1 for UI inspiration** (Wake-word dictation composer).
4. [ ] **Step 4: Create `MemoryManager`** using SQLite for local "facts" storage.
5. [ ] **Step 5: Build the Bargain Detection Logic** to trigger on currency symbols.
6. [ ] **Step 6: Create a basic Settings Activity** for user customization.
