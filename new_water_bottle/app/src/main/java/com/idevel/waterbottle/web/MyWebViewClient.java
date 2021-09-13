package com.idevel.waterbottle.web;

import android.content.Context;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.idevel.waterbottle.BuildConfig;
import com.idevel.waterbottle.utils.DLog;

import java.util.concurrent.CountDownLatch;

/**
 * The MyWebViewClient Class.
 *
 * @author : jjbae
 */
public abstract class MyWebViewClient extends WebViewClient implements ReceivedErrorListener {

    /**
     * The context.
     */
    private Context mContext;

    public CountDownLatch mLatch;

    /**
     * Instantiates a new my web view client.
     *
     * @param context the context
     */
    public MyWebViewClient(Context context) {
        this.mContext = context;
    }

    /* (non-Javadoc)
     * @see android.webkit.WebViewClient#onReceivedError(android.webkit.WebView, int, java.lang.String, java.lang.String)
     */
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        DLog.e("error code ==>> " + errorCode);
        DLog.e("error description ==>> " + description);
        DLog.e("error failingUrl ==>> " + failingUrl);
        super.onReceivedError(view, errorCode, description, failingUrl);

        if (!BuildConfig.DEBUG) {
            showErrorPage();
        }
    }

    /* (non-Javadoc)
     * @see android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView, java.lang.String)
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        DLog.d("bjj shouldOverrideUrlLoading ==>> " + url);

        return super.shouldOverrideUrlLoading(view, url);
//        if (url.startsWith("tel:")) {
//            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
//            mContext.startActivity(intent);
//            return true;
//        } else {
////            try {
////                mLatch = WebviewSessionSync.syncGate;
////                if (mLatch != null && mLatch.getCount() > 0) {
////                    DLog.d("***** show progress bar");
////                    setUntouchableProgress(View.VISIBLE);
////
////                    mLatch.await(2000, TimeUnit.MILLISECONDS);
////
////                    DLog.d("***** hide progress bar");
////                    setUntouchableProgress(View.GONE);
////                }
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
//            return super.shouldOverrideUrlLoading(view, url);
//        }
    }

    /* (non-Javadoc)
     * @see android.webkit.WebViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        DLog.d("url ==>> " + url);
        super.onPageFinished(view, url);
    }

    /* (non-Javadoc)
     * @see com.idevel.waterbottle.web.ReceivedErrorListener#showErrorPage()
     */
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
