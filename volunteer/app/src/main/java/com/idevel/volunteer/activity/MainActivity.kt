package com.idevel.volunteer.activity


import ApiManager
import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.KeyguardManager
import android.content.ClipData
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
import android.os.*
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
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import api.OnResultListener
import com.idevel.volunteer.BuildConfig
import com.idevel.volunteer.MyApplication
import com.idevel.volunteer.R
import com.idevel.volunteer.broadcast.DataSaverChangeReceiver
import com.idevel.volunteer.broadcast.NetworkChangeReceiver
import com.idevel.volunteer.dialog.AgentPopupDialog
import com.idevel.volunteer.dialog.CustomAlertDialog
import com.idevel.volunteer.fcm.PushPreferences.IS_NOTI
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_LINK_TYPE
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_LINK_URL
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_SHOWTIME
import com.idevel.volunteer.interfaces.IDataSaverListener
import com.idevel.volunteer.interfaces.NetworkChangeListener
import com.idevel.volunteer.utils.*
import com.idevel.volunteer.utils.wrapper.LocaleWrapper
import com.idevel.volunteer.web.BaseWebView
import com.idevel.volunteer.web.MyWebChromeClient
import com.idevel.volunteer.web.MyWebViewClient
import com.idevel.volunteer.web.UrlData.*
import com.idevel.volunteer.web.constdata.*
import com.idevel.volunteer.web.interfaces.IWebBridgeApi
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.onestore.iap.api.*
import com.onestore.iap.api.PurchaseClient.*
import kr.co.medialog.SettingInfoData
import kr.co.medialog.UploadInfoData
import okhttp3.ResponseBody
import java.io.*
import java.lang.StringBuilder
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
    private var mSplashView: View? = null //초기 로딩시 보여주기 위한 view.
    private var mErrorView: View? = null //The network error view.
    private var mWebview: BaseWebView? = null //mobile view page 연결을 위한 webview.
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
    private val mNetworkChangeReceiver = NetworkChangeReceiver() //네트워크 check
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
//                .setObject("Main Page", "android-app://com.idevel.volunteer/http/host/path", "http://host/path")
//                .build()


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //캡쳐 방지
//        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        mApiManager = ApiManager.getInstance(this@MainActivity)
        CookieSyncManager.createInstance(this@MainActivity)

        //구글 인앱 결재
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

        //네트워크 연결 여부
        if (!isNetworkConnected(this)) {
            showErrorDlg(NETWORK_CONNECTION_ERROR)
            return
        }

        //data saver 설정 여부
//        if (checkDataSave(this)) {
//            showDataSaveDlg(R.string.popup_title_data_save, R.string.popup_msg_data_save)
//            return
//        }

        // 앱 사용 중 data saver 사용 Listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mDataSaverChangeReceiver.setListener(dataSaverListener)
        }

        // 앱 사용 중 data network 변경 Listener
        mNetworkChangeReceiver.setListener(networkListener)

        //TODO 추후에 해당 주석 풀어야함 (결제 테스트 용)
        //debug build인 경우 히든 메뉴 이동
//        if (BuildConfig.DEBUG) {
//            goToDevActivity()
//        } else {
//            // 앱 설정
        checkSettingInfo()
//        }
    }

    private fun goToDevActivity() {
        val i = Intent(this, DevActivity::class.java)
        startActivityForResult(i, DEV_REQUEST_CODE)
    }

    // push 클릭
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
                        mWebview!!.loadUrl(link!!)
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
        //임시
        mHandler.sendEmptyMessageDelayed(HANDLER_SPLASH, 2000L)


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
     * 스플래시 애니메이션 설정
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

        //다른앱 위에 화면 그리기 권한을 받는다
        if (!isCanDrawOverlays(this@MainActivity)) {
            showPopupPermissionPopup()
        }

        //모든 파일 접근 가능 권한
        if (!isCanAllFileAcess()) {
            gotoAllFileAcessPermission()
        }

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

                if (url == "https://www.1393kfsp.or.kr/login.php") {
                    removeSplash()
                }

                DLog.e("bjj mWebViewClient onPageFinished : $url, ${mSettingdata?.main_url}")
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
        }

        mWebview?.webViewClient = mWebViewClient!!
        mWebview?.webChromeClient = mWebChromeClient!!
    }

    private fun checkPushData(intent: Intent) {
        // 푸시를 통해 진입한 경우가 아님

        DLog.e("bjj checkPushData :: " + MyApplication.instance.isChating + " ^ " + intent.extras)

        if (null == intent.extras) {
            return
        }

        if (mAgentPopupDialog != null) {
            mAgentPopupDialog!!.dismiss()
            mAgentPopupDialog = null
        }

        // 8.0미만 버전에서 뱃지 카운트 0 으로 설정
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
                        mWebview!!.loadUrl(link!!)
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
//                    // 구매하였으면 소비하여 없앤 후 다시 구매하게 하는 로직. 만약 1번 구매 후 계속 이어지게 할 것이면 아래 함수는 주석처리.
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

    private fun showPopupPermissionPopup() {
        val alertDialog = CustomAlertDialog(this)

        alertDialog.setDataSaveLayout(R.string.common_popup_btn_setting, R.string.common_popup_msg_popup_player)
        alertDialog.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog.setOkClickListener(OnClickListener { v ->
            alertDialog.dismiss()

            when (v.id) {
                R.id.btn_ok -> {
                    gotoOverlayPermission()
                }
                R.id.btn_cancel -> {
                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog.show()
        }
    }

    /**
     * Show PermissionDenyDialog dlg.
     */
    private fun showPermissionDenyDialog(permissionStr: String) {
        val alertDialog = CustomAlertDialog(this)
        alertDialog.setCancelable(false)

//        if (permissionStr.contains("READ_PHONE_NUMBERS")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg1)
//        } else if (permissionStr.contains("READ_PHONE_STATE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg1)
//        } else if (permissionStr.contains("MANAGE_EXTERNAL_STORAGE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg5)
//        } else if (permissionStr.contains("READ_EXTERNAL_STORAGE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg2)
//        } else if (permissionStr.contains("WRITE_EXTERNAL_STORAGE")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg2)
//        } else if (permissionStr.contains("READ_CALL_LOG")) {
//            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg3)
//        } else {
            alertDialog.setDataSaveLayout(R.string.permissionDeny_title, R.string.permissionDeny_msg4)
//        }

        alertDialog.setButtonString(R.string.popup_btn_ok_dta_save, R.string.popup_btn_cancel_dta_save)

        alertDialog.setOkClickListener(OnClickListener { v ->
            alertDialog.dismiss()
            when (v.id) {
                R.id.btn_cancel -> finish()
                R.id.btn_ok -> {
                    if (permissionStr.contains("MANAGE_EXTERNAL_STORAGE")) {
                        gotoAllFileAcessPermission()
                    } else {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }

                    finish()
                }
            }
        })

        if (!isFinishing) {
            alertDialog.show()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun gotoOverlayPermission() {
        val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${this@MainActivity.packageName}"))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ContextCompat.startActivity(this@MainActivity, i, ActivityOptionsCompat.makeBasic().toBundle())
    }

    private fun gotoAllFileAcessPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri

        ContextCompat.startActivity(this@MainActivity, intent, ActivityOptionsCompat.makeBasic().toBundle())
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

        // 앱 종료시 PurchaseClient를 이용하여 서비스를 terminate 시킵니다.
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
//            //TODO 이걸로 보낼지
////                Bundle[{INAPP_PURCHASE_DATA={"orderId":"GPA.3353-4786-9413-58565","packageName":"com.idevel.volunteer","productId":"single_month","purchaseTime":1597240260045,"purchaseState":0,"developerPayload":"inapp:single_month:0075b5a1-6a7a-417c-ac00-6c993441b85c","purchaseToken":"dgkbeeoiclkjpdoafoogehim.AO-J1OzUdS_d7A_lyCiKHUC825T4DDQu86PdwB0R0tRgrrv28mBL4YRbY1v_hoXsWW8gNt-34QRjgIPLyBvXeEoPf4FbDwytbUTjd0hSzOMw9pEIhU3NO6XQhC3s8F_L81UwzFOOnM4x"}, INAPP_DATA_SIGNATURE=CITI8oTFUphcpTels23QpNxcWIZzdBULJK5VZwtIta9dL46mOlTTfMQeYM13Wk5NUdhbxQ3D1vPsYxEed+nEb30jkDqQyX7UMnFhIiTp6T8VI1PIsuQzhoIyWoCfh0Q3eP7W+yYjw8VluD2ebTo69T6x4LqO6y3lcQxlkMrKL9pRbG6m7NgqoBRrREBm8H9fPexplvmc//AEmQUmGIpoGoE8MRMoxEPkC0+3l+HhFk3qktZX1pzb4vufUtHjpCevygI2qQfil0Nt1gtccE3/6p8bUu5MDf1vNct+qvsQ5X1QkF02a9d+xuDmnIxXg+CFqu5GIBA3p/iI2Np2s1knrw==, RESPONSE_CODE=0}]
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

                    if (intent?.data != null) { //1개 이미지
                        DLog.e("bjj PICK_FROM_ALBUM AA " + intent.data)

                        sendGalleryImage(intent.data!!)
                    } else {
                        val clipData: ClipData = intent?.clipData!!
                        if (clipData.itemCount > 3) {
                            Toast.makeText(this, "3장이상 업로드 할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            if (clipData.itemCount > 0) {
                                for (index in 0 until clipData.itemCount) {
                                    DLog.e("bjj PICK_FROM_ALBUM BB " + index + " ^ " + clipData.getItemAt(index).uri)

                                    sendGalleryImage(clipData.getItemAt(index).uri, index, clipData.itemCount)
                                }
                            }
                        }
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
                //TODO 카카오 로그인 이후
            }

            X_PAY_REQUEST_CODE -> {
            }

//            ONESTORE_PURCHASE_REQUEST_CODE -> {
//                /*
//                 * launchPurchaseFlowAsync API 호출 시 전달받은 intent 데이터를 handlePurchaseData를 통하여 응답값을 파싱합니다.
//                 * 파싱 이후 응답 결과를 launchPurchaseFlowAsync 호출 시 넘겨준 PurchaseFlowListener 를 통하여 전달합니다.
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
//                // TODO onestore 로그인 이후
//            }
        }
    }

    /**
     * 권한 체크
     */
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dexter.withContext(this).withPermissions(MyApplication.PERMISSIONS).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    report.let {
                        DLog.e("bjj PERMISSION checkPermission onPermissionsChecked aa ==>> "
                                + it.areAllPermissionsGranted() + " ^ "
                                + it.isAnyPermissionPermanentlyDenied
                        )

                        if (it.areAllPermissionsGranted()) { // 모든 권한 허용
                            setMainView()
                        } else if (it.isAnyPermissionPermanentlyDenied) {
                            DLog.e("bjj PERMISSION checkPermission onPermissionsChecked bb ==>> "
                                    + Build.VERSION.SDK_INT)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                DLog.e("bjj PERMISSION checkPermission onPermissionsChecked bb ==>> "
                                        + Build.VERSION.SDK_INT + " ^ "
                                        + Environment.isExternalStorageManager())
                                if (Environment.isExternalStorageManager()) {
                                    setMainView()
                                } else {
                                    showPermissionDenyDialog("MANAGE_EXTERNAL_STORAGE")
                                }

                                return
                            }

                            showPermissionDenyDialog("")
                        } else {
                            finish()
                        }

                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    DLog.e("bjj PERMISSION checkPermission onPermissionRationaleShouldBeShown ==>> " + permissions)

                    token?.continuePermissionRequest()
                }
            }).onSameThread().check()

            return false
        }

        return true
    }



    private fun removeSplash() {
        if (isFinishing) {
            return
        }

        (this@MainActivity as Activity).runOnUiThread {
            setSplash()
        }
    }

    //공유 팝업노출
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
//        // PurchaseClient 초기화 - context 와 Signature 체크를 위한 public key 를 파라미터로 넘겨줍니다.
//        mPurchaseClient = PurchaseClient(this, AppSecurity.getPublicKey())
//        // 원스토어 서비스로 인앱결제를 위한 서비스 바인딩을 요청합니다.
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
                val isAuto = SharedPreferencesUtil.getBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.AUTO_LOGIN)

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

        override fun downloadFile(fileURL: String, fileName: String) {
            (this@MainActivity as Activity).runOnUiThread {
                testRecordDownload(fileURL, fileName)
            }
        }
    }

    companion object {
        private val HANDLER_NETWORK_TIMER = 1 // The network timer handler.
        private val HANDLER_SPLASH = 2 // 스플래시 종료 핸들러.
        private val HANDLER_SPLASH_DELAY = 3 // 스플래시 delay.

        private val DEV_REQUEST_CODE = 1000 // 히든 메뉴에서 돌아왔을 때 flag값.
        private val X_PAY_REQUEST_CODE = 999
        private val KAKAO_LOGIN_REQUEST_CODE = 998
        private val TEL_REQUEST_CODE = 997
        private val PICK_FROM_ALBUM = 996
        private val PICK_FROM_CAMERA = 995


        private val PING_TIME = 100000 //The ping time.
        private val NETWORK_CONNECTION_ERROR = 1 //The network connection error.
        private val TIMEOUT_ERROR = 2 //The timeout error.
        private val APP_VERSION_CHECK = 4 //앱 버전 check.

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

            // 네트워크 전환 시 onNetworkDisconnected 들어왔을 경우 1초 딜레이 후 네트워크 상태 체크하여 네트워크 차단팝업 발생하도록 함
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

    // 현재 kakao만 적용됨
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
                    // Intent 생성
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

                    // 실행 가능한 앱이 있으면 앱 실행
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, KAKAO_LOGIN_REQUEST_CODE)

                        DLog.e("bjj urlLoading intent ACTIVITY: ${intent.getPackage()}")

                        return true
                    }

                    // Fallback URL이 있으면 현재 웹뷰에 로딩
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
//        // * 구매 완료시 호출
//        // productId: 구매한 sku (ex) no_ads)
//        // details: 결제 관련 정보
////        val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(true).toJsonString())
//
//        DLog.e("bjj BILLING onBillingInitialized")
//    }
//
//    override fun onPurchaseHistoryRestored() {
//        // * 구매 정보가 복원되었을때 호출
//        // bp.loadOwnedPurchasesFromGoogle() 하면 호출 가능
////        val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////        SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        DLog.e("bjj BILLING onPurchaseHistoryRestored")
//    }
//
//    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
//        // * 구매 오류시 호출
//        // errorCode == Constants.BILLING_RESPONSE_RESULT_USER_CANCELED 일때는
//        // 사용자가 단순히 구매 창을 닫은것임으로 이것 제외하고 핸들링하기.
//
////         val skuDetails: SkuDetails = googleBp?.getPurchaseListingDetails(productId) as SkuDetails
//
//
//        DLog.e("bjj BILLING onProductPurchased " + productId + " ^ " + details)
//
//        //TODO 아랫걸로 보낼지 activityresult로 보낼지 판단
//        //single_month purchased at Wed Aug 12 22:51:00 GMT+09:00 2020(GPA.3353-4786-9413-58565). Token: dgkbeeoiclkjpdoafoogehim.AO-J1OzUdS_d7A_lyCiKHUC825T4DDQu86PdwB0R0tRgrrv28mBL4YRbY1v_hoXsWW8gNt-34QRjgIPLyBvXeEoPf4FbDwytbUTjd0hSzOMw9pEIhU3NO6XQhC3s8F_L81UwzFOOnM4x, Signature: CITI8oTFUphcpTels23QpNxcWIZzdBULJK5VZwtIta9dL46mOlTTfMQeYM13Wk5NUdhbxQ3D1vPsYxEed+nEb30jkDqQyX7UMnFhIiTp6T8VI1PIsuQzhoIyWoCfh0Q3eP7W+yYjw8VluD2ebTo69T6x4LqO6y3lcQxlkMrKL9pRbG6m7NgqoBRrREBm8H9fPexplvmc//AEmQUmGIpoGoE8MRMoxEPkC0+3l+HhFk3qktZX1pzb4vufUtHjpCevygI2qQfil0Nt1gtccE3/6p8bUu5MDf1vNct+qvsQ5X1QkF02a9d+xuDmnIxXg+CFqu5GIBA3p/iI2Np2s1knrw==
//        mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(true, details.toString()).toJsonString())
//
//
////        if (productId.equals("single_month")) {
//        // TODO: 구매 해 주셔서 감사합니다! 메세지 보내기
////            val isBool = googleBp?.isPurchased(BILLING_SUBSCRIBE_MONTH_PRODUCT_ID) ?: false
////            SharedPreferencesUtil.setBoolean(this@MainActivity, SharedPreferencesUtil.Cmd.PURCHASED_SINGLE_MONTH, isBool)
//
//        // * 광고 제거는 1번 구매하면 영구적으로 사용하는 것이므로 consume하지 않지만,
//        // 만약 게임 아이템 100개를 주는 것이라면 아래 메소드를 실행시켜 다음번에도 구매할 수 있도록 소비처리를 해줘야한다.
//        // bp.consumePurchase(Config.Sku);
////        }
//    }
//
//    override fun onBillingError(errorCode: Int, error: Throwable?) {
//        // * 처음에 초기화됬을때.
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

            Toast.makeText(this, "사용자의 위치 정보 권한을 허용하지 않았습니다", Toast.LENGTH_SHORT).show()
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
//     * 원스토어 INAPP
//     */
//    private var mPurchaseClient: PurchaseClient? = null
//    private var progressDialog: ProgressDialog? = null
//
//    /*
//     * PurchaseClient의 connect API 콜백 리스너
//     * 바인딩 성공/실패 및 원스토어 서비스 업데이트가 필요한지에 대한 응답을 넘겨줍니다.
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
//            DLog.e("bjj inapp connect onError, 원스토어 서비스앱의 업데이트가 필요합니다")
//            updateOrInstallOneStoreService()
//        }
//    }
//
//    /*
//     * 앱구동시 isBillingSupportedAsync API를 이용하여 인앱결제를 진행할 수 있는 상태인지 확인합니다.
//     * 이후 구매내역조회 API를 이용하여 소비되지 않는 소모성상품(inapp)과 자동결제중인 월정액상품(auto) 목록을 받아옵니다.
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
//     * PurchaseClient의 isBillingSupportedAsync (지원여부조회) API 콜백 리스너
//     */
//    private var mBillingSupportedListener: BillingSupportedListener = object : BillingSupportedListener {
//        override fun onSuccess() {
//            DLog.e("bjj inapp isBillingSupportedAsync onSuccess")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(true).toJsonString())
//
//            hideProgress()
//
//            // isBillingSupportedAsync 호출 성공시에,
//            // 구매내역정보를 호출을 진행하여 관리형상품 및 월정액상품 구매내역에 대해서 받아옵니다.
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
//            // RESULT_NEED_LOGIN 에러시에 개발사의 애플리키에션 life cycle에 맞춰 명시적으로 원스토어 로그인을 호출합니다.
//            if (IapResult.RESULT_NEED_LOGIN == result) {
//                loadLoginFlow()
//            }
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp isBillingSupportedAsync onError, onErrorSecurityException 원스토어 서비스와 연결을 할 수 없습니다")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//
//            hideProgress()
//            oneStoreAlert("원스토어 서비스와 연결을 할 수 없습니다")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp isBillingSupportedAsync onError, onErrorSecurityException 비정상 앱에서 결제가 요청되었습니다")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//
//            hideProgress()
//            oneStoreAlert("비정상 앱에서 결제가 요청되었습니다")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp isBillingSupportedAsync onError, onErrorSecurityException 원스토어 서비스앱의 업데이트가 필요합니다")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//    }
//
//    /*
//     * PurchaseClient의 queryProductsAsync API (상품정보조회) 콜백 리스너
//     */
//    private var mQueryProductsListener: QueryProductsListener = object : QueryProductsListener {
//        override fun onSuccess(productDetails: List<ProductDetail>) {
//            DLog.e("bjj inapp queryProductsAsync onSuccess, $productDetails")
//
//            hideProgress()
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp queryProductsAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
//
//            hideProgress()
//            oneStoreAlert("원스토어 서비스와 연결을 할 수 없습니다")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp queryProductsAsync onError, 비정상 앱에서 결제가 요청되었습니다")
//
//            hideProgress()
//            oneStoreAlert("비정상 앱에서 결제가 요청되었습니다")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp queryProductsAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
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
//     * PurchaseClient의 queryPurchasesAsync API (구매내역조회) 콜백 리스너
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
//            DLog.e("bjj inapp queryPurchasesAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
//
//            hideProgress()
//            oneStoreAlert("원스토어 서비스와 연결을 할 수 없습니다")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp queryPurchasesAsync onError, 비정상 앱에서 결제가 요청되었습니다")
//
//            hideProgress()
//            oneStoreAlert("비정상 앱에서 결제가 요청되었습니다")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp queryPurchasesAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
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
//     * PurchaseClient의 consumeAsync API (상품소비) 콜백 리스너
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
////            alert("결재 성공 " + purchaseData.productId, true)
//        }
//
//        override fun onErrorRemoteException() {
//            DLog.e("bjj inapp consumeAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
//            hideProgress()
//            oneStoreAlert("원스토어 서비스와 연결을 할 수 없습니다")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp consumeAsync onError, 비정상 앱에서 결제가 요청되었습니다")
//            hideProgress()
//            oneStoreAlert("비정상 앱에서 결제가 요청되었습니다")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp consumeAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
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
//     * PurchaseClient의 manageRecurringProductAsync API (월정액상품 상태변경) 콜백 리스너
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
//            DLog.e("bjj inapp manageRecurringProductAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
//
//            hideProgress()
//            oneStoreAlert("원스토어 서비스와 연결을 할 수 없습니다")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp manageRecurringProductAsync onError, 비정상 앱에서 결제가 요청되었습니다")
//
//            hideProgress()
//            oneStoreAlert("비정상 앱에서 결제가 요청되었습니다")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp manageRecurringProductAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
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
//     * PurchaseClient의 launchPurchaseFlowAsync API (구매) 콜백 리스너
//     */
//    private var mPurchaseFlowListener: PurchaseFlowListener = object : PurchaseFlowListener {
//        override fun onSuccess(purchaseData: PurchaseData) {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onSuccess, $purchaseData")
//            hideProgress()
//
//            // 구매완료 후 developer payload 검증을 수해한다.
//            if (!isValidPayload(purchaseData.developerPayload)) {
//                DLog.e("bjj inapp onSuccess() :: payload is not valid.")
////                alert(getString(R.string.msg_alert_dev_payload_invalid))
//
//                mWebview?.sendEvent(tatalkServerScript.GET_REQUEST_BUY_PRODUCT_INFO, ReturnRequestBuyProductInfo(false, "").toJsonString())
//                return
//            }
//
//            // 구매완료 후 signature 검증을 수행한다.
//            val validPurchase = AppSecurity.verifyPurchase(purchaseData.purchaseData, purchaseData.signature)
//            DLog.e("bjj inapp onSuccess() :: verifyPurchase $validPurchase")
//
//            if (validPurchase) {
//                if (AppConstant.PRODUCT_AUTO_SUBSCRIBE_MONTH_001.equals(purchaseData.productId)) {
//                    // 월정액상품이면 소비를 수행하지 않는다.
////                    alert(getString(R.string.msg_purchase_success_auto), true)
////                    saveMonthlyItem(purchaseData)
//                } else {
//                    // 관리형상품(inapp)은 구매 완료 후 소비를 수행한다.
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
//            DLog.e("bjj inapp launchPurchaseFlowAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
//            hideProgress()
//            oneStoreAlert("원스토어 서비스와 연결을 할 수 없습니다")
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onError, 비정상 앱에서 결제가 요청되었습니다")
//            hideProgress()
//            oneStoreAlert("비정상 앱에서 결제가 요청되었습니다")
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp launchPurchaseFlowAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
//            hideProgress()
//            updateOrInstallOneStoreService()
//        }
//    }
//
//    /*
//     * PurchaseClient의 launchLoginFlowAsync API (로그인) 콜백 리스너
//     */
//    private var mLoginFlowListener: LoginFlowListener = object : LoginFlowListener {
//        override fun onSuccess() {
//            DLog.e("bjj inapp launchLoginFlowAsync onSuccess")
//            hideProgress()
//            // 개발사에서는 로그인 성공시에 대한 이후 시나리오를 지정하여야 합니다.
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
//            DLog.e("bjj inapp launchLoginFlowAsync onError, 원스토어 서비스와 연결을 할 수 없습니다")
//            hideProgress()
//            oneStoreAlert("원스토어 서비스와 연결을 할 수 없습니다")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//        }
//
//        override fun onErrorSecurityException() {
//            DLog.e("bjj inapp launchLoginFlowAsync onError, 비정상 앱에서 결제가 요청되었습니다")
//            hideProgress()
//            oneStoreAlert("비정상 앱에서 결제가 요청되었습니다")
//
//            mWebview?.sendEvent(tatalkServerScript.GET_READT_ONESTORE_BILLING_INFO, ReturnReadyOneStoreBilling(false).toJsonString())
//        }
//
//        override fun onErrorNeedUpdateException() {
//            DLog.e("bjj inapp launchLoginFlowAsync onError, 원스토어 서비스앱의 업데이트가 필요합니다")
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
//                    .setPositiveButton("확인", null)
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
//            oneStoreAlertDecision("원스토어 계정 정보를 확인 할 수 없습니다. 로그인 하시겠습니까?", DialogInterface.OnClickListener { dialog, which ->
//                if (!mPurchaseClient!!.launchLoginFlowAsync(IAP_API_VERSION, this@MainActivity, ONESTORE_LOGIN_REQUEST_CODE, mLoginFlowListener)) {
//                    // listener is null
//                }
//            })
//        }
//    }
//
//    // 구매내역조회에서 받아온 관리형상품(inapp)의 경우 Signature 검증을 진행하고, 성공할 경우 상품소비를 진행합니다.
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
//    // 구매내역조회에서 받아온 월정액상품(auto)의 경우 Signature 검증을 진행하고, 성공할 경우 게임 UI 시나리오를 위해 상품정보를 저장해놓습니다.
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
//    // 관리형상품(inapp)의 구매완료 이후 또는 구매내역조회 이후 소비되지 않는 관리형상품에 대해서 소비를 진행합니다.
//    private fun consumeItem(purchaseData: PurchaseData) {
//        DLog.e("bjj inapp consumeItem() :: getProductId - " + purchaseData.productId + " getPurchaseId -" + purchaseData.purchaseId)
//
//        if (mPurchaseClient != null) {
//            mPurchaseClient!!.consumeAsync(IAP_API_VERSION, purchaseData, mConsumeListener)
//        }
//    }
//
//    // 월정액상품(auto)의 상태변경(해지예약 / 해지예약 취소)를 진행합니다.
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
//    //구메요청
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
//    // 구매요청
//    private fun buyProduct(productId: String, productType: ProductType) {
//        DLog.e("bjj inapp buyProduct() - productId:" + productId + " productType: " + productType.type)
//
//        if (MyApplication.instance.isGoogleMarket) { // google inapp
//            if (IapEnum.ProductType.IN_APP.type.equals(productType.type, ignoreCase = true)) { //단건
//                if (googleBp?.isPurchased(productId) == true) {
//                    // 구매하였으면 소비하여 없앤 후 다시 구매하게 하는 로직. 만약 1번 구매 후 계속 이어지게 할 것이면 아래 함수는 주석처리.
//                    googleBp?.consumePurchase(productId)
//                }
//
//                googleBp?.purchase(this@MainActivity, productId)
//            } else if (IapEnum.ProductType.AUTO.type.equals(productType.type, ignoreCase = true)) { // 구독
//                googleBp?.subscribe(this@MainActivity, productId)
//            }
//        } else { // onestore inapp
//            if (mPurchaseClient != null) {
//                /*
//         * TODO: AppSecurity.generatePayload() 는 예제일 뿐, 각 개발사의 규칙에 맞는 payload를 생성하여야 한다.
//         *
//         * 구매요청을 위한 Developer payload를 생성합니다.
//         * Developer Payload 는 상품의 구매 요청 시에 개발자가 임의로 지정 할 수 있는 문자열입니다.
//         * 이 Payload 값은 결제 완료 이후에 응답 값에 다시 전달 받게 되며 결제 요청 시의 값과 차이가 있다면 구매 요청에 변조가 있다고 판단 하면 됩니다.
//         * Payload 검증을 통해 Freedom 과 같은 변조 된 요청을 차단 할 수 있으며, Payload 의 발급 및 검증 프로세스를 자체 서버를 통해 이루어 지도록합니다.
//         */
//
//                val devPayload = AppSecurity.generatePayload()
//
//                // 구매 후 dev payload를 검증하기 위하여 프리퍼런스에 임시로 저장합니다.
//                savePayloadString(devPayload)
//                showProgress(this)
//
//                /*
//         * PurchaseClient의 launchPurchaseFlowAsync API를 이용하여 구매요청을 진행합니다.
//         * 상품명을 공백("")으로 요청할 경우 개발자센터에 등록된 상품명을 결제화면에 노출됩니다. 구매시 지정할 경우 해당 문자열이 결제화면에 노출됩니다.
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
//    // 구매내역조회 요청
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
//     * 게임 내 상품의 상품정보조회를 진행합니다.
//     * 개발사에서는 상품정보조회를 진행할 상품ID를 인자로 넘겨줘야합니다.
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
//    // 개발자센터에 등록된 상품정보를 조회합니다. 개발사에서는 상품정보 조회를 하고자 하는 상품ID를 String ArrayList로 전달합니다.
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

//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = MediaStore.Images.Media.CONTENT_TYPE
//        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        DLog.e("bjj doTakeAlbumAction :: " + Build.MANUFACTURER)

        if (Build.MANUFACTURER == "samsung") {
            val intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            intent.putExtra("CONTENT_TYPE", "*/*")
            intent.addCategory(Intent.CATEGORY_DEFAULT)

            startActivityForResult(intent, PICK_FROM_ALBUM)
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"

            startActivityForResult(intent, PICK_FROM_ALBUM)
        }
    }

    private fun doTakePhotoAction(type: String, param: String) {
        mCameraReturnParam = param
        mCameraReturnType = type

        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE

        startActivityForResult(intent, PICK_FROM_CAMERA)
    }

    private fun sendGalleryImage(uri: Uri, index: Int = 0, totalCount: Int = 0) {
        DLog.e("bjj camera sendGalleryImage :: path " + getImageFile(uri)?.absolutePath)

        if (totalCount > 0) {
            uploadFile(getImageFile(uri)?.absolutePath, index, totalCount)
        } else {
            uploadFile(getImageFile(uri)?.absolutePath)
        }
    }

    private fun sendCameraImage(bitmap: Bitmap) {
        DLog.e("bjj camera sendCameraImage :: bitmap " + bitmap + " ^ " + getImageFile(getCaptureImageUri(bitmap)!!)?.absolutePath)

        uploadFile(getImageFile(getCaptureImageUri(bitmap)!!)?.absolutePath)
    }

    private fun resizeBitmapImageFn(bmpSource: Bitmap, maxResolution: Int): Bitmap? {
        val iWidth = bmpSource.width //비트맵이미지의 넓이
        val iHeight = bmpSource.height //비트맵이미지의 높이
        var newWidth = iWidth
        var newHeight = iHeight
        var rate = 0.0f

        DLog.e("bjj resizeBitmapImageFnaa $iWidth ^ $iHeight")

        //이미지의 가로 세로 비율에 맞게 조절
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

    private fun uploadFile(filePath: String?, index: Int = 0, totalCount: Int = 0) {
        val url = URL(getUploadUrl(this@MainActivity))

        var returnParamStr = mCameraReturnParam

        if (totalCount > 0) {
            val sb = StringBuilder()
            sb.append(mCameraReturnParam).append(index).append("/").append(totalCount)

            returnParamStr = sb.toString()
        }

        DLog.e("bjj uploadFile :: " + filePath + " ^ " + returnParamStr)

        mApiManager?.uploadFile(url.toString(), filePath ?: "", filePath ?: "", mCameraReturnType
                ?: "", returnParamStr ?: "", object : OnResultListener<Any> {
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


    private var recordDownloadTask: AsyncTask<Void, Void, Boolean>? = null

    @SuppressLint("StaticFieldLeak")
    private fun testRecordDownload(fileURL: String, fileName: String) {
        val baseUrl = "https://1393kfsp.or.kr"

        mApiManager?.startRecordDownload(baseUrl, fileURL, object : OnResultListener<Any> {
            override fun onResult(result: Any, flag: Int) {
                recordDownloadTask?.cancel(true)
                recordDownloadTask = object : AsyncTask<Void, Void, Boolean>() {
                    override fun doInBackground(vararg voids: Void): Boolean {
                        return saveRecordFile(result as ResponseBody, fileName)
                    }

                    override fun onPostExecute(result: Boolean?) {
                    }
                }

                recordDownloadTask?.execute()
            }

            override fun onFail(error: Any, flag: Int) {
            }
        })
    }

    @Synchronized
    private fun saveRecordFile(body: ResponseBody, fileName: String): Boolean {
        try {
            // 파일있으면 삭제부터 한다.
//            deleteFile(fileName, pkgName)

//      1
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            var savedRootFilePath = rootPath
//      2
//            var savedRootFilePath = if (rootPath.endsWith("/")) {
//                "${rootPath}Android${File.separator}data${File.separator}${packageName}"
//            } else {
//                "${rootPath}${File.separator}Android${File.separator}data${File.separator}${packageName}"
//            }
//      3
//            var savedRootFilePath = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!.path


            savedRootFilePath = "$savedRootFilePath${File.separator}Download${File.separator}"
            var sohoDownloadFolder = File(savedRootFilePath, "${File.separator}1393")

            if (!sohoDownloadFolder.exists()) {
                sohoDownloadFolder.mkdirs()
            }

            val destinationFile = File(savedRootFilePath, "1393${File.separator}${fileName}")
            var inputStream: InputStream? = null
            var ost: OutputStream? = null

            DLog.e("bjj saveRecordFile :: INIT :: "
                    + savedRootFilePath
                    + " ^ " + sohoDownloadFolder.exists()
                    + " ^ " + sohoDownloadFolder.path
                    + " ^ " + sohoDownloadFolder.isDirectory
                    + " ^ " + sohoDownloadFolder.isFile
                    + " ^ " + destinationFile.path
                    + " ^ " + destinationFile.isDirectory
                    + " ^ " + destinationFile.isFile)
            try {
                try {
                    inputStream = body.byteStream()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                ost = FileOutputStream(destinationFile, true)

                val data = ByteArray(32 * 1024)
                var fileSizeDownloaded: Long = 0
                val fileSize = body.contentLength()

                while (true) {
                    val read = inputStream?.read(data) ?: break

                    if (read == -1) {
                        break
                    }

                    if (destinationFile.length() == fileSize) {
                        break
                    }

                    DLog.e("bjj saveRecordFile :: ING "
                            + destinationFile.path + " ^ "
                            + destinationFile.length() + " ^ "
                            + fileSizeDownloaded + " ^ "
                            + fileSize)

                    ost.write(data, 0, read)
                    fileSizeDownloaded += read.toLong()
                }

                ost.flush()

                DLog.e("bjj saveRecordFile :: DONE "
                        + destinationFile.path + " ^ "
                        + destinationFile.length())

                (this@MainActivity as Activity).runOnUiThread {
                    openFileManager(destinationFile.path, true)
                }

                return true
            } catch (e: IOException) {
                e.printStackTrace()

                (this@MainActivity as Activity).runOnUiThread {
                    openFileManager(destinationFile.path, false)
                }

                DLog.e("bjj saveRecordFile :: FAIL aa " + e)
                return false
            } finally {
                inputStream?.close()
                ost?.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()

            (this@MainActivity as Activity).runOnUiThread {
                openFileManager("", false)
            }

            DLog.e("bjj saveRecordFile :: FAIL bb " + e)
            return false
        }
    }

    private fun openFileManager(path: String, isSuccess: Boolean) {
        if (!path.isEmpty()) {
            Toast.makeText(this, "다운로드 완료.\n" + path + "에서 확인해주세요.", Toast.LENGTH_SHORT).show()
        }

        mWebview?.sendEvent(IdevelServerScript.SET_DOWNLOAD_FILE, DownloadFileStatusInfo(isSuccess).toJsonString())
    }
}