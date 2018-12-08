package com.stevenschoen.putionew.account

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import com.stevenschoen.putionew.R

class DiskUsageDrawable(context: Context) : GradientDrawable() {

  private val usedPaint = Paint().apply {
    color = ContextCompat.getColor(context, R.color.putio_accent_dark)
  }
  private val usedRect = Rect()

  var usedFraction = 0f
    set(value) {
      field = value
      invalidateSelf()
    }

  init {
    shape = GradientDrawable.RECTANGLE
    color = ContextCompat.getColorStateList(context, R.color.putio_accent)
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    canvas.drawRect(
        0f,
        0f,
        usedFraction * bounds.right,
        bounds.bottom.toFloat(),
        usedPaint
    )
  }

}
