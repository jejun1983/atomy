package com.idevel.momschoice

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.idevel.momschoice.notification.PushNotification

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set

        var PERMISSIONS = arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,

//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION,

                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }


    var isGoogleMarket: Boolean = true
    var isChating: Boolean = false
    val inappTest: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            makeNotiChannel()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun makeNotiChannel() {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // The user-visible name of the channel.
        val name2 = getString(R.string.noti_channel_name_push)
        // The user-visible description of the channel.
        val importance2 = NotificationManager.IMPORTANCE_HIGH
        val mChannel2 = NotificationChannel(PushNotification.CHANNEL_ID_PUSH, name2, importance2)

        // Configure the notification channel.
//        mChannel2.enableLights(true)
//        mChannel2.enableVibration(false)
        mChannel2.setSound(null, null)



        mChannel2.setShowBadge(true)

        mNotificationManager.createNotificationChannel(mChannel2)
    }
}
