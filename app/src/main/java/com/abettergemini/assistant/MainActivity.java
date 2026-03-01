package com.abettergemini.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;

public class MainActivity extends Activity {
    // Core
    private PreferencesManager prefs;
    private EncryptedPrefsManager encryptedPrefs;
    private AICoreClient aiClient;
    private ThemeManager theme;

    // UI refs
    private LinearLayout chatHistory;
    private EditText chatInput;
    private View settingsView;
    private View chatView;
    private TextView statusText;
    private android.widget.ProgressBar statusProgress;
    private int exchangeCount = 0;

    // Auto-refresh
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private boolean autoRefreshRunning = false;
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatusBar();
            if (!aiClient.isModelReady()) {
                refreshHandler.postDelayed(this, 5000);
            } else {
                autoRefreshRunning = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
        encryptedPrefs = new EncryptedPrefsManager(this);
        aiClient = new AICoreClient(this);
        theme = new ThemeManager(prefs.isDarkMode());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.bg());

        // === Header Bar ===
        root.addView(createHeaderBar());

        // === Status Bar ===
        root.addView(createStatusBar());

        // === Chat View (default) ===
        chatView = createChatView();
        root.addView(chatView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // === Settings View (hidden) ===
        settingsView = createSettingsView();
        settingsView.setVisibility(View.GONE);
        root.addView(settingsView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        setContentView(root);
        startAutoRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusBar();
        if (!aiClient.isModelReady()) startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    // ========== HEADER BAR ==========

    private View createHeaderBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(20), dp(16), dp(20), dp(12));
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(theme.surface());

        // App title
        TextView title = new TextView(this);
        title.setText("Mate");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTextColor(theme.headerText());
        title.setTypeface(null, Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Sun/Moon toggle
        TextView themeBtn = new TextView(this);
        themeBtn.setText(theme.isDarkMode() ? "☀️" : "🌙");
        themeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        themeBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        themeBtn.setOnClickListener(v -> {
            prefs.setDarkMode(!prefs.isDarkMode());
            recreate();
        });
        bar.addView(themeBtn);

        // Gear icon
        ImageButton gearBtn = new ImageButton(this);
        gearBtn.setImageResource(android.R.drawable.ic_menu_preferences);
        gearBtn.setColorFilter(theme.textDim());
        gearBtn.setBackgroundColor(Color.TRANSPARENT);
        gearBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        gearBtn.setOnClickListener(v -> toggleSettings());
        bar.addView(gearBtn);

        return bar;
    }

    // ========== STATUS BAR ==========

    private View createStatusBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(20), dp(10), dp(20), dp(10));
        bar.setBackgroundColor(theme.surface());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Model name chip
        TextView modelChip = new TextView(this);
        modelChip.setText(prefs.getSelectedModel());
        modelChip.setTextColor(ThemeManager.ACCENT);
        modelChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        modelChip.setTypeface(null, Typeface.BOLD);
        modelChip.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setCornerRadius(dp(12));
        chipBg.setColor(theme.chipBg());
        chipBg.setStroke(1, ThemeManager.ACCENT);
        modelChip.setBackground(chipBg);
        row.addView(modelChip);

        // Spacer
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

        // Status text
        statusText = new TextView(this);
        statusText.setTextColor(theme.textDim());
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        row.addView(statusText);

        bar.addView(row);

        // Download progress bar
        statusProgress = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        statusProgress.setMax(100);
        statusProgress.setProgress(0);
        statusProgress.setVisibility(View.GONE);
        statusProgress.setPadding(0, dp(6), 0, 0);
        bar.addView(statusProgress);

        // Register download listener
        aiClient.setProgressListener(percent -> {
            statusProgress.setVisibility(View.VISIBLE);
            statusProgress.setProgress(percent);
            statusText.setText("📥 " + percent + "%");
            modelChip.setText(prefs.getSelectedModel());
            if (percent >= 100) {
                statusProgress.setVisibility(View.GONE);
                statusText.setText("⚙️ Loading...");
            }
        });

        // Thin separator line
        View sep = new View(this);
        sep.setBackgroundColor(theme.separator());
        bar.addView(sep, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        updateStatusBar();
        return bar;
    }

    private void updateStatusBar() {
        if (statusText != null) {
            statusText.setText(aiClient.getStatusText());
        }
    }

    private void startAutoRefresh() {
        if (autoRefreshRunning) return;
        autoRefreshRunning = true;
        refreshHandler.postDelayed(autoRefreshRunnable, 5000);
    }

    private void stopAutoRefresh() {
        autoRefreshRunning = false;
        refreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    // ========== CHAT VIEW ==========

    private View createChatView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Chat history scroll
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        chatHistory = new LinearLayout(this);
        chatHistory.setOrientation(LinearLayout.VERTICAL);
        chatHistory.setPadding(dp(16), dp(12), dp(16), dp(12));
        scroll.addView(chatHistory);
        layout.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // Input area
        LinearLayout inputArea = new LinearLayout(this);
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setGravity(Gravity.CENTER_VERTICAL);
        inputArea.setPadding(dp(12), dp(8), dp(12), dp(12));
        inputArea.setBackgroundColor(theme.surface());

        chatInput = new EditText(this);
        chatInput.setHint("Ask Mate anything...");
        chatInput.setHintTextColor(theme.inputHint());
        chatInput.setTextColor(theme.text());
        chatInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        chatInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        chatInput.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dp(24));
        inputBg.setColor(theme.inputBg());
        chatInput.setBackground(inputBg);
        inputArea.addView(chatInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Send button
        TextView sendBtn = new TextView(this);
        sendBtn.setText("➤");
        sendBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setGravity(Gravity.CENTER);
        int btnSize = dp(44);
        GradientDrawable sendBg = new GradientDrawable();
        sendBg.setShape(GradientDrawable.OVAL);
        sendBg.setColor(ThemeManager.ACCENT);
        sendBtn.setBackground(sendBg);
        sendBtn.setOnClickListener(v -> sendMessage());
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(btnSize, btnSize);
        sendParams.setMargins(dp(8), 0, 0, 0);
        inputArea.addView(sendBtn, sendParams);

        layout.addView(inputArea);
        return layout;
    }

    // ========== SETTINGS VIEW ==========

    private View createSettingsView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(32));

        // Back button
        TextView backBtn = new TextView(this);
        backBtn.setText("← Back to Chat");
        backBtn.setTextColor(ThemeManager.ACCENT);
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backBtn.setPadding(dp(4), dp(8), dp(8), dp(16));
        backBtn.setOnClickListener(v -> toggleSettings());
        layout.addView(backBtn);

        // Card 1: AI Model
        layout.addView(createModelCard());

        // Card 2: Personality & Mood
        layout.addView(createPersonalityCard());

        // Card 3: Voice
        layout.addView(createVoiceCard());

        // Card 4: Memory Vault
        layout.addView(createMemoryCard());

        // Card 5: Advanced
        layout.addView(createAdvancedCard());

        scroll.addView(layout);
        return scroll;
    }

    private View createModelCard() {
        LinearLayout card = createCard("🤖  AI Model");

        ModelConfig[] models = ModelConfig.getAvailableModels();
        String[] names = new String[models.length];
        for (int i = 0; i < models.length; i++) {
            names[i] = models[i].getFormattedName();
        }

        android.widget.Spinner spinner = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names);
        spinner.setAdapter(adapter);

        String current = prefs.getSelectedModel();
        for (int i = 0; i < models.length; i++) {
            if (models[i].displayName.equals(current)) {
                spinner.setSelection(i);
                break;
            }
        }

        TextView modelInfo = new TextView(this);
        ModelConfig currentConfig = ModelConfig.findByName(current);
        modelInfo.setText(currentConfig.description + "\n" + 
                (currentConfig.backend == ModelConfig.Backend.LLAMA_CPP ? "Engine: llama.cpp" : "Engine: MediaPipe") +
                " • " + currentConfig.sizeLabel);
        modelInfo.setTextColor(theme.textDim());
        modelInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        modelInfo.setPadding(0, dp(4), 0, dp(8));

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                String newModel = models[pos].displayName;
                String oldModel = prefs.getSelectedModel();
                prefs.setSelectedModel(newModel);
                modelInfo.setText(models[pos].description + "\n" +
                        (models[pos].backend == ModelConfig.Backend.LLAMA_CPP ? "Engine: llama.cpp" : "Engine: MediaPipe") +
                        " • " + models[pos].sizeLabel);
                if (!newModel.equals(oldModel)) {
                    aiClient.switchModel();
                    addChatMessage("System", "Switching to " + newModel + "...");
                    startAutoRefresh();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        card.addView(spinner);
        card.addView(modelInfo);

        // Download button
        TextView dlBtn = createActionButton("Download / Reload Model");
        dlBtn.setOnClickListener(v -> {
            aiClient.switchModel();
            addChatMessage("System", "Loading " + prefs.getSelectedModel() + "...");
            startAutoRefresh();
        });
        card.addView(dlBtn);

        return card;
    }

    private View createPersonalityCard() {
        LinearLayout card = createCard("🎭  Personality & Mood");

        // Personality type
        addLabel(card, "Core Personality");
        android.widget.Spinner persSpinner = new android.widget.Spinner(this);
        String[] opts = {"Helpful", "Funny", "Sad", "Happy", "Childish", "Sarcastic", "Professional"};
        persSpinner.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opts));
        String currentPers = prefs.getPersonality();
        for (int i = 0; i < opts.length; i++) {
            if (opts[i].equals(currentPers)) { persSpinner.setSelection(i); break; }
        }
        persSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                prefs.setPersonality(opts[pos]);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        card.addView(persSpinner);

        // Mood dropdown
        addLabel(card, "Current Mood");
        android.widget.Spinner moodSpinner = new android.widget.Spinner(this);
        String[] moods = {"Neutral", "Cheerful", "Pensive", "Energetic", "Calm", "Playful"};
        moodSpinner.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, moods));
        String currentMood = prefs.getMood();
        for (int i = 0; i < moods.length; i++) {
            if (moods[i].equals(currentMood)) { moodSpinner.setSelection(i); break; }
        }
        moodSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                prefs.setMood(moods[pos]);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        card.addView(moodSpinner);

        // Sliders
        card.addView(createSlider("Intensity", prefs.getPersonalityIntensity(), v -> prefs.setPersonalityIntensity(v)));
        card.addView(createSlider("Verbosity", prefs.getVerbosity(), v -> prefs.setVerbosity(v)));
        card.addView(createSlider("Formality", prefs.getFormality(), v -> prefs.setFormality(v)));
        card.addView(createSlider("Humor", prefs.getHumor(), v -> prefs.setHumor(v)));

        return card;
    }

    private View createVoiceCard() {
        LinearLayout card = createCard("🔊  Voice Output");

        addLabel(card, "TTS Voice");
        android.widget.Spinner vSpinner = new android.widget.Spinner(this);
        String[] vOpts = {"Female", "Male"};
        vSpinner.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, vOpts));
        vSpinner.setSelection(prefs.getVoiceGender().equals("Male") ? 1 : 0);
        vSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                prefs.setVoiceGender(vOpts[pos]);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        card.addView(vSpinner);

        return card;
    }

    private View createMemoryCard() {
        LinearLayout card = createCard("🔒  Memory Vault (Encrypted)");

        card.addView(createMemoryInput("Your Name", encryptedPrefs.getUserName(),
                v -> encryptedPrefs.saveUserName(v)));
        card.addView(createMemoryInput("Date of Birth", encryptedPrefs.getUserDob(),
                v -> encryptedPrefs.saveUserDob(v)));
        card.addView(createMemoryInput("Family / Pets", encryptedPrefs.getFamilyMembers(),
                v -> encryptedPrefs.saveFamilyMembers(v)));

        return card;
    }

    private View createAdvancedCard() {
        LinearLayout card = createCard("⚙️  Advanced");

        TextView freeBtn = createActionButton("Free Model from RAM");
        freeBtn.setOnClickListener(v -> {
            aiClient.unloadModel();
            addChatMessage("System", prefs.getSelectedModel() + " unloaded from RAM.");
            updateStatusBar();
        });
        card.addView(freeBtn);

        TextView sysBtn = createActionButton("System Assistant Settings");
        sysBtn.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)); }
            catch (Exception ignored) {}
        });
        card.addView(sysBtn);

        // About
        TextView about = new TextView(this);
        about.setText("\nMate — local-first, privacy-focused AI.\nAll inference runs 100% on-device.");
        about.setTextColor(theme.textDim());
        about.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        about.setPadding(0, dp(8), 0, 0);
        card.addView(about);

        return card;
    }

    // ========== UI HELPERS ==========

    private LinearLayout createCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(theme.card());
        card.setBackground(bg);
        card.setElevation(dp(2));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(params);

        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(theme.headerText());
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(0, 0, 0, dp(8));
        card.addView(header);

        return card;
    }

    private void addLabel(LinearLayout parent, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(theme.textDim());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setPadding(0, dp(10), 0, dp(2));
        parent.addView(label);
    }

    private TextView createActionButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(ThemeManager.ACCENT);
        btn.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(8), 0, dp(4));
        btn.setLayoutParams(p);
        return btn;
    }

    private View createSlider(String label, int current, OnTraitChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(4));

        TextView tv = new TextView(this);
        tv.setText(label + ": " + current);
        tv.setTextColor(theme.text());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        SeekBar sb = new SeekBar(this);
        sb.setMax(10);
        sb.setProgress(current);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                tv.setText(label + ": " + progress);
                listener.onChanged(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        row.addView(tv);
        row.addView(sb);
        return row;
    }

    private LinearLayout createMemoryInput(String label, String currentVal, OnMemoryChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(theme.textDim());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        EditText et = new EditText(this);
        et.setText(currentVal);
        et.setHint("Enter " + label.toLowerCase());
        et.setHintTextColor(theme.inputHint());
        et.setTextColor(theme.text());
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        et.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable etBg = new GradientDrawable();
        etBg.setCornerRadius(dp(8));
        etBg.setColor(theme.inputBg());
        et.setBackground(etBg);

        TextView saveBtn = new TextView(this);
        saveBtn.setText("Save");
        saveBtn.setTextColor(ThemeManager.ACCENT);
        saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        saveBtn.setTypeface(null, Typeface.BOLD);
        saveBtn.setPadding(dp(8), dp(6), dp(8), dp(6));
        saveBtn.setOnClickListener(v -> {
            listener.onChanged(et.getText().toString().trim());
            addChatMessage("System", label + " saved securely.");
        });

        row.addView(tv);
        row.addView(et);
        row.addView(saveBtn);
        return row;
    }

    // ========== TOGGLE ==========

    private void toggleSettings() {
        if (settingsView.getVisibility() == View.VISIBLE) {
            settingsView.setVisibility(View.GONE);
            chatView.setVisibility(View.VISIBLE);
        } else {
            settingsView.setVisibility(View.VISIBLE);
            chatView.setVisibility(View.GONE);
        }
    }

    // ========== CHAT LOGIC ==========

    private void sendMessage() {
        String query = chatInput.getText().toString().trim();
        if (query.isEmpty()) return;
        addChatMessage("You", query);
        chatInput.setText("");

        // Web URL handling
        if (query.startsWith("http://") || query.startsWith("https://")) {
            addChatMessage("System", "Fetching website...");
            new Thread(() -> {
                String text = WebScraper.fetchAndExtractText(query);
                if (text.startsWith("Error")) {
                    runOnUiThread(() -> addChatMessage("Error", text));
                    return;
                }
                String maxText = text.length() > 2500 ? text.substring(0, 2500) : text;
                aiClient.generateResponse("Summarize: " + maxText, "", new AICoreClient.ResponseCallback() {
                    @Override public void onSuccess(String response) {
                        runOnUiThread(() -> addChatMessage("Mate", response));
                    }
                    @Override public void onError(Throwable t) {
                        runOnUiThread(() -> addChatMessage("Error", t.getMessage()));
                    }
                });
            }).start();
            return;
        }

        // Context compaction
        exchangeCount++;
        if (exchangeCount >= 5) {
            StringBuilder ctx = new StringBuilder();
            for (int i = 0; i < chatHistory.getChildCount(); i++) {
                View v = chatHistory.getChildAt(i);
                if (v instanceof TextView) ctx.append(((TextView) v).getText()).append("\n");
            }
            addChatMessage("System", "Compacting memory...");
            aiClient.generateResponse("Summarize: " + ctx, "", new AICoreClient.ResponseCallback() {
                @Override public void onSuccess(String summary) {
                    runOnUiThread(() -> {
                        chatHistory.removeAllViews();
                        addChatMessage("System", "Memory compacted.");
                        exchangeCount = 0;
                        doGenerate(query, summary);
                    });
                }
                @Override public void onError(Throwable t) {
                    runOnUiThread(() -> addChatMessage("Error", "Compaction: " + t.getMessage()));
                }
            });
        } else {
            doGenerate(query, "");
        }
    }

    private void doGenerate(String query, String context) {
        // Show typing indicator
        TextView typingView = new TextView(this);
        typingView.setText("Mate is thinking...");
        typingView.setTextColor(theme.textDim());
        typingView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        typingView.setPadding(dp(16), dp(8), dp(16), dp(8));
        chatHistory.addView(typingView);

        final long startTime = System.currentTimeMillis();
        aiClient.generateResponse(query, context, new AICoreClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    chatHistory.removeView(typingView);
                    long elapsed = System.currentTimeMillis() - startTime;
                    handleToolResponse(response, elapsed);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    chatHistory.removeView(typingView);
                    addChatMessage("Error", t.getMessage());
                });
            }
        });
    }

    private void handleToolResponse(String response, long elapsedMs) {
        boolean launched = ToolExecutor.handleLaunch(this, response);
        String searchQuery = ToolExecutor.extractSearchQuery(response);
        String clean = ToolExecutor.stripCommands(response);

        if (!clean.isEmpty()) {
            String timeStr = String.format("%.1fs", elapsedMs / 1000.0);
            addChatBubble("Mate", clean, timeStr);
        }

        if (launched) addChatMessage("System", "App launched.");

        if (searchQuery != null) {
            addChatMessage("System", "Searching: " + searchQuery);
            new Thread(() -> {
                String results = ToolExecutor.searchWeb(searchQuery);
                aiClient.generateResponse("Summarize search results:\n" + results, "", new AICoreClient.ResponseCallback() {
                    @Override public void onSuccess(String s) {
                        runOnUiThread(() -> addChatMessage("Mate", ToolExecutor.stripCommands(s)));
                    }
                    @Override public void onError(Throwable t) {
                        runOnUiThread(() -> addChatMessage("Error", t.getMessage()));
                    }
                });
            }).start();
        }
    }

    // ========== CHAT BUBBLES ==========

    private void addChatMessage(String sender, String message) {
        addChatBubble(sender, message, null);
    }

    private void addChatBubble(String sender, String message, String timing) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));

        boolean isUser = sender.equals("You");
        boolean isSystem = sender.equals("System") || sender.equals("Error");

        if (isUser) {
            bg.setColor(theme.userBubble());
        } else if (isSystem) {
            bg.setColor(theme.systemBubble());
            bg.setStroke(1, theme.textDim());
        } else {
            bg.setColor(theme.aiBubble());
        }
        bubble.setBackground(bg);

        // Message text
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextColor(isSystem ? theme.textDim() : (isUser ? Color.WHITE : theme.aiBubbleText()));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setLineSpacing(dp(2), 1.0f);
        bubble.addView(tv);

        // Timing label
        if (timing != null) {
            TextView timeLabel = new TextView(this);
            timeLabel.setText(timing);
            timeLabel.setTextColor(theme.textDim());
            timeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            timeLabel.setPadding(0, dp(4), 0, 0);
            timeLabel.setGravity(Gravity.END);
            bubble.addView(timeLabel);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(isUser ? dp(48) : 0, dp(4), isUser ? 0 : dp(48), dp(4));
        params.gravity = isUser ? Gravity.END : Gravity.START;

        chatHistory.addView(bubble, params);

        // Auto-scroll
        ((ScrollView) chatHistory.getParent()).post(() ->
                ((ScrollView) chatHistory.getParent()).fullScroll(View.FOCUS_DOWN));
    }

    // ========== UTILITIES ==========

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private boolean isMateDefaultAssistant() {
        boolean isRoleHeld = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.app.role.RoleManager rm = getSystemService(android.app.role.RoleManager.class);
            if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                isRoleHeld = rm.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT);
            }
        }
        String assistant = Settings.Secure.getString(getContentResolver(), "assistant");
        boolean isSecure = assistant != null && assistant.contains("com.abettergemini.assistant");
        String voice = Settings.Secure.getString(getContentResolver(), "voice_interaction_service");
        boolean isVoice = voice != null && voice.contains("com.abettergemini.assistant");
        return isRoleHeld || isSecure || isVoice;
    }

    interface OnTraitChangeListener { void onChanged(int value); }
    interface OnMemoryChangeListener { void onChanged(String value); }
}
