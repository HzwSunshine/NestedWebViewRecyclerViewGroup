package com.hzw.nested.example;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hzw.nested.NestedScrollWebView;
import com.hzw.nested.NestedWebViewRecyclerViewGroup;

public class NestedFragmentActivity extends AppCompatActivity implements View.OnClickListener {

    private NestedWebViewRecyclerViewGroup parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested_fragment);
        parent = findViewById(R.id.nest_parent);
        TextView tvComment = findViewById(R.id.tv_comment);
        TextView tvLastRead = findViewById(R.id.tv_last_read);
        tvComment.setOnClickListener(this);
        tvLastRead.setOnClickListener(this);
        initWebView();

        //评论是一个以Fragment的方式存在
        FragmentManager manager = getSupportFragmentManager();
        CommentFragment fragment = (CommentFragment) manager.findFragmentByTag(CommentFragment.class.getName());
        FragmentTransaction transaction = manager.beginTransaction();
        if (fragment == null) {
            fragment = new CommentFragment();
            transaction.add(R.id.fragment_container, fragment, CommentFragment.class.getName());
        } else {
            transaction.attach(fragment);
        }
        transaction.commit();


        //当你的评论的 RecyclerView 在一个Fragment中时，有两种方式可以将 RecyclerView
        //和 NestedWebViewRecyclerViewGroup 关联起来

        //1. 在 NestedWebViewRecyclerViewGroup 的内部如果在解析布局文件时，如果没有找到 RecyclerView ，
        //那么在界面显示时会尝试重新获取 RecyclerView ，这种情况不需要你再做额外的事情

        //2. 如果你还有更特殊的用法，当 NestedWebViewRecyclerViewGroup 在界面显示时都无法获取到 RecyclerView
        //那么可以调用 NestedWebViewRecyclerViewGroup 的 setRecyclerView 方法，将两者关联
        //parent.setRecyclerView(your recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        int scrollY = parent.getCurrentScrollY();
        ReadUtil.saveRead(this, scrollY);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        //手动添加WebView的方式，便于WebView的复用以及其他
        WebView webView = new NestedScrollWebView(this);
        FrameLayout container = findViewById(R.id.webView_container);
        container.addView(webView);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://github.com/HzwSunshine/NestedWebViewRecyclerViewGroup");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_comment:
                parent.switchView(7);
                break;
            case R.id.tv_last_read:
                int last = ReadUtil.getRead(this);
                parent.scrollToPosition(last);
                break;
        }
    }
}
