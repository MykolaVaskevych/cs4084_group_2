package com.example.appdevelopmentprojectfinal.utils;

import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for handling YouTube videos in WebView
 */
public class YouTubeHelper {
    private static final String TAG = "YouTubeHelper";

    // Patterns for extracting YouTube video IDs
    private static final String YOUTUBE_URL_PATTERN = "(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:embed\\/|watch\\?v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})(?:[\\?&].*)?$";

    /**
     * Load a YouTube video into a WebView
     * 
     * @param webView WebView to load the video into
     * @param youtubeUrl YouTube URL (can be full URL, embed URL, or youtu.be URL)
     * @param autoPlay Whether to autoplay the video
     */
    public static void loadYoutubeVideo(WebView webView, String youtubeUrl, boolean autoPlay) {
        if (webView == null) {
            Log.e(TAG, "WebView is null");
            return;
        }

        if (youtubeUrl == null || youtubeUrl.isEmpty()) {
            Log.e(TAG, "YouTube URL is null or empty");
            return;
        }

        Log.i(TAG, "Loading YouTube video: " + youtubeUrl);

        // Extract video ID from URL
        String videoId = extractYoutubeVideoId(youtubeUrl);
        if (videoId == null) {
            Log.e(TAG, "Could not extract video ID from URL: " + youtubeUrl);
            return;
        }

        Log.v(TAG, "Extracted video ID: " + videoId);

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Build the embed HTML
        String embedHtml = getEmbedHtml(videoId, autoPlay);
        
        // Load the HTML into the WebView
        webView.loadData(embedHtml, "text/html", "utf-8");
    }

    /**
     * Extract the YouTube video ID from a URL
     * 
     * @param url YouTube URL
     * @return Video ID or null if not found
     */
    public static String extractYoutubeVideoId(String url) {
        Pattern pattern = Pattern.compile(YOUTUBE_URL_PATTERN);
        Matcher matcher = pattern.matcher(url);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * Generate the embed HTML for a YouTube video
     * 
     * @param videoId YouTube video ID
     * @param autoPlay Whether to autoplay the video
     * @return HTML string for embedding the video
     */
    private static String getEmbedHtml(String videoId, boolean autoPlay) {
        String autoPlayParam = autoPlay ? "&autoplay=1" : "";
        
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<style>" +
               "body { margin: 0; padding: 0; }" +
               ".video-container { position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden; }" +
               ".video-container iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class=\"video-container\">" +
               "<iframe width=\"560\" height=\"315\" " +
               "src=\"https://www.youtube.com/embed/" + videoId + "?si=S5KGzdEw8ie4IUq1" + autoPlayParam + "\" " +
               "title=\"YouTube video player\" frameborder=\"0\" " +
               "allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" " +
               "referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>" +
               "</div>" +
               "</body>" +
               "</html>";
    }
}