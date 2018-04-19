package com.readingwindow.readingwindowexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.readingwindow.ReadingWindow;

public class MainActivity extends AppCompatActivity {
    ReadingWindow readingWindow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView myWebView = findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.loadUrl("https://blog.trello.com/automation-magic-trello-boards");
         readingWindow=findViewById(R.id.readingWindow);
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readingWindow.getVisibility()==View.VISIBLE) {
                    readingWindow.setVisibility(View.GONE);
                } else {
                    readingWindow.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
