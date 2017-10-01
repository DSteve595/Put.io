package com.stevenschoen.putionew.files

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.ImageViewCompat
import android.support.v4.widget.TextViewCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.stevenschoen.putionew.*
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.files.PutioMp4Status
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.components.support.RxFragment
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class FileDetailsFragment : RxFragment() {

    companion object {
        val EXTRA_FILE = "file"

        const val REQUEST_DOWNLOAD = 1
        const val REQUEST_DOWNLOAD_MP4 = 2

        fun newInstance(context: Context, file: PutioFile): FileDetailsFragment {
            if (file.isFolder) {
                throw IllegalStateException("FileDetailsFragment created for the wrong kind of file: " +
                        "${file.name} (ID ${file.id}), type ${file.contentType}")
            }
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            return Fragment.instantiate(context, FileDetailsFragment::class.java.name, args) as FileDetailsFragment
        }
    }

    val fileDownloads by lazy { putioApp.fileDownloadDatabase.fileDownloadsDao() }
    val fileDownloadHelper by lazy { FileDownloadHelper(context) }

    var onBackPressed: (() -> Any)? = null
    var castCallbacks: PutioApplication.CastCallbacks? = null

    val file by lazy { arguments.getParcelable<PutioFile>(EXTRA_FILE)!! }
    val showMp4Options by lazy { file.isVideo && !file.isMp4 }

    lateinit var loader: FileDetailsLoader
    var mp4Loader: Mp4StatusLoader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loader = FileDetailsLoader.get(loaderManager, context, file)
        if (showMp4Options) mp4Loader = Mp4StatusLoader.get(loaderManager, context, file)
                .apply { refreshOnce() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.filedetails, container, false).apply {
            val useVideoTitleBackground = file.isVideo && !file.screenshot.isNullOrBlank()

            val titleView: TextView = findViewById(R.id.filedetails_title)
            titleView.text = file.name
            var titleBackgroundColorAnimator: Animator? = null
            var lastTitleBackgroundColor: Int? = null
            fun setTitleBackgroundColor(color: Int, animate: Boolean) {
                if (color == lastTitleBackgroundColor) return
                if (animate && Build.VERSION.SDK_INT >= 21) {
                    val startingColor = lastTitleBackgroundColor ?: color
                    titleBackgroundColorAnimator = ObjectAnimator.ofArgb(
                            titleView, "backgroundColor", startingColor, color).apply {
                        start()
                    }
                } else {
                    titleBackgroundColorAnimator?.cancel()
                    titleBackgroundColorAnimator = null
                    titleView.setBackgroundColor(color)
                }
                lastTitleBackgroundColor = color
            }

            val screenshotView: ImageView = findViewById(R.id.filedetails_screenshot)
            val screenshotBlurryView: ImageView = findViewById(R.id.filedetails_screenshot_blurry)
            val screenshotTopScrimView: View = findViewById(R.id.filedetails_screenshot_scrim_top)
            val screenshotTitleScrimView: View = findViewById(R.id.filedetails_screenshot_scrim_title)

            val fileGraphicView: View = findViewById(R.id.filedetails_graphic_file)

            val backView: ImageButton = findViewById(R.id.filedetails_back)
            backView.setOnClickListener {
                onBackPressed?.invoke()
            }

            titleView.setPadding(titleView.paddingLeft,
                    resources.getDimensionPixelSize(if (useVideoTitleBackground)
                        R.dimen.filedetails_title_top_padding_video
                    else
                        R.dimen.filedetails_title_top_padding_notvideo),
                    titleView.paddingRight, titleView.paddingBottom)
            if (useVideoTitleBackground) {
                ImageViewCompat.setImageTintList(backView,
                        ColorStateList.valueOf(Color.WHITE))
                titleView.setTextColor(Color.WHITE)
                titleView.background = null
                titleView.setShadowLayer(6f, 0f, 2f, Color.BLACK)
                Picasso.with(context)
                        .load(file.icon)
                        .transform(PutioUtils.BlurTransformation(context, 4f))
                        .into(screenshotBlurryView)
                val screenshotTarget = object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
                        val scrimColor = Color.parseColor("#80000000")
                        screenshotTopScrimView.background = ScrimUtil.makeCubicGradientScrimDrawable(
                                scrimColor, 6, Gravity.TOP)
                        screenshotTitleScrimView.background = ScrimUtil.makeCubicGradientScrimDrawable(
                                scrimColor, 8, Gravity.BOTTOM)

                        screenshotView.setImageBitmap(bitmap)
                        if (from == Picasso.LoadedFrom.NETWORK) {
                            screenshotView.alpha = 0f
                            screenshotView.animate().alpha(1f).withEndAction {
                                screenshotBlurryView.visibility = View.INVISIBLE
                            }
                            screenshotTopScrimView.alpha = 0f
                            screenshotTopScrimView.animate().alpha(1f)
                            screenshotTitleScrimView.alpha = 0f
                            screenshotTitleScrimView.animate().alpha(1f)
                        }
                    }
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) { }
                    override fun onBitmapFailed(errorDrawable: Drawable?) { }
                }
                Picasso.with(context)
                        .load(file.screenshot)
                        .into(screenshotTarget)
                lifecycle()
                        .filter { it == FragmentEvent.DESTROY_VIEW }
                        .first(FragmentEvent.DESTROY_VIEW)
                        .toCompletable()
                        .subscribe {
                            Picasso.with(context).cancelRequest(screenshotTarget)
                        }
                fileGraphicView.visibility = View.GONE
            } else {
                titleView.setBackgroundColor(ContextCompat.getColor(context, R.color.putio_filedetails_notdownloaded))
                screenshotView.visibility = View.GONE
                screenshotBlurryView.visibility = View.GONE
                screenshotTopScrimView.visibility = View.GONE
                screenshotTitleScrimView.visibility = View.GONE
            }

            val accessedView: TextView = findViewById(R.id.filedetails_accessed)
            if (file.isAccessed) {
                val accessed = PutioUtils.parseIsoTime(activity, file.firstAccessedAt)
                accessedView.text = getString(R.string.accessed_on_x_at_x, accessed[0], accessed[1])
            } else {
                accessedView.text = getString(R.string.never_accessed)
            }

            val createdView: TextView = findViewById(R.id.filedetails_created)
            val created = PutioUtils.parseIsoTime(activity, file.createdAt)
            createdView.text = getString(R.string.created_on_x_at_x, created[0], created[1])

            val filesizeView: TextView = findViewById(R.id.filedetails_filesize)
            filesizeView.text = PutioUtils.humanReadableByteCount(file.size!!, false)

            val crcView: TextView = findViewById(R.id.filedetails_crc32)
            crcView.text = file.crc32

            fun setActionDrawable(actionView: Button, drawable: Drawable?) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(actionView,
                        drawable, null, null, null)
            }
            fun setActionDrawable(actionView: Button, @DrawableRes drawableRes: Int) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(actionView,
                        drawableRes, 0, 0, 0)
            }
            val blankActionDrawable = ContextCompat.getDrawable(context, R.drawable.blank_24)

            val playActionView: Button = findViewById(R.id.filedetails_play)
            val playProgressBarView: View = findViewById(R.id.filedetails_play_progressbar)
            val playMoreView: View = findViewById(R.id.filedetails_play_more)
            val playMorePopup = PopupMenu(context, playMoreView)
            playMoreView.setOnTouchListener(playMorePopup.dragToOpenListener)
            playMoreView.setOnClickListener {
                playMorePopup.show()
            }
            val playOriginalItem = playMorePopup.menu.add(R.string.stream_original).setOnMenuItemClickListener {
                play(false)
                true
            }

            val downloadActionView: Button = findViewById(R.id.filedetails_download)
            val downloadProgressBarView: View = findViewById(R.id.filedetails_download_progressbar)
            val downloadMoreView: View = findViewById(R.id.filedetails_download_more)
            val downloadMorePopup = PopupMenu(context, downloadMoreView)
            downloadMoreView.setOnTouchListener(downloadMorePopup.dragToOpenListener)
            downloadMoreView.setOnClickListener {
                downloadMorePopup.show()
            }
            val downloadOriginalItem = downloadMorePopup.menu.add(R.string.download_original).setOnMenuItemClickListener {
                download(false)
                true
            }
            val sendItem = downloadMorePopup.menu.add(R.string.send).setOnMenuItemClickListener {
                Single.fromCallable {
                    fileDownloads.getByFileIdSynchronous(file.id)!!
                }
                        .bindToLifecycle(this@FileDetailsFragment)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            val uri = Uri.parse(it.uri)
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                                    .setType("application/octet-stream")
                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                    getString(R.string.send_x, file.name)))
                        }, { error ->
                            PutioUtils.getRxJavaThrowable(error).printStackTrace()
                            Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                        })
                true
            }
            val deleteItem = downloadMorePopup.menu.add(R.string.delete).setOnMenuItemClickListener {
                deleteDownload()
                true
            }
            val cancelItem = downloadMorePopup.menu.add(R.string.cancel).setOnMenuItemClickListener {
                deleteDownload()
                true
            }

            var lastDownloadStatus: FileDownload.Status? = null
            val fileDownload = fileDownloads.getByFileId(file.id)
                    .bindToLifecycle(this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .toObservable()
            val fileDownloadWithDefault = fileDownload.startWith(FileDownload(file.id,
                    null, FileDownload.Status.NotDownloaded, null, null))
            val downloadStatus = fileDownload
                    .map { it.status }
            var lastMp4PercentDone = 0

            data class DownloadAndMp4Status(
                    val download: FileDownload, val mp4Status: PutioMp4Status)
            val mp4Status = if (showMp4Options) {
                mp4Loader!!.mp4Status()
                        .startWith(PutioMp4Status().apply {
                            // Assume it's completed, most common case
                            status = PutioMp4Status.Status.Completed
                        })
                        .bindToLifecycle(this)
            } else if (useVideoTitleBackground) {
                Observable.just(PutioMp4Status().apply { status = PutioMp4Status.Status.AlreadyMp4 })
            } else {
                playActionView.visibility = View.GONE
                playProgressBarView.visibility = View.GONE
                playMoreView.visibility = View.GONE
                Observable.just(PutioMp4Status().apply { status = PutioMp4Status.Status.NotVideo })
            }
            val downloadAndMp4Status: Observable<DownloadAndMp4Status> = Observable.combineLatest(
                    fileDownloadWithDefault, mp4Status, BiFunction { newDownload, newMp4Status ->
                DownloadAndMp4Status(newDownload, newMp4Status)
            })
            downloadAndMp4Status.subscribe { (newDownload, newMp4Status) ->
                val downloadDone = newDownload.status == FileDownload.Status.Downloaded
                val downloadedMp4 = newDownload.downloadedMp4 ?: false
                val playMp4String = if (downloadDone && downloadedMp4) R.string.play else R.string.stream
                val playOriginalString = if (downloadDone && !downloadedMp4) R.string.play_original else R.string.stream_original
                when (newMp4Status.status!!) {
                    PutioMp4Status.Status.AlreadyMp4 -> {
                        playActionView.text = getString(playMp4String)
                        playActionView.setOnClickListener { play(false) }
                        setActionDrawable(playActionView, R.drawable.ic_filedetails_play)
                        playProgressBarView.visibility = View.INVISIBLE
                        playMoreView.visibility = View.GONE
                        playOriginalItem.isVisible = false
                    }
                    PutioMp4Status.Status.NotAvailable,
                    PutioMp4Status.Status.Error -> {
                        playActionView.text = getString(R.string.convert_mp4)
                        playActionView.setOnClickListener { mp4Loader!!.startConversion() }
                        setActionDrawable(playActionView, R.drawable.ic_filedetails_convert)
                        playProgressBarView.visibility = View.INVISIBLE
                        playMoreView.visibility = View.VISIBLE
                        playOriginalItem.isVisible = true
                    }
                    PutioMp4Status.Status.InQueue,
                    PutioMp4Status.Status.Preparing,
                    PutioMp4Status.Status.Converting,
                    PutioMp4Status.Status.Finishing -> {
                        val percentDone = newMp4Status.percentDone
                        val text = if (percentDone in lastMp4PercentDone..99) {
                            lastMp4PercentDone = percentDone
                            getString(R.string.converting_mp4_x, percentDone)
                        } else {
                            getString(R.string.converting_mp4)
                        }
                        playActionView.text = text
                        playActionView.setOnClickListener(null)
                        setActionDrawable(playActionView, blankActionDrawable)
                        playProgressBarView.visibility = View.VISIBLE
                        playMoreView.visibility = View.VISIBLE
                        playOriginalItem.isVisible = true
                    }
                    PutioMp4Status.Status.Completed -> {
                        playActionView.text = getString(playMp4String)
                        playActionView.setOnClickListener { play(true) }
                        setActionDrawable(playActionView, R.drawable.ic_filedetails_play)
                        playProgressBarView.visibility = View.INVISIBLE
                        playMoreView.visibility = View.VISIBLE
                        playOriginalItem.isVisible = true
                    }
                    PutioMp4Status.Status.NotVideo -> { }
                }
                playOriginalItem.title = getString(playOriginalString)

                when (newDownload.status) {
                    FileDownload.Status.Downloaded -> {
                        downloadProgressBarView.visibility = View.INVISIBLE
                        if (useVideoTitleBackground) {
                            setActionDrawable(downloadActionView, R.drawable.ic_filedetails_check)
                            downloadActionView.text = getString(R.string.downloaded)
                            downloadActionView.setOnClickListener(null)
                        } else {
                            setActionDrawable(downloadActionView, R.drawable.ic_filedetails_open)
                            downloadActionView.text = getString(R.string.open)
                            downloadActionView.setOnClickListener {
                                open()
                            }
                        }
                        downloadMoreView.visibility = View.VISIBLE
                        downloadOriginalItem.isVisible = false
                        sendItem.isVisible = true
                        deleteItem.isVisible = true
                        cancelItem.isVisible = false
                    }
                    FileDownload.Status.InProgress -> {
                        downloadProgressBarView.visibility = View.VISIBLE
                        setActionDrawable(downloadActionView, blankActionDrawable)
                        downloadActionView.text = getString(R.string.downloading)
                        downloadActionView.setOnClickListener(null)
                        downloadMoreView.visibility = View.VISIBLE
                        downloadOriginalItem.isVisible = false
                        sendItem.isVisible = false
                        deleteItem.isVisible = false
                        cancelItem.isVisible = true
                    }
                    FileDownload.Status.NotDownloaded -> {
                        downloadProgressBarView.visibility = View.INVISIBLE
                        setActionDrawable(downloadActionView, R.drawable.ic_filedetails_download)
                        val mp4Ready = newMp4Status.status == PutioMp4Status.Status.Completed
                        downloadActionView.text = getString(if (mp4Ready || file.isMp4 || !useVideoTitleBackground)
                            R.string.download
                        else
                            R.string.download_original)
                        downloadActionView.setOnClickListener {
                            if (mp4Ready) {
                                download(true)
                            } else {
                                download(false)
                            }
                        }
                        val showDownloadOriginal = showMp4Options && mp4Ready
                        downloadMoreView.visibility = if (showDownloadOriginal) View.VISIBLE else View.GONE
                        downloadOriginalItem.isVisible = showDownloadOriginal
                        sendItem.isVisible = false
                        deleteItem.isVisible = false
                        cancelItem.isVisible = false
                    }
                }
                lastDownloadStatus = newDownload.status
            }

            if (!useVideoTitleBackground) {
                downloadStatus.subscribe {
                    val animate = (lastDownloadStatus != null)
                    val backgroundColorRes = when (it!!) {
                        FileDownload.Status.Downloaded -> R.color.putio_filedetails_downloaded
                        FileDownload.Status.InProgress -> R.color.putio_filedetails_inprogress
                        FileDownload.Status.NotDownloaded -> R.color.putio_filedetails_notdownloaded
                    }
                    setTitleBackgroundColor(ContextCompat.getColor(
                            context, backgroundColorRes), animate)
                }
            }
        }
    }

    private fun play(mp4: Boolean) {
        val url = file.getStreamUrl(putioApp.putioUtils!!, mp4)
        castCallbacks?.load(file, url, putioApp.putioUtils!!)
    }

    private fun download(mp4: Boolean = false) {
        if (fileDownloadHelper.hasPermission()) {
            if (file.isVideo) {
                fileDownloadHelper.downloadVideo(file, mp4 && !file.isMp4)
            } else {
                fileDownloadHelper.downloadFile(file)
            }.subscribe()
        } else {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    if (mp4) REQUEST_DOWNLOAD_MP4 else REQUEST_DOWNLOAD)
        }
    }

    private fun open() {
        Single.fromCallable {
            fileDownloads.getByFileIdSynchronous(file.id)!!.uri
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { Uri.parse(it) }
                .subscribe({ fileUri ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW)
                                .setDataAndType(fileUri, file.contentType)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.noactivityfound, Toast.LENGTH_SHORT).show()
                    }
                }, { error ->
                    Toast.makeText(context, "Error opening", Toast.LENGTH_SHORT).show()
                    PutioUtils.getRxJavaThrowable(error).printStackTrace()
                })
    }

    private fun deleteDownload() {
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
    }

    override fun onResume() {
        super.onResume()

        loader.checkDownload()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_DOWNLOAD -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    download(false)
                }
            }
            REQUEST_DOWNLOAD_MP4 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    download(true)
                }
            }
        }
    }
}