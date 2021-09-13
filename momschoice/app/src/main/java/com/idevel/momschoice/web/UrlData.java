package com.idevel.momschoice.web;

import android.content.Context;

import com.idevel.momschoice.utils.DLog;

/**
 * The UrlData Class.
 *
 * @author : jjbae
 */
public class UrlData {
  /**
   * The Constant LOGIN_PAGE_URL.
   */
  public static String NORMAL_SERVER_URL = "https://momschoice.wtest.biz/";

  public static String getSettingUrl(Context context) {
    String url = "";

//        if (BuildConfig.DEBUG) {
//            url = SharedPreferencesUtil.getString(context, SharedPreferencesUtil.Cmd.SETTING_URL);
//        } else {
//            // 상용
    url = "https://momschoice.wtest.biz/app/image.php";
//        }

    DLog.e("bjj UrlData :: SETTING " + url);

    return url;
  }
}
