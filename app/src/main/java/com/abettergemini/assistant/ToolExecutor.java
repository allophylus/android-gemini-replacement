package com.abettergemini.assistant;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolExecutor {
    private static final String TAG = "ToolExecutor";
    private static final Pattern LAUNCH_PATTERN = Pattern.compile("\\[LAUNCH:(.+?)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEARCH_PATTERN = Pattern.compile("\\[SEARCH:(.+?)\\]", Pattern.CASE_INSENSITIVE);

    /**
     * Check if the LLM response contains a LAUNCH command and execute it.
     * Returns true if an app was launched.
     */
    public static boolean handleLaunch(Context context, String response) {
        Matcher matcher = LAUNCH_PATTERN.matcher(response);
        if (matcher.find()) {
            String appName = matcher.group(1).trim();
            return launchApp(context, appName);
        }
        return false;
    }

    /**
     * Check if the LLM response contains a SEARCH command.
     * Returns the search query, or null if no command found.
     */
    public static String extractSearchQuery(String response) {
        Matcher matcher = SEARCH_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Strip all [LAUNCH:...] and [SEARCH:...] tags from the response for clean display.
     */
    public static String stripCommands(String response) {
        String cleaned = LAUNCH_PATTERN.matcher(response).replaceAll("");
        cleaned = SEARCH_PATTERN.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    /**
     * Fuzzy-match an app name against installed applications and launch it.
     */
    public static boolean launchApp(Context context, String appName) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String lowerTarget = appName.toLowerCase();
        ApplicationInfo bestMatch = null;
        int bestScore = 0;

        for (ApplicationInfo app : apps) {
            String label = pm.getApplicationLabel(app).toString().toLowerCase();
            if (label.equals(lowerTarget)) {
                bestMatch = app;
                break; // Exact match
            }
            if (label.contains(lowerTarget) || lowerTarget.contains(label)) {
                int score = label.length();
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = app;
                }
            }
        }

        if (bestMatch != null) {
            Intent launchIntent = pm.getLaunchIntentForPackage(bestMatch.packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Log.d(TAG, "Launched: " + bestMatch.packageName);
                return true;
            }
        }

        Log.w(TAG, "Could not find app: " + appName);
        return false;
    }

    /**
     * Search DuckDuckGo's static HTML endpoint and return the top results as text.
     * This runs a network request and MUST be called off the main thread.
     */
    public static String searchWeb(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            Elements results = doc.select(".result__body");
            StringBuilder sb = new StringBuilder();
            sb.append("Web Search Results for '").append(query).append("':\n\n");

            int count = 0;
            for (Element result : results) {
                if (count >= 5) break;

                Element titleEl = result.select(".result__a").first();
                Element snippetEl = result.select(".result__snippet").first();

                String title = titleEl != null ? titleEl.text() : "No title";
                String snippet = snippetEl != null ? snippetEl.text() : "No description";
                String link = titleEl != null ? titleEl.attr("href") : "";

                sb.append(count + 1).append(". ").append(title).append("\n");
                sb.append("   ").append(snippet).append("\n");
                if (!link.isEmpty()) sb.append("   Link: ").append(link).append("\n");
                sb.append("\n");
                count++;
            }

            if (count == 0) {
                sb.append("No results found.");
            }

            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Web search failed", e);
            return "Web search failed: " + e.getMessage();
        }
    }
}
