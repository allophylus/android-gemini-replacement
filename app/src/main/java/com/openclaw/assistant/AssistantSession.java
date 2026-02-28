package com.openclaw.assistant;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.graphics.drawable.GradientDrawable;
import java.util.ArrayList;
import java.util.List;

public class AssistantSession extends VoiceInteractionSession {
    private static final String TAG = "Mate";
    private TextView resultText;
    private ProgressBar thinkingBar;
    private AICoreClient aiClient;
    private MemoryManager memory;

    public AssistantSession(Context context) {
        super(context);
        this.aiClient = new AICoreClient(context);
        this.memory = new MemoryManager(context);
    }

    @Override
    public View onCreateContentView() {
        // Main Container (ClawControl v1.3.1 Inspired)
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.BOTTOM);
        layout.setPadding(48, 48, 48, 64);

        // Rounded Background (Modern Slate)
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(64f);
        shape.setColor(Color.parseColor("#F8F9FA"));
        layout.setBackground(shape);

        // Thinking Indicator (Claw-style subtle bar)
        thinkingBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        thinkingBar.setIndeterminate(true);
        thinkingBar.setVisibility(View.VISIBLE);
        layout.addView(thinkingBar);

        // Response Text
        resultText = new TextView(getContext());
        resultText.setText("Mate is listening...");
        resultText.setTextColor(Color.parseColor("#212529"));
        resultText.setTextSize(18f);
        resultText.setPadding(0, 24, 0, 24);
        layout.addView(resultText);

        // Dictation Pulse Placeholder (Microphone visual)
        TextView micIndicator = new TextView(getContext());
        micIndicator.setText("🎤");
        micIndicator.setGravity(Gravity.CENTER);
        micIndicator.setTextSize(24f);
        layout.addView(micIndicator);

        return layout;
    }

    @Override
    public void onHandleAssist(android.service.voice.VoiceInteractionSession.AssistState state) {
        super.onHandleAssist(state);
        AssistStructure structure = state.getAssistStructure();

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
        
        // Process Context with Gemini Nano
        String dummyPrompt = "Analyze this screen.";
        aiClient.generateResponse(dummyPrompt, combinedContext, new AICoreClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                resultText.post(() -> {
                    thinkingBar.setVisibility(View.GONE);
                    resultText.setText(response);
                });
                memory.storeFact("last_query", dummyPrompt);
            }

            @Override
            public void onError(Throwable t) {
                resultText.post(() -> {
                    thinkingBar.setVisibility(View.GONE);
                    resultText.setText("Gemini Nano Error: " + t.getMessage());
                });
            }
        });

        processContextForBargains(combinedContext);
    }

    private void parseNode(AssistStructure.ViewNode node, List<String> textList) {
        if (node == null) return;
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
