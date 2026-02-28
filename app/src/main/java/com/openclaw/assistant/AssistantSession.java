package com.openclaw.assistant;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class AssistantSession extends VoiceInteractionSession {
    private static final String TAG = "Mate";

    public AssistantSession(Context context) {
        super(context);
    }

    @Override
    public void onHandleAssist(android.service.voice.VoiceInteractionSession.AssistState state) {
        super.onHandleAssist(state);
        Bundle data = state.getAssistData();
        AssistStructure structure = state.getAssistStructure();
        AssistContent content = state.getAssistContent();

        Log.d(TAG, "Assist triggered. Parsing screen content...");

        List<String> screenText = new ArrayList<>();
        if (structure != null) {
            int nodeCount = structure.getWindowNodeCount();
            for (int i = 0; i < nodeCount; i++) {
                AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
                parseNode(windowNode.getRootViewNode(), screenText);
            }
        }

        // This is where the local LLM (Gemini Nano) would receive the context
        String combinedContext = String.join("\n", screenText);
        Log.d(TAG, "Extracted Context: " + combinedContext);

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
        // Basic placeholder logic for local bargain detection
        if (context.contains("$") || context.toLowerCase().contains("price")) {
            Log.d(TAG, "Potential bargain/price detected in context.");
            // Here we would query local SQLite for price history
        }
    }
}
