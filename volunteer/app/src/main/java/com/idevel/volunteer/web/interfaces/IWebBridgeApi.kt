package com.idevel.volunteer.web.interfaces

import com.idevel.volunteer.web.constdata.RequestBuyProductInfo
import com.idevel.volunteer.web.constdata.RequestCallPhoneInfo
import com.idevel.volunteer.web.constdata.RequestExternalWebInfo


/**
 * The BridgeListener
 * @company : jjbae
 */
interface IWebBridgeApi {
    fun removeSplash()                                      //webUI에서 화면 load가 끝나면 splash 화면을 제거
    fun getPushRegId()                                      //push reg id를 저장
    fun restartApp()                                        //app 재시작
    fun finishApp()                                         //app 종료
    fun getAppVersion()                                     //앱 버전을 return(6자리)
    fun requestCallPhone(data: RequestCallPhoneInfo)        //전화요청
    fun requestExternalWeb(data: RequestExternalWebInfo)    //전달한 url로 외부 브라우져 요청
    fun pageClearHistory() {}                               //web page history 초기화
    fun openSharePopup(url: String) {}                      //공유 팝업노출

    fun getGpsInfo() {}                                     //현재 위치 정보

    fun readyOneStoreBilling()                              //oneStore 결재를 할 수 있는 상태
    fun requestBuyProduct(data: RequestBuyProductInfo)      //결재 요청

    fun openCamera(type: String, param: String)             //카메라 호출
    fun openGallery(type: String, param: String)            //갤러리 호출

    fun setPushVibrate(isBool: Boolean)                     // push 진동
    fun setPushBeep(isBool: Boolean)                        //push 사운드

    fun setAutoLogin(isAuto: Boolean)
    fun getAutoLogin()
    fun setAccount(id: String, pw: String)
    fun getAccount()

    fun downloadFile(fileURL: String, fileName: String)
}