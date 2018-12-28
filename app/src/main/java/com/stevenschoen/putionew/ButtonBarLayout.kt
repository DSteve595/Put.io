package com.stevenschoen.putionew

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.use

class ButtonBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.buttonBarLayoutStyle,
    defStyleRes: Int = R.style.Widget_Putio_ButtonBarLayout
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

  private var buttonPadding: Int = 0

  init {
    orientation = HORIZONTAL
    gravity = Gravity.END

    context.obtainStyledAttributes(attrs, R.styleable.ButtonBarLayout, defStyleAttr, defStyleRes)
        .use { attrsArray ->
          buttonPadding = attrsArray.getDimensionPixelSizeOrThrow(
              R.styleable.ButtonBarLayout_buttonPadding
          )
        }

    dividerDrawable = GradientDrawable().apply {
      setSize(buttonPadding, 0)
    }
    showDividers = SHOW_DIVIDER_MIDDLE
  }

}
