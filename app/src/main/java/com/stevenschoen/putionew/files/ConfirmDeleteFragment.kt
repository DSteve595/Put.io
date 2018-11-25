package com.stevenschoen.putionew.files

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.stevenschoen.putionew.R

class ConfirmDeleteFragment : AppCompatDialogFragment() {

  companion object {
    const val EXTRA_AMOUNT = "amount"

    fun newInstance(context: Context, amount: Int): ConfirmDeleteFragment {
      val args = Bundle()
      args.putInt(EXTRA_AMOUNT, amount)
      return Fragment.instantiate(context, ConfirmDeleteFragment::class.java.name, args) as ConfirmDeleteFragment
    }
  }

  val amount by lazy { arguments!!.getInt(EXTRA_AMOUNT) }

  var callbacks: Callbacks? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(context!!)
        .setTitle(resources.getQuantityString(R.plurals.deletetitle, amount, amount))
        .setMessage(resources.getQuantityString(R.plurals.deletebody, amount, amount))
        .setPositiveButton(R.string.delete) { _, _ -> callbacks?.onDeleteSelected() }
        .setNegativeButton(R.string.cancel, null)
        .show()
  }

  interface Callbacks {
    fun onDeleteSelected()
  }
}
