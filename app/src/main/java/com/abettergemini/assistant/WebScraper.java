package com.abettergemini.assistant;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;

public class WebScraper {

    public static String fetchAndExtractText(String url) {
        try {
            // Act like a standard desktop browser to avoid casual blocking
            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10000)
                    .get();

            String text = doc.body().text();

            // Clean up massive whitespace blocks and newlines if they exist
            text = text.replaceAll("\\s+", " ").trim();

            return text;
        } catch (IOException e) {
            return "Error fetching website content: " + e.getMessage();
        }
    }
}
