package com.wuzhanning.yougame;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import java.io.ByteArrayInputStream;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private boolean ready;
    private static final String HOST = "game.local";
    private static final Set<String> FILES = new HashSet<>(Arrays.asList("index.html", "game.js", "physics.js", "tv.js", "style.css"));
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String path = request.getUrl().getPath();
                String file = path == null ? "" : path.substring(1);
                if ("https".equals(request.getUrl().getScheme()) && HOST.equals(request.getUrl().getHost()) && FILES.contains(file)) {
                    String mime = file.endsWith(".js") ? "application/javascript" : file.endsWith(".css") ? "text/css" : "text/html";
                    try { return new WebResourceResponse(mime, "UTF-8", getAssets().open(file)); } catch (java.io.IOException ignored) { }
                }
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public void onPageFinished(WebView view, String url) { ready = true; }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                android.util.Log.d("YouGame", message.message() + " @ " + message.sourceId() + ":" + message.lineNumber());
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("https://" + HOST + "/index.html?tv=1");
    }
    private String action(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return "left";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "right";
            case KeyEvent.KEYCODE_DPAD_UP: return "up";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "down";
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_BUTTON_A: return "ok";
            case KeyEvent.KEYCODE_BUTTON_B: return "bomb";
            case KeyEvent.KEYCODE_BUTTON_X: return "shake";
            case KeyEvent.KEYCODE_MENU: case KeyEvent.KEYCODE_BUTTON_SELECT: return "menu";
            case KeyEvent.KEYCODE_BUTTON_START: case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: return "pause";
            case KeyEvent.KEYCODE_BACK: return "back";
            default: return null;
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        String action = action(event.getKeyCode());
        if (action == null) return super.dispatchKeyEvent(event);
        if (ready && event.getAction() == KeyEvent.ACTION_DOWN && (event.getRepeatCount() == 0 || action.equals("left") || action.equals("right") || action.equals("up") || action.equals("down"))) {
            web.evaluateJavascript("window.tvInput && window.tvInput('" + action + "')", result -> { if (action.equals("back") && result.equals("false")) finish(); });
        }
        return true;
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (ready && (event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            float axis = event.getAxisValue(MotionEvent.AXIS_X);
            if (Math.abs(axis) < .2f) axis = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            axis = Math.abs(axis) < .2f ? 0 : Math.max(-1, Math.min(1, axis));
            web.evaluateJavascript("window.tvAxis=" + axis, null);
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }
    @Override protected void onPause() {
        if (web != null) { web.evaluateJavascript("window.tvAxis=0;if(typeof saveGame==='function'){if(!paused&&!ended)togglePause();saveGame();}", null); web.onPause(); }
        super.onPause();
    }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() { if (web != null) web.destroy(); super.onDestroy(); }
}
