package com.abettergemini.assistant;

import org.junit.Test;
import static org.junit.Assert.*;

public class ComprehensiveLogicTest {

    @Test
    public void testBargainDetectionLogic() {
        // Test various price formats
        assertTrue("Standard dollar", detect("$99.99"));
        assertTrue("Text price", detect("The price is high"));
        assertTrue("Euro symbol", detect("Price: 50€"));
        assertFalse("Random text", detect("Hello world"));
    }

    private boolean detect(String text) {
        return text.contains("$") || text.toLowerCase().contains("price") || text.contains("€");
    }

    @Test
    public void testPersonaPromptSynthesis() {
        // Test Verbosity
        assertEquals("Be extremely brief. ", synthesize(2, 5, 5));
        assertEquals("Be very detailed and thorough. ", synthesize(9, 5, 5));
        
        // Test Formality
        assertTrue(synthesize(5, 9, 5).contains("professional"));
        assertTrue(synthesize(5, 2, 5).contains("casual"));
        
        // Test Humor
        assertTrue(synthesize(5, 5, 9).contains("sarcastic"));
    }

    private String synthesize(int v, int f, int h) {
        StringBuilder sb = new StringBuilder();
        if (v <= 3) sb.append("Be extremely brief. ");
        else if (v >= 8) sb.append("Be very detailed and thorough. ");

        if (f >= 8) sb.append("Maintain a highly professional, executive tone. ");
        else if (f <= 3) sb.append("Use casual language and slang. ");

        if (h >= 8) sb.append("Include sarcastic or playful remarks. ");
        return sb.toString();
    }

    @Test
    public void testDayNightLogic() {
        // Testing the logic used in AssistantSession.java
        assertTrue("Midnight is night", isNight(0));
        assertTrue("11 PM is night", isNight(23));
        assertFalse("Noon is day", isNight(12));
        assertFalse("8 AM is day", isNight(8));
    }

    private boolean isNight(int hour) {
        return (hour < 6 || hour > 18);
    }
}
