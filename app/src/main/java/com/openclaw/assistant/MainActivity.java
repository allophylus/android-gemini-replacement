package com.openclaw.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);

        TextView text = new TextView(this);
        text.setText(
                "Mate Assistant is installed!\n\nPlease ensure Mate is set as your Default Digital Assistant App in Android Settings.");
        text.setGravity(Gravity.CENTER);
        text.setTextSize(18f);

        Button settingsBtn = new Button(this);
        settingsBtn.setText("Open Assistant Settings");
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            startActivity(intent);
        });

        layout.addView(text);
        layout.addView(settingsBtn);

        setContentView(layout);
    }
}
