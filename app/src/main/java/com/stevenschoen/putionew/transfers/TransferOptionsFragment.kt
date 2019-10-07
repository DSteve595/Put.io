package com.stevenschoen.putionew.transfers

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.stevenschoen.putionew.PutioBottomSheetDialogFragment
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.transfers.PutioTransfer

class TransferOptionsFragment : PutioBottomSheetDialogFragment() {

  companion object {
    const val EXTRA_TRANSFER = "transfer"

    fun newInstance(context: Context, transfer: PutioTransfer): TransferOptionsFragment {
      val args = Bundle()
      args.putParcelable(EXTRA_TRANSFER, transfer)

      return Fragment.instantiate(context, TransferOptionsFragment::class.java.name, args) as TransferOptionsFragment
    }
  }

  val transfer by lazy { arguments!!.getParcelable<PutioTransfer>(EXTRA_TRANSFER)!! }

  var callbacks: Callbacks? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    val view = inflater.inflate(R.layout.transfer_options, container, false)

    val nameView = view.findViewById<TextView>(R.id.transfer_options_name)
    nameView.text = transfer.name

    val retryView = view.findViewById<View>(R.id.transfer_options_retry)
    retryView.setOnClickListener {
      callbacks?.onRetrySelected()
    }

    if (transfer.status == "ERROR") {
      retryView.visibility = View.VISIBLE
      retryView.isEnabled = true
    } else {
      retryView.visibility = View.GONE
      retryView.isEnabled = false
    }

    val removeView = view.findViewById<View>(R.id.transfer_options_remove)
    removeView.setOnClickListener {
      callbacks?.onRemoveSelected()
    }

    return view
  }

  interface Callbacks {
    fun onRetrySelected()
    fun onRemoveSelected()
  }
}
