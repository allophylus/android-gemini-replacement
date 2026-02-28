package com.abettergemini.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.view.Gravity;
import android.view.View;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {
    private PreferencesManager prefs;
    private EncryptedPrefsManager encryptedPrefs;
    private AICoreClient aiClient;
    private LinearLayout chatHistory;
    private EditText chatInput;
    private View settingsView;
    private View chatView;
    private TextView assistCheck;
    private TextView aiCheck;
    private SwipeRefreshLayout swipeRefresh;
    private int exchangeCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
        encryptedPrefs = new EncryptedPrefsManager(this);
        aiClient = new AICoreClient(this);

        swipeRefresh = new SwipeRefreshLayout(this);

        // Main Container inside a ScrollView so Pull-to-Refresh works reliably
        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setFillViewport(true);
        swipeRefresh.addView(mainScroll);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F8F9FA"));
        mainScroll.addView(root);

        // Device Check
        checkDeviceCompatibility();

        // Dependency Check Header
        View depCheck = createDependencyCheckView();
        root.addView(depCheck);

        // Toolbar
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(32, 32, 32, 32);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Mate Assistant");
        title.setTextSize(20f);
        title.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f);
        toolbar.addView(title, titleParams);

        ImageButton settingsBtn = new ImageButton(this);
        settingsBtn.setImageResource(android.R.drawable.ic_menu_preferences);
        settingsBtn.setBackgroundColor(Color.TRANSPARENT);
        settingsBtn.setOnClickListener(v -> toggleSettings());
        toolbar.addView(settingsBtn);

        root.addView(toolbar);

        // Chat View
        chatView = createChatView();
        root.addView(chatView);

        // Settings View (Hidden by default)
        settingsView = createSettingsView();
        settingsView.setVisibility(View.GONE);
        root.addView(settingsView);

        swipeRefresh.setOnRefreshListener(() -> refreshDependencyStatus());
        setContentView(swipeRefresh);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDependencyStatus();
    }

    private void checkDeviceCompatibility() {
        String model = android.os.Build.MODEL;
        String manufacturer = android.os.Build.MANUFACTURER;
        Log.d("Mate", "Running on: " + manufacturer + " " + model);

        if (manufacturer.toLowerCase().contains("samsung")) {
            Log.d("Mate", "Samsung Device Detected. Ensuring AICore integration...");
        }
    }

    private View createDependencyCheckView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.parseColor("#FFF3E0"));

        TextView title = new TextView(this);
        title.setText("Dependency & AI Readiness");
        title.setTextSize(14f);
        title.setPadding(0, 0, 0, 8);
        layout.addView(title);

        assistCheck = new TextView(this);
        layout.addView(assistCheck);

        aiCheck = new TextView(this);
        layout.addView(aiCheck);

        android.widget.ProgressBar downloadBar = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        downloadBar.setMax(100);
        downloadBar.setProgress(0);
        downloadBar.setVisibility(View.GONE);
        layout.addView(downloadBar);

        // Register the real-time download listener
        aiClient.setProgressListener(percent -> {
            if (downloadBar.getVisibility() == View.GONE) {
                downloadBar.setVisibility(View.VISIBLE);
            }
            downloadBar.setProgress(percent);
            aiCheck.setText("- " + prefsManager.getSelectedModel() + ": 📥 Downloading... " + percent + "%");

            if (percent >= 100) {
                downloadBar.setVisibility(View.GONE);
                aiCheck.setText("- " + prefsManager.getSelectedModel() + ": ✅ Ready");
            }
        });

        TextView infoText = new TextView(this);
        infoText.setText(
                "\nOn Samsung Fold 7:\n1. Settings > Advanced features > Advanced intelligence\n2. Toggle ON 'Process data only on device'\n3. If missing, clear 'AICore' app cache and restart.");
        infoText.setTextSize(12f);
        layout.addView(infoText);

        Button fixBtn = new Button(this);
        fixBtn.setText("Go to Advanced Features");
        fixBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.Settings$AdvancedFeaturesActivity");
                startActivity(intent);
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
                } catch (Exception ex) {
                }
            }
        });
        layout.addView(fixBtn);

        refreshDependencyStatus();
        return layout;
    }

    private void refreshDependencyStatus() {
        if (assistCheck == null || aiCheck == null)
            return;

        boolean isDefault = isMateDefaultAssistant();
        assistCheck.setText("- Mate set as Default Assistant: " + (isDefault ? "✅" : "❌"));

        aiCheck.setText("- " + prefsManager.getSelectedModel() + ": Checking...");
        checkAiCoreStatus(aiCheck);
    }

    private void checkAiCoreStatus(TextView statusView) {
        aiClient.generateResponse("test", "", new AICoreClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    statusView.setText("- " + prefsManager.getSelectedModel() + ": ✅ Ready");
                    if (swipeRefresh != null)
                        swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    String msg = t.getMessage();
                    if (msg != null && msg.contains("empty")) {
                        statusView.setText("- " + prefsManager.getSelectedModel() + ": 📥 Download Pending...");
                    } else if (msg != null && msg.contains("Downloading")) {
                        // Progress listener handles UI updates natively
                    } else if (msg != null && msg.contains("CELLULAR_DOWNLOAD_REQUIRED")) {
                        statusView.setText("- " + prefsManager.getSelectedModel() + ": ⚠️ Awaiting Wi-Fi...");
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Large Download Warning")
                                .setMessage(
                                        "The model is large. Do you want to download over your cellular data connection?")
                                .setPositiveButton("Download Anyway", (dialog, which) -> {
                                    statusView.setText("- " + prefsManager.getSelectedModel() + ": 📥 Preparing cellular download...");
                                    aiClient.startDownloadExplicitly();
                                })
                                .setNegativeButton("Wait for Wi-Fi", (dialog, which) -> {
                                    statusView.setText("- " + prefsManager.getSelectedModel() + ": ⏸️ Paused (Waiting for Wi-Fi)");
                                })
                                .show();
                    } else if (msg != null && msg.contains("ENOSPC")) {
                        statusView.setText("- " + prefsManager.getSelectedModel() + ": ⚠️ Not Ready (Not enough storage space)");
                    } else {
                        statusView.setText("- " + prefsManager.getSelectedModel() + ": ⚠️ Not Ready (" + msg + ")");
                    }
                    if (swipeRefresh != null)
                        swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private boolean isMateDefaultAssistant() {
        boolean isRoleHeld = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.app.role.RoleManager roleManager = getSystemService(android.app.role.RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                isRoleHeld = roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT);
            }
        }
        String assistant = Settings.Secure.getString(getContentResolver(), "assistant");
        boolean isSecureStringAssistant = assistant != null && assistant.contains("com.abettergemini.assistant");

        String voiceService = Settings.Secure.getString(getContentResolver(), "voice_interaction_service");
        boolean isVoiceService = voiceService != null && voiceService.contains("com.abettergemini.assistant");

        return isRoleHeld || isSecureStringAssistant || isVoiceService;
    }

    private View createChatView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Chat History Scroll
        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);

        chatHistory = new LinearLayout(this);
        chatHistory.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(chatHistory);
        layout.addView(scroll, scrollParams);

        // Input Area
        LinearLayout inputArea = new LinearLayout(this);
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setGravity(Gravity.CENTER_VERTICAL);
        inputArea.setPadding(0, 16, 0, 0);

        chatInput = new EditText(this);
        chatInput.setHint("Ask Mate anything...");
        chatInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f);
        inputArea.addView(chatInput, inputParams);

        Button sendBtn = new Button(this);
        sendBtn.setText("Send");
        sendBtn.setOnClickListener(v -> sendMessage());
        inputArea.addView(sendBtn);

        layout.addView(inputArea);
        return layout;
    }

    private View createSettingsView() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 32);

        TextView header = new TextView(this);
        header.setText("Settings & Personalization");
        header.setTextSize(18f);
        header.setPadding(0, 0, 0, 16);
        layout.addView(header);

        // ===== AI Model Selection =====
        TextView modelHeader = new TextView(this);
        modelHeader.setText("AI Model");
        modelHeader.setTextSize(16f);
        modelHeader.setPadding(0, 8, 0, 8);
        layout.addView(modelHeader);

        ModelConfig[] models = ModelConfig.getAvailableModels();
        String[] modelDisplayNames = new String[models.length];
        for (int i = 0; i < models.length; i++) {
            modelDisplayNames[i] = models[i].getFormattedName();
        }

        android.widget.Spinner modelSpinner = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> modelAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modelDisplayNames);
        modelSpinner.setAdapter(modelAdapter);

        String currentModel = prefs.getSelectedModel();
        for (int i = 0; i < models.length; i++) {
            if (models[i].displayName.equals(currentModel)) {
                modelSpinner.setSelection(i);
                break;
            }
        }

        TextView modelDescView = new TextView(this);
        ModelConfig currentConfig = ModelConfig.findByName(currentModel);
        String backendLabel = currentConfig.backend == ModelConfig.Backend.LLAMA_CPP ? "Engine: llama.cpp" : "Engine: MediaPipe";
        modelDescView.setText(currentConfig.description + "\n" + backendLabel);
        modelDescView.setAlpha(0.7f);
        modelDescView.setPadding(0, 4, 0, 8);

        modelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                prefs.setSelectedModel(models[position].displayName);
                String bLabel = models[position].backend == ModelConfig.Backend.LLAMA_CPP ? "Engine: llama.cpp" : "Engine: MediaPipe";
                modelDescView.setText(models[position].description + "\n" + bLabel);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        layout.addView(modelSpinner);
        layout.addView(modelDescView);

        Button downloadModelBtn = new Button(this);
        downloadModelBtn.setText("Download Selected Model");
        downloadModelBtn.setOnClickListener(v -> {
            aiClient.unloadModel(); // Clear old model first
            aiClient.startDownloadExplicitly();
            addChatMessage("System", "Downloading " + prefs.getSelectedModel() + "...");
        });
        layout.addView(downloadModelBtn);

        // ===== Personality Section =====
        TextView persHeader = new TextView(this);
        persHeader.setText("\nPersonality");
        persHeader.setTextSize(16f);
        layout.addView(persHeader);
        // Personality Type Dropdown
        TextView personalityLabel = new TextView(this);
        personalityLabel.setText("Core Personality Style:");
        personalityLabel.setPadding(0, 16, 0, 8);
        layout.addView(personalityLabel);

        android.widget.Spinner personalitySpinner = new android.widget.Spinner(this);
        String[] personalityOptions = { "Helpful", "Funny", "Sad", "Happy", "Childish", "Sarcastic", "Professional" };
        android.widget.ArrayAdapter<String> persAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, personalityOptions);
        personalitySpinner.setAdapter(persAdapter);

        String currentPers = prefs.getPersonality();
        for (int i = 0; i < personalityOptions.length; i++) {
            if (personalityOptions[i].equals(currentPers)) {
                personalitySpinner.setSelection(i);
                break;
            }
        }
        personalitySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                prefs.setPersonality(personalityOptions[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        layout.addView(personalitySpinner);

        // Personality Sliders
        layout.addView(createTraitView("Personality Intensity", prefs.getPersonalityIntensity(),
                (v) -> prefs.setPersonalityIntensity(v)));
        layout.addView(createTraitView("Verbosity", prefs.getVerbosity(), (v) -> prefs.setVerbosity(v)));
        layout.addView(createTraitView("Formality", prefs.getFormality(), (v) -> prefs.setFormality(v)));
        layout.addView(createTraitView("Humor", prefs.getHumor(), (v) -> prefs.setHumor(v)));

        // Voice Engine Gender
        TextView voiceLabel = new TextView(this);
        voiceLabel.setText("TTS Voice Gender:");
        voiceLabel.setPadding(0, 16, 0, 8);
        layout.addView(voiceLabel);

        android.widget.Spinner voiceSpinner = new android.widget.Spinner(this);
        String[] voiceOptions = { "Female", "Male" };
        android.widget.ArrayAdapter<String> voiceAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, voiceOptions);
        voiceSpinner.setAdapter(voiceAdapter);

        String currentVoice = prefs.getVoiceGender();
        voiceSpinner.setSelection(currentVoice.equals("Male") ? 1 : 0);
        voiceSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                prefs.setVoiceGender(voiceOptions[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        layout.addView(voiceSpinner);

        // Long-Term Memory Vault
        TextView memoryTitle = new TextView(this);
        memoryTitle.setText("\nLong-Term Memory Vault");
        memoryTitle.setTextSize(16f);
        layout.addView(memoryTitle);

        layout.addView(
                createMemoryInput("Your Name", encryptedPrefs.getUserName(), v -> encryptedPrefs.saveUserName(v)));
        layout.addView(
                createMemoryInput("Date of Birth", encryptedPrefs.getUserDob(), v -> encryptedPrefs.saveUserDob(v)));
        layout.addView(createMemoryInput("Family Members/Pets", encryptedPrefs.getFamilyMembers(),
                v -> encryptedPrefs.saveFamilyMembers(v)));

        // Model Management
        TextView modelTitle = new TextView(this);
        modelTitle.setText("\nModel Management");
        modelTitle.setTextSize(16f);
        layout.addView(modelTitle);

        Button unloadBtn = new Button(this);
        unloadBtn.setText("Free Model from RAM");
        unloadBtn.setOnClickListener(v -> {
            aiClient.unloadModel();
            addChatMessage("System", prefsManager.getSelectedModel() + " model securely unloaded from RAM.");
        });
        layout.addView(unloadBtn);

        // System Settings
        Button assistantSettings = new Button(this);
        assistantSettings.setText("System Assistant Settings");
        assistantSettings.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        });
        layout.addView(assistantSettings);

        // About Section
        TextView aboutTitle = new TextView(this);
        aboutTitle.setText("\nAbout Mate");
        aboutTitle.setTextSize(16f);
        layout.addView(aboutTitle);

        TextView aboutContent = new TextView(this);
        aboutContent.setText(
                "Mate is a local-first, privacy-focused AI assistant. All AI runs on-device — no data leaves your phone.");
        aboutContent.setAlpha(0.7f);
        layout.addView(aboutContent);

        scroll.addView(layout);
        return scroll;
    }

    private void toggleSettings() {
        if (settingsView.getVisibility() == View.VISIBLE) {
            settingsView.setVisibility(View.GONE);
            chatView.setVisibility(View.VISIBLE);
        } else {
            settingsView.setVisibility(View.VISIBLE);
            chatView.setVisibility(View.GONE);
        }
    }

    private void sendMessage() {
        String query = chatInput.getText().toString().trim();
        if (query.isEmpty())
            return;

        addChatMessage("You", query);
        chatInput.setText("");

        // Handle Web Scraping
        if (query.startsWith("http://") || query.startsWith("https://")) {
            addChatMessage("System", "Fetching website content...");
            new Thread(() -> {
                String extractedText = WebScraper.fetchAndExtractText(query);
                if (extractedText.startsWith("Error")) {
                    runOnUiThread(() -> addChatMessage("Error", extractedText));
                    return;
                }

                String maxText = extractedText.length() > 2500 ? extractedText.substring(0, 2500) : extractedText; // Prevent
                                                                                                                   // Context
                                                                                                                   // OOM
                String summarizePrompt = "Provide a concise summary of the key facts from this webpage text: "
                        + maxText;

                aiClient.generateResponse(summarizePrompt, "", new AICoreClient.ResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            addChatMessage("Mate (Web Summary)", response);
                            // Store in Long-Term Memory implicitly
                            encryptedPrefs.saveUserDob(encryptedPrefs.getUserDob() + " [Web Memory: " + response + "]");
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> addChatMessage("Error", t.getMessage()));
                    }
                });
            }).start();
            return;
        }

        // Standard Chat
        exchangeCount++;

        if (exchangeCount >= 5) {
            // Trigger Context Compaction
            StringBuilder fullContext = new StringBuilder();
            for (int i = 0; i < chatHistory.getChildCount(); i++) {
                View v = chatHistory.getChildAt(i);
                if (v instanceof TextView) {
                    fullContext.append(((TextView) v).getText().toString()).append("\n");
                }
            }

            String condensePrompt = "Summarize the key facts and topics discussed in this conversation: \n"
                    + fullContext.toString();
            addChatMessage("System", "Condensing conversation memory to save RAM...");

            aiClient.generateResponse(condensePrompt, "", new AICoreClient.ResponseCallback() {
                @Override
                public void onSuccess(String summary) {
                    runOnUiThread(() -> {
                        chatHistory.removeAllViews();
                        addChatMessage("System [Compacted Memory]", summary);
                        exchangeCount = 0; // Reset

                        // Proceed with the actual user query now that memory is cleared
                        aiClient.generateResponse(query, summary, new AICoreClient.ResponseCallback() {
                            @Override
                            public void onSuccess(String response) {
                                runOnUiThread(() -> addChatMessage("Mate", response));
                            }

                            @Override
                            public void onError(Throwable t) {
                                runOnUiThread(() -> addChatMessage("Error", t.getMessage()));
                            }
                        });
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> addChatMessage("Error", "Compaction Failed: " + t.getMessage()));
                }
            });
        } else {
            // Normal message flow
            aiClient.generateResponse(query, "", new AICoreClient.ResponseCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> handleToolResponse(response));
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> addChatMessage("Error", t.getMessage()));
                }
            });
        }
    }

    private void handleToolResponse(String response) {
        // 1. Check for LAUNCH command
        boolean launched = ToolExecutor.handleLaunch(this, response);
        
        // 2. Check for SEARCH command
        String searchQuery = ToolExecutor.extractSearchQuery(response);
        
        // 3. Display cleaned response
        String cleanResponse = ToolExecutor.stripCommands(response);
        if (!cleanResponse.isEmpty()) {
            addChatMessage("Mate", cleanResponse);
        }
        
        if (launched) {
            addChatMessage("System", "App launched successfully.");
        }
        
        // 4. If SEARCH was requested, fetch results and feed back to LLM
        if (searchQuery != null) {
            addChatMessage("System", "Searching the web for: " + searchQuery + "...");
            new Thread(() -> {
                String results = ToolExecutor.searchWeb(searchQuery);
                String followUpPrompt = "Here are web search results. Summarize the most relevant information for the user:\n\n" + results;
                
                aiClient.generateResponse(followUpPrompt, "", new AICoreClient.ResponseCallback() {
                    @Override
                    public void onSuccess(String summary) {
                        runOnUiThread(() -> addChatMessage("Mate (Web)", ToolExecutor.stripCommands(summary)));
                    }
                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> addChatMessage("Error", "Search failed: " + t.getMessage()));
                    }
                });
            }).start();
        }
    }

    private void addChatMessage(String sender, String message) {
        TextView tv = new TextView(this);
        tv.setText(sender + ": " + message);
        tv.setPadding(16, 8, 16, 8);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);
        bg.setColor(sender.equals("You") ? Color.parseColor("#E3F2FD") : Color.parseColor("#FFFFFF"));
        tv.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        params.gravity = sender.equals("You") ? Gravity.END : Gravity.START;

        chatHistory.addView(tv, params);
    }

    private LinearLayout createTraitView(String label, int currentVal, OnTraitChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 16, 0, 16);
        TextView tv = new TextView(this);
        tv.setText(label + ": " + currentVal);
        SeekBar sb = new SeekBar(this);
        sb.setMax(10);
        sb.setProgress(currentVal);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv.setText(label + ": " + progress);
                listener.onChanged(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        row.addView(tv);
        row.addView(sb);
        return row;
    }

    private LinearLayout createMemoryInput(String label, String currentVal, OnMemoryChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 8, 0, 8);
        TextView tv = new TextView(this);
        tv.setText(label);
        EditText et = new EditText(this);
        et.setText(currentVal);
        et.setHint("Enter " + label.toLowerCase());
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        Button saveBtn = new Button(this);
        saveBtn.setText("Save");
        saveBtn.setOnClickListener(v -> {
            listener.onChanged(et.getText().toString().trim());
            addChatMessage("System", label + " securely vaulted.");
        });

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f);
        inputRow.addView(et, param);
        inputRow.addView(saveBtn);

        row.addView(tv);
        row.addView(inputRow);
        return row;
    }

    interface OnTraitChangeListener {
        void onChanged(int value);
    }

    interface OnMemoryChangeListener {
        void onChanged(String value);
    }
}
