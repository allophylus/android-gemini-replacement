package com.openclaw.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.view.Gravity;

public class MainActivity extends Activity {
    private PreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);

        TextView text = new TextView(this);
        text.setText("Mate Assistant Personalization");
        text.setGravity(Gravity.CENTER);
        text.setTextSize(22f);
        layout.addView(text);

        // Verbosity Slider
        layout.addView(createTraitView("Verbosity", prefs.getVerbosity(), (v) -> prefs.setVerbosity(v)));
        // Formality Slider
        layout.addView(createTraitView("Formality", prefs.getFormality(), (v) -> prefs.setFormality(v)));
        // Humor Slider
        layout.addView(createTraitView("Humor", prefs.getHumor(), (v) -> prefs.setHumor(v)));

        Button settingsBtn = new Button(this);
        settingsBtn.setText("Open System Assistant Settings");
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            startActivity(intent);
        });
        layout.addView(settingsBtn);

        setContentView(layout);
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv.setText(label + ": " + progress);
                listener.onChanged(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        row.addView(tv);
        row.addView(sb);
        return row;
    }

    interface OnTraitChangeListener {
        void onChanged(int value);
    }
}
