package com.idevel.waterbottle.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

/**
 * OnPhone
 * Class: CharacterTextView
 * Created by Han on 2017-08-04.
 *
 * Description: 텍스트 뷰 wrapping을 문자 단위로 처리하도록 함
 */
class CharacterTextView: TextView {
    constructor(context: Context) : super(context)
    {
//        FontUtil.applyTypeface(context, this)
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    {
//        FontUtil.applyTypeface(context, this, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr:Int): super(context, attrs, defStyleAttr)
    {
//        FontUtil.applyTypeface(context, this, attrs)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text.toString().replace(" ", "\u00A0"), type)
    }
}