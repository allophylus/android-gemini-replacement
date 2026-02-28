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
     * Returns the full list of models with verified free public download URLs.
     */
    public static ModelConfig[] getAvailableModels() {
        return new ModelConfig[]{
                // ===== MediaPipe Models (TFLite) =====
                new ModelConfig(
                        "Gemma 1.1 2B",
                        "gemma-1.1-2b-it-int4.bin",
                        "https://huggingface.co/t-ghosh/gemma-tflite/resolve/main/gemma-1.1-2b-it-int4.bin",
                        500000000L,
                        "Google Gemma 1.1. Fast, reliable on all Android devices.",
                        Backend.MEDIAPIPE, false, "~1.4GB"
                ),
                new ModelConfig(
                        "Gemma 2B",
                        "gemma-2b-it-cpu-int4.bin",
                        "https://huggingface.co/ASahu16/gemma/resolve/main/gemma-2b-it-cpu-int4.bin",
                        1000000000L,
                        "Original Gemma 2B. Proven stable baseline.",
                        Backend.MEDIAPIPE, false, "~1.34GB"
                ),

                // ===== llama.cpp Models (GGUF) =====
                new ModelConfig(
                        "Moondream2",
                        "moondream2-text-model-f16.gguf",
                        "https://huggingface.co/moondream/moondream2-gguf/resolve/main/moondream2-text-model-f16.gguf",
                        500000000L,
                        "Vision model. Can describe images, read text in photos.",
                        Backend.LLAMA_CPP, true, "~1.5GB"
                ),
                new ModelConfig(
                        "SmolVLM 500M",
                        "smolvlm-500m-instruct-q8_0.gguf",
                        "https://huggingface.co/ggml-org/SmolVLM-500M-Instruct-GGUF/resolve/main/SmolVLM-500M-Instruct-Q8_0.gguf",
                        300000000L,
                        "Tiny vision model. Ultra-fast, fits any device.",
                        Backend.LLAMA_CPP, true, "~0.5GB"
                ),
                new ModelConfig(
                        "Qwen2-VL 2B",
                        "qwen2-vl-2b-instruct-q4_k_m.gguf",
                        "https://huggingface.co/Qwen/Qwen2-VL-2B-Instruct-GGUF/resolve/main/qwen2-vl-2b-instruct-q4_k_m.gguf",
                        1000000000L,
                        "Alibaba's vision model. Strong OCR and image understanding.",
                        Backend.LLAMA_CPP, true, "~1.6GB"
                ),
                new ModelConfig(
                        "Phi-3.5 Mini",
                        "phi-3.5-mini-instruct-q4_k_m.gguf",
                        "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
                        1500000000L,
                        "Microsoft. Best reasoning at this size. Text-only.",
                        Backend.LLAMA_CPP, false, "~1.8GB"
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
        return getAvailableModels()[0]; // Default to Gemma 1.1
    }
}
