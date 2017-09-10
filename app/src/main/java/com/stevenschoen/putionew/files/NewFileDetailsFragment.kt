package com.stevenschoen.putionew.files

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v4.widget.TextViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp
import com.trello.rxlifecycle2.components.support.RxFragment
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class NewFileDetailsFragment : RxFragment() {

    companion object {
        val EXTRA_FILE = "file"

        fun newInstance(context: Context, file: PutioFile): NewFileDetailsFragment {
            if (file.isFolder || file.isMedia) {
                throw IllegalStateException("FileDetailsFragment created for the wrong kind of file:" +
                        "${file.name} (ID ${file.id}), type ${file.contentType}")
            }
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            return Fragment.instantiate(context, NewFileDetailsFragment::class.java.name, args) as NewFileDetailsFragment
        }
    }

    var onBackPressed: (() -> Any)? = null

    val file by lazy { arguments.getParcelable<PutioFile>(EXTRA_FILE)!! }

    lateinit var loader: NewFileDetailsLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loader = NewFileDetailsLoader.get(loaderManager, context, file)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.newfiledetails, container, false).apply {
            val titleView: TextView = findViewById(R.id.newfiledetails_title)
            titleView.text = file.name

            val backView: View = findViewById(R.id.newfiledetails_back)
            backView.setOnClickListener {
                onBackPressed?.invoke()
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

            val actionView: Button = findViewById(R.id.newfiledetails_action)
            fun setActionDrawable(@DrawableRes drawableRes: Int) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(actionView,
                        drawableRes, 0, 0, 0)
            }
            fun showActionDownload() {
                setActionDrawable(R.drawable.ic_filedetails_download)
                actionView.text = getString(R.string.download)
                actionView.setOnClickListener {
                    putioApp.putioUtils!!.downloadFiles(activity, PutioUtils.ACTION_NOTHING, file)
                }
            }
            fun showActionOpen() {
                setActionDrawable(R.drawable.ic_filedetails_open)
                actionView.text = getString(R.string.open)
                actionView.setOnClickListener {
                    Single.fromCallable {
                        putioApp.fileDownloadDatabase.fileDownloadsDao().getByFileIdSynchronous(file.id)!!.uri
                    }.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .map { Uri.parse(it) }
                            .subscribe({ fileUri ->
                                startActivity(Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(fileUri, file.contentType)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                            }, { error ->
                                Toast.makeText(context, "error opening", Toast.LENGTH_SHORT).show()
                                PutioUtils.getRxJavaThrowable(error).printStackTrace()
                            })
                }
            }
            putioApp.fileDownloadDatabase.fileDownloadsDao().getByFileId(file.id)
                    .bindToLifecycle(this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map { it.status }
                    .startWith(FileDownload.Status.NotDownloaded)
                    .subscribe {
                        when (it!!) {
                            FileDownload.Status.Downloaded -> {
                                showActionOpen()
                            }
                            FileDownload.Status.InProgress -> showActionDownload()
                            FileDownload.Status.NotDownloaded -> showActionDownload()
                        }
                    }

            val moreView: View = findViewById(R.id.newfiledetails_more)
            val morePopup = PopupMenu(context, moreView).apply {
                menu.add(R.string.delete).setOnMenuItemClickListener {
                    val fileDownloads = putioApp.fileDownloadDatabase.fileDownloadsDao()
                    Single.fromCallable {
                        fileDownloads.getByFileIdSynchronous(file.id)
                    }.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                val downloadManager = context.getSystemService(Activity.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.remove(it!!.downloadId!!)
                                AsyncTask.execute {
                                    markFileNotDownloaded(context, it)
                                }
                            }, { error ->
                                Toast.makeText(context, "error deleting", Toast.LENGTH_SHORT).show()
                                PutioUtils.getRxJavaThrowable(error).printStackTrace()
                            })
                    true
                }
            }
            moreView.setOnTouchListener(morePopup.dragToOpenListener)
            moreView.setOnClickListener {
                morePopup.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        loader.checkDownload()
    }
}