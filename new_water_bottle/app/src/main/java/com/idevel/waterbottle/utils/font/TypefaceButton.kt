package com.idevel.waterbottle.utils.font

import android.content.Context
import android.util.AttributeSet

class TypefaceButton : androidx.appcompat.widget.AppCompatButton {
    constructor(context: Context) : super(context) {
        FontManager.applyTypeface(context, this)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        FontManager.applyTypeface(context, this, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        FontManager.applyTypeface(context, this, attrs)
    }
}
