package com.idevel.volunteer.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.idevel.volunteer.activity.MainActivity
import java.util.*


fun isAppRunning(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val activitys = activityManager.getRunningTasks(3)
    var isActivityFound = false
    for (i in activitys.indices) {
        if (activitys[i].topActivity.toString().contains(MainActivity::class.java.name, ignoreCase = true)) {
            isActivityFound = true
        }
    }
    DLog.d("isAppRunning : $isActivityFound")
    return isActivityFound
}

fun isForeground(context: Context): Boolean {
    val packageName: String = context.getPackageName()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = activityManager.getRunningTasks(1)
    return info[0].topActivity!!.className.contains(packageName)
}


fun getUUID(context: Context): String {
    var uuid = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.UUID) ?: ""
    if (uuid.isEmpty()) {
        uuid = UUID.randomUUID().toString()
        SharedPreferencesUtil.setString(context, SharedPreferencesUtil.Cmd.UUID, uuid)
    }

    return uuid
}

fun isCanDrawOverlays(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return Settings.canDrawOverlays(context)
    }

    return true
}

fun isCanAllFileAcess(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return Environment.isExternalStorageManager()
    }

    return true
}