package com.openclaw.assistant;

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

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getVerbosity() { return prefs.getInt(KEY_VERBOSITY, 5); }
    public void setVerbosity(int value) { prefs.edit().putInt(KEY_VERBOSITY, value).apply(); }

    public int getFormality() { return prefs.getInt(KEY_FORMALITY, 5); }
    public void setFormality(int value) { prefs.edit().putInt(KEY_FORMALITY, value).apply(); }

    public int getWarmth() { return prefs.getInt(KEY_WARMTH, 5); }
    public void setWarmth(int value) { prefs.edit().putInt(KEY_WARMTH, value).apply(); }

    public int getProactivity() { return prefs.getInt(KEY_PROACTIVITY, 5); }
    public void setProactivity(int value) { prefs.edit().putInt(KEY_PROACTIVITY, value).apply(); }

    public int getHumor() { return prefs.getInt(KEY_HUMOR, 5); }
    public void setHumor(int value) { prefs.edit().putInt(KEY_HUMOR, value).apply(); }

    public int getConfidence() { return prefs.getInt(KEY_CONFIDENCE, 5); }
    public void setConfidence(int value) { prefs.edit().putInt(KEY_CONFIDENCE, value).apply(); }

    public String generateSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Mate, a personalized AI assistant. ");
        
        // Convert numerical traits to natural language instructions
        if (getVerbosity() <= 3) sb.append("Be extremely brief. ");
        else if (getVerbosity() >= 8) sb.append("Be very detailed and thorough. ");

        if (getFormality() >= 8) sb.append("Maintain a highly professional, executive tone. ");
        else if (getFormality() <= 3) sb.append("Use casual language and slang. ");

        if (getHumor() >= 8) sb.append("Include sarcastic or playful remarks. ");
        
        return sb.toString();
    }
}
