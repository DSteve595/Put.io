package com.stevenschoen.putionew.transfers.add

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment

abstract class BaseFragment(val destinationPickerContainerId: Int) : AppCompatDialogFragment() {

  val destinationPickerFragment: DestinationPickerFragment?
    get() = childFragmentManager.findFragmentByTag(FRAGTAG_DESTINATION_PICKER) as DestinationPickerFragment?

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val addTransferActivity = activity as AddTransferActivity
    if (!addTransferActivity.hasPrechosenDestinationFolder) {
      if (childFragmentManager.findFragmentByTag(FRAGTAG_DESTINATION_PICKER) == null) {
        val fragment = Fragment.instantiate(addTransferActivity, DestinationPickerFragment::class.java.name)
        childFragmentManager.beginTransaction()
            .add(destinationPickerContainerId, fragment, FRAGTAG_DESTINATION_PICKER)
            .commitNow()
      }
    } else {
      val holderView = view.findViewById<View>(destinationPickerContainerId)
      holderView.visibility = View.GONE
    }
  }

  companion object {
    const val FRAGTAG_DESTINATION_PICKER = "dest_picker"
  }
}
