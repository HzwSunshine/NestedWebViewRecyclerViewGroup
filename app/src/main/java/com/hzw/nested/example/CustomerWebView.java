package com.hzw.nested.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import com.hzw.nested.NestedScrollWebView;

/**
 * author: hzw
 * time: 2019-05-08 19:35
 * description:自定义的WebView
 */
public class CustomerWebView extends NestedScrollWebView {
    public CustomerWebView(Context context) {
        super(context);
        init();
    }

    public CustomerWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomerWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);
        setWebViewClient(new WebViewClient());
        setWebChromeClient(new WebChromeClient());
    }
}
