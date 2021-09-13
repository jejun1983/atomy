package com.idevel.tatalk.web;

import android.content.Context;

import com.idevel.tatalk.BuildConfig;
import com.idevel.tatalk.utils.DLog;
import com.idevel.tatalk.utils.SharedPreferencesUtil;

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
public class UrlData {
    /**
     * The Constant LOGIN_PAGE_URL.
     */
    public static String NORMAL_SERVER_URL = "https://tatalk.wtest.biz/main/";

    public static String getSettingUrl(Context context) {
        String url = "";

        if (BuildConfig.DEBUG) {
            url = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.SETTING_URL);
        } else {
            // 상용
            url = "https://app.bottleletter.co.kr/app/version.php";
        }

        DLog.e("bjj UrlData :: SETTING "+url);

        return url;
    }
}
