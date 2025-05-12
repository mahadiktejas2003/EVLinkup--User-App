package com.example.evchargingfinal;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ChatBot extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);
        WebView webView = findViewById(R.id.webview);

        // Enable necessary WebView settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setLoadsImagesAutomatically(true);

        // Handle SSL error for ngrok URL
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.proceed(); // Ignore SSL certificate errors
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Log when page finishes loading
                android.util.Log.d("WebView", "Page loaded: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Log any errors
                android.util.Log.e("WebView", "Error: " + description);
            }
        });

        // Load the ngrok URL
        webView.loadUrl("https://cf5b-2401-4900-8fc9-b50d-4cad-9245-295e-9f79.ngrok-free.app/chatbot/e17b4354-4cec-42e1-82c7-06a7879d63b9");
    }
}