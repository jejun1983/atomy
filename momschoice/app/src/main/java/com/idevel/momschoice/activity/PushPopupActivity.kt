package com.idevel.momschoice.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.*
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.idevel.momschoice.R
import com.idevel.momschoice.fcm.PushDataNotiParser
import com.idevel.momschoice.fcm.PushDataPopupParser
import com.idevel.momschoice.fcm.PushPreferences.IS_NOTI
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_IMAGE
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_LINK_TYPE
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_LINK_URL
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_MESSAGE
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_SHOWTIME
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_TARGET
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_TITLE
import com.idevel.momschoice.fcm.PushPreferences.PUSH_DATA_TYPE
import com.idevel.momschoice.notification.PushNotification.sendNotification
import com.idevel.momschoice.utils.DLog
import com.idevel.momschoice.utils.isAppRunning
import org.json.JSONObject


/**
 * Created by jjbae on 2017-07-17.
 */

class PushPopupActivity : Activity(), View.OnClickListener {
    private var pushType: String? = null
    private var pushDataNotiParser: PushDataNotiParser? = null
    private var pushDataPopupParser: PushDataPopupParser? = null
    private var isScreenOn = true


    /**
     * 메인 액티비티를 실행하는 인텐트 생성
     */
    private val mainActivityIntent: Intent
        get() {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

//            if (parser.isUseInPopup) {
//                i.putExtra(PUSH_USE_IN_POPUP, "Y")
//
            DLog.e("bjj PushPopupActivity ImageUrl : " + pushDataPopupParser?.popup_img)
            DLog.e("bjj PushPopupActivity Title : " + pushDataPopupParser?.popup_title)
            DLog.e("bjj PushPopupActivity Message : " + pushDataPopupParser?.popup_message)
            DLog.e("bjj PushPopupActivity PopupUrl : " + pushDataPopupParser?.popup_link)

            i.putExtra(PUSH_DATA_TYPE, pushType)

            i.putExtra(IS_NOTI, 1)

            i.putExtra(PUSH_DATA_TITLE, pushDataPopupParser?.popup_title)
            i.putExtra(PUSH_DATA_MESSAGE, pushDataPopupParser?.popup_message)
            i.putExtra(PUSH_DATA_TARGET, pushDataPopupParser?.popup_target)
            i.putExtra(PUSH_DATA_LINK_URL, pushDataPopupParser?.popup_link)
            i.putExtra(PUSH_DATA_IMAGE, pushDataPopupParser?.popup_img)
            i.putExtra(PUSH_DATA_SHOWTIME, pushDataPopupParser?.popup_showtime)

            return i
        }

    /**
     * 메인 액티비티를 실행하는 인텐트 생성
     */
    private val notiIntent: Intent
        get() {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

//            if (parser.isUseInPopup) {
//                i.putExtra(PUSH_USE_IN_POPUP, "Y")
//
            DLog.e("bjj PushPopupActivity notiIntent ImageUrl : " + pushDataNotiParser?.noti_img)
            DLog.e("bjj PushPopupActivity notiIntent Title : " + pushDataNotiParser?.noti_title)
            DLog.e("bjj PushPopupActivity notiIntent Message : " + pushDataNotiParser?.noti_message)
            DLog.e("bjj PushPopupActivity notiIntent PopupUrl : " + pushDataNotiParser?.noti_link)

            i.putExtra(IS_NOTI, 0)

            i.putExtra(PUSH_DATA_TITLE, pushDataNotiParser?.noti_title)
            i.putExtra(PUSH_DATA_MESSAGE, pushDataNotiParser?.noti_message)
            i.putExtra(PUSH_DATA_LINK_URL, pushDataNotiParser?.noti_link)
            i.putExtra(PUSH_DATA_LINK_TYPE, pushDataNotiParser?.noti_target)
            i.putExtra(PUSH_DATA_IMAGE, pushDataNotiParser?.noti_img)

            return i
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val myBundleData: Bundle = intent.getBundleExtra(PUSH_DATA)

        pushType = myBundleData.get("push_type").toString()
        pushDataNotiParser = gson().fromJson(myBundleData.get("noti").toString(), PushDataNotiParser::class.java)
        pushDataPopupParser = gson().fromJson(myBundleData.get("popup").toString(), PushDataPopupParser::class.java)


//        DLog.e("bjj PushPopupActivity :: DATA "
//                + myBundleData + " ^ "
//                + myBundleData.get("push_type") + " ^ "
//                + myBundleData.get("noti") + " ^ "
//                + myBundleData.get("popup"))


        // 화면 꺼져있는 경우 잠금화면 상태에서 팝업 띄움
        if (!isScreenOn()) {
            isScreenOn = false
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        window.attributes.width = WindowManager.LayoutParams.MATCH_PARENT

        findViewById<Button>(R.id.btn_1_close)?.setOnClickListener(this)
        findViewById<Button>(R.id.btn_2_close)?.setOnClickListener(this)
        findViewById<Button>(R.id.btn_2_ok)?.setOnClickListener(this)

        DLog.e("bjj PushPopupActivity :: pushDataNotiParser DATA "
                + pushDataNotiParser?.noti_display + "^"
                + pushDataPopupParser?.popup_display)

        if (pushDataNotiParser?.noti_display == "show") {
            setNotification()
        }

        Handler().postDelayed({
            if (pushDataPopupParser?.popup_display == "show") {
                setPushPopup()
            } else {
                finish()
            }
        }, 1000L)
    }

    /**
     * 단말기 화면이 켜져있는지 여부
     */
    private fun isScreenOn(): Boolean {
        return try {
            val powerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                powerManager.isScreenOn
            } else {
                powerManager.isInteractive
            }
        } catch (e: NullPointerException) {
            false
        }
    }

    /**
     * Notification 설정
     */
    private fun setNotification() {
        DLog.e("bjj PushNotification :: setNotification " + pushDataNotiParser?.noti_img)

        if (pushDataNotiParser?.noti_img.isNullOrEmpty()) {
            sendNotification(this, notiIntent, pushDataNotiParser?.noti_title, pushDataNotiParser?.noti_message)
        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(pushDataNotiParser?.noti_img)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            sendNotification(this@PushPopupActivity, notiIntent, pushDataNotiParser?.noti_title, pushDataNotiParser?.noti_message, resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
        }
    }

    private fun setNotiSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone.play()
        } else if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val millisecond: Long = 700
            vibrator.vibrate(millisecond)
        }
    }

    /**
     * 팝업 설정
     */
    private fun setPushPopup() {
        DLog.e("bjj PushPopupActivity setPushPopup : " + isAppRunning(this) + " ^ " + isScreenOn)

        if (isAppRunning(this) && isScreenOn) {
//            if (parser.isUseInPopup) {
//                // 앱 실행중, 내부팝업 사용인 경우 내부팝업만 띄움
//                // 내부팝업 데이터 전달
//                // MainActivity onNewIntent에서 처리
            startActivity(mainActivityIntent)
            finish()

//            } else {
//                // 앱 실행중, 내부팝업 미사용인 경우 외부팝업을 띄움. 버튼 한개
//                // MainActivity onNewIntent에서 처리
//                initLayout(true)
//            }
        } else {
            // 앱 실행중이 아닌경우 외부팝업 띄움. 버튼 두개
//            initLayout(false)
        }
    }


    /**
     * 레이아웃 초기화
     */
//    private fun initLayout(useOneButton: Boolean) {
//        if (parser.isUseNotiImage) {
//            if (!parser.notiTitle.isNullOrBlank() && !parser.notiContents.isNullOrBlank()) {
//                setContentView(R.layout.dialog_type_imagetext)
//            } else {
//                setContentView(R.layout.dialog_type_image)
//            }
//            (findViewById<View>(R.id.image) as ImageView).setImageBitmap(parser.notiImage)
//        } else {
//            setContentView(R.layout.dialog_type_text)
//        }
//
//        (findViewById<View>(R.id.text_title) as TextView?)?.text = parser.notiTitle
//        (findViewById<View>(R.id.text_body) as TextView?)?.text = parser.notiContents
//
//        if (useOneButton) {
//            findViewById<Button>(R.id.btn_1_close)?.visibility = View.VISIBLE
//            findViewById<LinearLayout>(R.id.layout_btn_2)?.visibility = View.GONE
//        } else {
//            findViewById<Button>(R.id.btn_1_close)?.visibility = View.GONE
//            findViewById<LinearLayout>(R.id.layout_btn_2)?.visibility = View.VISIBLE
//        }
//    }

    /**
     * push data 확인 테스트
     */
    private fun initTest() {
        val body = findViewById<TextView>(R.id.text_body)
        body.text = ""

        val buttonOk = findViewById<Button>(R.id.btn_cancel)
        buttonOk.setOnClickListener(this)

        val data = intent.getBundleExtra(PUSH_DATA)

        for (key in data.keySet()) {
            try {
                val s = data.getString(key)
                DLog.v("$key : $s")
                val jsonObject = JSONObject(s)

                val i = jsonObject.keys()
                while (true) {
                    if (!i.hasNext()) break

                    val k = i.next()

                    val log = "[" + key + "]" + k + " : " + jsonObject.getString(k)
                    DLog.e(log)

                    body.text = body.text.toString() + log + "\n"
                }

            } catch (e: Exception) {

            }

        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_2_ok -> {
//                PushNotification.clearNotification(this)
//                startActivity(mainActivityIntent)
//                finish()

                DLog.e("bjj PushPopupActivity btn_2_ok : ")
            }

            R.id.btn_1_close, R.id.btn_2_close -> {
//                finish()

                DLog.e("bjj PushPopupActivity btn_1_close  btn_2_close : ")
            }
        }
    }

    override fun onResume() {
        // 꺼진 화면에서 팝업 뜬 경우 몇 초 후 닫음
        if (!isScreenOn) {
            Handler().postDelayed({ finish() }, 7000L)
        }

        super.onResume()
    }

    private fun gson(): Gson {
        return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }
}
