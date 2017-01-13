package com.stevenschoen.putionew.tv

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.UIUtils

class HorizontalStackLayout : ViewGroup {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    val layerOffset by lazy { resources.getDimensionPixelSize(R.dimen.tv_layer_offset) }
    val layerElevation by lazy { resources.getDimension(R.dimen.tv_layer_elevation) }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val childLeft = paddingLeft;
        val childTop = paddingTop;
        val childRight = measuredWidth - paddingRight;
        val childBottom = measuredHeight - paddingBottom;
        val childWidth = childRight - childLeft;
        val childHeight = childBottom - childTop;

        for (i in 0..childCount - 1) {
            val child = getChildAt(i)
            val offset = (i * layerOffset)
            child.measure(MeasureSpec.makeMeasureSpec(childWidth - offset, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
            child.layout(childLeft + offset, childTop, childRight, childBottom)
            if (UIUtils.hasLollipop()) {
                child.elevation = (i * layerElevation)
            }
        }
    }

    // Hacky, but without it sometimes the d-pad can go to the wrong page and become stuck
    override fun focusSearch(focused: View?, direction: Int): View? {
        return null
    }
}