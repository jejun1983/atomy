package com.idevel.waterbottle.utils

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager

const val NETWORK_TYPE_3G = "3G"
const val NETWORK_TYPE_4G = "4G"
const val NETWORK_TYPE_5G = "5G"
const val NETWORK_TYPE_WIFI = "WIFI"
const val NETWORK_TYPE_WIRE = "WIRE"
const val NETWORK_TYPE_ETC = "ETC"


//네트워크 상태 check
fun getNetworkInfo(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    try {
        when (cm.activeNetworkInfo?.type) {
            ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_BLUETOOTH -> {
                return NETWORK_TYPE_WIFI
            }

            ConnectivityManager.TYPE_MOBILE -> {
                return when {
//                        Check5GManager.getInstance().isSupport5GNetwork() -> NETWORK_TYPE_5G
                    cm.activeNetworkInfo.subtype == TelephonyManager.NETWORK_TYPE_LTE -> NETWORK_TYPE_4G
                    else -> NETWORK_TYPE_3G
                }
            }

            else -> {
                return NETWORK_TYPE_ETC
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return NETWORK_TYPE_ETC
}