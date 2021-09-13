package com.idevel.tatalk.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.idevel.tatalk.R
import com.idevel.tatalk.utils.SharedPreferencesUtil
import com.idevel.tatalk.utils.getIconBadgeIntent
import com.idevel.tatalk.utils.setIconBadge

/**
 * Created by jjbae on 2017-08-03.
 */

object PushNotification {
    private const val WATER_BOTTLE_NOTI_ID = 17971797

    // 푸시 노티
    const val CHANNEL_ID_PUSH = "com_idevel_tatalk"

    /**
     * 실제 디바에스에 GCM으로부터 받은 메세지를 알려주는 함수이다. 디바이스 Notification Center에 나타난다.
     *
     * @param title
     * @param message
     */
    fun sendNotification(context: Context, intent: Intent, title: String?, message: String?, image: Bitmap? = null) {
        clearNotification(context)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_PUSH)
                .setSmallIcon(R.mipmap.app_icon)
                .setLargeIcon(image)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setNumber(1)
                .setDeleteIntent(getIconBadgeIntent(context, 0))
                .setContentText(message)
//                .setSound(defaultSoundUri)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

//        image?.let {
//            val bigPictureStyle = NotificationCompat.BigPictureStyle(notificationBuilder)
//            bigPictureStyle.bigPicture(it)
//                    .setBigContentTitle(title)
//                    .setSummaryText(message)
//        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(WATER_BOTTLE_NOTI_ID, notificationBuilder.build())
        setIconBadge(context, 1)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isPushVibrate = SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Cmd.PUSH_VIBRATE)
            val isPushbeep = SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Cmd.PUSH_BEEP)

            if (isPushVibrate) {
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vib.vibrate(1500)
                }
            }

            if (isPushbeep) {
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                var ringtone: Ringtone = RingtoneManager.getRingtone(context, defaultSoundUri)

                ringtone.play()
            }
        }
    }

    private fun clearNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(WATER_BOTTLE_NOTI_ID)
    }
}
