package com.idevel.waterbottle.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.idevel.waterbottle.R

/**
 * Created by djsworld on 2017-08-04.
 */

class CustomAlertDialog(context: Context) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    private var buttonListener: View.OnClickListener? = null

    private var isErrorLayout = false
    private var isDataSaveLayout = false

    private var mTitleId: Int = 0
    private var mContentId: Int = 0

    private var mBtnOkTextId: Int = 0
    private var mBtnCancelTextId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 다이얼로그 외부 화면 흐리게 표현
        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.8f
        window.attributes = lpWindow


        setContentView(R.layout.dialog_update_version)

        if (isErrorLayout) {
            findViewById<View>(R.id.text_update_popup_3).visibility = View.GONE

            if (0 == mTitleId) {
                findViewById<View>(R.id.text_update_popup_1).visibility = View.GONE
            } else {
                (findViewById<View>(R.id.text_update_popup_1) as TextView).setText(mTitleId)
            }

            (findViewById<View>(R.id.text_update_popup_2) as TextView).setText(mContentId)

            findViewById<View>(R.id.btn_layout).visibility = View.GONE
            findViewById<View>(R.id.btn_error).visibility = View.VISIBLE
            findViewById<View>(R.id.btn_error).setOnClickListener(buttonListener)
        } else if (isDataSaveLayout) {
            findViewById<View>(R.id.text_update_popup_3).visibility = View.GONE

            if (0 == mTitleId) {
                findViewById<View>(R.id.text_update_popup_1).visibility = View.GONE
            } else {
                (findViewById<View>(R.id.text_update_popup_1) as TextView).setText(mTitleId)
            }

            (findViewById<View>(R.id.text_update_popup_2) as TextView).setText(mContentId)

            findViewById<View>(R.id.btn_cancel).setOnClickListener(buttonListener)
            findViewById<View>(R.id.btn_ok).setOnClickListener(buttonListener)

            if (0 != mBtnCancelTextId) {
                (findViewById<View>(R.id.btn_cancel) as Button).setText(mBtnCancelTextId)
            }
            if (0 != mBtnOkTextId) {
                (findViewById<View>(R.id.btn_ok) as Button).setText(mBtnOkTextId)
            }
        } else {
            findViewById<View>(R.id.btn_cancel).setOnClickListener(buttonListener)
            findViewById<View>(R.id.btn_ok).setOnClickListener(buttonListener)
        }
    }

    fun setErrorLayout(title: Int, content: Int) {
        isErrorLayout = true
        mTitleId = title
        mContentId = content
    }

    fun setDataSaveLayout(title: Int, content: Int) {
        isDataSaveLayout = true
        mTitleId = title
        mContentId = content
    }

    fun setOkClickListener(clickListener: View.OnClickListener) {
        buttonListener = clickListener
    }

    fun setButtonString(okBtnTextRes: Int, cancelBtnTextRes: Int) {
        mBtnOkTextId = okBtnTextRes
        mBtnCancelTextId = cancelBtnTextRes
    }

}
