package com.shredforge.tabview;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class AlphaTabRenderer {
    private WebView view;
    private WebEngine engine;
    private volatile String lastSvg = "";
    private volatile boolean pageLoaded;

    public AlphaTabRenderer() {}

    public void attachTo(WebView view) {
        this.view = Objects.requireNonNull(view, "view");
        this.engine = view.getEngine();
    }

    public void loadAlphaTab() {
        ensureEngine();
        URL url = AlphaTabRenderer.class.getResource("/com/shredforge/alphatab/index.html");
        if (url == null) {
            throw new IllegalStateException("alphatab/index.html not found on classpath");
        }
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                pageLoaded = true;
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
            }
        });
        engine.load(url.toExternalForm());
    }

    public void renderFromSongsterrJson(String json) {
        ensureEngine();
        if (!pageLoaded) {
            CountDownLatch latch = new CountDownLatch(1);
            engine.getLoadWorker().stateProperty().addListener((o, a, b) -> {
                if (b == Worker.State.SUCCEEDED) {
                    latch.countDown();
                }
            });
            try { latch.await(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        String arg = toJsString(json == null ? "" : json);
        // Defer invocation until renderScore is defined in the page
        String script = "(function(json){\n" +
                "  function tryCall(){\n" +
                "    if (typeof renderScore === 'function'){ renderScore(json); }\n" +
                "    else { setTimeout(tryCall, 100); }\n" +
                "  }\n" +
                "  tryCall();\n" +
                "})(" + arg + ");";
        Platform.runLater(() -> engine.executeScript(script));
    }

    public String getRenderedSvg() {
        return lastSvg == null ? "" : lastSvg;
    }

    private void ensureEngine() {
        if (engine == null) {
            if (view == null) {
                view = new WebView();
            }
            engine = view.getEngine();
        }
    }

    private static String toJsString(String text) {
        String s = text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return '"' + s + '"';
    }

    public final class JavaConnector {
        public void onSvgReady(String svg) {
            lastSvg = svg;
        }
    }
}
