package com.idevel.volunteer.web;

import android.content.Context;

import com.idevel.volunteer.utils.DLog;

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
public class UrlData {
    /**
     * The Constant LOGIN_PAGE_URL.
     */
    public static String NORMAL_SERVER_URL = "https://www.1393kfsp.or.kr/login.php";

    public static String getUploadUrl(Context context) {
        String url = "";

//        if (BuildConfig.DEBUG) {
//            url = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.SETTING_URL);
//        } else {
//            // 상용
            url = "https://www.1393kfsp.or.kr/app/image.php";
//        }

        DLog.e("bjj UrlData :: getUploadUrl "+url);

        return url;
    }
}
