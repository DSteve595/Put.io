package com.stevenschoen.putionew.files

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.stevenschoen.putionew.AutoExpandingBottomSheetDialogFragment
import com.stevenschoen.putionew.R

class DownloadIndividualOrZipFragment : AutoExpandingBottomSheetDialogFragment() {

  var callbacks: Callbacks? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.download_individualorzip, container, false).apply {
      val individualView = findViewById<View>(R.id.download_individualorzip_individual)
      individualView.setOnClickListener {
        callbacks?.onIndividualSelected()
        dismiss()
      }

      val zipView = findViewById<View>(R.id.download_individualorzip_zip)
      zipView.setOnClickListener {
        callbacks?.onZipSelected()
        dismiss()
      }

      val toolbarView = findViewById<Toolbar>(R.id.download_individualorzip_toolbar)
      toolbarView.setNavigationOnClickListener {
        dialog.cancel()
      }
    }
  }

  override fun onCancel(dialog: DialogInterface) {
    callbacks?.onCanceled()
    super.onCancel(dialog)
  }

  interface Callbacks {
    fun onIndividualSelected()
    fun onZipSelected()
    fun onCanceled()
  }
}
