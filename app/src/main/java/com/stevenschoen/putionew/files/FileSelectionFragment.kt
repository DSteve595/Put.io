package com.stevenschoen.putionew.files

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.stevenschoen.putionew.R
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject

class FileSelectionFragment : Fragment() {

    companion object {
        val STATE_AMOUNT_SELECTED = "amt_sel"
    }

    var callbacks: Callbacks? = null

    val amountSelected = BehaviorSubject.create<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.file_selection, container, false)

        val cancelView = view.findViewById(R.id.file_selection_cancel)
        cancelView.setOnClickListener {
            callbacks?.onCancel()
        }

        val renameView = view.findViewById(R.id.file_selection_rename)
        renameView.setOnClickListener { callbacks?. onRenameSelected()}
        val downloadView = view.findViewById(R.id.file_selection_download)
        downloadView.setOnClickListener { callbacks?.onDownloadSelected() }
        val copyLinkView = view.findViewById(R.id.file_selection_copylink)
        copyLinkView.setOnClickListener { callbacks?.onCopyLinkSelected() }
        val moveView = view.findViewById(R.id.file_selection_move)
        moveView.setOnClickListener { callbacks?.onMoveSelected() }
        val deleteView = view.findViewById(R.id.file_selection_delete)
        deleteView.setOnClickListener { callbacks?.onDeleteSelected() }

        val titleView = view.findViewById(R.id.file_selection_title) as TextView

        fun updateAmount(amount: Int) {
            titleView.text = getString(R.string.x_selected, amount)
            if (amount > 1) {
                renameView.visibility = View.GONE
            } else {
                renameView.visibility = View.VISIBLE
            }
        }

        if (savedInstanceState != null) {
            updateAmount(savedInstanceState.getInt(STATE_AMOUNT_SELECTED))
        }
        amountSelected
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateAmount)

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