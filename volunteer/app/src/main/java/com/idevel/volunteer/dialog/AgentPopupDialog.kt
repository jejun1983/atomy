package com.idevel.volunteer.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.idevel.volunteer.R
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_IMAGE
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_LINK_URL
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_MESSAGE
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_TARGET
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_TITLE
import com.idevel.volunteer.fcm.PushPreferences.PUSH_DATA_TYPE
import com.idevel.volunteer.utils.DLog

/**
 * Created by jjbae on 2017-08-04.
 */

class AgentPopupDialog(context: Context, intent: Intent) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar), View.OnClickListener {
    private var mType: String? = null
    private var mTitle: String? = null
    private var mMessage: String? = null
    private var mTarget: String? = null
    private var mPopupUrl: String? = null
    private var mPopupImage: String? = null
    private var okListener: View.OnClickListener? = null
    private var rootView: ConstraintLayout? = null
    private var mContext: Context? = null

    init {
        setData(context, intent)
    }

    private fun setData(context: Context, intent: Intent) {
        mContext = context

        mType = intent.getStringExtra(PUSH_DATA_TYPE)
        mTitle = intent.getStringExtra(PUSH_DATA_TITLE)
        mMessage = intent.getStringExtra(PUSH_DATA_MESSAGE)
        mTarget = intent.getStringExtra(PUSH_DATA_TARGET)
        mPopupUrl = intent.getStringExtra(PUSH_DATA_LINK_URL)
        mPopupImage = intent.getStringExtra(PUSH_DATA_IMAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.8f
        window!!.attributes = lpWindow

        DLog.e("bjj AgentPopupDialog onCreate :: ")

        when (mType) {
            "001" -> {
                setContentView(R.layout.dialog_type_text)

                rootView = (findViewById<View>(R.id.root_view) as ConstraintLayout)

                (findViewById<View>(R.id.text_title) as TextView).text = mTitle
                (findViewById<View>(R.id.text_body) as TextView).text = mMessage

                Glide.with(mContext!!).load(mPopupImage).into(findViewById<ImageView>(R.id.popup_img))

                findViewById<View>(R.id.btn_1_close).visibility = View.GONE
                findViewById<View>(R.id.layout_btn_2).visibility = View.VISIBLE

                findViewById<View>(R.id.btn_2_close).setOnClickListener(this)
                findViewById<View>(R.id.btn_2_ok).setOnClickListener(okListener)
            }
            else -> {

            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_1_close, R.id.btn_2_close -> dismiss()
        }
    }

    override fun dismiss() {
        if (mType == "003") {
            (findViewById<View>(R.id.webview_popup) as WebView?)?.destroy()
        }

        super.dismiss()
    }

    fun setOkClickListener(clickListener: View.OnClickListener) {
        okListener = clickListener
    }


    fun setDisappearClose() {
        if (rootView != null) {
            var anim: Animation = AnimationUtils.loadAnimation(mContext, R.anim.disappear)

            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {
                    DLog.e("bjj setDisappearClose :: onAnimationRepeat")
                }

                override fun onAnimationEnd(animation: Animation?) {
                    DLog.e("bjj setDisappearClose :: onAnimationEnd")
                    dismiss()
                }

                override fun onAnimationStart(animation: Animation?) {
                    DLog.e("bjj setDisappearClose :: onAnimationStart")
                }
            })

            rootView!!.startAnimation(anim)
        } else {
            dismiss()
        }
    }
}