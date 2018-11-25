package com.stevenschoen.putionew.files

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile

class CreateFolderFragment : DialogFragment() {

  companion object {
    const val EXTRA_PARENT_FOLDER = "parent_folder"

    fun newInstance(context: Context, parentFolder: PutioFile): CreateFolderFragment {
      val args = Bundle()
      args.putParcelable(EXTRA_PARENT_FOLDER, parentFolder)
      return Fragment.instantiate(context, CreateFolderFragment::class.java.name, args) as CreateFolderFragment
    }
  }

  val parentFolder by lazy { arguments!!.getParcelable<PutioFile>(EXTRA_PARENT_FOLDER) }

  var callbacks: Callbacks? = null

  lateinit var nameView: EditText

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = AlertDialog.Builder(context!!)
        .setTitle(R.string.create_folder)
        .setView(R.layout.create_folder_dialog)
        .setPositiveButton(R.string.create) { _, _ ->
          callbacks?.onNameEntered(nameView.text.toString())
        }
        .setNegativeButton(R.string.cancel, null)
        .show()

    val nameInputLayoutView = dialog.findViewById<TextInputLayout>(R.id.new_folder_name_input)!!
    nameView = nameInputLayoutView.editText!!

    return dialog
  }

  interface Callbacks {
    fun onNameEntered(folderName: String)
  }
}
