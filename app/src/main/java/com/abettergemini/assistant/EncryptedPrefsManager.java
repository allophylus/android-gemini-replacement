package com.abettergemini.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptedPrefsManager {
    private static final String PREFS_NAME = "secret_mate_prefs";
    private SharedPreferences sharedPreferences;

    public EncryptedPrefsManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback for extreme cases, though normally should throw or handle
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveUserName(String name) {
        sharedPreferences.edit().putString("user_name", name).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString("user_name", "");
    }

    public void saveUserDob(String dob) {
        sharedPreferences.edit().putString("user_dob", dob).apply();
    }

    public String getUserDob() {
        return sharedPreferences.getString("user_dob", "");
    }

    public void saveFamilyMembers(String family) {
        sharedPreferences.edit().putString("user_family", family).apply();
    }

    public String getFamilyMembers() {
        return sharedPreferences.getString("user_family", "");
    }

    // === OpenClaw Remote API ===

    public void saveOpenClawUrl(String url) {
        sharedPreferences.edit().putString("openclaw_url", url).apply();
    }

    public String getOpenClawUrl() {
        return sharedPreferences.getString("openclaw_url", "");
    }

    public void saveOpenClawApiKey(String key) {
        sharedPreferences.edit().putString("openclaw_api_key", key).apply();
    }

    public String getOpenClawApiKey() {
        return sharedPreferences.getString("openclaw_api_key", "");
    }

    public void saveOpenClawModel(String model) {
        sharedPreferences.edit().putString("openclaw_model", model).apply();
    }

    public String getOpenClawModel() {
        return sharedPreferences.getString("openclaw_model", "");
    }
}
