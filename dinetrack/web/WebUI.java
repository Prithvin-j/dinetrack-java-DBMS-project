package dinetrack.web;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class WebUI {
    private static byte[] cachedHtml = null;

    public static synchronized byte[] getHtmlBytes() {
        if (cachedHtml != null) {
            return cachedHtml;
        }
        try {
            Path[] candidatePaths = {
                Paths.get("dinetrack", "web", "index.html"),
                Paths.get("index.html"),
                Paths.get("web", "index.html")
            };
            for (Path p : candidatePaths) {
                if (Files.exists(p)) {
                    cachedHtml = Files.readAllBytes(p);
                    return cachedHtml;
                }
            }
        } catch (Exception ignored) {}

        String fallback = "<!DOCTYPE html><html><head><title>DineTrack</title></head>"
                + "<body style='font-family:sans-serif;padding:2rem;'><h1>DineTrack Web GUI</h1>"
                + "<p>index.html was not found in dinetrack/web/index.html.</p></body></html>";
        return fallback.getBytes(StandardCharsets.UTF_8);
    }
}
