package com.idevel.waterbottle.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager


//fun getCTN(context: Context): String {
//    if (BuildConfig.DEBUG) {
//        var devCtn = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.DEV_LAST_USED_CTN)
//
//        if (!devCtn.isNullOrEmpty()) {
//            return SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.DEV_LAST_USED_CTN)!!
//        }
//    }
//
//    val mgr = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//    try {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//                return ""
//            }
//        }
//        var userPhone: String? = mgr.line1Number
//        if (null == userPhone) {
//            DLog.e("getLine1Number is null")
//            return ""
//        }
//        userPhone = userPhone.replace("+82", "0")
//        return userPhone
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//
//    return ""
//}

/**
 * Gets the version name.
 *
 * @param context the context
 * @return the version name
 */
fun getVersionName(context: Context): String? {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}

/**
 * Checks if is network connected.
 *
 * @param context the context
 * @return true, if is network connected
 */
fun isNetworkConnected(context: Context?): Boolean {
    val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetwork = cm.activeNetworkInfo
    if (null != activeNetwork) {
        if (ConnectivityManager.TYPE_WIFI == activeNetwork.type || ConnectivityManager.TYPE_MOBILE == activeNetwork.type) {
            return true
        }
    }
    return false
}

/**
 * Gets the phone number key.
 *
 * @param context the conext
 * @return the phone number key
 */
//fun getPhoneNumberKey(context: Context?): String? {
//    return SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.NOTI_ALRAM_PHONE_NUMBER)
//}

fun checkDataSave(context: Context?): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val connMgr = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (connMgr != null && connMgr.isActiveNetworkMetered) {
            if (connMgr.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                return true
            }
        }
    }
    return false
}

fun hasUsim(context: Context) = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).simState != TelephonyManager.SIM_STATE_ABSENT


/**
 * navigation bar 존재여부 (soft key)
 * @param context
 * @return
 */
fun hasSoftNavigation(context: Context): Boolean {
    var hasSoftNavigation = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        val resources = context.resources

        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) {
            hasSoftNavigation = resources.getBoolean(id)
        }
    }

    return hasSoftNavigation
}