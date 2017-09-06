package com.stevenschoen.putionew.transfers.add

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.TextView
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject

class FromFileFragment : BaseFragment(R.id.addtransfer_file_destination_holder) {

    var callbacks: Callbacks? = null

    val torrentUri = BehaviorSubject.createDefault(Uri.EMPTY)
    var didFirstRequest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_TORRENT_URI)) {
                didFirstRequest = true
                torrentUri.onNext(savedInstanceState.getParcelable<Uri>(STATE_TORRENT_URI))
            }
            if (savedInstanceState.containsKey(STATE_DID_FIRST_REQUEST)) {
                didFirstRequest = savedInstanceState.getBoolean(STATE_DID_FIRST_REQUEST)
            }
        } else {
            if (arguments != null && arguments.containsKey(EXTRA_PRECHOSEN_TORRENT_URI)) {
                didFirstRequest = true
                torrentUri.onNext(arguments.getParcelable<Uri>(EXTRA_PRECHOSEN_TORRENT_URI) as Uri)
            }
        }

        if (torrentUri.value == null && !didFirstRequest) {
            requestChooseFile()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.addtransfer_file, container, false)

        val fileView = view.findViewById<TextView>(R.id.addtransfer_file_file)
        fileView.setOnClickListener {
            requestChooseFile()
        }

        val clearFileView = view.findViewById<View>(R.id.addtransfer_file_clear)
        clearFileView.setOnClickListener {
            torrentUri.onNext(Uri.EMPTY)
        }

        val notATorrentView = view.findViewById<View>(R.id.addtransfer_file_notatorrent)

        val addView = view.findViewById<View>(R.id.addtransfer_file_add)
        addView.setOnClickListener {
            callbacks?.onFileSelected(torrentUri.value!!)
        }

        val cancelView = view.findViewById<View>(R.id.addtransfer_file_cancel)
        cancelView.setOnClickListener {
            dismiss()
        }

        torrentUri.observeOn(AndroidSchedulers.mainThread())
                .subscribe { newTorrentUri ->
                    if (newTorrentUri != Uri.EMPTY) {
                        fileView.text = PutioUtils.getNameFromUri(context, newTorrentUri)
                        clearFileView.visibility = View.VISIBLE
                        if (isTorrent(newTorrentUri)) {
                            notATorrentView.visibility = View.GONE
                            addView.isEnabled = true
                        } else {
                            notATorrentView.visibility = View.VISIBLE
                            addView.isEnabled = false
                        }
                    } else {
                        fileView.text = getString(R.string.choose_a_file)
                        clearFileView.visibility = View.GONE
                        notATorrentView.visibility = View.GONE
                        addView.isEnabled = false
                    }
                }

        return view
    }

    fun requestChooseFile() {
        didFirstRequest = true
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*" // application/x-bittorrent isn't working for some reason
            addCategory(Intent.CATEGORY_OPENABLE)
        }, REQUEST_CHOOSE_FILE)
    }

    fun isTorrent(uri: Uri): Boolean {
        var mimetype = context.contentResolver.getType(uri)
        if (mimetype == null) {
            mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.path))
        }
        return (mimetype != null && mimetype == "application/x-bittorrent"
                || PutioUtils.getNameFromUri(context, uri).endsWith("torrent"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CHOOSE_FILE -> {
                data?.data?.let {
                    torrentUri.onNext(it)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setTitle(R.string.add_transfer)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        torrentUri.value?.let {
            outState.putParcelable(STATE_TORRENT_URI, it)
        }
        outState.putBoolean(STATE_DID_FIRST_REQUEST, didFirstRequest)
    }

    interface Callbacks {
        fun onFileSelected(torrentUri: Uri)
    }

    companion object {
        val STATE_TORRENT_URI = "uri"
        val STATE_DID_FIRST_REQUEST = "did_first"

        val EXTRA_PRECHOSEN_TORRENT_URI = "uri"

        val REQUEST_CHOOSE_FILE = 1

        fun newInstance(context: Context, preChosenTorrentUri: Uri?): FromFileFragment {
            val args = Bundle()
            preChosenTorrentUri?.let { args.putParcelable(EXTRA_PRECHOSEN_TORRENT_URI, it) }

            return Fragment.instantiate(context, FromFileFragment::class.java.name, args) as FromFileFragment
        }
    }
}