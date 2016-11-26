package com.stevenschoen.putionew.files

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.trello.rxlifecycle.components.support.RxFragment
import rx.android.schedulers.AndroidSchedulers
import java.util.*

abstract class FileListFragment<CallbacksClass: FileListFragment.Callbacks> : RxFragment() {

    companion object {
        const val STATE_FILES = "files"
        const val STATE_CHECKED_IDS = "checked_ids"

        const val EXTRA_CAN_SELECT = "can_select"

        fun <T> addArguments(fragment: T, canSelect: Boolean): T where T : FileListFragment<*> {
            fragment.arguments = fragment.arguments.apply {
                putBoolean(EXTRA_CAN_SELECT, canSelect)
            }
            return fragment
        }
    }

    val canSelect by lazy { arguments.getBoolean(EXTRA_CAN_SELECT) }

    var callbacks: CallbacksClass? = null

    lateinit var currentViewHolderView: View
    lateinit var currentViewBackView: View
    lateinit var currentFolderNameView: TextView
    lateinit var currentSearchHolderView: View
    lateinit var currentSearchQueryView: TextView
    lateinit var currentSearchFolderView: TextView

    lateinit var loadingView: View
    lateinit var emptySubfolderView: View
    lateinit var filesView: RecyclerView
    lateinit var swipeRefreshView: SwipeRefreshLayout

    val files = ArrayList<PutioFile>()

    var filesAdapter: FileListAdapter? = null

    val selectionHelper by lazy { SelectionHelper() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filesAdapter = FileListAdapter(files,
                onFileClicked = { file, fileHolder ->
                    if (filesAdapter!!.isInCheckMode()) {
                        filesAdapter!!.togglePositionChecked(fileHolder.adapterPosition)
                    } else {
                        callbacks?.onFileSelected(file)
                    }
                },
                onFileLongClicked = { file, fileHolder ->
                    if (canSelect) {
                        filesAdapter!!.togglePositionChecked(fileHolder.adapterPosition)
                    }
                })
        filesAdapter!!.setItemsCheckedChangedListener(object : FileListAdapter.OnItemsCheckedChangedListener {
            override fun onItemsCheckedChanged() {
                if (!selectionHelper.isShowing()) {
                    if (filesAdapter!!.isInCheckMode()) {
                        selectionHelper.show()
                    }
                } else {
                    selectionHelper.invalidate()
                }
            }
        })

        if (savedInstanceState != null) {
            files.addAll(savedInstanceState.getParcelableArrayList(STATE_FILES))
            filesAdapter!!.addCheckedIds(*savedInstanceState.getLongArray(STATE_CHECKED_IDS))
        }
        getSelectionFragment()?.amountSelected?.onNext(filesAdapter!!.checkedCount())
        if (filesAdapter!!.isInCheckMode()) {
            callbacks?.onSelectionStarted()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.file_list, container, false).apply {
            currentViewHolderView = findViewById(R.id.files_currentview_holder)
            currentViewBackView = currentViewHolderView.findViewById(R.id.files_currentview_back)
            currentViewBackView.setOnClickListener {
                callbacks?.onBackSelected()
            }
            currentFolderNameView = currentViewHolderView.findViewById(R.id.files_currentfolder_name) as TextView
            currentSearchHolderView = currentViewHolderView.findViewById(R.id.folder_currentsearch_holder)
            currentSearchQueryView = currentSearchHolderView.findViewById(R.id.folder_currentsearch_query) as TextView
            currentSearchFolderView = currentSearchHolderView.findViewById(R.id.folder_currentsearch_parent) as TextView

            loadingView = findViewById(R.id.files_loading)
            emptySubfolderView = findViewById(R.id.files_empty_subfolder)

            filesView = findViewById(R.id.folder_list) as RecyclerView
            filesView.adapter = filesAdapter
            filesView.layoutManager = LinearLayoutManager(context)
            swipeRefreshView = findViewById(R.id.folder_swiperefresh) as SwipeRefreshLayout
            swipeRefreshView.setColorSchemeResources(R.color.putio_accent)
            swipeRefreshView.setOnRefreshListener {
                refresh()
            }
        }
    }

    fun updateViewState() {
        if (files.isEmpty()) {
            if (isRefreshing()) {
                loadingView.visibility = View.VISIBLE
                emptySubfolderView.visibility = View.GONE
            } else {
                loadingView.visibility = View.GONE
                emptySubfolderView.visibility = View.VISIBLE
            }
        } else {
            loadingView.visibility = View.GONE
            emptySubfolderView.visibility = View.GONE
        }
    }

    fun getSelectionFragment(): FileSelectionFragment? {
        return childFragmentManager.findFragmentByTag(FolderFragment.FRAGTAG_SELECTION) as FileSelectionFragment?
    }

    private fun selectionRename() {
        val renameFragment = RenameFragment.newInstance(context, getCheckedFiles().first())
        renameFragment.show(childFragmentManager, FolderFragment.FRAGTAG_RENAME)
    }

    private fun selectionDownloadFiles() {
        val checkedFiles = getCheckedFiles()
        if (checkedFiles.size > 1) {
            val downloadFragment = Fragment.instantiate(context, DownloadIndividualOrZipFragment::class.java.name) as DownloadIndividualOrZipFragment
            downloadFragment.show(childFragmentManager, FolderFragment.FRAGTAG_DOWNLOAD_INDIVIDUALORZIP)
        } else if (checkedFiles.size == 1) {
            PutioApplication.get(context).putioUtils.downloadFiles(activity, PutioUtils.ACTION_NOTHING, *checkedFiles.toTypedArray())
        } else {
            throw IllegalStateException("Download started with no file IDs!")
        }
    }

    private fun selectionCopyLinks() {
        val checkedFiles = getCheckedFiles()
        val utils = PutioApplication.get(context).putioUtils
        if (checkedFiles.size == 1) {
            val file = checkedFiles[0]
            if (file.isFolder) {
                utils.copyZipDownloadLink(activity, file)
            } else {
                utils.copyDownloadLink(activity, file)
            }
        } else if (checkedFiles.size > 0) {
            utils.copyZipDownloadLink(activity, *getCheckedFiles().toTypedArray())
        }
    }

    private fun selectionMove() {
        startActivityForResult(Intent(context, DestinationFolderActivity::class.java), FolderFragment.REQUEST_CHOOSE_MOVE_DESTINATION)
    }

    private fun selectionDelete() {
        val deleteFragment = ConfirmDeleteFragment.newInstance(context, filesAdapter!!.checkedCount())
        deleteFragment.show(childFragmentManager, FolderFragment.FRAGTAG_DELETE)
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when (childFragment.tag) {
            FolderFragment.FRAGTAG_SELECTION -> {
                childFragment as FileSelectionFragment
                filesAdapter?.let {
                    childFragment.amountSelected.onNext(it.checkedCount())
                }
                childFragment.callbacks = object : FileSelectionFragment.Callbacks {
                    override fun onRenameSelected() {
                        selectionRename()
                    }
                    override fun onDownloadSelected() {
                        selectionDownloadFiles()
                        filesAdapter!!.clearChecked()
                    }
                    override fun onCopyLinkSelected() {
                        selectionCopyLinks()
                        filesAdapter!!.clearChecked()
                    }
                    override fun onMoveSelected() {
                        selectionMove()
                    }
                    override fun onDeleteSelected() {
                        selectionDelete()
                    }
                    override fun onCancel() {
                        childFragmentManager.beginTransaction()
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                                .remove(childFragment)
                                .commitNow()
                        callbacks?.onSelectionEnded()
                        filesAdapter!!.clearChecked()
                    }
                }
            }
            FolderFragment.FRAGTAG_RENAME -> {
                childFragment as RenameFragment
                childFragment.callbacks = object : RenameFragment.Callbacks {
                    override fun onRenamed(newName: String) {
                        PutioApplication.get(context).putioUtils.restInterface
                                .renameFile(getCheckedFiles().first().id, newName)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    filesAdapter!!.clearChecked()
                                    refresh()
                                }
                    }
                }
            }
            FolderFragment.FRAGTAG_DOWNLOAD_INDIVIDUALORZIP -> {
                childFragment as DownloadIndividualOrZipFragment
                childFragment.callbacks = object : DownloadIndividualOrZipFragment.Callbacks {
                    override fun onIndividualSelected() {
                        PutioApplication.get(context).putioUtils.downloadFiles(activity, PutioUtils.ACTION_NOTHING, *getCheckedFiles().toTypedArray())
                    }
                    override fun onZipSelected() {
                        val checkedFiles = getCheckedFiles()
                        val filename = checkedFiles.take(5).joinToString(separator = ", ", transform = { it.name }) + ".zip"
                        PutioUtils.download(activity,
                                Uri.parse(PutioApplication.get(context).putioUtils.getZipDownloadUrl(*filesAdapter!!.checkedIds.toLongArray())), filename)
                                .subscribe {
                                    Toast.makeText(context, getString(R.string.downloadstarted), Toast.LENGTH_SHORT).show()
                                }
                    }
                    override fun onCanceled() { }
                }
            }
            FolderFragment.FRAGTAG_DELETE -> {
                childFragment as ConfirmDeleteFragment
                childFragment.callbacks = object : ConfirmDeleteFragment.Callbacks {
                    override fun onDeleteSelected() {
                        PutioApplication.get(context).putioUtils.restInterface
                                .deleteFile(PutioUtils.longsToString(*filesAdapter!!.checkedIds.toLongArray()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    filesAdapter!!.clearChecked()
                                    refresh()
                                }
                    }
                }
            }
        }
    }

    fun getCheckedFiles(): List<PutioFile> {
        return filesAdapter!!.checkedIds.map { id -> files.find { it.id == id }!! }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_FILES, files)
        outState.putLongArray(STATE_CHECKED_IDS, filesAdapter!!.checkedIds.toLongArray())
    }

    abstract fun refresh()
    abstract fun isRefreshing(): Boolean

    inner class SelectionHelper() {
        fun isShowing(): Boolean {
            return getSelectionFragment() != null
        }

        fun invalidate() {
            val count = filesAdapter!!.checkedCount()
            if (count == 0) {
                childFragmentManager.beginTransaction()
                        .remove(getSelectionFragment())
                        .commitNow()
                callbacks?.onSelectionEnded()
            } else {
                getSelectionFragment()!!.amountSelected.onNext(count)
            }
        }

        fun show() {
            callbacks?.onSelectionStarted()
            val selectionFragment = Fragment.instantiate(context, FileSelectionFragment::class.java.name) as FileSelectionFragment
            childFragmentManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.folder_selection_holder, selectionFragment, FolderFragment.FRAGTAG_SELECTION)
                    .commitNow()
        }
    }

    interface Callbacks {
        fun onFileSelected(file: PutioFile)
        fun onBackSelected()
        fun onSelectionStarted()
        fun onSelectionEnded()
    }
}