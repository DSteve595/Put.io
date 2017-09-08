package com.stevenschoen.putionew.transfers.add

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.stevenschoen.putionew.*
import com.stevenschoen.putionew.model.PutioUploadInterface
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.transfers.TransfersActivity
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

class AddTransferActivity : AppCompatActivity() {

    companion object {
        const val ACTION_ADDTRANSFER_LINK = "com.stevenschoen.putionew.ADDTRANSFER_LINK"
        const val ACTION_ADDTRANSFER_FILE = "com.stevenschoen.putionew.ADDTRANSFER_FILE"

        const val EXTRA_PRECHOSEN_DESTINATION_FOLDER = "init_dest_folder"

        private const val FRAGTAG_ADDTRANSFER_PICKTYPE = "add_type"
        private const val FRAGTAG_ADDTRANSFER_FILE = "file"
        private const val FRAGTAG_ADDTRANSFER_LINK = "link"
    }

    var prechosenDestinationFolder: PutioFile? = null

    val hasPrechosenDestinationFolder: Boolean
        get() = (intent.extras?.containsKey(EXTRA_PRECHOSEN_DESTINATION_FOLDER)) ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PutioApplication.get(this).isLoggedIn) {
            startActivity(Intent(this, PutioActivity::class.java))
            finish()
            return
        }

        if (intent.hasExtra(EXTRA_PRECHOSEN_DESTINATION_FOLDER)) {
            prechosenDestinationFolder = intent.extras.getParcelable(EXTRA_PRECHOSEN_DESTINATION_FOLDER)
        }

        if (intent != null) {
            when (intent.action) {
                Intent.ACTION_VIEW -> when (intent.scheme) {
                    "http", "https", "magnet" -> {
                        showLinkFragmentIfNotShowing(intent.dataString)
                    }
                    "file", "content" -> {
                        showFileFragmentIfNotShowing(intent.data)
                    }
                }
                ACTION_ADDTRANSFER_LINK -> showLinkFragmentIfNotShowing()
                ACTION_ADDTRANSFER_FILE -> showFileFragmentIfNotShowing()
                else -> showPickTypeFragmentIfNotShowing()
            }
        } else {
            showPickTypeFragmentIfNotShowing()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                finish()
            }
        }
    }

    fun showPickTypeFragmentIfNotShowing() {
        if (supportFragmentManager.findFragmentByTag(FRAGTAG_ADDTRANSFER_PICKTYPE) == null) {
            val pickTypeFragment = Fragment.instantiate(this, PickTypeFragment::class.java.name) as PickTypeFragment
            val transaction = supportFragmentManager.beginTransaction()
            transaction.addToBackStack("pick")
            pickTypeFragment.show(transaction, FRAGTAG_ADDTRANSFER_PICKTYPE)
        }
    }

    fun showLinkFragmentIfNotShowing(link: String? = null) {
        if (supportFragmentManager.findFragmentByTag(FRAGTAG_ADDTRANSFER_LINK) == null) {
            val fileFragment = FromUrlFragment.newInstance(this@AddTransferActivity, link)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.addToBackStack("url")
            fileFragment.show(transaction, FRAGTAG_ADDTRANSFER_LINK)
        }
    }

    fun showFileFragmentIfNotShowing(torrentUri: Uri? = null) {
        if (supportFragmentManager.findFragmentByTag(FRAGTAG_ADDTRANSFER_FILE) == null) {
            val fileFragment = FromFileFragment.newInstance(this@AddTransferActivity, torrentUri)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.addToBackStack("file")
            fileFragment.show(transaction, FRAGTAG_ADDTRANSFER_FILE)
        }
    }

    private fun uploadTransferUrl(link: String, parentId: Long, extract: Boolean) {
        val notif = UploadNotif()
        notif.start();

        val restInterface = PutioApplication.get(this).putioUtils.restInterface
        restInterface.addTransferUrl(link, extract, parentId).subscribe(
                { response ->
                    notif.succeeded()
                },
                { error ->
                    notif.failed()
                    error.printStackTrace()
                    if (!isDestroyed) {
                        runOnUiThread {
                            Toast.makeText(this, "Error: " + error.message, Toast.LENGTH_LONG).show()
                        }
                    }
                })
    }

    private fun uploadTransferFile(torrentUri: Uri, parentId: Long) {
        val notif = UploadNotif()
        notif.start()

        val uploadInterface = PutioApplication.get(this).putioUtils
                .makePutioRestInterface(PutioUtils.uploadBaseUrl)
                .create(PutioUploadInterface::class.java)

        val file: File
        if (torrentUri.scheme == ContentResolver.SCHEME_CONTENT) {
            file = File(cacheDir, "upload.torrent")
            val cr = contentResolver
            try {
                FileUtils.copyInputStreamToFile(cr.openInputStream(torrentUri)!!, file)
            } catch (e: IOException) {
                e.printStackTrace()
                notif.failed()
                return
            }

        } else {
            file = File(torrentUri.path)
        }
        val requestBody = RequestBody.create(MediaType.parse("application/x-bittorrent"), file)
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
        uploadInterface.uploadFile(filePart, parentId).subscribe({
            notif.succeeded()
        }, { error ->
            notif.failed()
            PutioUtils.getRxJavaThrowable(error).printStackTrace()
            if (!isDestroyed) {
                runOnUiThread {
                    Toast.makeText(this, "Error: " + error.message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    fun getDestinationFolder(fileOrUrlFragment: BaseFragment?): PutioFile {
        prechosenDestinationFolder?.let { return it }
        return fileOrUrlFragment!!.destinationPickerFragment!!.destination
    }

    inner class UploadNotif {
        private val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun start() {
            val notifBuilder = NotificationCompat.Builder(this@AddTransferActivity, NOTIFICATION_CHANNEL_ID_TRANSFERS)
            notifBuilder
                    .setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentTitle(getString(R.string.notification_title_uploading_torrent))
                    .setSmallIcon(R.drawable.ic_notificon_transfer)
                    .setTicker(getString(R.string.notification_ticker_uploading_torrent))
                    .setProgress(1, 0, true)
            var notif = notifBuilder.build()
            notif.ledARGB = Color.parseColor("#FFFFFF00")
            try {
                notifManager.notify(1, notif)
            } catch (e: IllegalArgumentException) {
                notifBuilder
                        .setContentIntent(PendingIntent.getActivity(
                                this@AddTransferActivity, 0, Intent(this@AddTransferActivity, PutioActivity::class.java), 0))
                notif = notifBuilder.build()
                notif.ledARGB = Color.parseColor("#FFFFFF00")
                notifManager.notify(1, notif)
            }

        }

        fun succeeded() {
            val notifBuilder = NotificationCompat.Builder(this@AddTransferActivity, NOTIFICATION_CHANNEL_ID_TRANSFERS)
            notifBuilder
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentTitle(getString(R.string.notification_title_uploaded_torrent))
                    .setContentText(getString(R.string.notification_body_uploaded_torrent))
                    .setSmallIcon(R.drawable.ic_notificon_transfer)
            val viewTransfersIntent = Intent(this@AddTransferActivity, TransfersActivity::class.java)
            viewTransfersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            notifBuilder
                    .setContentIntent(PendingIntent.getActivity(
                            this@AddTransferActivity, 0, viewTransfersIntent, PendingIntent.FLAG_CANCEL_CURRENT))
            //				notifBuilder.addAction(R.drawable.ic_notif_watch, "Watch", null) TODO
            notifBuilder
                    .setTicker(getString(R.string.notification_ticker_uploaded_torrent))
                    .setProgress(0, 0, false)
            val notif = notifBuilder.build()
            notif.ledARGB = Color.parseColor("#FFFFFF00")
            notifManager.notify(1, notif)
        }

        fun failed() {
            val notifBuilder = NotificationCompat.Builder(this@AddTransferActivity, NOTIFICATION_CHANNEL_ID_TRANSFERS)
            notifBuilder
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentTitle(getString(R.string.notification_title_error))
                    .setContentText(getString(R.string.notification_body_error))
                    .setSmallIcon(R.drawable.ic_notificon_transfer)
            val retryNotifIntent = PendingIntent.getActivity(
                    this@AddTransferActivity, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            notifBuilder
                    .addAction(
                            R.drawable.ic_notif_retry,
                            getString(R.string.notification_button_retry),
                            retryNotifIntent)
                    .setContentIntent(retryNotifIntent)
                    .setTicker(getString(R.string.notification_ticker_error))
            val notif = notifBuilder.build()
            notif.ledARGB = Color.parseColor("#FFFFFF00")
            notifManager.notify(1, notif)
        }
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        when (fragment.tag) {
            FRAGTAG_ADDTRANSFER_PICKTYPE -> {
                fragment as PickTypeFragment
                fragment.callbacks = object : PickTypeFragment.Callbacks {
                    override fun onLinkSelected() {
                        showLinkFragmentIfNotShowing()
                    }
                    override fun onFileSelected() {
                        showFileFragmentIfNotShowing()
                    }
                }
            }
            FRAGTAG_ADDTRANSFER_LINK -> {
                fragment as FromUrlFragment
                fragment.callbacks = object : FromUrlFragment.Callbacks {
                    override fun onLinkSelected(link: String, extract: Boolean) {
                        uploadTransferUrl(link, getDestinationFolder(fragment).id, extract)
                        finish()
                    }
                }
            }
            FRAGTAG_ADDTRANSFER_FILE -> {
                fragment as FromFileFragment
                fragment.callbacks = object : FromFileFragment.Callbacks {
                    override fun onFileSelected(torrentUri: Uri) {
                        uploadTransferFile(torrentUri, getDestinationFolder(fragment).id)
                        finish()
                    }
                }
            }
        }
    }
}