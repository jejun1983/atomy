package com.idevel.waterbottle.web;

import android.content.Context;

import com.idevel.waterbottle.BuildConfig;
import com.idevel.waterbottle.utils.DLog;
import com.idevel.waterbottle.utils.SharedPreferencesUtil;

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
public class UrlData {
    /**
     * The Constant LOGIN_PAGE_URL.
     */
    public static String NORMAL_SERVER_URL = "http://d-app.bottleletter.co.kr/index.php";

    public static String getSettingUrl(Context context) {
        String url = "";

        if (BuildConfig.DEBUG) {
            url = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.SETTING_URL);
        } else {
            // 상용
            url = "https://app.bottleletter.co.kr/app/version.php";

            // 개발
//            url = "http://d-app.bottleletter.co.kr/app/version.php";
        }

        DLog.e("bjj UrlData :: SETTING "+url);

        return url;
    }
}
