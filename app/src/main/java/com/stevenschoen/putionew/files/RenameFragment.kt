package com.stevenschoen.putionew.files

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.widget.TextView
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.trello.rxlifecycle.components.support.RxAppCompatDialogFragment

class RenameFragment : RxAppCompatDialogFragment() {

    companion object {
        val EXTRA_FILE = "file"

        val STATE_NAME = "name"

        fun newInstance(context: Context, file: PutioFile): RenameFragment {
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            return Fragment.instantiate(context, RenameFragment::class.java.name, args) as RenameFragment
        }
    }

    val file by lazy { arguments.getParcelable<PutioFile>(EXTRA_FILE) }

    var callbacks: Callbacks? = null

    lateinit var nameView: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.renametitle)
                .setView(R.layout.rename_dialog)
                .setPositiveButton(R.string.rename) { dialogInterface, which ->
                    callbacks?.onRenamed(nameView.text.toString())
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

        nameView = dialog.findViewById(R.id.rename_name) as TextView
        if (savedInstanceState == null) {
            nameView.text = file.name
        }

        val undoView = dialog.findViewById(R.id.rename_undo)!!
        undoView.setOnClickListener {
            nameView.text = file.name
        }

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_NAME, nameView.text.toString())
    }

    interface Callbacks {
        fun onRenamed(newName: String)
    }
}