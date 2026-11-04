package dev.neonlyrics.port;

import java.awt.Desktop;
import java.net.URI;

/** 26.3 removed {@code Util$OS#openUri}; this keeps the mod's link buttons working. */
public final class Urls {
    private Urls() {
    }

    public static void open(String url) {
        try {
            URI uri = URI.create(url);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (Exception ignored) {
            // opening a browser is best-effort
        }
    }

    public static void open(URI uri) {
        open(uri.toString());
    }
}
