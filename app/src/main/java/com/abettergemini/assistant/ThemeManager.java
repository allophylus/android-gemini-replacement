package com.abettergemini.assistant;

import android.graphics.Color;

/**
 * Provides theme colors for light and dark mode.
 * All color logic is centralized here for testability and modularity.
 * No Android Context dependency — only uses android.graphics.Color for parsing.
 */
public class ThemeManager {
    private boolean darkMode;

    // Dark theme colors
    private static final int DARK_BG = Color.parseColor("#0F0F1A");
    private static final int DARK_SURFACE = Color.parseColor("#1A1A2E");
    private static final int DARK_CARD = Color.parseColor("#242442");
    private static final int DARK_INPUT_BG = Color.parseColor("#2A2A48");
    private static final int DARK_TEXT = Color.parseColor("#E8E8F0");
    private static final int DARK_TEXT_DIM = Color.parseColor("#9090B0");
    private static final int DARK_USER_BUBBLE = Color.parseColor("#6C63FF");
    private static final int DARK_AI_BUBBLE = Color.parseColor("#2A2A48");
    private static final int DARK_SYSTEM_BUBBLE = Color.parseColor("#1A1A30");
    private static final int DARK_SEPARATOR = Color.parseColor("#2A2A48");
    private static final int DARK_INPUT_HINT = Color.parseColor("#505070");

    // Light theme colors
    private static final int LIGHT_BG = Color.parseColor("#F5F5FA");
    private static final int LIGHT_SURFACE = Color.parseColor("#FFFFFF");
    private static final int LIGHT_CARD = Color.parseColor("#FFFFFF");
    private static final int LIGHT_INPUT_BG = Color.parseColor("#EEEEF4");
    private static final int LIGHT_TEXT = Color.parseColor("#1A1A2E");
    private static final int LIGHT_TEXT_DIM = Color.parseColor("#6B6B8A");
    private static final int LIGHT_USER_BUBBLE = Color.parseColor("#6C63FF");
    private static final int LIGHT_AI_BUBBLE = Color.parseColor("#E8E8F0");
    private static final int LIGHT_SYSTEM_BUBBLE = Color.parseColor("#F0F0F8");
    private static final int LIGHT_SEPARATOR = Color.parseColor("#E0E0EA");
    private static final int LIGHT_INPUT_HINT = Color.parseColor("#9090B0");

    // Shared accent colors (same in both themes)
    public static final int ACCENT = Color.parseColor("#6C63FF");
    public static final int ACCENT_SECONDARY = Color.parseColor("#FF6584");
    public static final int SUCCESS = Color.parseColor("#4CAF50");

    public ThemeManager(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean dark) { this.darkMode = dark; }

    public int bg()           { return darkMode ? DARK_BG : LIGHT_BG; }
    public int surface()      { return darkMode ? DARK_SURFACE : LIGHT_SURFACE; }
    public int card()         { return darkMode ? DARK_CARD : LIGHT_CARD; }
    public int inputBg()      { return darkMode ? DARK_INPUT_BG : LIGHT_INPUT_BG; }
    public int text()         { return darkMode ? DARK_TEXT : LIGHT_TEXT; }
    public int textDim()      { return darkMode ? DARK_TEXT_DIM : LIGHT_TEXT_DIM; }
    public int userBubble()   { return darkMode ? DARK_USER_BUBBLE : LIGHT_USER_BUBBLE; }
    public int aiBubble()     { return darkMode ? DARK_AI_BUBBLE : LIGHT_AI_BUBBLE; }
    public int systemBubble() { return darkMode ? DARK_SYSTEM_BUBBLE : LIGHT_SYSTEM_BUBBLE; }
    public int separator()    { return darkMode ? DARK_SEPARATOR : LIGHT_SEPARATOR; }
    public int inputHint()    { return darkMode ? DARK_INPUT_HINT : LIGHT_INPUT_HINT; }

    public int headerText()   { return darkMode ? Color.WHITE : Color.parseColor("#1A1A2E"); }
    public int chipBg()       { return darkMode ? Color.parseColor("#1E1E3F") : Color.parseColor("#EEEEF4"); }
    public int chipStroke()   { return ACCENT; }
    public int bubbleText()   { return Color.WHITE; }
    public int aiBubbleText() { return darkMode ? Color.WHITE : Color.parseColor("#1A1A2E"); }
    public int cardShadow()   { return darkMode ? 0 : Color.parseColor("#20000000"); }
}
