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
import java.util.ArrayList;
import java.util.List;

public class AssistantSession extends VoiceInteractionSession {
    private static final String TAG = "Mate";
    private TextView resultText;
    private AICoreClient aiClient;
    private MemoryManager memory;

    public AssistantSession(Context context) {
        super(context);
        this.aiClient = new AICoreClient(context);
        this.memory = new MemoryManager(context);
    }

    @Override
    public View onCreateContentView() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.BOTTOM);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.parseColor("#E0FFFFFF"));

        resultText = new TextView(getContext());
        resultText.setText("Mate is thinking...");
        resultText.setTextColor(Color.BLACK);
        resultText.setTextSize(18f);

        layout.addView(resultText);
        return layout;
    }

    @Override
    public void onHandleAssist(android.service.voice.VoiceInteractionSession.AssistState state) {
        super.onHandleAssist(state);
        AssistStructure structure = state.getAssistStructure();

        Log.d(TAG, "Assist triggered. Parsing screen content...");

        List<String> screenText = new ArrayList<>();
        if (structure != null) {
            int nodeCount = structure.getWindowNodeCount();
            for (int i = 0; i < nodeCount; i++) {
                AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
                parseNode(windowNode.getRootViewNode(), screenText);
            }
        }

        String combinedContext = String.join("\n", screenText);
        Log.d(TAG, "Extracted Context: " + combinedContext);

        // Process Context with Gemini Nano
        String dummyPrompt = "What am I looking at right now?";
        aiClient.generateResponse(dummyPrompt, combinedContext, new AICoreClient.ResponseCallback() {
            @Override
            public void onSuccess(String response) {
                // Since this runs in the AI thread, update UI on Main Thread
                resultText.post(() -> resultText.setText(response));
                memory.storeFact("last_query", dummyPrompt);
            }

            @Override
            public void onError(Throwable t) {
                resultText.post(() -> resultText.setText("Gemini Nano Error: " + t.getMessage()));
            }
        });

        // For Bargain Hunting: Check extracted text for currency/prices
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
            Log.d(TAG, "Potential bargain/price detected in context.");
            // Log to local memory
            memory.logPrice("Item from screen", 0.0, "$", "Screen Scraper");
        }
    }
}
