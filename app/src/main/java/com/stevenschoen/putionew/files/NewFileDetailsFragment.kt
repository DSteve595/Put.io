package com.stevenschoen.putionew.files

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.ScrimUtil
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp
import com.trello.rxlifecycle2.android.FragmentEvent
import com.trello.rxlifecycle2.components.support.RxFragment
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class NewFileDetailsFragment : RxFragment() {

    companion object {
        val EXTRA_FILE = "file"

        fun newInstance(context: Context, file: PutioFile): NewFileDetailsFragment {
            if (file.isFolder) {
                throw IllegalStateException("FileDetailsFragment created for the wrong kind of file:" +
                        "${file.name} (ID ${file.id}), type ${file.contentType}")
            }
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            return Fragment.instantiate(context, NewFileDetailsFragment::class.java.name, args) as NewFileDetailsFragment
        }
    }

    val fileDownloads by lazy { putioApp.fileDownloadDatabase.fileDownloadsDao() }

    var onBackPressed: (() -> Any)? = null

    val file by lazy { arguments.getParcelable<PutioFile>(EXTRA_FILE)!! }
    val showMp4Options by lazy { file.isVideo && !file.isMp4 }

    lateinit var loader: NewFileDetailsLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loader = NewFileDetailsLoader.get(loaderManager, context, file)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.newfiledetails, container, false).apply {
            val useVideoTitleBackground = file.isVideo && !file.screenshot.isNullOrBlank()

            val titleView: TextView = findViewById(R.id.newfiledetails_title)
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

            val screenshotView: ImageView = findViewById(R.id.newfiledetails_screenshot)
            val screenshotBlurryView: ImageView = findViewById(R.id.newfiledetails_screenshot_blurry)
            val screenshotTopScrimView: View = findViewById(R.id.newfiledetails_screenshot_scrim_top)
            val screenshotTitleScrimView: View = findViewById(R.id.newfiledetails_screenshot_scrim_title)

            val fileGraphicView: View = findViewById(R.id.newfiledetails_graphic_file)

            val backView: ImageButton = findViewById(R.id.newfiledetails_back)
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
                titleView.setShadowLayer(4f, 0f, 2f, Color.BLACK)
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
            val progressBarView: View = findViewById(R.id.newfiledetails_progressbar)
            fun setActionDrawable(drawable: Drawable?) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(actionView,
                        drawable, null, null, null)
            }
            fun setActionDrawable(@DrawableRes drawableRes: Int) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(actionView,
                        drawableRes, 0, 0, 0)
            }
            fun showActionDownload() {
                progressBarView.visibility = View.INVISIBLE
                setActionDrawable(R.drawable.ic_filedetails_download)
                actionView.text = getString(R.string.download)
                actionView.setOnClickListener {
                    putioApp.putioUtils!!.downloadFiles(activity, PutioUtils.ACTION_NOTHING, file)
                }
            }
            val blankDrawable = ContextCompat.getDrawable(context, R.drawable.blank_24)
            fun showActionDownloading() {
                progressBarView.visibility = View.VISIBLE
                setActionDrawable(blankDrawable)
                actionView.text = getString(R.string.downloading)
                actionView.setOnClickListener(null)
            }
            fun showActionOpen() {
                progressBarView.visibility = View.INVISIBLE
                setActionDrawable(R.drawable.ic_filedetails_open)
                actionView.text = getString(R.string.open)
                actionView.setOnClickListener {
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
            }

            val moreView: View = findViewById(R.id.newfiledetails_more)
            val morePopup = PopupMenu(context, moreView)
            val sendItem = morePopup.menu.add(R.string.send).setOnMenuItemClickListener {
                Single.fromCallable {
                    fileDownloads.getByFileIdSynchronous(file.id)!!
                }
                        .bindToLifecycle(this@NewFileDetailsFragment)
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
            val deleteItem = morePopup.menu.add(R.string.delete).setOnMenuItemClickListener {
                deleteDownload()
                true
            }
            val cancelItem = morePopup.menu.add(R.string.cancel).setOnMenuItemClickListener {
                deleteDownload()
                true
            }
            moreView.setOnTouchListener(morePopup.dragToOpenListener)
            moreView.setOnClickListener {
                morePopup.show()
            }

            var lastStatus: FileDownload.Status? = null
            val status = fileDownloads.getByFileId(file.id)
                    .bindToLifecycle(this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map { it.status }

            status
                    .startWith(FileDownload.Status.NotDownloaded)
                    .subscribe {
                        when (it!!) {
                            FileDownload.Status.Downloaded -> {
                                showActionOpen()
                                moreView.visibility = View.VISIBLE
                                sendItem.isVisible = true
                                deleteItem.isVisible = true
                                cancelItem.isVisible = false
                            }
                            FileDownload.Status.InProgress -> {
                                showActionDownloading()
                                moreView.visibility = View.VISIBLE
                                sendItem.isVisible = false
                                deleteItem.isVisible = false
                                cancelItem.isVisible = true
                            }
                            FileDownload.Status.NotDownloaded -> {
                                showActionDownload()
                                moreView.visibility = View.GONE
                                sendItem.isVisible = false
                                deleteItem.isVisible = false
                                cancelItem.isVisible = false
                            }
                        }
                        lastStatus = it
                    }

            if (!useVideoTitleBackground) {
                status.subscribe {
                    val animate = (lastStatus != null)
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
}