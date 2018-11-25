package com.stevenschoen.putionew

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class PutioBottomSheetDialogFragment : BottomSheetDialogFragment() {

  open val isAutoExpanding: Boolean = true
  open val showNavBarColor: Boolean = true

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = if (showNavBarColor) {
      object : BottomSheetDialog(context!!, theme) {

        override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)

          window?.apply {
            addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
            )
            if (showNavBarColor) {
              val container = peekDecorView().findViewById<ViewGroup>(com.google.android.material.R.id.container)!!
              container.fitsSystemWindows = false
              val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!
              bottomSheet.setOnApplyWindowInsetsListener { v, insets ->
                v.updatePadding(bottom = insets.systemWindowInsetBottom)
                insets.consumeSystemWindowInsets()
              }
            }
          }
        }
      }
    } else {
      super.onCreateDialog(savedInstanceState) as BottomSheetDialog
    }

    dialog.setOnShowListener {
      val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!
      if (isAutoExpanding) {
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
      }
    }

    return dialog
  }

}
