package com.stevenschoen.putionew.files

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.stevenschoen.putionew.R

class FileTitleBarView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val backView: View

    init {
        View.inflate(context, R.layout.file_title_bar, this)
        titleView = findViewById(R.id.file_title_bar_name)
        backView = findViewById(R.id.file_title_bar_back)
    }

    var title: CharSequence
        set(value) {
            titleView.text = value
        }
        get() = titleView.text
}