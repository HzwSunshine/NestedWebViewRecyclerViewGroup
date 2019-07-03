package com.hzw.nested.example;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.hzw.nested.NestedWebViewRecyclerViewGroup;

public class WrapRvActivity extends AppCompatActivity implements View.OnClickListener {

    private NestedWebViewRecyclerViewGroup parent;
    private RvAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrap_rv);
        recyclerView = findViewById(R.id.nest_rv);
        parent = findViewById(R.id.nest_parent);
        TextView tvComment = findViewById(R.id.tv_comment);
        TextView addItem = findViewById(R.id.addItem);
        TextView deleteItem = findViewById(R.id.deleteItem);
        tvComment.setOnClickListener(this);
        addItem.setOnClickListener(this);
        deleteItem.setOnClickListener(this);
        findViewById(R.id.hide_rv).setOnClickListener(this);
        findViewById(R.id.tv_height).setOnClickListener(this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        adapter = new RvAdapter(true);
        recyclerView.setAdapter(adapter);
        initWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebView webView = findViewById(R.id.nest_webView);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://www.cnblogs.com/xxxxxx.html");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_comment:
                parent.switchView(0);
                break;
            case R.id.addItem:
                adapter.addItem();
                break;
            case R.id.deleteItem:
                adapter.deleteItem();
                break;
            case R.id.hide_rv:
                if (recyclerView.getVisibility() == View.VISIBLE) {
                    recyclerView.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.tv_height:
                View change = findViewById(R.id.changeView);
                if (change.getVisibility() == View.VISIBLE) {
                    change.setVisibility(View.GONE);
                } else {
                    change.setVisibility(View.VISIBLE);
                }
                break;
        }
    }
}