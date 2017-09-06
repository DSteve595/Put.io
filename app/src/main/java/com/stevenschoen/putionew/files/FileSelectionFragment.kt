package com.stevenschoen.putionew.files

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.stevenschoen.putionew.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject

class FileSelectionFragment : Fragment() {

    companion object {
        val STATE_AMOUNT_SELECTED = "amt_sel"
    }

    var callbacks: Callbacks? = null

    val amountSelected = BehaviorSubject.create<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.file_selection, container, false)

        val cancelView = view.findViewById<View>(R.id.file_selection_cancel)
        cancelView.setOnClickListener {
            callbacks?.onCancel()
        }

        val downloadView = view.findViewById<View>(R.id.file_selection_download)
        downloadView.setOnClickListener { callbacks?.onDownloadSelected() }
        downloadView.setOnLongClickListener {
            Toast.makeText(context, R.string.download, Toast.LENGTH_SHORT).show()
            true
        }

        val copyLinkView = view.findViewById<View>(R.id.file_selection_copylink)
        copyLinkView.setOnClickListener { callbacks?.onCopyLinkSelected() }
        copyLinkView.setOnLongClickListener {
            Toast.makeText(context, R.string.copy_dl_link, Toast.LENGTH_SHORT).show()
            true
        }

        val deleteView = view.findViewById<View>(R.id.file_selection_delete)
        deleteView.setOnClickListener { callbacks?.onDeleteSelected() }
        deleteView.setOnLongClickListener {
            Toast.makeText(context, R.string.delete, Toast.LENGTH_SHORT).show()
            true
        }

        val idRename = 1
        val idMove = 2

        val moreView = view.findViewById<View>(R.id.file_selection_more)
        moreView.setOnClickListener {
            val popup = PopupMenu(context, moreView)
            if (amountSelected.value == 1) popup.menu.add(0, idRename, 0, R.string.rename)
            popup.menu.add(0, idMove, 0, R.string.move)
            popup.setOnMenuItemClickListener { menuItem ->
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
            popup.show()
        }

        val titleView = view.findViewById<TextView>(R.id.file_selection_title)

        fun updateAmount(amount: Int) {
            titleView.text = getString(R.string.x_selected, amount)
        }

        if (savedInstanceState != null) {
            updateAmount(savedInstanceState.getInt(STATE_AMOUNT_SELECTED))
        }
        amountSelected
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateAmount,
                        { error ->
                            error.printStackTrace()
                            Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
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