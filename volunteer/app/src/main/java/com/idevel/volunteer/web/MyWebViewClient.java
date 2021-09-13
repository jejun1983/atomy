package com.idevel.volunteer.web;

import android.content.Context;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.idevel.volunteer.BuildConfig;
import com.idevel.volunteer.utils.DLog;

/**
 * The MyWebViewClient Class.
 *
 * @author : jjbae
 */
public abstract class MyWebViewClient extends WebViewClient implements ReceivedErrorListener {
    private Context mContext;

    public MyWebViewClient(Context context) {
        this.mContext = context;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);

        DLog.e("error code ==>> " + errorCode);
        DLog.e("error description ==>> " + description);
        DLog.e("error failingUrl ==>> " + failingUrl);

        if (!BuildConfig.DEBUG) {
            showErrorPage();
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        DLog.d("bjj shouldOverrideUrlLoading ==>> " + url);

        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        DLog.d("url ==>> " + url);
    }

    @Override
    public abstract void showErrorPage();

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        DLog.e("ssl error ==>> " + error.getUrl());

        // 특정 웹뷰 버전에서 웹페이지 정상적으로 로드 못하는 이슈
        // url이 유모비 사이트면 실행되도록 예외처리
        if (error.getUrl().startsWith(UrlData.NORMAL_SERVER_URL)) {
            handler.proceed();
            return;
        }

        super.onReceivedSslError(view, handler, error);
    }

    public void setUntouchableProgress(int visible) {
    }
}