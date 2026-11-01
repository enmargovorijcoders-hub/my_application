package com.enmar.pandsapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.SeekBar;

/** The P&S site in a WebView, with a zoom slider along the bottom. */
public class MainActivity extends Activity {
    static final String HOME = "https://enmargovorijcoders-hub.github.io/my-webpage/";
    static final int MIN_ZOOM = 50;    // percent
    static final int MAX_ZOOM = 200;   // percent
    static final int DEFAULT_ZOOM = 100;

    private WebView web;
    private int zoom;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        final SharedPreferences prefs = getSharedPreferences("pandsapp", MODE_PRIVATE);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, prefs.getInt("zoom", DEFAULT_ZOOM)));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(false);          // zooming is done with the slider
        s.setBuiltInZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                applyZoom();
            }
        });

        // The bar along the bottom.
        LinearLayout bar = new LinearLayout(this);
        bar.setBackgroundColor(0xFF00FF00); // RGB 0, 255, 0
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));

        SeekBar seek = new SeekBar(this);
        styleSeekBar(seek);
        seek.setMax(MAX_ZOOM - MIN_ZOOM);
        seek.setProgress(zoom - MIN_ZOOM);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                zoom = MIN_ZOOM + progress;
                if (fromUser) applyZoom();
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                prefs.edit().putInt("zoom", zoom).commit();
            }
        });
        bar.addView(seek, new LinearLayout.LayoutParams(-1, -2));
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);

        if (b != null) web.restoreState(b);
        else web.loadUrl(HOME);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Scales the whole page (text, images and layout) to the slider's percentage. */
    private void applyZoom() {
        String js = "document.documentElement.style.zoom='" + (zoom / 100f) + "';";
        if (Build.VERSION.SDK_INT >= 19) web.evaluateJavascript(js, null);
        else web.loadUrl("javascript:" + js);
    }

    /** Yellow fill, grey track and a grey handle, drawn in code so it looks the same on every Android version. */
    private void styleSeekBar(SeekBar seek) {
        int trackHeight = dp(24);

        GradientDrawable track = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF8A8C88, 0xFF5E605D});
        track.setCornerRadius(dp(12));
        track.setSize(dp(200), trackHeight);

        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFD000, 0xFFF5A800});
        fill.setCornerRadius(dp(12));
        fill.setSize(dp(200), trackHeight);
        ClipDrawable clip = new ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL);

        LayerDrawable layers = new LayerDrawable(new Drawable[]{track, clip});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        seek.setProgressDrawable(layers);

        GradientDrawable thumb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFE4E4E4, 0xFF959595});
        thumb.setCornerRadius(dp(5));
        thumb.setStroke(dp(1), 0xFF6E6E6E);
        thumb.setSize(dp(14), dp(36));
        seek.setThumb(thumb);
        seek.setPadding(dp(14), dp(4), dp(14), dp(4));
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
