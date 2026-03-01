package com.abettergemini.assistant;

/**
 * Stores metadata for compatible on-device LLM models.
 * Each model specifies its inference backend, vision capability, and download URL.
 */
public class ModelConfig {

    public enum Backend {
        MEDIAPIPE,  // TFLite via MediaPipe LlmInference
        LLAMA_CPP   // GGUF via llama.cpp JNI
    }

    public final String displayName;
    public final String fileName;
    public final String downloadUrl;
    public final long minFileSize;
    public final String description;
    public final Backend backend;
    public final boolean hasVision;
    public final String sizeLabel;

    public ModelConfig(String displayName, String fileName, String downloadUrl,
                       long minFileSize, String description, Backend backend,
                       boolean hasVision, String sizeLabel) {
        this.displayName = displayName;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.minFileSize = minFileSize;
        this.description = description;
        this.backend = backend;
        this.hasVision = hasVision;
        this.sizeLabel = sizeLabel;
    }

    /**
     * Returns formatted display string with vision badge and backend info.
     */
    public String getFormattedName() {
        String vision = hasVision ? " 👁️" : "";
        String eng = backend == Backend.LLAMA_CPP ? " [llama.cpp]" : " [MediaPipe]";
        return displayName + vision + eng + " " + sizeLabel;
    }

    /**
     * Returns the full list of models. Public GGUF models listed first.
     * Gemma (MediaPipe) models require HuggingFace login for download.
     */
    public static ModelConfig[] getAvailableModels() {
        return new ModelConfig[]{
                // ===== llama.cpp Models (GGUF) — Public Downloads =====
                new ModelConfig(
                        "Qwen2-VL 2B",
                        "Qwen2-VL-2B-Instruct-Q4_K_M.gguf",
                        "https://huggingface.co/ggml-org/Qwen2-VL-2B-Instruct-GGUF/resolve/main/Qwen2-VL-2B-Instruct-Q4_K_M.gguf",
                        500000000L,
                        "Alibaba. Chat + vision. Strong OCR and image understanding.",
                        Backend.LLAMA_CPP, true, "~1.0GB"
                ),
                new ModelConfig(
                        "Phi-3.5 Mini",
                        "Phi-3.5-mini-instruct-Q4_K_M.gguf",
                        "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
                        1500000000L,
                        "Microsoft. Best reasoning at this size. Text-only.",
                        Backend.LLAMA_CPP, false, "~2.3GB"
                )
        };
    }

    /**
     * Find a ModelConfig by its display name.
     */
    public static ModelConfig findByName(String displayName) {
        for (ModelConfig m : getAvailableModels()) {
            if (m.displayName.equals(displayName)) return m;
        }
        return getAvailableModels()[0]; // Default to Qwen2-VL 2B
    }
}
