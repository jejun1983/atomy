package com.idevel.momschoice.activity


import ApiManager
import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.view.WindowManager
import android.webkit.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import api.OnResultListener
import com.idevel.momschoice.BuildConfig
import com.idevel.momschoice.MyApplication
import com.idevel.momschoice.R
import com.idevel.momschoice.broadcast.DataSaverChangeReceiver
import com.idevel.momschoice.broadcast.NetworkChangeReceiver
import com.idevel.momschoice.dialog.AgentPopupDialog
import com.idevel.momschoice.dialog.CustomAlertDialog
import com.idevel.momschoice.fcm.PushPreferences.IS_NOTI
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_LINK_TYPE
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_LINK_URL
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_SHOWTIME
import com.idevel.momschoice.interfaces.IDataSaverListener
import com.idevel.momschoice.interfaces.NetworkChangeListener
import com.idevel.momschoice.utils.*
import com.idevel.momschoice.utils.wrapper.LocaleWrapper
import com.idevel.momschoice.web.BaseWebView
import com.idevel.momschoice.web.MyWebChromeClient
import com.idevel.momschoice.web.MyWebViewClient
import com.idevel.momschoice.web.UrlData.NORMAL_SERVER_URL
import com.idevel.momschoice.web.UrlData.getSettingUrl
import com.idevel.momschoice.web.constdata.*
import com.idevel.momschoice.web.interfaces.IWebBridgeApi
import com.onestore.iap.api.*
import com.onestore.iap.api.PurchaseClient.*
import kr.co.medialog.SettingInfoData
import kr.co.medialog.UploadInfoData
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import kotlin.system.exitProcess

/**
 * main activity class.
 *
 * @author : jjbae
 */
class MainActivity : FragmentActivity()
//        , BillingProcessor.IBillingHandler
{
    private var mAgentPopupDialog: AgentPopupDialog? = null
    private var mSplashView: View? = null //?????? ????????? ???????????? ?????? view.
    private var mErrorView: View? = null //The network error view.
    private var mWebview: BaseWebView? = null //mobile view page ????????? ?????? webview.
    private var mWebViewClient: MyWebViewClient? = null //The web view client.
    private var mWebChromeClient: MyWebChromeClient? = null //The web chrome client.
    private var isRestartApp = false //The is restart app.
    private var isMain = false

    private var mWebviewSub: RelativeLayout? = null // sub webView parent
    private var mSettingdata: SettingInfoData? = null

    private val mHandler = WeakHandler(this) //UI handler

    private var mReTry: Int = 0
    private var mLocationManager: LocationManager? = null

    private var mApiManager: ApiManager? = null
    private val mNetworkChangeReceiver = NetworkChangeReceiver() //???????????? check
    private val mDataSaverChangeReceiver = DataSaverChangeReceiver()
    var isIntoNotiLandingUrl: Boolean = false

    //billing
//    private var billing_subscribe_test_btn: Button? = null
//    private var billing_single_test_btn: Button? = null
//    private var googleBp: BillingProcessor? = null

    //camera & gallery
    private var mCameraReturnParam: String? = null
    private var mCameraReturnType: String? = null
    private var camera_test_btn: Button? = null
    private var gallery_test_btn: Button? = null
//    private val firebaseAction: Action
//        get() = Action.Builder(Action.Builder.VIEW_ACTION)
//                .setObject("Main Page", "android-app://com.idevel.momschoice/http/host/path", "http://host/path")
//                .build()


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //?????? ??????
//        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        mApiManager = ApiManager.getInstance(this@MainActivity)
        CookieSyncManager.createInstance(this@MainActivity)

        //?????? ?????? ??????
//        if (MyApplication.instance.isGoogleMarket) {
//            googleBp = BillingProcessor(this@MainActivity, BILLING_LICENS_KEY, this@MainActivity)
//            googleBp?.initialize()
//        }

        mWebviewSub = findViewById(R.id.webview_sub)
        mWebview = findViewById(R.id.webview_main)
        mSplashView = findViewById(R.id.view_splash)
        mSplashView?.visibility = View.VISIBLE
        mErrorView = findViewById(R.id.view_error)
        mErrorView?.visibility = View.GONE

        cleanCookie()

        //???????????? ?????? ??????
        if (!isNetworkConnected(this)) {
            showErrorDlg(NETWORK_CONNECTION_ERROR)
            return
        }

        //data saver ?????? ??????
//        if (checkDataSave(this)) {
//            showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            return
//        }

        // ??? ?????? ??? data saver ?????? Listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mDataSaverChangeReceiver.setListener(dataSaverListener)
        }

        // ??? ?????? ??? data network ?????? Listener
        mNetworkChangeReceiver.setListener(networkListener)

        //TODO ????????? ?????? ?????? ???????????? (?????? ????????? ???)
        //debug build??? ?????? ?????? ?????? ??????
//        if (BuildConfig.DEBUG) {
//            goToDevActivity()
//        } else {
//            // ??? ??????
        checkSettingInfo()
//        }
    }

    private fun goToDevActivity() {
        val i = Intent(this, DevActivity::class.java)
        startActivityForResult(i, DEV_REQUEST_CODE)
    }

    // push ??????
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val isNoti = intent.getIntExtra(IS_NOTI, -1)
        DLog.e("bjj onNewIntent :: " + isNoti + " ^ " + mWebview)

        if (isNoti == 1) {
            checkPushData(intent)
        } else if (isNoti == 0) {
            val linkType = intent.getStringExtra(PUSH_DATA_LINK_TYPE)
            val link = intent.getStringExtra(PUSH_DATA_LINK_URL)

            if (!linkType.isNullOrEmpty()) {
                if (linkType.contains("_webview")) {
                    isIntoNotiLandingUrl = true

                    if (mWebview != null) {
                        mWebview!!.loadUrl(link)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mDataSaverChangeReceiver, IntentFilter(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED))
        }

        registerReceiver(mNetworkChangeReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))

        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onResume").toJsonString())
        mWebview?.onResume()
    }

    override fun onPause() {
        super.onPause()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterReceiver(mDataSaverChangeReceiver)
        }

        unregisterReceiver(mNetworkChangeReceiver)

        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onPause").toJsonString())
        mWebview?.onPause()
    }

    private fun checkSettingInfo() {
        //??????
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH, 0L)


//        var storeInfoStr = if (MyApplication.instance.isGoogleMarket) {
//            "?store=google"
//        } else {
//            "?store=onestore"
//        }
//
//        val url = URL(getSettingUrl(this@MainActivity) + storeInfoStr)
//        mApiManager?.getSettingInfo(url.toString(), object : OnResultListener<Any> {
//            override fun onResult(result: Any, flag: Int) {
//                if (result == null) {
//                    return
//                }
//
//                mSettingdata = result as SettingInfoData
//
//                DLog.e("bjj checkSettingInfo :: "
//                        + mSettingdata?.main_url + " ^ "
//                        + mSettingdata?.os + " ^ "
//                        + mSettingdata?.version)
//
//                NORMAL_SERVER_URL = mSettingdata?.main_url?.replace("\\\\", "")
//
//                if (isIntoNotiLandingUrl) {
//                    val link = intent.getStringExtra(PUSH_DATA_LINK_URL)
//
//                    NORMAL_SERVER_URL = if (link.isNullOrEmpty()) {
//                        mSettingdata?.main_url?.replace("\\\\", "")
//                    } else {
//                        link
//                    }
//                }
//
//                if (mSettingdata?.version.isNullOrEmpty()) {
//                    appVersionCal("", true)
//                } else {
//                    appVersionCal(mSettingdata?.version ?: "")
//                }
//            }
//
//            override fun onFail(error: Any, flag: Int) {
//                appVersionCal("", true)
//            }
//        })
    }

    private fun appVersionCal(version: String, isError: Boolean = false) {
        if (isError) {
            showAlertDlg(0, R.string.popup_msg_error_version, APP_VERSION_CHECK)
        } else {
            mReTry = 0

            val serverVersion = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val appVersion = getVersionName(this)!!.split("\\.".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()

            DLog.d("bjj appVersionCal "
                    + appVersion?.size + " ^ "
                    + serverVersion[0] + " ^ "
                    + serverVersion[1] + " ^ "
                    + serverVersion[2] + " ^ "
                    + appVersion[0]!! + " ^ "
                    + appVersion[1]!! + " ^ "
                    + appVersion[2]!!)

            if (Integer.parseInt(appVersion[0] + appVersion[1]) < Integer.parseInt(serverVersion[0] + serverVersion[1])) {
                //MAJOR
                DLog.d("bjj appVersionCal MAJOR")
                showOtherAppVersionDlg()

            } else if (Integer.parseInt(appVersion[2]) < Integer.parseInt(serverVersion[2]) &&
                    Integer.parseInt(appVersion[0] + appVersion[1]) == Integer.parseInt(serverVersion[0] + serverVersion[1])) {
                //MINOR
                DLog.d("bjj appVersionCal MINOR")
                showOtherAppVersionDlg()
            } else {
                DLog.d("bjj appVersionCal NOTTING")
                mHandler.sendEmptyMessage(HANDLER_SPLASH)
            }
        }
    }

    /**
     * ???????????? ??????????????? ??????
     */
    private fun setSplash() {
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH_DELAY, 300L)
    }

    /**
     * Sets the main view.
     */
    private fun setMainView() {
        if (mWebview == null) {
            return
        }

        mHandler.sendEmptyMessageDelayed(HANDLER_NETWORK_TIMER, PING_TIME.toLong())

        mWebview?.setBackgroundColor(Color.WHITE)
        mWebview?.setJSInterface(iWebBridgeApi)
        mWebview?.loadUrl(NORMAL_SERVER_URL)

        mWebViewClient = object : MyWebViewClient(this) {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                DLog.e("bjj mWebViewClient onPageStarted : $url")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                if (isIntoNotiLandingUrl) {
                    showMainView(true)
                }

                MyApplication.instance.isChating = url.contains("note_history.php") || url.contains("friend_history.php")

                isMain = url.contains("/intro/index.php")

                DLog.e("bjj mWebViewClient onPageFinished : $url, ${mSettingdata?.main_url}")

                // TODO ??????. ?????? ??????
                removeSplash()
            }

            override fun showErrorPage() {
                DLog.e("bjj mWebViewClient showErrorPage : ")
                showErrorView()
            }

            override fun setUntouchableProgress(visible: Int) {
                DLog.e("bjj mWebViewClient setUntouchableProgress : $visible")
            }

            @SuppressWarnings("deprecation")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return urlLoading(view, Uri.parse(url))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    urlLoading(view, request?.getUrl())
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        mWebChromeClient = object : MyWebChromeClient(this, findViewById<View>(R.id.mainview) as RelativeLayout) {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (View.GONE == mSplashView?.visibility) {
                    super.onProgressChanged(view, newProgress)
                }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return super.onJsAlert(view, url, message, result)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                DLog.e("bjj MyWebChromeClient onCreateWindow ")

                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onCloseWindow(window: WebView) {
                DLog.e("bjj MyWebChromeClient onCloseWindow ")

                super.onCloseWindow(window)
            }
        }

        mWebview?.webViewClient = mWebViewClient
        mWebview?.webChromeClient = mWebChromeClient
    }

    private fun checkPushData(intent: Intent) {
        // ????????? ?????? ????????? ????????? ??????

        DLog.e("bjj checkPushData :: " + MyApplication.instance.isChating + " ^ " + intent.extras)

        if (null == intent.extras) {
            return
        }

        if (mAgentPopupDialog != null) {
            mAgentPopupDialog!!.dismiss()
            mAgentPopupDialog = null
        }

        // 8.0?????? ???????????? ?????? ????????? 0 ?????? ??????
        setIconBadge(this, 0)

        val linkUrl = intent.getStringExtra(PUSH_DATA_LINK_URL)
        mAgentPopupDialog = AgentPopupDialog(this, intent)

        mAgentPopupDialog!!.setOkClickListener(OnClickListener {
            mAgentPopupDialog!!.dismiss()

            if (linkUrl == null || linkUrl.equals("", ignoreCase = true)) {
                return@OnClickListener
            } else {
                if (mWebview != null) {
                    mWebview!!.loadUrl(linkUrl)
                }
            }
        })

        mAgentPopupDialog!!.show()

        val showTime = intent.getStringExtra(PUSH_DATA_SHOWTIME)

        if (!showTime.isNullOrEmpty()) {
            Handler().postDelayed({
                if (mAgentPopupDialog!!.isShowing) {
                    mAgentPopupDialog!!.setDisappearClose()
                }
            }, showTime.toLong())
        }
    }

    /**
     * Show error view.
     */
    fun showErrorView() {
        mErrorView?.visibility = View.VISIBLE

        val homeBtn = mErrorView?.findViewById<Button>(R.id.homeBtn)
        homeBtn?.setOnClickListener { finish() }
    }

    /**
     * Show main view.
     */
    private fun showMainView(isNotiLandingFinissh: Boolean = false) {
        mHandler.removeMessages(HANDLER_NETWORK_TIMER)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        mSplashView?.visibility = View.GONE
        mErrorView?.visibility = View.GONE

        val isNoti = intent.getIntExtra(IS_NOTI, -1)
        DLog.e("bjj onNewIntent :: showMainView " + isNotiLandingFinissh + " ^ " + isNoti + " ^ " + mWebview)

        if (!isNotiLandingFinissh && isNoti == 0) {
            val linkType = intent.getStringExtra(PUSH_DATA_LINK_TYPE)
            val link = intent.getStringExtra(PUSH_DATA_LINK_URL)

            if (!linkType.isNullOrEmpty()) {
                if (linkType.contains("_webview")) {
                    isIntoNotiLandingUrl = true

                    if (mWebview != null) {
                        mWebview!!.loadUrl(link)
                    }
                }
            }
        }

        //TODO google, onestore INAPP test
//        billing_subscribe_test_btn = findViewById(R.id.billing_test_btn)
//        billing_subscribe_test_btn?.setOnClickListener {
//            if (MyApplication.instance.isGoogleMarket) {
//                googleBp?.subscribe(this@MainActivity, BILLING_SUBSCRIBE_MONTH_PRODUCT_ID)
//            } else {
//                initOneStore()
//
//                Handler().postDelayed({
//                    val productType = getItemType(AppConstant.PRODUCT_AUTO_SUBSCRIBE_MONTH_001)
//                    requestBilling(AppConstant.PRODUCT_AUTO_SUBSCRIBE_MONTH_001, productType)
//                }, 3000L)
//            }
//        }
//        billing_single_test_btn = findViewById(R.id.billing_single_test_btn)
//        billing_single_test_btn?.setOnClickListener {
//            if (MyApplication.instance.isGoogleMarket) {
//                DLog.e("bjj BILLING buy :: " + googleBp?.isPurchased(BILLING_SINGLE_MONTH_PRODUCT_ID))
//
//                if (googleBp?.isPurchased(BILLING_SINGLE_MONTH_PRODUCT_ID) == true) {
//                    // ?????????????????? ???????????? ?????? ??? ?????? ???????????? ?????? ??????. ?????? 1??? ?????? ??? ?????? ???????????? ??? ????????? ?????? ????????? ????????????.
//                    googleBp?.consumePurchase(BILLING_SINGLE_MONTH_PRODUCT_ID)
//                }
//
//                googleBp?.purchase(this, BILLING_SINGLE_MONTH_PRODUCT_ID)
//            } else {
//                initOneStore()
//
//                Handler().postDelayed({
//                    val productType = getItemType(AppConstant.PRODUCT_INAPP_SINGLE_MONTH_002)
//                    requestBilling(AppConstant.PRODUCT_INAPP_SINGLE_MONTH_002, productType)
//                }, 3000L)
//            }
//        }

        //TODO camera & gallery
        camera_test_btn = findViewById(R.id.camera_test_btn)
        camera_test_btn?.setOnClickListener {
            doTakePhotoAction("profile", "")
        }
        gallery_test_btn = findViewById(R.id.gallery_test_btn)
        gallery_test_btn?.setOnClickListener {
            doTakeAlbumAction("profile", "")
        }

        if (BuildConfig.DEBUG) {
//            billing_subscribe_test_btn?.visibility = View.VISIBLE
//            billing_single_test_btn?.visibility = View.VISIBLE
//
//            camera_test_btn?.visibility = View.VISIBLE
//            gallery_test_btn?.visibility = View.VISIBLE
        }
    }

    private fun showAppFinishPopup() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog.setCancelable(true)
        alertDialog.setDataSaveLayout(0, R.string.popup_app_finish_message)
        alertDialog.setButtonString(R.string.popup_app_finish_ok, R.string.popup_app_finish_cancel)

        alertDialog.setOkClickListener(OnClickListener { v ->
            when (v.id) {
                R.id.btn_ok -> {
                    alertDialog.dismiss()

                    finish()
                }
                R.id.btn_cancel -> {
                    alertDialog.dismiss()
                }
            }
        })

        if (!isFinishing) {
            alertDialog.show()
        }
    }

    /**
     * Show other app version dlg.
     */
    private fun showOtherAppVersionDlg() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog?.dismiss()
            when (v.id) {
                R.id.btn_ok -> gotoPlayStore()
                R.id.btn_cancel -> finish()
            }
        })

        if (!isFinishing && !isDestroyed) {
            alertDialog?.show()
        }
    }

    private fun showDataSaveDlg(title: Int, content: Int) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setCancelable(false)
        alertDialog?.setDataSaveLayout(title, content)
        alertDialog?.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog!!.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    val intent = Intent()
                    intent.action = Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri

                    startActivity(intent)
                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show alert dlg.
     */
    private fun showAlertDlg(title: Int, content: Int, errorType: Int) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setErrorLayout(title, content)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog?.dismiss()

            if (v.id == R.id.btn_error) {
                when (errorType) {
                    NETWORK_CONNECTION_ERROR, TIMEOUT_ERROR -> {
                        finish()
                    }
                    APP_VERSION_CHECK -> {
                        if (mReTry > 2) {
                            finish()
                        } else {
                            mReTry++
                            checkSettingInfo()
                        }
                    }
                    else -> {
                    }
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show PermissionDenyDialog dlg.
     */
    private fun showPermissionDenyDialog() {
        val alertDialog = CustomAlertDialog(this)
        alertDialog?.setCancelable(false)
        alertDialog?.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg)
        alertDialog?.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog?.setOkClickListener(OnClickListener { v ->
            alertDialog!!.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")

                    startActivity(intent)
                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog?.show()
        }
    }

    /**
     * Show network error dlg.
     *
     * @param errorType the error type
     */
    private fun showErrorDlg(errorType: Int) {
        var titleRes = R.string.popup_title_server_error
        var msgRes = R.string.popup_msg_server_error

        when (errorType) {
            NETWORK_CONNECTION_ERROR -> {
                titleRes = R.string.popup_title_network_error
                msgRes = R.string.popup_msg_network_error
            }
            TIMEOUT_ERROR -> {
                titleRes = R.string.popup_title_server_error
                msgRes = R.string.popup_msg_server_error
            }
        }

        showAlertDlg(titleRes, msgRes, errorType)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            DLog.e("bjj onKeyDown ==>> " + isMain + " ^ " + mWebview!!.canGoBack())

            mWebview?.let {
                if (it.canGoBack()) {
                    if (isMain) {
                        showAppFinishPopup()
                    } else {
                        it.goBack()
                    }
                } else {
                    showAppFinishPopup()
                }

                return false
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun webviewDestroy(webview: BaseWebView) {
        webview?.let {
            it.stopLoading()
            it.removeAllViews()
            it.clearHistory()
            it.clearCache(true)
            it.destroy()
        }
    }

    /**
     * Goto play store.
     */
    private fun gotoPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW)

        if (MyApplication.instance.isGoogleMarket) {
            intent.data = Uri.parse("market://details?id=$packageName")
        } else {
            intent.data = Uri.parse("https://onesto.re/0000748084")
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        mWebview?.sendEvent(IdevelServerScript.SET_APP_STATUS, AppStatusInfo("onDestroy").toJsonString())

        cleanCookie()

        mWebview?.let {
            webviewDestroy(it)
        }

//        if (MyApplication.instance.isGoogleMarket) {
//            if (googleBp != null) {
//                googleBp!!.release()
//            }
//        }

        super.onDestroy()

        // ??? ????????? PurchaseClient??? ???????????? ???????????? terminate ????????????.
//        if (mPurchaseClient != null) {
//            mPurchaseClient!!.terminate()
//        }
    }

    /**
     * Restart app.
     */
    fun restartApp() {
        cleanCookie()
        isRestartApp = true

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        exitProcess(0)
    }

    /**
     * Clean cookie.
     */
    private fun cleanCookie() {
        DLog.e("isRestartApp ==>> $isRestartApp")

        if (!isRestartApp) {
            this@MainActivity.runOnUiThread {
                mWebview?.clearCache(true)
                mWebview?.clearHistory()

                val cookieSyncMngr = CookieSyncManager.createInstance(this@MainActivity)
                cookieSyncMngr.startSync()
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookie()
                cookieManager.removeSessionCookie()
                cookieSyncMngr.stopSync()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
//        FirebaseUserActions.getInstance().start(firebaseAction)
    }

    public override fun onStop() {
        super.onStop()
//        FirebaseUserActions.getInstance().end(firebaseAction)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        // google inapp result
//        if (MyApplication.instance.isGoogleMarket) {
//            val isGoogleBpResult = googleBp?.handleActivityResult(requestCode, resultCode, intent)
//            val googleBpResultCode = intent?.extras?.get("RESPONSE_CODE")
//            val googleBpResultData = intent?.extras?.get("INAPP_PURCHASE_DATA")
//
//            DLog.e("bjj BILLING onActivityResult "
//                    + isGoogleBpResult + " ^ "
//                    + intent + " ^ "
//                    + intent?.extras + " ^ "
//                    + googleBpResultCode + " ^ "
//                    + requestCode + " ^ "
//                    + resultCode)
//
////            if(googleBpResultCode == Constants.BILLING_RESPONSE_RESULT_OK){
//            //TODO ????????? ?????????
////                Bundle[{INAPP_PURCHASE_DATA={"orderId":"GPA.3353-4786-9413-58565","packageName":"com.idevel.momschoice","productId":"single_month","purchaseTime":1597240260045,"purchaseState":0,"developerPayload":"inapp:single_month:0075b5a1-6a7a-417c-ac00-6c993441b85c","purchaseToken":"dgkbeeoiclkjpdoafoogehim.AO-J1OzUdS_d7A_lyCiKHUC825T4DDQu86PdwB0R0tRgrrv28mBL4YRbY1v_hoXsWW8gNt-34QRjgIPLyBvXeEoPf4FbDwytbUTjd0hSzOMw9pEIhU3NO6XQhC3s8F_L81UwzFOOnM4x"}, INAPP_DATA_SIGNATURE=CITI8oTFUphcpTels23QpNxcWIZzdBULJK5VZwtIta9dL46mOlTTfMQeYM13Wk5NUdhbxQ3D1vPsYxEed+nEb30jkDqQyX7UMnFhIiTp6T8VI1PIsuQzhoIyWoCfh0Q3eP7W+yYjw8VluD2ebTo69T6x4LqO6y3lcQxlkMrKL9pRbG6m7NgqoBRrREBm8H9fPexplvmc//AEmQUmGIpoGoE8MRMoxEPkC0+3l+HhFk3qktZX1pzb4vufUtHjpCevygI2qQfil0Nt1gtccE3/6p8bUu5MDf1vNct+qvsQ5X1QkF02a9d+xuDmnIxXg+CFqu5GIBA3p/iI2Np2s1knrw==, RESPONSE_CODE=0}]
////                mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(true, googleBpResultData.toString()).toJsonString())
////            }
//
//            if (isGoogleBpResult == true) {
//                return
//            }
//        }

        super.onActivityResult(requestCode, resultCode, intent)

        when (requestCode) {
            PICK_FROM_ALBUM -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (intent?.data != null) {
                        sendGalleryImage(intent.data!!)
                    }
                }
            }

            PICK_FROM_CAMERA -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (intent?.extras?.get("data") != null) {
                        if (intent?.extras?.get("data") is Bitmap) {
                            sendCameraImage(intent?.extras?.get("data") as Bitmap)
                        }
                    }
                }
            }

            DEV_REQUEST_CODE -> {
                checkSettingInfo()
            }

            TEL_REQUEST_CODE -> {
            }

            KAKAO_LOGIN_REQUEST_CODE -> {
                //TODO ????????? ????????? ??????
            }

            X_PAY_REQUEST_CODE -> {
            }

//            ONESTORE_PURCHASE_REQUEST_CODE -> {
//                /*
//                 * launchPurchaseFlowAsync API ?????? ??? ???????????? intent ???????????? handlePurchaseData??? ????????? ???????????? ???????????????.
//                 * ?????? ?????? ?????? ????????? launchPurchaseFlowAsync ?????? ??? ????????? PurchaseFlowListener ??? ????????? ???????????????.
//                 */
//                if (resultCode == Activity.RESULT_OK) {
//                    if (mPurchaseClient != null) {
//                        if (!mPurchaseClient!!.handlePurchaseData(intent)) {
//                            DLog.e("bjj onActivityResult handlePurchaseData false ");
//                            // listener is null
//                        }
//                    }
//                } else {
//                    DLog.e("bjj onActivityResult user canceled");
//                    // user canceled , do nothing..
//                }
//            }
//
//            ONESTORE_LOGIN_REQUEST_CODE -> {
//                // TODO onestore ????????? ??????
//            }
        }
    }

    /**
     * ?????? ??????
     */
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val listPermissionsNeeded = MyApplication.PERMISSIONS.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            return if (listPermissionsNeeded.isNotEmpty()) {
//                val viewPermission = findViewById<View>(R.id.view_permissioin)
//                viewPermission.visibility = View.VISIBLE
//
//                findViewById<ImageButton>(R.id.btn_permission_ok).setOnClickListener {
                Handler().postDelayed({
                    showPermissionView(listPermissionsNeeded)
                }, 1000L)
//                }
//
//                findViewById<ImageButton>(R.id.btn_permission_cancel).setOnClickListener {
//                    finish()
//                }

                false
            } else { // ?????? ????????? ??????
                true
            }
        }

        return true
    }

    private fun showPermissionView(listPermissionsNeeded: List<String>) {
//        val viewPermission = findViewById<View>(R.id.view_permissioin)
        var isPermissionGranted = true

        for (permissions in listPermissionsNeeded) {
            if (ActivityCompat.checkSelfPermission(this, permissions) != PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = false
                break
            }
        }

        // Permission ?????? ??????
        if (isPermissionGranted) {
//            viewPermission.visibility = View.GONE
        }
        // Permission ?????? ??????
        else {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty()) {
            var isGranted = true

            for (granted in grantResults) {
                if (granted != PackageManager.PERMISSION_GRANTED) isGranted = false
            }

            if (isGranted) {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

                if (keyguardManager.isKeyguardLocked) {
                    return
                }

//                val viewPermission = findViewById<View>(R.id.view_permissioin)
//                viewPermission.visibility = View.GONE

                setMainView()
            } else {
                var isRationale = true
                val listPermissionsNeeded = MyApplication.PERMISSIONS.filter {
                    ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }

                for (permissions in listPermissionsNeeded) {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions)) {
                        isRationale = false
                        break
                    }
                }

                if (!isRationale) {
                    // ?????????????????? ????????????
                    showPermissionDenyDialog()
                } else {
                    finish()
                }
            }
        }
    }

    private fun removeSplash() {
        if (isFinishing) {
            return
        }

        (this@MainActivity as Activity).runOnUiThread {
            setSplash()
        }
    }

    //?????? ????????????
    private fun openSharePopup(url: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, url)
        sendIntent.type = "text/plain"

        startActivity(Intent.createChooser(sendIntent, ""))
    }

    // web page clear history
    private fun pageClearHistory() {
        mWebview?.clearHistory()
        mWebview?.removeAllViews()
    }

//    private fun initOneStore() {
//        // PurchaseClient ????????? - context ??? Signature ????????? ?????? public key ??? ??????????????? ???????????????.
//        mPurchaseClient = PurchaseClient(this, AppSecurity.getPublicKey())
//        // ???????????? ???????????? ??????????????? ?????? ????????? ???????????? ???????????????.
//        mPurchaseClient?.connect(mServiceConnectionListener)
//    }

    /**
     * WEB INTERFACE
     */
    private val iWebBridgeApi = object : IWebBridgeApi {
        override fun pageClearHistory() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.pageClearHistory()
            }
        }

        override fun openSharePopup(url: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.openSharePopup(url)
            }
        }

        override fun getPushRegId() {
            (this@MainActivity as Activity).runOnUiThread {
                val regId = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_REG_ID)
                mWebview?.sendEvent(IdevelServerScript.GET_PUSH_REG_ID, getPushRegIdInfo(regId!!, "AOS").toJsonString())
            }
        }

        override fun restartApp() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.restartApp()
            }
        }

        override fun finishApp() {
            (this@MainActivity as Activity).runOnUiThread {
                System.exit(0)
            }
        }

        override fun getAppVersion() {
            (this@MainActivity as Activity).runOnUiThread {
                val version = getVersionName(this@MainActivity)
                mWebview?.sendEvent(IdevelServerScript.GET_APP_VERSION, GetAppVersionInfo(version!!).toJsonString())
            }
        }

        override fun requestCallPhone(data: RequestCallPhoneInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                try {
                    val callIntent = Intent(Intent.ACTION_DIAL)
                    callIntent.data = Uri.parse("tel:${data.phoneNumber}")
                    startActivity(callIntent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }

        override fun requestExternalWeb(data: RequestExternalWebInfo) {
            (this@MainActivity as Activity).runOnUiThread {
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.url))
                startActivity(intent)
            }
        }

        override fun removeSplash() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.removeSplash()
            }
        }

        override fun getGpsInfo() {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.getGPSLoacation()
            }
        }

        override fun readyOneStoreBilling() {
//            (this@MainActivity as Activity).runOnUiThread {
//                this@MainActivity.initOneStore()
//            }
        }

        override fun requestBuyProduct(data: RequestBuyProductInfo) {
//            (this@MainActivity as Activity).runOnUiThread {
//                val productType = getItemType(data.productId)
//                this@MainActivity.requestBilling(data.productId, productType)
//            }
        }

        override fun openCamera(type: String, param: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.doTakePhotoAction(type, param)
            }
        }

        override fun openGallery(type: String, param: String) {
            (this@MainActivity as Activity).runOnUiThread {
                this@MainActivity.doTakeAlbumAction(type, param)
            }
        }

        override fun setPushVibrate(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_VIBRATE, isBool)
            }
        }

        override fun setPushBeep(isBool: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_BEEP, isBool)
            }
        }


        override fun setAutoLogin(isAuto: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN, isAuto)
            }
        }

        override fun getAutoLogin() {
            (this@MainActivity as Activity).runOnUiThread {
                val isAuto = SharedPreferencesUtil.getBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN, false)

                mWebview?.sendEvent(IdevelServerScript.GET_AUTO_LOGIN, AutoLoginInfo(isAuto).toJsonString())
            }
        }

        override fun setAccount(id: String, pw: String) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_ID, id)
                SharedPreferencesUtil.setString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_PW, pw)
            }
        }

        override fun getAccount() {
            (this@MainActivity as Activity).runOnUiThread {
                val id = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_ID)
                val pw = SharedPreferencesUtil.getString(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN_PW)

                mWebview?.sendEvent(IdevelServerScript.GET_ACCOUNT, AccountInfo(id!!, pw!!).toJsonString())
            }
        }

        override fun setPushOnOff(isOnOff: Boolean) {
            (this@MainActivity as Activity).runOnUiThread {
                SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PUSH_ONOFF, isOnOff)
            }
        }
    }

    companion object {
        private val HANDLER_NETWORK_TIMER = 1 // The network timer handler.
        private val HANDLER_SPLASH = 2 // ???????????? ?????? ?????????.
        private val HANDLER_SPLASH_DELAY = 3 // ???????????? delay.

        private val DEV_REQUEST_CODE = 1000 // ?????? ???????????? ???????????? ??? flag???.
        private val X_PAY_REQUEST_CODE = 999
        private val KAKAO_LOGIN_REQUEST_CODE = 998
        private val TEL_REQUEST_CODE = 997
        private val PICK_FROM_ALBUM = 996
        private val PICK_FROM_CAMERA = 995


        private val PING_TIME = 100000 //The ping time.
        private val NETWORK_CONNECTION_ERROR = 1 //The network connection error.
        private val TIMEOUT_ERROR = 2 //The timeout error.
        private val APP_VERSION_CHECK = 4 //??? ?????? check.

//        //onestore inapp
//        private val ONESTORE_LOGIN_REQUEST_CODE = 1001
//        private val ONESTORE_PURCHASE_REQUEST_CODE = 2001
//        private val IAP_API_VERSION = 5
//
//        //google inapp
//        private val REQ_PERMISSION_CODE = 1
//        private val BILLING_SUBSCRIBE_MONTH_PRODUCT_ID = "subscribe_month"
//        private val BILLING_SINGLE_MONTH_PRODUCT_ID = "single_month"
//        private val BILLING_LICENS_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsEH3Bt7tpFlqDW57EgiDX0FT19NRCoTEGG8SIj002k2nbc/zlhahr3LalchTfVsJ9ctB1Mls37qWYxWhxnWRk86HV25n1422FQzJL807kx8vwastA/ZcBtl7AjOj0POWEGKO2paIE2IJ9npsBZlVLvRaBiAe25sPIPgcCH9nsI037JUxzExbJJE4T1o1IQtVNOm9CucTG413qjtETfQr2kAGrZ3xRX1gdPmChvqvXfxJCogXDQY5M5Ki1qRCI+htim8AxtRi6uXwwrkg6Q7bGIv36wDF+6azc3wugtei+OiDPpxlh+ADOAnJNqSuMYoSbqob3Uaj8VxqFyGK9/MgoQIDAQAB"
    }

    private class WeakHandler(act: MainActivity) : Handler() {
        private val ref: WeakReference<MainActivity> = WeakReference(act)

        override fun handleMessage(msg: Message) {
            val act = ref.get()

            if (act != null) {
                when (msg.what) {
                    HANDLER_NETWORK_TIMER -> {
                        act.showErrorDlg(TIMEOUT_ERROR)
                    }
                    HANDLER_SPLASH -> if (act.checkPermission()) {
                        act.setMainView()
                    }
                    HANDLER_SPLASH_DELAY -> {
                        act.showMainView()
                    }
                }
            }
        }
    }

    private val networkListener = object : NetworkChangeListener {
        override fun onNetworkDisconnected() {
            DLog.e("bjj Listener onNetworkDisconnected")

            // ???????????? ?????? ??? onNetworkDisconnected ???????????? ?????? 1??? ????????? ??? ???????????? ?????? ???????????? ???????????? ???????????? ??????????????? ???
            (this@MainActivity as Activity).runOnUiThread {
                Handler().postDelayed({
                    if (getNetworkInfo(applicationContext) == NETWORK_TYPE_ETC) {
                        showErrorDlg(NETWORK_CONNECTION_ERROR)
                    }
                }, 1000L)
            }
        }

        override fun onNetworkconnected() {
            DLog.e("bjj Listener onNetworkconnected")
        }

        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

//            (this@MainActivity as Activity).runOnUiThread {
//                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            }
        }
    }

    private var dataSaverListener = object : IDataSaverListener {
        override fun onDataSaverChanged() {
            DLog.e("bjj Listener onDataSaverChanged")

//            (this@MainActivity as Activity).runOnUiThread {
//                showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            }
        }
    }

    // ?????? kakao??? ?????????
    private fun urlLoading(view: WebView?, uri: Uri?): Boolean {
        if (uri.toString().isNullOrEmpty()) {
            return false
        }

        val url = uri.toString()
        val scheme = uri!!.scheme

        DLog.e("bjj uri.toString() = $uri, ${uri!!.scheme}")

        when (scheme) {
            "https" -> return false
            "intent" -> {
                try {
                    // Intent ??????
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

                    // ?????? ????????? ?????? ????????? ??? ??????
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, KAKAO_LOGIN_REQUEST_CODE)

                        DLog.e("bjj urlLoading intent ACTIVITY: ${intent.getPackage()}")

                        return true
                    }

                    // Fallback URL??? ????????? ?????? ????????? ??????
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (fallbackUrl != null) {
                        view?.loadUrl(fallbackUrl)

                        DLog.e("bjj urlLoading intent FALLBACK: $fallbackUrl")

                        return true
                    }

                    DLog.e("bjj urlLoading intent Could not parse anythings")

                } catch (e: URISyntaxException) {
                    DLog.e("bjj urlLoading intent Invalid intent request", e)
                }

                return true
            }
            "tel" -> {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
                startActivityForResult(dialIntent, TEL_REQUEST_CODE)

                return true
            }
            else -> return false
        }
    }

    private fun isInstalledApp(context: Context, packageName: String?): Boolean {
        val appList = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in appList) {
            if (appInfo.packageName == packageName) {
                return true
            }
        }

        return false
    }


    //Google BILLING
//    override fun onBillingInitialized() {
//        // * ?????? ????????? ??????
//        // productId: ????????? sku (ex) no_ads)
//        // details: ?????? ?????? ??????
////        val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(true).toJsonString())
//
//        DLog.e("bjj BILLING onBillingInitialized")
//    }
//
//    override fun onPurchaseHistoryRestored() {
//        // * ?????? ????????? ?????????????????? ??????
//        // bp.loadOwnedPurchasesFromGoogle() ?????? ?????? ??????
////        val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        DLog.e("bjj BILLING onPurchaseHistoryRestored")
//    }
//
//    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
//        // * ?????? ????????? ??????
//        // errorCode == Constants.BILLING_RESPONSE_RESULT_USER_CANCELED ?????????
//        // ???????????? ????????? ?????? ?????? ?????????????????? ?????? ???????????? ???????????????.
//
////         val skuDetails: SkuDetails = googleBp?.getPurchaseListingDetails(productId) as SkuDetails
//
//
//        DLog.e("bjj BILLING onProductPurchased " + productId + " ^ " + details)
//
//        //TODO ???????????? ????????? activityresult??? ????????? ??????
//        //single_month purchased at Wed Aug 12 22:51:00 GMT+09:00 2020(GPA.3353-4786-9413-58565). Token: dgkbeeoiclkjpdoafoogehim.AO-J1OzUdS_d7A_lyCiKHUC825T4DDQu86PdwB0R0tRgrrv28mBL4YRbY1v_hoXsWW8gNt-34QRjgIPLyBvXeEoPf4FbDwytbUTjd0hSzOMw9pEIhU3NO6XQhC3s8F_L81UwzFOOnM4x, Signature: CITI8oTFUphcpTels23QpNxcWIZzdBULJK5VZwtIta9dL46mOlTTfMQeYM13Wk5NUdhbxQ3D1vPsYxEed+nEb30jkDqQyX7UMnFhIiTp6T8VI1PIsuQzhoIyWoCfh0Q3eP7W+yYjw8VluD2ebTo69T6x4LqO6y3lcQxlkMrKL9pRbG6m7NgqoBRrREBm8H9fPexplvmc//AEmQUmGIpoGoE8MRMoxEPkC0+3l+HhFk3qktZX1pzb4vufUtHjpCevygI2qQfil0Nt1gtccE3/6p8bUu5MDf1vNct+qvsQ5X1QkF02a9d+xuDmnIxXg+CFqu5GIBA3p/iI2Np2s1knrw==
//        mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(true, details.toString()).toJsonString())
//
//
////        if (productId.equals("single_month")) {
//        // TODO: ?????? ??? ????????? ???????????????! ????????? ?????????
////            val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////            SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        // * ?????? ????????? 1??? ???????????? ??????????????? ???????????? ???????????? consume?????? ?????????,
//        // ?????? ?????? ????????? 100?????? ?????? ???????????? ?????? ???????????? ???????????? ??????????????? ????????? ??? ????????? ??????????????? ???????????????.
//        // bp.consumePurchase(Config.Sku);
////        }
//    }
//
//    override fun onBillingError(errorCode: Int, error: Throwable?) {
//        // * ????????? ??????????????????.
//        val isPurchased = googleBp?.isPurchased(BILLING_SINGLE_MONTH_PRODUCT_ID) ?: false
//
//        if (errorCode != Constants.BILLING_RESPONSE_RESULT_USER_CANCELED) {
//            mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(false, "").toJsonString())
//        }
//
//        DLog.e("bjj BILLING onBillingError " + errorCode + " ^ " + error?.message + " ^ " + isPurchased + " ^ " + googleBp)
//    }


    /**
     * GPS
     */
    private fun getGPSLoacation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "???????????? ?????? ?????? ????????? ???????????? ???????????????", Toast.LENGTH_SHORT).show()
            return
        }

        if (mLocationManager == null) {
            mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, mLocationListener)
        mLocationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1f, mLocationListener)
    }


    private val mLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            var geoCoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses = geoCoder.getFromLocation(location!!.latitude, location!!.longitude, 1) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            DLog.d("bjj getGPSLoacation 00 : $addresses, ${addresses?.size}")

            if (addresses == null) {
                return
            }

            if (addresses.size == 0) {
                return
            }

            val address = addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

            val city = addresses[0].locality
            val state = addresses[0].adminArea
            val country = addresses[0].countryName
            val postalCode = addresses[0].postalCode
            val knownName = addresses[0].featureName // Only if available else return NULL

            val latitudeStr: String = java.lang.String.valueOf(location!!.latitude)
            val longitudeStr: String = java.lang.String.valueOf(location!!.longitude)

            if (location.provider == LocationManager.GPS_PROVIDER) {
                DLog.d("bjj getGPSLoacation aa : $address, ${latitudeStr}, ${longitudeStr}")
            } else {
                DLog.d("bjj getGPSLoacation bb : $address, ${latitudeStr}, ${longitudeStr}")
            }

            val gpsData = getLocationInfo(latitudeStr, longitudeStr, address)

            if (gpsData != null) {
                mWebview?.sendEvent(IdevelServerScript.GET_GPS_INFO, gpsData!!.toJsonString())
                mLocationManager?.removeUpdates(this)
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }


//    /**
//     * ???????????? INAPP
//     */
//    private var mPurchaseClient: PurchaseClient? = null
//    private var progressDialog: ProgressDialog? = null
//
//    /*
//     * PurchaseClient??? connect API ?????? ?????????
//     * ????????? ??????/?????? ??? ???????????? ????????? ??????????????? ??????????????? ?????? ????????? ???????????????.
//     */
//    private var mServiceConnectionListener: ServiceConnectionListener = object : ServiceConnectionListener {
//        override fun onConnected() {
//            DLog.e("bjj inapp Service connected")
//            checkBillingSupportedAndLoadPurchases()
//        }
//
//        override fun onDisconnected() {
//            DLog.e("bjj inapp Service disconnected")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp connect onError, ???????????? ??????????????? ??????????????? ???????????????")
//            updateOrInstallOneStoreService()
//        }
//    }
//
//    /*
//     * ???????????? isBillingSupportedAsync API??? ???????????? ??????????????? ????????? ??? ?????? ???????????? ???????????????.
//     * ?????? ?????????????????? API??? ???????????? ???????????? ?????? ???????????????(inapp)??? ?????????????????? ???????????????(auto) ????????? ???????????????.
//     */
//    private fun checkBillingSupportedAndLoadPurchases() {
//        DLog.e("bjj inapp checkBillingSupportedAndLoadPurchases()")
//
//        if (mPurchaseClient != null) {
//            showProgress(this)
//            mPurchaseClient!!.isBillingSupportedAsync(IAP_API_VERSION, mBillingSupportedListener)
//        }
//    }
//
//    private fun updateOrInstallOneStoreService() {
//        PurchaseClient.launchUpdateOrInstallFlow(this)
//    }
//
//    /*
//     * PurchaseClient??? isBillingSupportedAsync (??????????????????) API ?????? ?????????
//     */
//    private var mBillingSupportedListener: BillingSupportedListener = object : BillingSupportedListener {
//        override fun onSuccess() {
//            DLog.e("bjj inapp isBillingSupportedAsync onSuccess")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(true).toJsonString())
//
//            hideProgress()
//
//            // isBillingSupportedAsync ?????? ????????????,
//            // ????????????????????? ????????? ???????????? ??????????????? ??? ??????????????? ??????????????? ????????? ???????????????.
//            loadPurchases()
//        }
//
//        override fun onError(result: IapResult) {
//            DLog.e("bjj inapp isBillingSupportedAsync onError, $result")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//
//            hideProgress()
//
//            // RESULT_NEED_LOGIN ???????????? ???????????? ?????????????????? life cycle??? ?????? ??????????????? ???????????? ???????????? ???????????????.
//            if (IapResult.RESULT_NEED_LOGIN == result) {
//                loadLoginFlow()
//            }
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp isBillingSupportedAsync onError, onErrorSecurityException ???????????? ???????????? ????????? ??? ??? ????????????")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//
//            hideProgress()
//            oneStoreAlert("???????????? ???????????? ????????? ??? ??? ????????????")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp isBillingSupportedAsync onError, onErrorSecurityException ????????? ????????? ????????? ?????????????????????")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//
//            hideProgress()
//            oneStoreAlert("????????? ????????? ????????? ?????????????????????")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp isBillingSupportedAsync onError, onErrorSecurityException ???????????? ??????????????? ??????????????? ???????????????")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//    }
//
//    /*
//     * PurchaseClient??? queryProductsAsync API (??????????????????) ?????? ?????????
//     */
//    private var mQueryProductsListener: QueryProductsListener = object : QueryProductsListener {
//        override fun onSuccess(productDetails: List<ProductDetail>) {
//            DLog.e("bjj inapp queryProductsAsync onSuccess, $productDetails")
//
//            hideProgress()
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp queryProductsAsync onError, ???????????? ???????????? ????????? ??? ??? ????????????")
//
//            hideProgress()
//            oneStoreAlert("???????????? ???????????? ????????? ??? ??? ????????????")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp queryProductsAsync onError, ????????? ????????? ????????? ?????????????????????")
//
//            hideProgress()
//            oneStoreAlert("????????? ????????? ????????? ?????????????????????")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp queryProductsAsync onError, ???????????? ??????????????? ??????????????? ???????????????")
//
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//
//        override fun onError(result: IapResult) {
//            DLog.e("bjj inapp queryProductsAsync onError, $result")
//
//            hideProgress()
//            oneStoreAlert(result.description)
//        }
//    }
//
//    /*
//     * PurchaseClient??? queryPurchasesAsync API (??????????????????) ?????? ?????????
//     */
//    private var mQueryPurchaseListener: QueryPurchaseListener = object : QueryPurchaseListener {
//        override fun onSuccess(purchaseDataList: List<PurchaseData>, productType: String) {
//            DLog.e("bjj inapp queryPurchasesAsync onSuccess, $purchaseDataList")
//
//            hideProgress()
//
//            if (IapEnum.ProductType.IN_APP.type.equals(productType, ignoreCase = true)) {
//                onLoadPurchaseInApp(purchaseDataList)
//            } else if (IapEnum.ProductType.AUTO.type.equals(productType, ignoreCase = true)) {
//                onLoadPurchaseAuto(purchaseDataList)
//            }
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp queryPurchasesAsync onError, ???????????? ???????????? ????????? ??? ??? ????????????")
//
//            hideProgress()
//            oneStoreAlert("???????????? ???????????? ????????? ??? ??? ????????????")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp queryPurchasesAsync onError, ????????? ????????? ????????? ?????????????????????")
//
//            hideProgress()
//            oneStoreAlert("????????? ????????? ????????? ?????????????????????")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp queryPurchasesAsync onError, ???????????? ??????????????? ??????????????? ???????????????")
//
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//
//        override fun onError(result: IapResult) {
//            DLog.e("bjj inapp queryPurchasesAsync onError, $result")
//
//            hideProgress()
//            oneStoreAlert(result.description)
//        }
//    }
//
//
//    /*
//     * PurchaseClient??? consumeAsync API (????????????) ?????? ?????????
//     */
//    private var mConsumeListener: ConsumeListener = object : ConsumeListener {
//        override fun onSuccess(purchaseData: PurchaseData) {
//            DLog.e("bjj inapp consumeAsync onSuccess, $purchaseData")
//
//            hideProgress()
//
////            updateCoinsPurchased(purchaseData.productId)
////            val text = String.format(getString(R.string.msg_purchase_success_inapp), getPurchasedCoins(purchaseData.productId))
//
////            alert("?????? ?????? " + purchaseData.productId, true)
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp consumeAsync onError, ???????????? ???????????? ????????? ??? ??? ????????????")
//            hideProgress()
//            oneStoreAlert("???????????? ???????????? ????????? ??? ??? ????????????")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp consumeAsync onError, ????????? ????????? ????????? ?????????????????????")
//            hideProgress()
//            oneStoreAlert("????????? ????????? ????????? ?????????????????????")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp consumeAsync onError, ???????????? ??????????????? ??????????????? ???????????????")
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//
//        override fun onError(result: IapResult) {
//            DLog.e("bjj inapp consumeAsync onError, $result")
//            hideProgress()
//            oneStoreAlert(result.description)
//        }
//    }
//
//    /*
//     * PurchaseClient??? manageRecurringProductAsync API (??????????????? ????????????) ?????? ?????????
//     */
//    private var mManageRecurringProductListener: ManageRecurringProductListener = object : ManageRecurringProductListener {
//        override fun onSuccess(purchaseData: PurchaseData, manageAction: String) {
//            DLog.e("bjj inapp manageRecurringProductAsync onSuccess, $manageAction $purchaseData")
//
//            hideProgress()
//
//            if (IapEnum.RecurringAction.CANCEL.type.equals(manageAction, ignoreCase = true)) {
//                oneStoreAlert(getString(R.string.msg_setting_cancel_auto_complete))
//            } else {
//                oneStoreAlert(getString(R.string.msg_setting_resubscribe_auto_complete))
//            }
//
//            loadPurchase(IapEnum.ProductType.AUTO)
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp manageRecurringProductAsync onError, ???????????? ???????????? ????????? ??? ??? ????????????")
//
//            hideProgress()
//            oneStoreAlert("???????????? ???????????? ????????? ??? ??? ????????????")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp manageRecurringProductAsync onError, ????????? ????????? ????????? ?????????????????????")
//
//            hideProgress()
//            oneStoreAlert("????????? ????????? ????????? ?????????????????????")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp manageRecurringProductAsync onError, ???????????? ??????????????? ??????????????? ???????????????")
//
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//
//        override fun onError(result: IapResult) {
//            DLog.e("bjj inapp manageRecurringProductAsync onError, $result")
//
//            hideProgress()
//            oneStoreAlert(result.description)
//        }
//    }
//
//    /*
//     * PurchaseClient??? launchPurchaseFlowAsync API (??????) ?????? ?????????
//     */
//    private var mPurchaseFlowListener: PurchaseFlowListener = object : PurchaseFlowListener {
//        override fun onSuccess(purchaseData: PurchaseData) {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onSuccess, $purchaseData")
//            hideProgress()
//
//            // ???????????? ??? developer payload ????????? ????????????.
//            if (!isValidPayload(purchaseData.developerPayload)) {
//                DLog.e("bjj inapp onSuccess() :: payload is not valid.")
////                alert(getString(R.string.msg_alert_dev_payload_invalid))
//
//                mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(false, "").toJsonString())
//                return
//            }
//
//            // ???????????? ??? signature ????????? ????????????.
//            val validPurchase = AppSecurity.verifyPurchase(purchaseData.purchaseData, purchaseData.signature)
//            DLog.e("bjj inapp onSuccess() :: verifyPurchase $validPurchase")
//
//            if (validPurchase) {
//                if (AppConstant.PRODUCT_AUTO_SUBSCRIBE_MONTH_001.equals(purchaseData.productId)) {
//                    // ????????????????????? ????????? ???????????? ?????????.
////                    alert(getString(R.string.msg_purchase_success_auto), true)
////                    saveMonthlyItem(purchaseData)
//                } else {
//                    // ???????????????(inapp)??? ?????? ?????? ??? ????????? ????????????.
//                    consumeItem(purchaseData)
//                }
//
//                mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(true, purchaseData.purchaseData).toJsonString())
//
//            } else {
////                alert(getString(R.string.msg_alert_signature_invalid))
//
//                mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(false, "").toJsonString())
//            }
//        }
//
//        override fun onError(result: IapResult) {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onError, $result")
//            hideProgress()
//            oneStoreAlert(result.description)
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onError, ???????????? ???????????? ????????? ??? ??? ????????????")
//            hideProgress()
//            oneStoreAlert("???????????? ???????????? ????????? ??? ??? ????????????")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onError, ????????? ????????? ????????? ?????????????????????")
//            hideProgress()
//            oneStoreAlert("????????? ????????? ????????? ?????????????????????")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onError, ???????????? ??????????????? ??????????????? ???????????????")
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//    }
//
//    /*
//     * PurchaseClient??? launchLoginFlowAsync API (?????????) ?????? ?????????
//     */
//    private var mLoginFlowListener: LoginFlowListener = object : LoginFlowListener {
//        override fun onSuccess() {
//            DLog.e("bjj inapp launchLoginFlowAsync onSuccess")
//            hideProgress()
//            // ?????????????????? ????????? ???????????? ?????? ?????? ??????????????? ??????????????? ?????????.
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(true).toJsonString())
//        }
//
//        override fun onError(result: IapResult) {
//            DLog.e("bjj inapp launchLoginFlowAsync onError, $result")
//            hideProgress()
//            oneStoreAlert(result.description)
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp launchLoginFlowAsync onError, ???????????? ???????????? ????????? ??? ??? ????????????")
//            hideProgress()
//            oneStoreAlert("???????????? ???????????? ????????? ??? ??? ????????????")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp launchLoginFlowAsync onError, ????????? ????????? ????????? ?????????????????????")
//            hideProgress()
//            oneStoreAlert("????????? ????????? ????????? ?????????????????????")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp launchLoginFlowAsync onError, ???????????? ??????????????? ??????????????? ???????????????")
//            hideProgress()
//            updateOrInstallOneStoreService()
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//        }
//    }
//
//    private fun showProgress(context: Context?) {
//        runOnUiThread(Runnable {
//            if (progressDialog != null && progressDialog!!.isShowing) {
//                return@Runnable
//            } else {
//                progressDialog = ProgressDialog(context)
//                progressDialog!!.setMessage("Server connection...")
//                progressDialog!!.show()
//            }
//        })
//    }
//
//    private fun hideProgress() {
//        progressDialog!!.dismiss()
//    }
//
//    private fun oneStoreAlert(message: String?) {
//        oneStoreAlert(message, false)
//    }
//
//    private fun oneStoreAlert(message: String?, isHtml: Boolean) {
//        runOnUiThread {
//            DLog.e("bjj oneStoreAlert :: " + message + " ^ " + isHtml)
//
//            val bld = AlertDialog.Builder(this@MainActivity)
//            bld.setMessage(if (isHtml) Html.fromHtml(message) else message)
//                    .setPositiveButton("??????", null)
//                    .create()
//                    .show()
//        }
//    }
//
//    private fun oneStoreAlertDecision(message: String?, posListener: DialogInterface.OnClickListener?) {
//        runOnUiThread {
//            DLog.e("bjj oneStoreAlertDecision :: " + message)
//
//            val bld = AlertDialog.Builder(this@MainActivity)
//            bld.setMessage(message)
//                    .setPositiveButton("ok", posListener)
//                    .setNegativeButton("cancel", null)
//                    .create()
//                    .show()
//        }
//    }
//
//    private fun loadLoginFlow() {
//        DLog.e("bjj inapp loadLoginFlow()")
//
//        if (mPurchaseClient != null) {
//            oneStoreAlertDecision("???????????? ?????? ????????? ?????? ??? ??? ????????????. ????????? ???????????????????", DialogInterface.OnClickListener { dialog, which ->
//                if (!mPurchaseClient!!.launchLoginFlowAsync(IAP_API_VERSION, this@MainActivity, ONESTORE_LOGIN_REQUEST_CODE, mLoginFlowListener)) {
//                    // listener is null
//                }
//            })
//        }
//    }
//
//    // ???????????????????????? ????????? ???????????????(inapp)??? ?????? Signature ????????? ????????????, ????????? ?????? ??????????????? ???????????????.
//    private fun onLoadPurchaseInApp(purchaseDataList: List<PurchaseData>) {
//        DLog.e("bjj inapp onLoadPurchaseInApp() :: purchaseDataList - $purchaseDataList")
//
//        for (purchase in purchaseDataList) {
//            val result = AppSecurity.verifyPurchase(purchase.purchaseData, purchase.signature)
//
//            if (result) {
//                consumeItem(purchase)
//            }
//        }
//    }
//
//    // ???????????????????????? ????????? ???????????????(auto)??? ?????? Signature ????????? ????????????, ????????? ?????? ?????? UI ??????????????? ?????? ??????????????? ?????????????????????.
//    private fun onLoadPurchaseAuto(purchaseDataList: List<PurchaseData>) {
//        DLog.e("bjj inapp onLoadPurchaseAuto() :: purchaseDataList - $purchaseDataList")
//
//        if (purchaseDataList.isEmpty()) {
//        }
//
//        for (purchase in purchaseDataList) {
//            val result = AppSecurity.verifyPurchase(purchase.purchaseData, purchase.signature)
//
//            if (result) {
//            }
//        }
//    }
//
//    // ???????????????(inapp)??? ???????????? ?????? ?????? ?????????????????? ?????? ???????????? ?????? ?????????????????? ????????? ????????? ???????????????.
//    private fun consumeItem(purchaseData: PurchaseData) {
//        DLog.e("bjj inapp consumeItem() :: getProductId - " + purchaseData.productId + " getPurchaseId -" + purchaseData.purchaseId)
//
//        if (mPurchaseClient != null) {
//            mPurchaseClient!!.consumeAsync(IAP_API_VERSION, purchaseData, mConsumeListener)
//        }
//    }
//
//    // ???????????????(auto)??? ????????????(???????????? / ???????????? ??????)??? ???????????????.
//    private fun manageRecurringAuto(purchaseData: PurchaseData, action: String) {
//        DLog.e("bjj inapp manageRecurringAuto() :: action - " + action + " purchaseId - " + purchaseData.purchaseId)
//
//        if (mPurchaseClient != null) {
//            showProgress(this)
//            mPurchaseClient!!.manageRecurringProductAsync(IAP_API_VERSION, purchaseData, action, mManageRecurringProductListener)
//        }
//    }
//
//    private fun isValidPayload(payload: String): Boolean {
//        val sp = getPreferences(Context.MODE_PRIVATE)
//        val savedPayload = sp.getString(AppConstant.KEY_PAYLOAD, "")
//
//        DLog.e("bjj inapp isValidPayload saved payload ::$savedPayload")
//        DLog.e("bjj inapp isValidPayload server payload ::$payload")
//
//        return savedPayload == payload
//    }
//
//    private fun savePayloadString(payload: String) {
//        val spe = getPreferences(Context.MODE_PRIVATE).edit()
//        spe.putString(AppConstant.KEY_PAYLOAD, payload)
//        spe.commit()
//    }
//
//    //????????????
//    private fun requestBilling(productId: String, productType: ProductType?) {
//        loadAllProducts()
//
//        if (productId.isNullOrEmpty() && productType != null) {
//            return
//        }
//
//        buyProduct(productId, productType!!)
//    }
//
//    // ????????????
//    private fun buyProduct(productId: String, productType: ProductType) {
//        DLog.e("bjj inapp buyProduct() - productId:" + productId + " productType: " + productType.type)
//
//        if (MyApplication.instance.isGoogleMarket) { // google inapp
//            if (IapEnum.ProductType.IN_APP.type.equals(productType.type, ignoreCase = true)) { //??????
//                if (googleBp?.isPurchased(productId) == true) {
//                    // ?????????????????? ???????????? ?????? ??? ?????? ???????????? ?????? ??????. ?????? 1??? ?????? ??? ?????? ???????????? ??? ????????? ?????? ????????? ????????????.
//                    googleBp?.consumePurchase(productId)
//                }
//
//                googleBp?.purchase(this@MainActivity, productId)
//            } else if (IapEnum.ProductType.AUTO.type.equals(productType.type, ignoreCase = true)) { // ??????
//                googleBp?.subscribe(this@MainActivity, productId)
//            }
//        } else { // onestore inapp
//            if (mPurchaseClient != null) {
//                /*
//         * TODO: AppSecurity.generatePayload() ??? ????????? ???, ??? ???????????? ????????? ?????? payload??? ??????????????? ??????.
//         *
//         * ??????????????? ?????? Developer payload??? ???????????????.
//         * Developer Payload ??? ????????? ?????? ?????? ?????? ???????????? ????????? ?????? ??? ??? ?????? ??????????????????.
//         * ??? Payload ?????? ?????? ?????? ????????? ?????? ?????? ?????? ?????? ?????? ?????? ?????? ?????? ?????? ?????? ????????? ????????? ?????? ????????? ????????? ????????? ?????? ?????? ?????????.
//         * Payload ????????? ?????? Freedom ??? ?????? ?????? ??? ????????? ?????? ??? ??? ?????????, Payload ??? ?????? ??? ?????? ??????????????? ?????? ????????? ?????? ????????? ??????????????????.
//         */
//
//                val devPayload = AppSecurity.generatePayload()
//
//                // ?????? ??? dev payload??? ???????????? ????????? ?????????????????? ????????? ???????????????.
//                savePayloadString(devPayload)
//                showProgress(this)
//
//                /*
//         * PurchaseClient??? launchPurchaseFlowAsync API??? ???????????? ??????????????? ???????????????.
//         * ???????????? ??????("")?????? ????????? ?????? ?????????????????? ????????? ???????????? ??????????????? ???????????????. ????????? ????????? ?????? ?????? ???????????? ??????????????? ???????????????.
//         */if (!mPurchaseClient!!.launchPurchaseFlowAsync(IAP_API_VERSION, this, ONESTORE_PURCHASE_REQUEST_CODE, productId, "",
//                                productType.type, devPayload, "", false, mPurchaseFlowListener)) {
//                    // listener is null
//                }
//            }
//        }
//    }
//
//    private fun getItemType(itemName: String): ProductType? {
//        when (itemName) {
//            AppConstant.PRODUCT_AUTO_SUBSCRIBE_MONTH_001 -> {
//                return ProductType.AUTO
//            }
//            AppConstant.PRODUCT_INAPP_SINGLE_MONTH_002,
//            AppConstant.PRODUCT_INAPP_SINGLE_MONTH_003,
//            AppConstant.PRODUCT_INAPP_SINGLE_MONTH_004,
//            AppConstant.PRODUCT_INAPP_SINGLE_MONTH_005,
//            AppConstant.PRODUCT_INAPP_SINGLE_MONTH_006,
//            AppConstant.PRODUCT_INAPP_SINGLE_MONTH_007 -> {
//                return ProductType.IN_APP
//            }
//            else -> {
//                return null
//            }
//        }
//    }
//
//    // ?????????????????? ??????
//    private fun loadPurchases() {
//        DLog.e("bjj inapp loadPurchases()")
//
//        loadPurchase(IapEnum.ProductType.IN_APP)
//        loadPurchase(IapEnum.ProductType.AUTO)
//    }
//
//    private fun loadPurchase(productType: ProductType) {
//        DLog.e("bjj inapp loadPurchase() :: productType - " + productType.type)
//
//        if (mPurchaseClient != null) {
//            showProgress(this)
//            mPurchaseClient!!.queryPurchasesAsync(IAP_API_VERSION, productType.type, mQueryPurchaseListener)
//        }
//    }
//
//    /*
//     * ?????? ??? ????????? ????????????????????? ???????????????.
//     * ?????????????????? ????????????????????? ????????? ??????ID??? ????????? ?????????????????????.
//     */
//    private fun loadAllProducts() {
//        val inapp = ArrayList<String>()
//        inapp.add(AppConstant.PRODUCT_INAPP_SINGLE_MONTH_002)
//        inapp.add(AppConstant.PRODUCT_INAPP_SINGLE_MONTH_003)
//        inapp.add(AppConstant.PRODUCT_INAPP_SINGLE_MONTH_004)
//        inapp.add(AppConstant.PRODUCT_INAPP_SINGLE_MONTH_006)
//        inapp.add(AppConstant.PRODUCT_INAPP_SINGLE_MONTH_007)
//        loadProducts(ProductType.IN_APP, inapp)
//
//        val auto = ArrayList<String>()
//        auto.add(AppConstant.PRODUCT_AUTO_SUBSCRIBE_MONTH_001)
//        loadProducts(ProductType.AUTO, auto)
//    }
//
//    // ?????????????????? ????????? ??????????????? ???????????????. ?????????????????? ???????????? ????????? ????????? ?????? ??????ID??? String ArrayList??? ???????????????.
//    private fun loadProducts(productType: ProductType, products: ArrayList<String>) {
//        DLog.e("bjj inapp loadProducts")
//
//        if (mPurchaseClient != null) {
//            showProgress(this)
//            mPurchaseClient!!.queryProductsAsync(IAP_API_VERSION, products, productType.type, mQueryProductsListener)
//        }
//    }

    private fun doTakeAlbumAction(type: String, param: String) {
        mCameraReturnParam = param
        mCameraReturnType = type

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        startActivityForResult(intent, PICK_FROM_ALBUM)
    }

    private fun doTakePhotoAction(type: String, param: String) {
        mCameraReturnParam = param
        mCameraReturnType = type

        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE

        startActivityForResult(intent, PICK_FROM_CAMERA)
    }

    private fun sendGalleryImage(uri: Uri) {
        DLog.e("bjj camera sendGalleryImage :: path " + getImageFile(uri)?.absolutePath)

        uploadFile(getImageFile(uri)?.absolutePath)
    }

    private fun sendCameraImage(bitmap: Bitmap) {
        DLog.e("bjj camera sendCameraImage :: bitmap " + bitmap + " ^ " + getImageFile(getCaptureImageUri(bitmap)!!)?.absolutePath)

        uploadFile(getImageFile(getCaptureImageUri(bitmap)!!)?.absolutePath)
    }

    private fun resizeBitmapImageFn(bmpSource: Bitmap, maxResolution: Int): Bitmap? {
        val iWidth = bmpSource.width //????????????????????? ??????
        val iHeight = bmpSource.height //????????????????????? ??????
        var newWidth = iWidth
        var newHeight = iHeight
        var rate = 0.0f

        DLog.e("bjj resizeBitmapImageFnaa $iWidth ^ $iHeight")

        //???????????? ?????? ?????? ????????? ?????? ??????
        if (iWidth > iHeight) {
            if (maxResolution < iWidth) {
                rate = maxResolution / iWidth.toFloat()
                newHeight = (iHeight * rate).toInt()
                newWidth = maxResolution
            }
        } else {
            if (maxResolution < iHeight) {
                rate = maxResolution / iHeight.toFloat()
                newWidth = (iWidth * rate).toInt()
                newHeight = maxResolution
            }
        }

        DLog.e("bjj resizeBitmapImageFnbb $newWidth ^ $newHeight")

        return Bitmap.createScaledBitmap(bmpSource, newWidth, newHeight, true)
    }

    private fun getImageFile(uri: Uri): File? {
        var uri: Uri? = uri
        val projection = arrayOf<String>(MediaStore.Images.Media.DATA)

        if (uri == null) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var mCursor: Cursor? = contentResolver.query(uri!!, projection, null, null, MediaStore.Images.Media.DATE_MODIFIED + " desc")

        if (mCursor == null || mCursor.getCount() < 1) {
            return null // no cursor or no record
        }

        val column_index: Int = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        mCursor.moveToFirst()

        val path: String = mCursor.getString(column_index)

        if (mCursor != null) {
            mCursor.close()
            mCursor = null
        }

        return File(path)
    }

    private fun getCaptureImageUri(inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)

        val path: String = MediaStore.Images.Media.insertImage(contentResolver, inImage, packageName + "_capture_" + System.currentTimeMillis(), null)

        return Uri.parse(path)
    }

    private fun uploadFile(filePath: String?) {
        val url = URL(getSettingUrl(this@MainActivity))

        mApiManager?.uploadFile(url.toString(), filePath ?: "", filePath ?: "", mCameraReturnType
                ?: "", mCameraReturnParam ?: "", object : OnResultListener<Any> {
            override fun onResult(result: Any, flag: Int) {
                if (result == null) {
                    return
                }

                val uploadInfoData = result as UploadInfoData

                var sucessStr: String
                var keyStr: String

                sucessStr = if (uploadInfoData?.isUploadSucess.isNullOrEmpty()) {
                    "FAIL"
                } else {
                    uploadInfoData?.isUploadSucess!!
                }

                keyStr = if (uploadInfoData?.fileKey.isNullOrEmpty()) {
                    ""
                } else {
                    uploadInfoData?.fileKey!!
                }

                mWebview?.sendEvent(IdevelServerScript.GET_REQUEST_FILE_UPLOAD_INFO, ReturnRequestFileUploadInfo(sucessStr, mCameraReturnType
                        ?: "", mCameraReturnParam ?: "", keyStr).toJsonString())
            }

            override fun onFail(error: Any, flag: Int) {
                mWebview?.sendEvent(IdevelServerScript.GET_REQUEST_FILE_UPLOAD_INFO, ReturnRequestFileUploadInfo("FAIL", mCameraReturnType
                        ?: "", mCameraReturnParam ?: "", "").toJsonString())
            }
        })
    }
}