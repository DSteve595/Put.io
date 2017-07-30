package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.trello.rxlifecycle2.components.support.RxFragment

class NewFileDetailsFragment : RxFragment() {

    companion object {
        val EXTRA_FILE = "file"

        fun newInstance(context: Context, file: PutioFile): NewFileDetailsFragment {
            if (file.isFolder || file.isMedia) {
                throw IllegalStateException("FileDetailsFragment created the wrong kind of file: ${file.name} (ID ${file.id}), type ${file.contentType}")
            }
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            return Fragment.instantiate(context, NewFileDetailsFragment::class.java.name, args) as NewFileDetailsFragment
        }
    }

    var onBackPressed: (() -> Any)? = null

    val file by lazy { arguments.getParcelable<PutioFile>(EXTRA_FILE) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.newfiledetails, container, false).apply {
            val titleView: TextView = findViewById(R.id.newfiledetails_title)
            titleView.text = file.name

            val backView: View = findViewById(R.id.newfiledetails_back)
            backView.setOnClickListener {

            }

            val accessedView: TextView = findViewById(R.id.newfiledetails_accessed)
            if (file.isAccessed) {
                val accessed = PutioUtils.parseIsoTime(activity, file.firstAccessedAt)
                accessedView.text = getString(R.string.accessed_on_x_at_x, accessed[0], accessed[1])
            } else {
                accessedView.text = getString(R.string.never_accessed)
            }

            val createdView: TextView = findViewById(R.id.newfiledetails_created)
            val created = PutioUtils.parseIsoTime(activity, file.createdAt)
            createdView.text = getString(R.string.created_on_x_at_x, created[0], created[1])

            val filesizeView: TextView = findViewById(R.id.newfiledetails_filesize)
            filesizeView.text = PutioUtils.humanReadableByteCount(file.size!!, false)

            val crcView: TextView = findViewById(R.id.newfiledetails_crc32)
            crcView.text = file.crc32

            val actionView: View = findViewById(R.id.newfiledetails_action)
            // Download or Open
            actionView.setOnClickListener {

            }

            val moreView: View = findViewById(R.id.newfiledetails_more)
            val morePopup = PopupMenu(context, moreView).apply {
                menu.add(R.string.delete).setOnMenuItemClickListener {
                    // Delete
                    true
                }
            }
            moreView.setOnTouchListener(morePopup.dragToOpenListener)
            moreView.setOnClickListener {
                morePopup.show()
            }
        }
    }
}