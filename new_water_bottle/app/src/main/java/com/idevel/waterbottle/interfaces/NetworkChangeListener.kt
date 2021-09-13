package com.idevel.waterbottle.interfaces

/**
 * NetworkChangeListener
 * @company : medialog
 * @author : jjbae
 * datasaver 상태변경 알림 인터페이스
 **/
interface NetworkChangeListener {
    fun onDataSaverChanged() {}
    fun onNetworkDisconnected() {}
    fun onNetworkconnected() {}
//    fun setIpv6Enable(isEnable: Boolean) {}
}