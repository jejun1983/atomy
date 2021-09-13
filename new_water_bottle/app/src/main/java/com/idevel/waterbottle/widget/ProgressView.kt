package com.idevel.waterbottle.widget

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import com.idevel.waterbottle.R
import kotlin.math.floor

/**
 * Created by djsworld on 2017-01-16.
 */

class ProgressView : androidx.appcompat.widget.AppCompatImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setAnimation(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        setAnimation(attrs)
    }


    private fun setAnimation(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ProgressView)
        val frameCount = a.getInt(R.styleable.ProgressView_frameCount, 8)
        val duration = a.getInt(R.styleable.ProgressView_duration, 1000)
        a.recycle()

        setAnimation(frameCount, duration)
    }

    private fun setAnimation(frameCount: Int, duration: Int) {
        val a = AnimationUtils.loadAnimation(context, R.anim.progress_anim)
        a.duration = duration.toLong()
        a.interpolator = Interpolator { input -> floor((input * frameCount).toDouble()).toFloat() / frameCount }

        startAnimation(a)
    }
}