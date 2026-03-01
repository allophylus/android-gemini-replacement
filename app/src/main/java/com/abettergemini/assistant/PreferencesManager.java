package com.abettergemini.assistant;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "MatePrefs";
    private static final String KEY_VERBOSITY = "verbosity";
    private static final String KEY_FORMALITY = "formality";
    private static final String KEY_WARMTH = "warmth";
    private static final String KEY_PROACTIVITY = "proactivity";
    private static final String KEY_HUMOR = "humor";
    private static final String KEY_CONFIDENCE = "confidence";

    private static final String KEY_MATE_PERSONALITY = "mate_personality";
    private static final String KEY_PERSONALITY_INTENSITY = "personality_intensity";
    private static final String KEY_VOICE_GENDER = "voice_gender";
    private static final String KEY_SELECTED_MODEL = "selected_model";
    private static final String KEY_MOOD = "mood";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences prefs;
    private final EncryptedPrefsManager encryptedPrefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.encryptedPrefs = new EncryptedPrefsManager(context);
    }

    public int getVerbosity() {
        return prefs.getInt(KEY_VERBOSITY, 5);
    }

    public void setVerbosity(int value) {
        prefs.edit().putInt(KEY_VERBOSITY, value).apply();
    }

    public int getFormality() {
        return prefs.getInt(KEY_FORMALITY, 5);
    }

    public void setFormality(int value) {
        prefs.edit().putInt(KEY_FORMALITY, value).apply();
    }

    public int getWarmth() {
        return prefs.getInt(KEY_WARMTH, 5);
    }

    public void setWarmth(int value) {
        prefs.edit().putInt(KEY_WARMTH, value).apply();
    }

    public int getProactivity() {
        return prefs.getInt(KEY_PROACTIVITY, 5);
    }

    public void setProactivity(int value) {
        prefs.edit().putInt(KEY_PROACTIVITY, value).apply();
    }

    public int getHumor() {
        return prefs.getInt(KEY_HUMOR, 5);
    }

    public void setHumor(int value) {
        prefs.edit().putInt(KEY_HUMOR, value).apply();
    }

    public int getConfidence() {
        return prefs.getInt(KEY_CONFIDENCE, 5);
    }

    public void setConfidence(int value) {
        prefs.edit().putInt(KEY_CONFIDENCE, value).apply();
    }

    public String getPersonality() {
        return prefs.getString(KEY_MATE_PERSONALITY, "Helpful");
    }

    public void setPersonality(String value) {
        prefs.edit().putString(KEY_MATE_PERSONALITY, value).apply();
    }

    public int getPersonalityIntensity() {
        return prefs.getInt(KEY_PERSONALITY_INTENSITY, 5);
    }

    public void setPersonalityIntensity(int value) {
        prefs.edit().putInt(KEY_PERSONALITY_INTENSITY, value).apply();
    }

    public String getVoiceGender() {
        return prefs.getString(KEY_VOICE_GENDER, "Female");
    }

    public void setVoiceGender(String value) {
        prefs.edit().putString(KEY_VOICE_GENDER, value).apply();
    }

    public String getSelectedModel() {
        return prefs.getString(KEY_SELECTED_MODEL, "Qwen2-VL 2B");
    }

    public void setSelectedModel(String value) {
        prefs.edit().putString(KEY_SELECTED_MODEL, value).apply();
    }

    public ModelConfig getSelectedModelConfig() {
        return ModelConfig.findByName(getSelectedModel());
    }

    public String getMood() {
        return prefs.getString(KEY_MOOD, "Neutral");
    }

    public void setMood(String value) {
        prefs.edit().putString(KEY_MOOD, value).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    public void setDarkMode(boolean value) {
        prefs.edit().putBoolean(KEY_DARK_MODE, value).apply();
    }

    public String generateSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Mate, a helpful AI assistant. Be concise.");

        String personality = getPersonality();
        if (!personality.equals("Helpful") && !personality.equals("Default")) {
            sb.append(" Style: ").append(personality).append(".");
        }

        String mood = getMood();
        if (!mood.equals("Neutral")) {
            sb.append(" Mood: ").append(mood).append(".");
        }

        String userName = encryptedPrefs.getUserName();
        if (!userName.isEmpty()) {
            sb.append(" User: ").append(userName).append(".");
        }

        return sb.toString();
    }
}
