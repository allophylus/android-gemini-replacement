package com.abettergemini.assistant;

import android.app.Activity;
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
    private AICoreClient aiClient;
    private LinearLayout chatHistory;
    private EditText chatInput;
    private View settingsView;
    private View chatView;
    private TextView assistCheck;
    private TextView aiCheck;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
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

        aiCheck.setText("- Gemini Nano: Checking models...");
        checkAiCoreStatus(aiCheck);
    }

    private void checkAiCoreStatus(TextView statusView) {
        aiClient.generateResponse("test", "", new AICoreClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    statusView.setText("- Gemini Nano (AICore): ✅ Ready (Downloaded)");
                    if (swipeRefresh != null)
                        swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    String msg = t.getMessage();
                    if (msg != null && msg.contains("empty")) {
                        statusView.setText("- Gemini Nano: 📥 Download Pending (Check Settings)");
                    } else {
                        statusView.setText("- Gemini Nano: ⚠️ Not Ready (" + msg + ")");
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
        header.setPadding(0, 0, 0, 32);
        layout.addView(header);

        // Personality Sliders
        layout.addView(createTraitView("Verbosity", prefs.getVerbosity(), (v) -> prefs.setVerbosity(v)));
        layout.addView(createTraitView("Formality", prefs.getFormality(), (v) -> prefs.setFormality(v)));
        layout.addView(createTraitView("Humor", prefs.getHumor(), (v) -> prefs.setHumor(v)));

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
                "Mate is a local-first, privacy-focused AI assistant built with Gemini Nano. No data leaves your Pixel 8+ device.");
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

        aiClient.generateResponse(query, "", new AICoreClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> addChatMessage("Mate", response));
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> addChatMessage("Error", t.getMessage()));
            }
        });
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

    interface OnTraitChangeListener {
        void onChanged(int value);
    }
}
