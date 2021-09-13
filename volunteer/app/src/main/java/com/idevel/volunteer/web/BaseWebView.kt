package com.idevel.volunteer.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import com.idevel.volunteer.BuildConfig
import com.idevel.volunteer.MyApplication
import com.idevel.volunteer.utils.DLog
import com.idevel.volunteer.web.constdata.IdevelServerScript
import com.idevel.volunteer.web.interfaces.IWebBridgeApi

@SuppressLint("SetJavaScriptEnabled")
class BaseWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : WebView(context, attrs, defStyleAttr) {
    init {
        val webSettings = settings

        if (BuildConfig.DEBUG) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            }
        }

        webSettings.javaScriptEnabled = true
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.useWideViewPort = true
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // 텍스트 크기 고정
        webSettings.textZoom = 100

        val userAgent = StringBuffer(webSettings.userAgentString)
        userAgent.append(";" + context.packageName)

        if (MyApplication.instance.isGoogleMarket) {
            userAgent.append(";" + "MARKET_GOOGLE")
        }

        DLog.e("bjj userAgentString :: "+userAgent.toString())

        webSettings.userAgentString = userAgent.toString()

        // text 글자 복사 방지
        isLongClickable = false
        setOnLongClickListener { true }
    }

    fun setJSInterface(api: IWebBridgeApi) {
        addJavascriptInterface(MyWebInterface(context, api), MyWebInterface.NAME)
    }

    fun sendEvent(function: String) {
        sendEvent(function, "")
    }

    fun sendEvent(function: IdevelServerScript) {
        sendEvent(function.scriptName, "")
    }

    fun sendEvent(function: IdevelServerScript, params: String) {
        sendEvent(function.scriptName, params)
    }

    fun sendEvent(function: String, params: String?) {
        (context as Activity).runOnUiThread {
            val hasParams = params != null && params.isNotEmpty()
            val script = if (hasParams) {
                "${MyWebInterface.webInvoker}('$function', $params)"
            } else {
                "${MyWebInterface.webInvoker}('$function')"
            }

            DLog.e("bjj data: script:  $script")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                evaluateJavascript(script, null)
            } else {
                loadUrl("javascript:" + script)
            }
        }
    }
}