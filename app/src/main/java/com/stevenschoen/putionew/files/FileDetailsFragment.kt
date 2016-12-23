package com.stevenschoen.putionew.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import com.stevenschoen.putionew.*
import com.stevenschoen.putionew.PutioApplication.CastCallbacks
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.files.PutioMp4Status
import com.trello.rxlifecycle.components.support.RxFragment
import com.trello.rxlifecycle.kotlin.bindToLifecycle
import rx.android.schedulers.AndroidSchedulers

class FileDetailsFragment : RxFragment() {

    companion object {
        val EXTRA_FILE = "file"

        val FRAGTAG_RENAME = "rename"
        val FRAGTAG_DELETE = "delete";

        fun newInstance(context: Context, file: PutioFile): FileDetailsFragment {
            if (file.isFolder) {
                throw IllegalStateException("FileDetailsFragment created on a folder, not a file: ${file.name} (ID ${file.id})")
            }
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            return Fragment.instantiate(context, FileDetailsFragment::class.java.name, args) as FileDetailsFragment
        }
    }

    val file by lazy {arguments.getParcelable<PutioFile>(EXTRA_FILE)}

    var screenshotLoader: FileScreenshotLoader? = null
    var mp4StatusLoader: Mp4StatusLoader? = null

    lateinit var toolbarView: Toolbar
    lateinit var titleView: TextView
    lateinit var infoMp4Checking: View
    lateinit var infoMp4Already: View
    lateinit var infoMp4Available: View
    lateinit var textMp4Available: TextView
    lateinit var checkBoxMp4Available: CheckBox
    lateinit var infoMp4NotAvailable: View
    lateinit var infoMp4Converting: View
    lateinit var infoMp4ConvertingText: TextView

    var callbacks: Callbacks? = null
    var castCallbacks: CastCallbacks? = null

    val utils by lazy { PutioApplication.get(context).putioUtils }

    private var imagePreview: ImageView? = null
    private var imagePreviewPlaceholder: ImageView? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.filedetails, container, false)

        toolbarView = view.findViewById(R.id.filedetails_toolbar) as Toolbar
        toolbarView.setNavigationOnClickListener {
            callbacks!!.onFileDetailsClosed(false)
        }
        toolbarView.inflateMenu(R.menu.menu_filedetails)
        if (file.isMedia) {
            val itemOpen = toolbarView.menu.findItem(R.id.menu_open)
            itemOpen.isVisible = false
            itemOpen.isEnabled = false
        }
        toolbarView.setOnMenuItemClickListener { item ->
            when (item!!.itemId) {
                R.id.menu_open -> {
                    initActionFile(PutioUtils.ACTION_OPEN)
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_download -> {
                    initActionFile(PutioUtils.ACTION_NOTHING)
                    return@setOnMenuItemClickListener true
                }
                R.id.menu_delete -> {
                    initDeleteFile()
                    return@setOnMenuItemClickListener true
                }
            }
            return@setOnMenuItemClickListener false
        }

        val scrimColor = Color.parseColor("#48000000")
        val toolbarScrimView = view.findViewById(R.id.filepreview_toolbar_scrim)
        toolbarScrimView.background = ScrimUtil.makeCubicGradientScrimDrawable(scrimColor, 8, Gravity.TOP)
        val titleScrimView = view.findViewById(R.id.filepreview_title_scrim)
        titleScrimView.background = ScrimUtil.makeCubicGradientScrimDrawable(scrimColor, 10, Gravity.BOTTOM)

        if (UIUtils.hasLollipop()) {
            val preview = view.findViewById(R.id.filepreview)
            preview.elevation = PutioUtils.pxFromDp(activity, 2f)
            toolbarView.elevation = resources.getDimension(R.dimen.appBarElevation)
        }

        titleView = view.findViewById(R.id.filepreview_title) as TextView
        titleView.text = file.name
        titleView.setOnClickListener {
            val renameFragment = RenameFragment.newInstance(context, file)
            renameFragment.show(childFragmentManager, FRAGTAG_RENAME)
        }

        val holderInfo = view.findViewById(R.id.holder_fileinfo) as ViewGroup

        infoMp4Checking = holderInfo.findViewById(R.id.holder_fileinfo_mp4_checking)
        infoMp4Already = holderInfo.findViewById(R.id.holder_fileinfo_mp4_already)
        infoMp4Available = holderInfo.findViewById(R.id.holder_fileinfo_mp4_available)
        infoMp4NotAvailable = holderInfo.findViewById(R.id.holder_fileinfo_mp4_notavailable)
        infoMp4Converting = holderInfo.findViewById(R.id.holder_fileinfo_mp4_converting)
        infoMp4ConvertingText = infoMp4Converting.findViewById(R.id.fileinfo_mp4_converting_text) as TextView
        if (file.isVideo) {
            checkBoxMp4Available = infoMp4Available.findViewById(R.id.checkbox_fileinfo_mp4) as CheckBox
            checkBoxMp4Available.setOnCheckedChangeListener { buttonView, isChecked ->
                updateMp4View(mp4StatusLoader!!.lastMp4Status())
            }
            textMp4Available = infoMp4Available.findViewById(R.id.text_fileinfo_mp4) as TextView
            infoMp4NotAvailable.setOnClickListener { view ->
                PutioApplication.get(context).putioUtils.restInterface.convertToMp4(file.id).subscribe() {
                    mp4StatusLoader!!.startRefreshing()
                }
                updateMp4View(null)
                view.isEnabled = false
            }
            updateMp4View(null)
        } else {
            infoMp4Checking.visibility = View.GONE
            infoMp4Already.visibility = View.GONE
            infoMp4Available.visibility = View.GONE
            infoMp4NotAvailable.visibility = View.GONE
            infoMp4Converting.visibility = View.GONE
        }

        val infoAccessed = holderInfo.findViewById(R.id.holder_fileinfo_accessedat)
        val textAccessed = infoAccessed.findViewById(R.id.text_fileinfo_accessedat) as TextView
        if (file.isAccessed) {
            val accessed = PutioUtils.parseIsoTime(activity, file.firstAccessedAt)
            textAccessed.text = getString(R.string.accessed_on_x_at_x, accessed[0], accessed[1])
        } else {
            textAccessed.text = getString(R.string.never_accessed)
        }

        val infoCreated = holderInfo.findViewById(R.id.holder_fileinfo_createdat)
        val textCreated = infoCreated.findViewById(R.id.text_fileinfo_createdat) as TextView
        val created = PutioUtils.parseIsoTime(activity, file.createdAt)
        textCreated.text = getString(R.string.created_on_x_at_x, created[0], created[1])

        val infoSize = holderInfo.findViewById(R.id.holder_fileinfo_size)
        val textSize = infoSize.findViewById(R.id.text_fileinfo_size) as TextView
        textSize.text = PutioUtils.humanReadableByteCount(file.size, false)

        val infoCrc32 = holderInfo.findViewById(R.id.holder_fileinfo_crc32)
        val textCrc32 = infoCrc32.findViewById(R.id.text_fileinfo_crc32) as TextView
        textCrc32.text = file.crc32

        val buttonPlay = view.findViewById(R.id.button_filedetails_play)
        if (file.isMedia) {
            buttonPlay.setOnClickListener {
                var mp4 = false
                if (file.isVideo) {
                    val mp4Status = mp4StatusLoader!!.lastMp4Status()
                    if (mp4Status != null) {
                        if (mp4Status == PutioMp4Status.Status.Completed) {
                            mp4 = checkBoxMp4Available.isChecked
                        }
                    } else if (file.isMp4Available) {
                        mp4 = checkBoxMp4Available.isChecked
                    }
                }

                val url = file.getStreamUrl(utils, mp4)
                castCallbacks!!.load(file, url, utils)
            }
        } else {
            buttonPlay.isEnabled = false
            buttonPlay.visibility = View.GONE
        }

        imagePreviewPlaceholder = view.findViewById(R.id.filepreview_image_placeholder) as ImageView
        Picasso.with(context)
                .load(file.icon)
                .transform(PutioUtils.BlurTransformation(activity, 4f))
                .into(imagePreviewPlaceholder)

        imagePreview = view.findViewById(R.id.filepreview_image) as ImageView

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (isAdded) {
            screenshotLoader = FileScreenshotLoader.get(loaderManager, context, file)
            screenshotLoader!!.load(true)
            screenshotLoader!!.screenshot()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        it?.let { updateImagePreview(it, true) }
                    }

            if (file.isVideo) {
                mp4StatusLoader = Mp4StatusLoader.get(loaderManager, context, file)
                mp4StatusLoader!!.mp4Status()
                        .bindToLifecycle(this)
                        .subscribe { status ->
                            updateMp4View(status)
                        }
                mp4StatusLoader!!.refreshOnce()
            }
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when (childFragment.tag) {
            FRAGTAG_RENAME -> {
                childFragment as RenameFragment
                childFragment.callbacks = object : RenameFragment.Callbacks {
                    override fun onRenamed(newName: String) {
                        titleView.text = newName
                        PutioApplication.get(context).putioUtils.restInterface
                                .renameFile(file.id, newName)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe()
                    }
                }
            }
            FRAGTAG_DELETE -> {
                childFragment as ConfirmDeleteFragment
                childFragment.callbacks = object : ConfirmDeleteFragment.Callbacks {
                    override fun onDeleteSelected() {
                        PutioApplication.get(context).putioUtils.restInterface
                                .deleteFile(file.id.toString())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    callbacks!!.onFileDetailsClosed(true)
                                }
                    }
                }
            }
        }
    }

    private fun initActionFile(mode: Int) {
        if (PutioUtils.idIsDownloaded(file.id)) {
            val dialog = PutioUtils.showPutioDialog(activity, getString(R.string.redownloadtitle), R.layout.dialog_redownload)

            val textBody = dialog.findViewById(R.id.text_redownloadbody) as TextView
            when (mode) {
                PutioUtils.ACTION_NOTHING -> textBody.text = getString(R.string.redownloadfordlbody)
                PutioUtils.ACTION_OPEN -> textBody.text = getString(R.string.redownloadforopenbody)
            }

            val buttonOpen = dialog.findViewById(R.id.button_redownload_open) as Button
            if (mode == PutioUtils.ACTION_OPEN) {
                buttonOpen.setOnClickListener {
                    PutioUtils.openDownloadedId(file.id, activity)
                    dialog.dismiss()
                }
            } else {
                buttonOpen.visibility = View.GONE
            }

            val buttonRedownload = dialog.findViewById(R.id.button_redownload_download) as Button
            buttonRedownload.setOnClickListener {
                PutioUtils.deleteId(file.id)
                utils!!.downloadFiles(activity, mode, file)
                dialog.dismiss()
            }

            val buttonCancel = dialog.findViewById(R.id.button_redownload_cancel) as Button
            buttonCancel.setOnClickListener { dialog.cancel() }
        } else {
            utils!!.downloadFiles(activity, mode, file)
        }
    }

    private fun initDeleteFile() {
        val confirmDeleteFragment = ConfirmDeleteFragment.newInstance(context, 1)
        confirmDeleteFragment.show(childFragmentManager, FRAGTAG_DELETE)
    }

    private fun updateMp4View(status: PutioMp4Status?) {
        if (status != null) {
            when (status.status!!) {
                PutioMp4Status.Status.NotAvailable -> {
                    infoMp4Checking.visibility = View.GONE
                    infoMp4Already.visibility = View.GONE
                    infoMp4Available.visibility = View.GONE
                    infoMp4Converting.visibility = View.GONE
                    infoMp4NotAvailable.visibility = View.VISIBLE
                }
                PutioMp4Status.Status.InQueue,
                PutioMp4Status.Status.Preparing,
                PutioMp4Status.Status.Converting,
                PutioMp4Status.Status.Finishing -> {
                    infoMp4Checking.visibility = View.GONE
                    infoMp4Already.visibility = View.GONE
                    infoMp4Available.visibility = View.GONE
                    infoMp4Converting.visibility = View.VISIBLE
                    infoMp4ConvertingText.text = getString(R.string.converting_mp4) + " (${status.percentDone}%)"
                    infoMp4NotAvailable.visibility = View.GONE
                }
                PutioMp4Status.Status.Completed -> {
                    infoMp4Checking.visibility = View.GONE
                    infoMp4Already.visibility = View.GONE
                    infoMp4Available.visibility = View.VISIBLE
                    infoMp4Converting.visibility = View.GONE
                    infoMp4NotAvailable.visibility = View.GONE

                    if (checkBoxMp4Available.isChecked) {
                        textMp4Available.text = getString(R.string.use_mp4)
                    } else {
                        textMp4Available.text = getString(R.string.dont_use_mp4)
                    }
                }
                PutioMp4Status.Status.AlreadyMp4 -> {
                    infoMp4Checking.visibility = View.GONE
                    infoMp4Already.visibility = View.VISIBLE
                    infoMp4Available.visibility = View.GONE
                    infoMp4Converting.visibility = View.GONE
                    infoMp4NotAvailable.visibility = View.GONE
                }
                PutioMp4Status.Status.Error -> {
                    infoMp4Checking.visibility = View.GONE
                    infoMp4Already.visibility = View.GONE
                    infoMp4Available.visibility = View.GONE
                    infoMp4Converting.visibility = View.GONE
                    infoMp4NotAvailable.visibility = View.VISIBLE
                }
            }
        } else {
            infoMp4Checking.visibility = View.VISIBLE
            infoMp4Already.visibility = View.GONE
            infoMp4Available.visibility = View.GONE
            infoMp4Converting.visibility = View.GONE
            infoMp4NotAvailable.visibility = View.GONE
        }
    }

    private fun updateImagePreview(bitmap: Bitmap, animate: Boolean) {
        if (animate) {
            updateImagePreview(bitmap, false)
            imagePreview!!.alpha = 0f
            imagePreview!!.animate().alpha(1f)
        } else {
            imagePreview!!.setImageBitmap(bitmap)
            imagePreview!!.postInvalidate()
//            if (UIUtils.hasLollipop()) {
//                Palette.from(bitmap).generate { palette ->
//                    if (isAdded) {
//                        val darkMuted = palette.darkMutedSwatch
//                        if (darkMuted != null) {
//                            val statusBarAnim = ValueAnimator.ofObject(ArgbEvaluator(),
//                                    activity.window.statusBarColor, darkMuted.rgb)
//                            statusBarAnim.addUpdateListener { animation ->
//                                if (isAdded && activity.window != null) {
//                                    activity.window.statusBarColor = animation.animatedValue as Int
//                                } else {
//                                    animation.cancel()
//                                }
//                            }
//                            statusBarAnim.start()
//                        }
//                    }
//                }
//            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        castCallbacks = activity as CastCallbacks
    }

    override fun onDetach() {
        castCallbacks = null
        super.onDetach()
    }

    interface Callbacks {
        fun onFileDetailsClosed(refreshParent: Boolean)
    }
}