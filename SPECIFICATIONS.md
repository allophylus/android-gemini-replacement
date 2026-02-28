# Mate Assistant - Specification & Design Document

## 1. Overview
**Mate** is an advanced, privacy-first, on-device digital assistant. It is designed to replace standard OEM assistants (like Google Gemini or Siri) while overcoming their limitations. Mate acts as a personalized intelligence layer that runs locally, offering long-term memory, customizable personas, and extensible skills, while eventually supporting cross-platform deployment.

## 2. Core Goals & Features
*   **100% On-Device Processing**: Primary reasoning happens locally (via MediaPipe GenAI/LlmInference, Gemini Nano/Gemma 2B, or local models like Llama.cpp) to ensure absolute privacy and zero-latency inference.
*   **Advanced Memory (RAG)**: Unlike standard assistants that forget context, Mate uses a local Vector Database (e.g., SQLite-vec) to store user preferences, personal facts, and past conversations, retrieving them via Retrieval-Augmented Generation (RAG).
*   **User Persona Ingestion**: Mate learns about the user through several methods:
    *   **Initial Setup Questionnaire**: A conversational onboarding flow asking for basic facts (name, profession, communication style preferences).
    *   **Passive Context Learning**: Extracting facts from everyday interactions (e.g., "Remind me to call my wife Sarah" -> learns wife's name is Sarah).
    *   **Profile Import**: Importing structured data. While there isn't a universally adopted "AI Profile" open standard yet, we can support common semantic web formats (like JSON-LD, FOAF - Friend of a Friend) or simple standardized JSON exports from LinkedIn/Google Takeout.
*   **Persona Engine (Behavioral Tuning)**: Users can define Mate's personality. We map these to tunable LLM prompt parameters inspired by the **Ocean/Big Five Personality Traits** and practical assistant needs:
    *   *Verbosity (Concisiveness)*: `[Extreme Brevity] <----> [Highly Detailed]`
    *   *Formality (Professionalism)*: `[Gen Z Chat / Slang] <----> [Executive Assistant]`
    *   *Warmth (Empathy/Supportiveness)*: `[Cold & Analytical] <----> [Highly Empathetic]`
    *   *Proactivity (Initiative)*: `[Reactive: Answers only] <----> [Proactive: Suggests next actions & reminders]`
    *   *Humor (Sass)*: `[Dry/Serious] <----> [Sarcastic/Playful]`
    *   *Confidence (Self-Doubt)*: `[Makes Assumptions] <----> [Always asks for clarification]`
    These attributes are stored locally as numerical values (1-10) and converted into natural language instructions injected into the system prompt.
*   **Custom Voice**: Integration with local Text-to-Speech (TTS) engines allowing customized, natural-sounding voices.
*   **Skill Expansion**: An extensible plugin system allowing Mate to interface with other apps (Calendar, Email) and APIs (Bargain Hunting, Smart Home) using Android Intents and local scripting.
*   **Cross-Platform Portability**: Core assistant logic, memory management, and persona definitions will be abstracted so they can be ported to OpenClaw platforms, iOS, and desktop environments in the future.

## 3. Technical Architecture
### 3.1. Android Implementation
*   **Framework**: Native Android (Java/Kotlin).
*   **Entry Point**: Implements `VoiceInteractionService` to register as the OS-level Default Digital Assistant.
*   **Screen Awareness**: Uses the `AssistStructure` API to read the currently active screen context when invoked.
*   **Inference Engine Bridge**: An abstraction layer (`AICoreClient`) over the MediaPipe GenAI SDK (LlmInference). Later adaptable to API-based endpoints or ONNX runtime for cross-platform.
*   **Storage Layer**: Standard Android SQLite combined with an embedded Vector Search extension (e.g., `sqlite-vec`).

### 3.2. Cross-Platform Abstraction Strategy
To ensure portability to iOS and OpenClaw:
*   **Business Logic Separation**: All RAG logic, prompt formatting, and skill definitions must be decoupled from Android-specific APIs (like `Context` or `Intents`).
*   **Core Core**: Written in Kotlin Multiplatform (KMP) or Rust for shared compilation across Android, iOS, and OpenClaw Linux environments.
*   **Platform Specifics**:
    *   *Android*: `VoiceInteractionService`, `AssistStructure`, Java Native Interface (JNI).
    *   *iOS*: SiriKit Intents, Application Extensions.
    *   *OpenClaw*: Custom daemon process, DBus/Wayland integration for screen parsing.

## 4. Limitations & Challenges
*   **Hardware Constraints**: On-device LLMs (like Gemini Nano) are heavily constrained by local RAM (typically requiring 4GB+ just for the model) and NPU capabilities.
*   **Context Window**: Local models have smaller context windows (e.g., 2k-4k tokens). The Memory/RAG system must be highly efficient at summarizing and filtering relevant facts before injecting them into the prompt.
*   **OS Restrictions**:
    *   *Android*: Reading screen context is a privileged action. Maintaining background processes without being killed by the OS battery manager is challenging.
    *   *iOS*: Apple severely restricts third-party apps from acting as default system-wide assistants or reading system-wide screen context.

## 5. Cost Structure
*   **User Cost**: Free. The core philosophy is local, private execution.
*   **Development Cost**: Minimal server infrastructure needed since everything is local. Potential costs include:
    *   Developer accounts (Google Play: $25 once, Apple Developer: $99/year).
    *   Development hardware (Requires latest Android devices with AICore for testing, Mac for iOS ports).
*   **Optional Cloud Sync**: If users want their "Mate Memory" synced across devices (Android <-> OpenClaw <-> iOS), an encrypted cloud backend route (AWS/Firebase) would incur standard cloud costs based on data volume.

## 6. Development Rules
1.  **Privacy First**: No telemetry, analytics, or user text should ever leave the device unless explicitly authorized for a specific remote skill.
2.  **Modular Interfaces**: The `InferenceEngine`, `MemoryStore`, and `SkillExecutor` must be written against abstract interfaces to ensure easy swapping for cross-platform ports.
3.  **Graceful Degradation**: If the local NPU/LLM is busy or unavailable, the system should gracefully fallback to basic static rules or notify the user, rather than crashing.
