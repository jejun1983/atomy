package com.idevel.waterbottle.web

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.idevel.waterbottle.R
import com.idevel.waterbottle.activity.MainActivity
import com.idevel.waterbottle.utils.DLog


/**
 * The MyWebChromeClient Class.
 *
 * @author : jjbae
 */
open class MyWebChromeClient(private val mContext: Context, mainView: RelativeLayout) : WebChromeClient() {
    private val progressbar: ProgressBar
    private val mMainView: View
    private val childLayout: RelativeLayout

    private var mCustomView: View? = null
    private var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
    private var mOriginalOrientation: Int = 0

    private var mFullscreenContainer: FrameLayout? = null

    private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)


    init {
        this.mMainView = mainView
        this.childLayout = mMainView.findViewById(R.id.webview_sub)
        this.progressbar = mMainView.findViewById(R.id.webview_progress)
    }

    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        DLog.e("bjj MyWebChromeClient onShowCustomView ==>> aa $view")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mCustomView != null) {
                callback.onCustomViewHidden()
                return
            }

            mOriginalOrientation = (mContext as Activity).requestedOrientation
            val decor = mContext.window.decorView as FrameLayout
            mFullscreenContainer = FullscreenHolder(mContext)
            mFullscreenContainer?.addView(view, COVER_SCREEN_PARAMS)
            decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS)
            mCustomView = view
            setFullscreen(true)
            mCustomViewCallback = callback
            //          mActivity.setRequestedOrientation(requestedOrientation);
        }

        super.onShowCustomView(view, callback)
    }

    override fun onShowCustomView(view: View, requestedOrientation: Int, callback: WebChromeClient.CustomViewCallback) {
        DLog.e("bjj MyWebChromeClient onShowCustomView ==>> bb $view")

        this.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        if (mCustomView == null) {
            return
        }

        setFullscreen(false)
        val decor = (mContext as Activity).window.decorView as FrameLayout
        decor.removeView(mFullscreenContainer)
        mFullscreenContainer = null
        mCustomView = null
        mCustomViewCallback?.onCustomViewHidden()
        mContext.requestedOrientation = mOriginalOrientation
    }

    private fun setFullscreen(enabled: Boolean) {
        val win = (mContext as Activity).window
        val winParams = win.attributes
        val bits = WindowManager.LayoutParams.FLAG_FULLSCREEN

        if (enabled) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
            if (mCustomView != null) {
                mCustomView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        win.attributes = winParams
    }

    private class FullscreenHolder(ctx: Context) : FrameLayout(ctx) {
        init {
            setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.black))
        }

        override fun onTouchEvent(evt: MotionEvent): Boolean {
            return true
        }
    }

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        DLog.e("bjj MyWebChromeClient onJsAlert ==>> $view")

        return jsAlert(message, result, false)
    }

    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        DLog.e("bjj MyWebChromeClient onJsConfirm ==>> $view")

        return jsAlert(message, result, true)
    }

    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
        DLog.e("bjj MyWebChromeClient onCreateWindow ==>> $resultMsg")

        childLayout.removeAllViews()

        val newView = BaseWebView(mContext)
        newView.tag = "newWebview"
        newView.webChromeClient = this
        newView.webViewClient = object : MyWebViewClient(mContext) {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                childLayout.bringToFront()
                childLayout.visibility = View.VISIBLE
            }

            override fun showErrorPage() {
                (mContext as MainActivity).showErrorView()
            }
        }

        newView.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        childLayout.addView(newView)

        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = newView
        resultMsg.sendToTarget()

        return true
    }

    override fun onCloseWindow(window: WebView) {
        DLog.e("bjj MyWebChromeClient onCloseWindow ==>> ")

        childLayout.visibility = View.GONE
        super.onCloseWindow(window)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        DLog.e("bjj MyWebChromeClient onProgressChanged ==>> " + newProgress)

        super.onProgressChanged(view, newProgress)

        progressbar.progress = newProgress
        if (newProgress >= 100) {
            progressbar.visibility = View.GONE
        } else {
            progressbar.visibility = View.VISIBLE
        }
    }

    /**
     * Js alert.
     *
     * @param message        the message
     * @param result         the result
     * @param hasNegativeBtn the has negative btn
     * @return true, if successful
     */
    private fun jsAlert(message: String, result: JsResult, hasNegativeBtn: Boolean): Boolean {
        if ((mContext as Activity).isFinishing) {
            return false
        }

        val builder = AlertDialog.Builder(mContext)
        builder.setTitle(com.idevel.waterbottle.R.string.popup_title_server_error)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok) { dialog, which -> result.confirm() }
        builder.setOnCancelListener { result.cancel() }
        if (hasNegativeBtn) {
            builder.setNegativeButton(android.R.string.cancel) { dialog, which -> result.cancel() }
        }
        builder.create()
        builder.show()

        return true
    }
}