package com.stevenschoen.putionew.transfers.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stevenschoen.putionew.AutoExpandingBottomSheetDialogFragment
import com.stevenschoen.putionew.R

class PickTypeFragment : AutoExpandingBottomSheetDialogFragment() {

  var callbacks: Callbacks? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.addtransfer_picktype, container, false)

    val linkView = view.findViewById<View>(R.id.addtransfer_picktype_link)
    linkView.setOnClickListener {
      callbacks?.onLinkSelected()
    }

    val fileView = view.findViewById<View>(R.id.addtransfer_picktype_file)
    fileView.setOnClickListener {
      callbacks?.onFileSelected()
    }

    return view
  }

  interface Callbacks {
    fun onLinkSelected()
    fun onFileSelected()
  }
}
