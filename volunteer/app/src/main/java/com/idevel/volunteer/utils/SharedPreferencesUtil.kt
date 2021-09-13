package com.idevel.volunteer.utils

import android.content.Context
import android.preference.PreferenceManager

/**
 * The SharedPreferencesUtil Class.
 *
 * @author : jjbae
 */
object SharedPreferencesUtil {

    /**
     * The Cmd Enum.
     *
     * @author : jjbae
     */
    enum class Cmd {
        POPUP_DATE,

        SETTING_URL,

//        MAIN_URL,
        PUSH_REG_ID,
        DEV_LAST_USED_CTN,
        UUID,
        PURCHASED_SINGLE_MONTH,

        PUSH_VIBRATE,
        PUSH_BEEP,

        AUTO_LOGIN,
        AUTO_LOGIN_ID,
        AUTO_LOGIN_PW
    }

    /**
     * SharedPreferences 초기화.
     *
     * @param context the context
     */
    @JvmStatic
    fun clear(context: Context?) {
        if (context == null) {
            return
        }
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        if (null != sp) {
            DLog.e("clear user info!! ")
            val edit = sp.edit()
            edit.putString(Cmd.PUSH_REG_ID.toString(), "")
            edit.putString(Cmd.POPUP_DATE.toString(), "")

            edit.commit()
        }
    }

    /**
     * Gets the string.
     *
     * @param context the context
     * @param cmd     the cmd
     * @return the string
     */
    @JvmStatic
    fun getString(context: Context?, cmd: Cmd): String? {
        if (context != null) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            if (null != sp) {
                return sp.getString(cmd.toString(), "")
            }
        }
        return null
    }

    /**
     * Sets the string.
     *
     * @param context the context
     * @param cmd     the cmd
     * @param data    the data
     */
    @JvmStatic
    fun setString(context: Context?, cmd: Cmd, data: String?) {
        if (context == null) {
            return
        }
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        if (null != sp) {
            DLog.d("CMD ==>> $cmd data ==>> $data")
            val edit = sp.edit()
            edit.putString(cmd.toString(), data)
            edit.commit()
        }
    }

    /**
     * Gets the boolean.
     *
     * @param context the context
     * @param cmd     the cmd
     * @return the boolean
     */
    @JvmStatic
    fun getBoolean(context: Context?, cmd: Cmd): Boolean {
        if (context != null) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            if (null != sp) {
                return sp.getBoolean(cmd.toString(), false)
            }
        }
        return false
    }

    /**
     * Sets the boolean.
     *
     * @param context the context
     * @param cmd     the cmd
     * @param data    the data
     */
    @JvmStatic
    fun setBoolean(context: Context?, cmd: Cmd, data: Boolean) {
        if (context == null) {
            return
        }
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        if (null != sp) {
            DLog.d("CMD ==>> $cmd data ==>> $data")
            val edit = sp.edit()
            edit.putBoolean(cmd.toString(), data)
            edit.commit()
        }
    }

    /**
     * Gets the Int
     *
     * @author djsworld
     */
    @JvmStatic
    fun getInt(context: Context?, cmd: Cmd): Int {
        if (context != null) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            if (null != sp) {
                return sp.getInt(cmd.toString(), 0)
            }
        }
        return 0
    }

    /**
     * Sets the Int
     *
     * @param context the context
     * @param cmd     the cmd
     * @param data    the data
     * @author djsworld
     */
    @JvmStatic
    fun setInt(context: Context?, cmd: Cmd, data: Int) {
        if (context == null) {
            return
        }
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        if (null != sp) {
            DLog.d("CMD ==>> $cmd data ==>> $data")
            val edit = sp.edit()
            edit.putInt(cmd.toString(), data)
            edit.commit()
        }
    }
}
