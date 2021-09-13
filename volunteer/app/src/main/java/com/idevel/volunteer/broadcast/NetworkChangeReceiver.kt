package com.idevel.volunteer.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Message
import com.idevel.volunteer.interfaces.NetworkChangeListener
import com.idevel.volunteer.utils.*
import java.lang.ref.WeakReference


/**
 * 네트워크 상태 변화를 받는 Receiver
 */
class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        const val NETWORK_DISCONNECTED = 0  // 네트워크 차단
        const val NETWORK_CONNECTED = 1 // 네트워크 활성화
    }

    var mListener: NetworkChangeListener? = null
    private var mHandler: NetworkStatusHandler = NetworkStatusHandler(this)

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null) return

        when (intent?.action) {
            ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED -> mListener?.onDataSaverChanged()
            else -> checkNetworkStatus(context)
        }

        DLog.e("bjj onReceive NetworkChangeReceiver " + intent?.action)
    }

    fun setListener(listener: NetworkChangeListener) {
        mListener = listener
    }

    private fun checkNetworkStatus(context: Context) {
        DLog.e("NetWork : ${getNetworkInfo(context)}")

        when (getNetworkInfo(context)) {
            NETWORK_TYPE_ETC -> {
                mHandler.sendDelay(NETWORK_DISCONNECTED)
                mHandler.removeMessages(NETWORK_CONNECTED)
            }
            NETWORK_TYPE_WIFI -> {
                mHandler.removeMessages(NETWORK_DISCONNECTED)
                mHandler.sendDelay(NETWORK_CONNECTED)
                // ipv6 false
//                mListener?.setIpv6Enable(false)
            }
            NETWORK_TYPE_4G, NETWORK_TYPE_5G -> {
                mHandler.removeMessages(NETWORK_DISCONNECTED)
                mHandler.sendDelay(NETWORK_CONNECTED)
                // chech ipv6
//                mListener?.setIpv6Enable(useIPv6(context))
            }
            else -> {
                mHandler.removeMessages(NETWORK_DISCONNECTED)
            }
        }
    }

    private class NetworkStatusHandler(act: NetworkChangeReceiver) : Handler() {

        private val ref: WeakReference<NetworkChangeReceiver> = WeakReference(act)

        fun sendDelay(what: Int) {
            sendMessageDelayed(obtainMessage(what), 1500)
        }

        override fun handleMessage(msg: Message) {
            val act = ref.get()
            if (act != null) {
                when (msg.what) {
                    NETWORK_DISCONNECTED -> {
                        // 네트워크 차단 팝업
                        act.mListener?.onNetworkDisconnected()
                    }
                    NETWORK_CONNECTED -> {
                        // 네트워크 활성화
                        act.mListener?.onNetworkconnected()
                    }
                }
            }
        }
    }

}