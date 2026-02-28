package com.abettergemini.assistant;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.graphics.drawable.GradientDrawable;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

public class AssistantSession extends VoiceInteractionSession {
    private static final String TAG = "Mate";
    private TextView resultText;
    private ProgressBar thinkingBar;
    private AICoreClient aiClient;
    private MemoryManager memory;
    private VoiceManager voice;
    private FrameLayout rootLayout;
    private View statusIndicator;

    public AssistantSession(Context context) {
        super(context);
        this.aiClient = new AICoreClient(context);
        this.memory = new MemoryManager(context);
        this.voice = new VoiceManager(context, new PreferencesManager(context));
    }

    @Override
    public View onCreateContentView() {
        rootLayout = new FrameLayout(getContext());

        // 1. Cozy Backdrop (Day/Night logic)
        updateTheme();

        // 2. Main Container (ClawControl v1.3.1 Inspired)
        LinearLayout contentLayout = new LinearLayout(getContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.BOTTOM);
        contentLayout.setPadding(48, 48, 48, 64);

        // Rounded Background (Modern Slate)
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(64f);
        shape.setColor(Color.parseColor("#F8F9FA"));
        contentLayout.setBackground(shape);

        // 3. Status Indicator (Inspired by Reddit "Working/Idle" dot)
        statusIndicator = new View(getContext());
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(24, 24);
        dotParams.setMargins(0, 0, 0, 16);
        statusIndicator.setLayoutParams(dotParams);
        setIndicatorStatus(false); // Default Idle
        contentLayout.addView(statusIndicator);

        // Thinking Indicator (Claw-style subtle bar)
        thinkingBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        thinkingBar.setIndeterminate(true);
        thinkingBar.setVisibility(View.GONE);
        contentLayout.addView(thinkingBar);

        // Response Text
        resultText = new TextView(getContext());
        resultText.setText("Mate is listening...");
        resultText.setTextColor(Color.parseColor("#212529"));
        resultText.setTextSize(18f);
        resultText.setPadding(0, 24, 0, 24);
        contentLayout.addView(resultText);

        // Dictation Pulse Placeholder (Microphone visual)
        TextView micIndicator = new TextView(getContext());
        micIndicator.setText("🎤");
        micIndicator.setGravity(Gravity.CENTER);
        micIndicator.setTextSize(24f);
        contentLayout.addView(micIndicator);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        rootLayout.addView(contentLayout, params);

        return rootLayout;
    }

    private void updateTheme() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour < 6 || hour > 18);

        // Cozy Background based on time
        if (isNight) {
            rootLayout.setBackgroundColor(Color.parseColor("#1A1A2E")); // Deep Midnight
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#E0E0E0")); // Soft Day
        }
    }

    private void setIndicatorStatus(boolean working) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        if (working) {
            dot.setColor(Color.parseColor("#4CAF50")); // Pulsing Green
        } else {
            dot.setColor(Color.parseColor("#9E9E9E")); // Idle Grey
        }
        statusIndicator.setBackground(dot);
    }

    @Override
    public void onHandleAssist(android.service.voice.VoiceInteractionSession.AssistState state) {
        super.onHandleAssist(state);
        AssistStructure structure = state.getAssistStructure();
        // Screenshot access requires the SHOW_SCREEN_VIOLATION check
        android.graphics.Bitmap screenshot = null;
        try {
            // Check for screenshot in older API or via reflection if necessary
            // In SDK 34, it's typically part of the AssistState if allowed
        } catch (Exception e) {
        }

        setIndicatorStatus(true);
        thinkingBar.setVisibility(View.VISIBLE);
        resultText.setText("Mate is thinking...");

        List<String> screenText = new ArrayList<>();
        if (structure != null) {
            int nodeCount = structure.getWindowNodeCount();
            for (int i = 0; i < nodeCount; i++) {
                AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
                parseNode(windowNode.getRootViewNode(), screenText);
            }
        }

        String combinedContext = String.join("\n", screenText);

        // Vision Logic: If screenshot is available, we could pass it to a
        // Vision-capable model
        // For now, we continue with text-based analysis of the screen structure.
        String dummyPrompt = "Analyze this screen.";

        aiClient.generateResponse(dummyPrompt, combinedContext, new AICoreClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                resultText.post(() -> {
                    setIndicatorStatus(false);
                    thinkingBar.setVisibility(View.GONE);
                    resultText.setText(response);
                    // Proactive Voice Feedback
                    voice.speak(response);
                });
                memory.storeFact("last_query", dummyPrompt);
            }

            @Override
            public void onError(Throwable t) {
                resultText.post(() -> {
                    setIndicatorStatus(false);
                    thinkingBar.setVisibility(View.GONE);
                    resultText.setText("Gemini Nano Error: " + t.getMessage());
                });
            }
        });

        processContextForBargains(combinedContext);
    }

    private void parseNode(AssistStructure.ViewNode node, List<String> textList) {
        if (node == null)
            return;
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            textList.add(text.toString());
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            parseNode(node.getChildAt(i), textList);
        }
    }

    private void processContextForBargains(String context) {
        if (context.contains("$") || context.toLowerCase().contains("price")) {
            memory.logPrice("Item from screen", 0.0, "$", "Screen Scraper");
        }
    }
}
