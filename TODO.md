# Android Gemini Replacement - Project Plan

## 🎯 Vision
A 100% local, privacy-first Android assistant that replaces Google Gemini/Assistant. It uses AICore (Gemini Nano) for reasoning and local SQLite for memory and personalization.

## 🛠 Feature List (Roadmap)
- [x] **Core Service Skeleton**: `VoiceInteractionService` implementation.
- [x] **Screen Awareness**: `AssistStructure` parsing to "see" active apps.
- [ ] **On-Device Reasoning**: Integration with Google AI Edge SDK (AICore/Gemini Nano).
- [ ] **Local Memory (RAG)**: SQLite-vec integration for storing facts and preferences.
- [ ] **Bargain Hunter**: Price extraction logic and local price-history database.
- [ ] **App Orchestration**: Intent-based control for Email and Calendar.
- [ ] **Personalization Engine**: Profile management (concise vs. verbose, etc.).
- [ ] **UI Overlay**: Minimalist assistant UI that appears over other apps.

## 📝 To-Do List (Active)
1. [ ] **Step 1: Setup Gradle for AI Edge SDK & SQLite** (Next Task)
2. [ ] **Step 2: Implement `AICoreClient`** to handle local inference.
3. [ ] **Step 3: Create `MemoryManager`** using SQLite for local "facts" storage.
4. [ ] **Step 4: Build the Bargain Detection Logic** to trigger on currency symbols.
5. [ ] **Step 5: Create a basic Settings Activity** for user customization.
