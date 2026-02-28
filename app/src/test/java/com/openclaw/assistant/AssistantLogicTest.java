package com.openclaw.assistant;

import org.junit.Test;
import static org.junit.Assert.*;

public class AssistantLogicTest {

    @Test
    public void testBargainDetection() {
        String context = "Buy this product for only 9.99";
        boolean detected = context.contains("$") || context.toLowerCase().contains("price");
        assertTrue("Bargain should be detected", detected);
    }

    @Test
    public void testVerbosityPrompt() {
        int verbosity = 2; // Low
        String prompt = "";
        if (verbosity <= 3) prompt = "Be extremely brief. ";
        
        assertEquals("Prompt should be brief for low verbosity", "Be extremely brief. ", prompt);
    }
}
