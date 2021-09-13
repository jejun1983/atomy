package com.idevel.waterbottle.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.idevel.waterbottle.activity.MainActivity


fun getIconBadgeIntent(context: Context, notiCnt: Int): PendingIntent {
    val badgeIntent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
    badgeIntent.putExtra("badge_count", notiCnt)
    badgeIntent.putExtra("badge_count_package_name", context.packageName)
    badgeIntent.putExtra("badge_count_class_name", MainActivity::class.java.name)

    return PendingIntent.getBroadcast(context, 0 /* Request code */, badgeIntent, PendingIntent.FLAG_ONE_SHOT)
}

fun setIconBadge(context: Context, notiCnt: Int) {
    val badgeIntent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
    badgeIntent.putExtra("badge_count", notiCnt)
    badgeIntent.putExtra("badge_count_package_name", context.packageName)
    badgeIntent.putExtra("badge_count_class_name", MainActivity::class.java.name)

    context.sendBroadcast(badgeIntent)
}



///**
// * Sets the phone number key null value.
// *
// * @param context the conext
// * @author djsworld
// */
//fun setNullValuePhoneNumberKey(context: Context) {
//    DLog.v("setNullValuePhoneNumberKey")
//
//    SharedPreferencesUtil.setString(context, SharedPreferencesUtil.Cmd.NOTI_ALRAM_PHONE_NUMBER, null)
//}