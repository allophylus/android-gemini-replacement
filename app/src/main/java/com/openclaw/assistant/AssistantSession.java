package com.openclaw.assistant;

import android.content.Context;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

public class AssistantSession extends VoiceInteractionSession {
    public AssistantSession(Context context) {
        super(context);
    }

    @Override
    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
        super.onHandleAssist(data, structure, content);
        // Handle the assist request here
    }
}
