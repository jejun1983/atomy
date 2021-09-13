package com.idevel.waterbottle.utils.font

import com.idevel.waterbottle.R

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView

import java.util.HashMap

object FontManager {

    private var fontMap: HashMap<String, Typeface>? = null

    fun applyTypeface(context: Context, tv: TextView, attrs: AttributeSet) {
        var fontPath: String? = ""

        val a = context.obtainStyledAttributes(attrs, R.styleable.TextFontAttr)
        val n = a.indexCount

        for (i in 0 until n) {
            val attr = a.getIndex(i)

            when (attr) {
                R.styleable.TextFontAttr_fontName -> fontPath = a.getString(attr)
            }
        }

        a.recycle()

        if (fontPath == null || fontPath.length <= 0) {
            applyTypeface(context, tv)
        } else {
            applyTypeface(context, tv, fontPath)
        }
    }

    @JvmOverloads
    fun applyTypeface(context: Context, tv: TextView, fontPath: String = "fonts/NotoSansKR-Regular.otf") {
        if (null == fontMap) {
            fontMap = HashMap()
        }

        var typeface = fontMap!![fontPath]

        if (null == typeface) {
            try {
                typeface = Typeface.createFromAsset(context.assets, fontPath)
                fontMap!![fontPath] = typeface
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }

        }

        tv.typeface = typeface
    }
}//custom font 적용시 활성화한다
