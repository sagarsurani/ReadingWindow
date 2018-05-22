package com.readingwindow.readingwindowexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.readingwindow.ReadingWindow;

public class MainActivity extends AppCompatActivity {


    int[] color=new int[]{R.color.colorAccent,R.color.colorPrimary,R.color.colorPrimaryDark,R.color.red};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView myWebView = findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.loadUrl("https://blog.trello.com/automation-magic-trello-boards");

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ReadingWindow.isVisible()) {
                    ReadingWindow.hide();
                } else {
                    ReadingWindow.show(MainActivity.this);
                   ReadingWindow.setDefaultSize(100,200);
                   ReadingWindow.setbackground(getResources().getColor(color[1]));



                   ReadingWindow.setReadingAreaColor(getResources().getColor(color[0]));
                    ReadingWindow.setReadingAreaAlpha(0.5f);
                   ReadingWindow.setBackgroundAlpha(0.4f);



                }
            }
        });







    }









}
