#include <android/log.h>
#include <chrono>
#include <jni.h>
#include <string>
#include <vector>

// llama.cpp headers
#include "llama.h"

#define TAG "LlamaCppJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Inline batch helpers (these were in common.h but common isn't built as a
// shared lib)
static void batch_add(llama_batch &batch, llama_token id, llama_pos pos,
                      const std::vector<llama_seq_id> &seq_ids, bool logits) {
  batch.token[batch.n_tokens] = id;
  batch.pos[batch.n_tokens] = pos;
  batch.n_seq_id[batch.n_tokens] = seq_ids.size();
  for (size_t i = 0; i < seq_ids.size(); ++i) {
    batch.seq_id[batch.n_tokens][i] = seq_ids[i];
  }
  batch.logits[batch.n_tokens] = logits;
  batch.n_tokens++;
}

static void batch_clear(llama_batch &batch) { batch.n_tokens = 0; }

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_abettergemini_assistant_LlamaCppBackend_nativeLoadModel(
    JNIEnv *env, jobject /* this */, jstring modelPath) {

  const char *path = env->GetStringUTFChars(modelPath, nullptr);
  LOGI("Loading model: %s", path);

  llama_model_params model_params = llama_model_default_params();
  model_params.n_gpu_layers = 99; // Offload as many layers to GPU as possible

  llama_model *model = llama_model_load_from_file(path, model_params);
  env->ReleaseStringUTFChars(modelPath, path);

  if (!model) {
    LOGE("Failed to load model");
    return 0;
  }

  LOGI("Model loaded successfully");
  return reinterpret_cast<jlong>(model);
}

JNIEXPORT jlong JNICALL
Java_com_abettergemini_assistant_LlamaCppBackend_nativeCreateContext(
    JNIEnv *env, jobject /* this */, jlong modelPtr, jint nCtx) {

  auto *model = reinterpret_cast<llama_model *>(modelPtr);
  llama_context_params ctx_params = llama_context_default_params();
  ctx_params.n_ctx = nCtx;
  ctx_params.n_batch = 512;

  llama_context *ctx = llama_init_from_model(model, ctx_params);
  if (!ctx) {
    LOGE("Failed to create context");
    return 0;
  }

  LOGI("Context created with n_ctx=%d", nCtx);
  return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jstring JNICALL
Java_com_abettergemini_assistant_LlamaCppBackend_nativeGenerate(
    JNIEnv *env, jobject /* this */, jlong contextPtr, jlong modelPtr,
    jstring prompt, jint maxTokens) {

  auto *ctx = reinterpret_cast<llama_context *>(contextPtr);
  auto *model = reinterpret_cast<llama_model *>(modelPtr);
  const char *promptStr = env->GetStringUTFChars(prompt, nullptr);

  // Tokenize the prompt
  const llama_vocab *vocab = llama_model_get_vocab(model);
  int n_prompt = strlen(promptStr);
  std::vector<llama_token> tokens(n_prompt + 16);
  int n_tokens = llama_tokenize(vocab, promptStr, n_prompt, tokens.data(),
                                tokens.size(), true, true);
  env->ReleaseStringUTFChars(prompt, promptStr);

  if (n_tokens < 0) {
    LOGE("Tokenization failed");
    return env->NewStringUTF("Error: tokenization failed");
  }
  tokens.resize(n_tokens);
  LOGI("Tokenized prompt: %d tokens", n_tokens);

  // Note: no KV cache clear API available in this llama.cpp version.
  // The prompt is re-evaluated from position 0 each call, which naturally
  // overwrites the relevant KV cache entries. Combined with repetition
  // penalty and time limits, this prevents loops.

  // Evaluate prompt tokens
  llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
  for (int i = 0; i < n_tokens; i++) {
    batch_add(batch, tokens[i], i, {0}, false);
  }
  batch.logits[batch.n_tokens - 1] = true;

  LOGI("Evaluating prompt...");
  if (llama_decode(ctx, batch) != 0) {
    LOGE("Decode failed during prompt evaluation");
    llama_batch_free(batch);
    return env->NewStringUTF("Error: decode failed");
  }
  LOGI("Prompt evaluated, starting generation...");

  // Generate tokens
  std::string result;
  llama_sampler *smpl =
      llama_sampler_chain_init(llama_sampler_chain_default_params());
  llama_sampler_chain_add(smpl, llama_sampler_init_top_k(40));
  llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
  llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
  llama_sampler_chain_add(smpl,
                          llama_sampler_init_penalties(64, 1.3f, 0.0f, 0.0f));
  llama_sampler_chain_add(smpl, llama_sampler_init_dist(42));

  int n_cur = n_tokens;
  auto start_time = std::chrono::steady_clock::now();
  const int TIME_LIMIT_SECS = 30;

  for (int i = 0; i < maxTokens; i++) {
    // Check time limit
    auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(
                       std::chrono::steady_clock::now() - start_time)
                       .count();
    if (elapsed > TIME_LIMIT_SECS) {
      LOGI("Time limit reached (%ds) at step %d", TIME_LIMIT_SECS, i);
      break;
    }

    llama_token new_token = llama_sampler_sample(smpl, ctx, -1);

    if (llama_vocab_is_eog(vocab, new_token)) {
      LOGI("EOS token at step %d", i);
      break;
    }

    char buf[256];
    int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, true);
    if (n > 0) {
      result.append(buf, n);
    }

    batch_clear(batch);
    batch_add(batch, new_token, n_cur, {0}, true);
    n_cur++;

    if (llama_decode(ctx, batch) != 0) {
      LOGE("Decode failed at step %d", i);
      break;
    }
  }

  llama_sampler_free(smpl);
  llama_batch_free(batch);

  LOGI("Generated %zu chars in %d steps", result.size(), n_cur - n_tokens);
  return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_abettergemini_assistant_LlamaCppBackend_nativeFreeModel(
    JNIEnv *env, jobject /* this */, jlong modelPtr) {
  auto *model = reinterpret_cast<llama_model *>(modelPtr);
  if (model) {
    llama_model_free(model);
    LOGI("Model freed");
  }
}

JNIEXPORT void JNICALL
Java_com_abettergemini_assistant_LlamaCppBackend_nativeFreeContext(
    JNIEnv *env, jobject /* this */, jlong contextPtr) {
  auto *ctx = reinterpret_cast<llama_context *>(contextPtr);
  if (ctx) {
    llama_free(ctx);
    LOGI("Context freed");
  }
}

} // extern "C"
