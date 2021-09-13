package com.idevel.dongmyung.fcm

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.idevel.dongmyung.MyApplication
import com.idevel.dongmyung.activity.MainActivity
import com.idevel.dongmyung.activity.PushPopupActivity
import com.idevel.dongmyung.fcm.PushPreferences.PUSH_DATA
import com.idevel.dongmyung.notification.PushNotification
import com.idevel.dongmyung.utils.DLog
import com.idevel.dongmyung.utils.isAppRunning
import com.idevel.dongmyung.utils.isForeground

/**
 * Created by jjbae on 2016-02-25.
 */
class FcmListenerService : FirebaseMessagingService() {
    @Override
    override fun onMessageReceived(message: RemoteMessage) {
        if (MyApplication.instance != null) {
            DLog.v("bjj FcmListenerService push isChating : " + MyApplication.instance.isChating)

            if (MyApplication.instance.isChating) {
                return
            }
        }

        val from = message.from

        DLog.v("bjj FcmListenerService push from : " + from)

        // intent로 넘길 push 데이터
        val data = message.data
        val bundle = Bundle()

        for (entry in data) {
            bundle.putString(entry.key, entry.value)
            DLog.v("bjj FcmListenerService push key : " + entry.key + " : " + entry.value)
        }

        DLog.e("bjj FcmListenerService push : "
                + isAppRunning(applicationContext) + " ^ "
                + isForeground(applicationContext) + " ^ "
                + applicationContext + " ^ "
                + bundle)

        if (!isAppRunning(applicationContext)) {
            var pushDataNotiParser: PushDataNotiParser = gson().fromJson(bundle.get("noti").toString(), PushDataNotiParser::class.java)

//            Handler(Looper.getMainLooper()).postDelayed({
//                Toast.makeText(this@FcmListenerService, bundle.get("noti").toString(), Toast.LENGTH_SHORT).show()
//            }, 0L)

            if (pushDataNotiParser?.noti_display == "show") {
                setNotification(pushDataNotiParser)
            }
        } else {
            if (!isForeground(applicationContext)) {
                var pushDataNotiParser: PushDataNotiParser = gson().fromJson(bundle.get("noti").toString(), PushDataNotiParser::class.java)

//                Handler(Looper.getMainLooper()).postDelayed({
//                    Toast.makeText(this@FcmListenerService, bundle.get("noti").toString(), Toast.LENGTH_SHORT).show()
//                }, 0L)

                if (pushDataNotiParser?.noti_display == "show") {
                    setNotification(pushDataNotiParser)
                }
            } else {
//                Handler(Looper.getMainLooper()).postDelayed({
//                    Toast.makeText(this@FcmListenerService, bundle.get("popup").toString(), Toast.LENGTH_SHORT).show()
//                }, 0L)

                // popup
                val i = Intent(applicationContext, PushPopupActivity::class.java)
                i.putExtra(PUSH_DATA, bundle)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                if (!isAppRunning(applicationContext)) {
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }

                startActivity(i)
            }
        }
    }

    /**
     * Notification 설정
     */
    private fun setNotification(pushDataNotiParser: PushDataNotiParser) {
        val notiIntent = Intent(this, MainActivity::class.java)
        notiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        DLog.e("bjj PushPopupActivity notiIntent ImageUrl : " + pushDataNotiParser?.noti_img)
        DLog.e("bjj PushPopupActivity notiIntent Title : " + pushDataNotiParser?.noti_title)
        DLog.e("bjj PushPopupActivity notiIntent Message : " + pushDataNotiParser?.noti_message)
        DLog.e("bjj PushPopupActivity notiIntent PopupUrl : " + pushDataNotiParser?.noti_link)

        notiIntent.putExtra(PushPreferences.IS_NOTI, 0)

        notiIntent.putExtra(PushPreferences.PUSH_DATA_TITLE, pushDataNotiParser?.noti_title)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_MESSAGE, pushDataNotiParser?.noti_message)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_LINK_URL, pushDataNotiParser?.noti_link)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_LINK_TYPE, pushDataNotiParser?.noti_target)
        notiIntent.putExtra(PushPreferences.PUSH_DATA_IMAGE, pushDataNotiParser?.noti_img)

        if (pushDataNotiParser?.noti_img.isNullOrEmpty()) {
            PushNotification.sendNotification(this, notiIntent, pushDataNotiParser?.noti_title, pushDataNotiParser?.noti_message)
        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(pushDataNotiParser?.noti_img)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            PushNotification.sendNotification(applicationContext, notiIntent, pushDataNotiParser?.noti_title, pushDataNotiParser?.noti_message, resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                    })
        }
    }

    private fun gson(): Gson {
        return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    }
}