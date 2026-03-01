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
        return prefs.getString(KEY_SELECTED_MODEL, "SmolVLM 500M");
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

    public String generateSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant. ");

        String personality = getPersonality();
        int intensity = getPersonalityIntensity();

        if (!personality.equals("Helpful") && !personality.equals("Default")) {
            sb.append("Your core personality is ").append(personality).append(". ");
            if (intensity >= 8) {
                sb.append("You must exhibit this personality trait extremely strongly in every single sentence. ");
            } else if (intensity <= 3) {
                sb.append("You should only show subtle, slight hints of this personality. ");
            } else {
                sb.append("Display this personality clearly but naturally. ");
            }
        }

        String userName = encryptedPrefs.getUserName();
        String userDob = encryptedPrefs.getUserDob();
        String userFamily = encryptedPrefs.getFamilyMembers();

        if (!userName.isEmpty() || !userDob.isEmpty() || !userFamily.isEmpty()) {
            sb.append("LONG-TERM MEMORY CONTEXT: The user you are talking to is named '").append(userName)
                    .append("'. ");
            if (!userDob.isEmpty())
                sb.append("Their birthday/age is '").append(userDob).append("'. ");
            if (!userFamily.isEmpty())
                sb.append("Their close family/pets are '").append(userFamily).append("'. ");
            sb.append("Always remember these facts about the user. ");
        }

        // Convert numerical traits to natural language instructions
        if (getVerbosity() <= 3)
            sb.append("Be extremely brief. ");
        else if (getVerbosity() >= 8)
            sb.append("Be very detailed and thorough. ");

        if (getFormality() >= 8)
            sb.append("Maintain a highly professional, executive tone. ");
        else if (getFormality() <= 3)
            sb.append("Use casual language and slang. ");

        if (getHumor() >= 8)
            sb.append("Include sarcastic or playful remarks. ");

        String mood = getMood();
        if (!mood.equals("Neutral")) {
            sb.append("Your current mood is ").append(mood).append(". Let this influence your tone. ");
        }

        // Tool Use Instructions (kept short to reduce prompt tokens)
        sb.append("\nTools: [LAUNCH:app name] to open apps, [SEARCH:query] to search web. Only use when asked.");

        return sb.toString();
    }
}
