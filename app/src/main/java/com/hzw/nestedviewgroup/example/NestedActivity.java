package com.hzw.nestedviewgroup.example;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import com.hzw.nestedviewgroup.NestedWebViewRecyclerViewGroup;

public class NestedActivity extends AppCompatActivity implements View.OnClickListener {

    private NestedWebViewRecyclerViewGroup parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested);
        RecyclerView recyclerView = findViewById(R.id.nest_rv);
        parent = findViewById(R.id.nest_parent);
        TextView tvComment = findViewById(R.id.tv_comment);
        TextView tvLastRead = findViewById(R.id.tv_last_read);
        tvComment.setOnClickListener(this);
        tvLastRead.setOnClickListener(this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(new RvAdapter());
        initWebView();
    }

    @Override
    public void onStop() {
        super.onStop();
        int scrollY = parent.getCurrentScrollY();
        ReadUtil.saveRead(this, scrollY);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebView webView = findViewById(R.id.nest_webView);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
//        webView.loadUrl("https://36kr.com/p/5180329.html");
        webView.loadUrl("https://github.com/tencent/mmkv");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_comment:
                parent.scrollToNextView(6);
                break;
            case R.id.tv_last_read:
                int last = ReadUtil.getRead(this);
                parent.scrollToPosition(last);
                break;
        }
    }
}
