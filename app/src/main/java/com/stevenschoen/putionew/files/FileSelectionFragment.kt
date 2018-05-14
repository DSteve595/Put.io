package com.stevenschoen.putionew.files

import android.os.Bundle
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.TooltipCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.stevenschoen.putionew.R
import com.trello.rxlifecycle2.components.support.RxFragment
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject

class FileSelectionFragment : RxFragment() {

  companion object {
    const val STATE_AMOUNT_SELECTED = "amt_sel"
  }

  var callbacks: Callbacks? = null
  var onSetupLayout: (() -> Unit)? = null
    set(value) {
      if (view?.isAttachedToWindow == true) {
        value?.invoke()
        field = null
      } else {
        field = value
      }
    }

  val amountSelected = BehaviorSubject.create<Int>()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.file_selection, container, false)

    val cancelView = view.findViewById<View>(R.id.file_selection_cancel)
    cancelView.setOnClickListener {
      callbacks?.onCancel()
    }
    TooltipCompat.setTooltipText(cancelView, getString(R.string.cancel))

    val downloadView = view.findViewById<View>(R.id.file_selection_download)
    downloadView.setOnClickListener { callbacks?.onDownloadSelected() }
    TooltipCompat.setTooltipText(downloadView, getString(R.string.download))

    val copyLinkView = view.findViewById<View>(R.id.file_selection_copylink)
    copyLinkView.setOnClickListener { callbacks?.onCopyLinkSelected() }
    TooltipCompat.setTooltipText(copyLinkView, getString(R.string.copy_dl_link))

    val deleteView = view.findViewById<View>(R.id.file_selection_delete)
    deleteView.setOnClickListener { callbacks?.onDeleteSelected() }
    TooltipCompat.setTooltipText(deleteView, getString(R.string.delete))

    val idRename = 1
    val idMove = 2

    val moreView = view.findViewById<View>(R.id.file_selection_more)
    val morePopup = PopupMenu(context!!, moreView)
    val rename = morePopup.menu.add(0, idRename, 0, R.string.rename)
    amountSelected
        .bindToLifecycle(this)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          if (it == 1) {
            rename.isVisible = true
            rename.isEnabled = true
          } else {
            rename.isVisible = false
            rename.isEnabled = false
          }
        }
    morePopup.menu.add(0, idMove, 0, R.string.move)
    morePopup.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        idRename -> {
          callbacks?.onRenameSelected();
          return@setOnMenuItemClickListener true
        }
        idMove -> {
          callbacks?.onMoveSelected()
          return@setOnMenuItemClickListener true
        }
      }
      false
    }
    moreView.setOnTouchListener(morePopup.dragToOpenListener)
    moreView.setOnClickListener {
      morePopup.show()
    }

    val titleView = view.findViewById<TextView>(R.id.file_selection_title)

    fun updateAmount(amount: Int) {
      titleView.text = getString(R.string.x_selected, amount)
    }

    if (savedInstanceState != null) {
      updateAmount(savedInstanceState.getInt(STATE_AMOUNT_SELECTED))
    }
    amountSelected
        .bindToLifecycle(this)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(::updateAmount)

    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View?) {
        onSetupLayout?.let {
          it.invoke()
          onSetupLayout = null
        }
      }

      override fun onViewDetachedFromWindow(v: View?) {}
    })

    return view
  }

  interface Callbacks {
    fun onCancel()
    fun onRenameSelected()
    fun onDownloadSelected()
    fun onCopyLinkSelected()
    fun onMoveSelected()
    fun onDeleteSelected()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putInt(STATE_AMOUNT_SELECTED, amountSelected.value)
  }
}
