package com.stevenschoen.putionew.files

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.trello.rxlifecycle.components.support.RxFragment
import com.trello.rxlifecycle.kotlin.bindToLifecycle
import rx.android.schedulers.AndroidSchedulers
import java.util.*

class FolderFragment : RxFragment() {

    companion object {
        val STATE_FILES = "files"
        val STATE_CHECKED_IDS = "checked_ids"

        val EXTRA_FOLDER = "folder"
        val EXTRA_PAD_FOR_FAB = "padforfab"
        val EXTRA_CAN_SELECT = "can_select"
        val EXTRA_SHOW_SEARCH = "show_search"
        val EXTRA_SHOW_CREATEFOLDER = "show_createfolder"

        val REQUEST_CHOOSE_MOVE_DESTINATION = 1

        val FRAGTAG_SELECTION = "selection"
        val FRAGTAG_RENAME = "rename"
        val FRAGTAG_DOWNLOAD_INDIVIDUALORZIP = "dl_indivorzip"
        val FRAGTAG_DELETE = "delete"

        fun newInstance(context: Context, folder: PutioFile,
                        padForFab: Boolean, canSelect: Boolean, showSearch: Boolean, showCreateFolder: Boolean): FolderFragment {
            if (!folder.isFolder) {
                throw IllegalStateException("FolderFragment created on a file, not a folder: ${folder.name} (ID ${folder.id})")
            }
            val args = Bundle()
            args.putParcelable(EXTRA_FOLDER, folder)
            args.putBoolean(EXTRA_PAD_FOR_FAB, padForFab)
            args.putBoolean(EXTRA_CAN_SELECT, canSelect)
            args.putBoolean(EXTRA_SHOW_SEARCH, showSearch)
            args.putBoolean(EXTRA_SHOW_CREATEFOLDER, showCreateFolder)
            return Fragment.instantiate(context, FolderFragment::class.java.name, args) as FolderFragment
        }
    }

    val folder by lazy { arguments.getParcelable<PutioFile>(EXTRA_FOLDER) }
    val padForFab by lazy { arguments.getBoolean(EXTRA_PAD_FOR_FAB) }
    val canSelect by lazy { arguments.getBoolean(EXTRA_CAN_SELECT) }
    val showSearch by lazy { arguments.getBoolean(EXTRA_SHOW_SEARCH) }
    val showCreateFolder by lazy { arguments.getBoolean(EXTRA_SHOW_CREATEFOLDER) }

    var folderLoader: FolderLoader? = null
    var callbacks: Callbacks? = null

    val files = ArrayList<PutioFile>()

    val actionModeHelper by lazy { ActionModeHelper() }

    lateinit var currentFolderHolderView: View
    lateinit var currentFolderBackView: View
    lateinit var currentFolderNameView: TextView
    lateinit var loadingView: View
    lateinit var emptyView: View
    lateinit var filesView: RecyclerView
    lateinit var swipeRefreshView: SwipeRefreshLayout

    var filesAdapter: FilesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        filesAdapter = FilesAdapter(files,
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
        filesAdapter!!.setItemsCheckedChangedListener(object : FilesAdapter.OnItemsCheckedChangedListener {
            override fun onItemsCheckedChanged() {
                if (!actionModeHelper.hasActionMode()) {
                    if (filesAdapter!!.isInCheckMode()) {
                        actionModeHelper.startFloatingActionMode()
                    }
                } else {
                    actionModeHelper.invalidateActionMode()
                }
            }
        })

        if (savedInstanceState != null) {
            files.addAll(savedInstanceState.getParcelableArrayList(STATE_FILES))
            filesAdapter!!.addCheckedIds(*savedInstanceState.getLongArray(STATE_CHECKED_IDS))
        }
        getSelectionFragment()?.let {
            it.amountSelected.onNext(filesAdapter!!.checkedCount())
        }
        if (filesAdapter!!.isInCheckMode()) {
            callbacks?.onSelectionStarted()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.folder, container, false).apply {
            currentFolderHolderView = findViewById(R.id.files_currentfolder_holder)
            currentFolderBackView = currentFolderHolderView.findViewById(R.id.files_currentfolder_back)
            currentFolderBackView.setOnClickListener {
                callbacks?.onBackSelected()
            }
            currentFolderNameView = currentFolderHolderView.findViewById(R.id.files_currentfolder_name) as TextView
            if (folder.id != 0L) {
                currentFolderHolderView.visibility = View.VISIBLE
                currentFolderNameView.text = folder.name
            } else {
                currentFolderHolderView.visibility = View.GONE
            }

            loadingView = findViewById(R.id.files_loading)
            emptyView = findViewById(if (folder.id == 0L) R.id.files_empty else R.id.files_empty_subfolder)

            filesView = findViewById(R.id.folder_list) as RecyclerView
            filesView.adapter = filesAdapter
            filesView.layoutManager = LinearLayoutManager(context)
            if (padForFab) PutioUtils.padForFab(filesView)
            swipeRefreshView = findViewById(R.id.folder_swiperefresh) as SwipeRefreshLayout
            swipeRefreshView.setColorSchemeResources(R.color.putio_accent)
            swipeRefreshView.setOnRefreshListener {
                folderLoader!!.refreshFolder()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_files, menu)

        if (!showSearch) menu.findItem(R.id.menu_search).apply {
            isVisible = false
            isEnabled = false
        }
        if (!showCreateFolder) menu.findItem(R.id.menu_createfolder).apply {
            isVisible = false
            isEnabled = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // TODO
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun updateViewState() {
        if (files.isEmpty()) {
            if (folderLoader!!.isRefreshing()) {
                loadingView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            } else {
                loadingView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            }
        } else {
            loadingView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }

    private fun selectionRename() {
        val renameFragment = RenameFragment.newInstance(context, getCheckedFiles().first())
        renameFragment.show(childFragmentManager, FRAGTAG_RENAME)
    }

    private fun selectionDownloadFiles() {
        val checkedFiles = getCheckedFiles()
        if (checkedFiles.size > 1) {
            val downloadFragment = Fragment.instantiate(context, DownloadIndividualOrZipFragment::class.java.name) as DownloadIndividualOrZipFragment
            downloadFragment.show(childFragmentManager, FRAGTAG_DOWNLOAD_INDIVIDUALORZIP)
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
        startActivityForResult(Intent(context, DestinationFolderActivity::class.java), REQUEST_CHOOSE_MOVE_DESTINATION)
    }

    private fun selectionDelete() {
        val deleteFragment = ConfirmDeleteFragment.newInstance(context, filesAdapter!!.checkedCount())
        deleteFragment.show(childFragmentManager, FRAGTAG_DELETE)
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when (childFragment.tag) {
            FRAGTAG_SELECTION -> {
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
            FRAGTAG_RENAME -> {
                childFragment as RenameFragment
                childFragment.callbacks = object : RenameFragment.Callbacks {
                    override fun onRenamed(newName: String) {
                        PutioApplication.get(context).putioUtils.restInterface
                                .renameFile(getCheckedFiles().first().id, newName)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    filesAdapter!!.clearChecked()
                                    folderLoader!!.refreshFolder()
                                }
                    }
                }
            }
            FRAGTAG_DOWNLOAD_INDIVIDUALORZIP -> {
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
            FRAGTAG_DELETE -> {
                childFragment as ConfirmDeleteFragment
                childFragment.callbacks = object : ConfirmDeleteFragment.Callbacks {
                    override fun onDeleteSelected() {
                        PutioApplication.get(context).putioUtils.restInterface
                                .deleteFile(PutioUtils.longsToString(*filesAdapter!!.checkedIds.toLongArray()))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    filesAdapter!!.clearChecked()
                                    folderLoader!!.refreshFolder()
                                }
                    }
                }
            }
        }
    }

    fun getSelectionFragment(): FileSelectionFragment? {
        return childFragmentManager.findFragmentByTag(FRAGTAG_SELECTION) as FileSelectionFragment?
    }

    fun getCheckedFiles(): List<PutioFile> {
        return filesAdapter!!.checkedIds.map { id -> files.find { it.id == id }!! }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        folderLoader = FolderLoader.get(loaderManager, context, folder)
        folderLoader!!.folder()
                .bindToLifecycle(this)
                .subscribe { response ->
                    files.clear()
                    files.addAll(response.files)
                    filesAdapter!!.notifyDataSetChanged()
                    if (response.fresh) {
                        swipeRefreshView.isRefreshing = false
                    }
                    updateViewState()
                }
        folderLoader!!.getCachedFile()
        folderLoader!!.refreshFolder(onlyIfStaleOrEmpty = true)
        updateViewState()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isAdded) {
            view!!.post {
                if (!userVisibleHint && filesAdapter!!.isInCheckMode()) {
                    filesAdapter!!.clearChecked()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHOOSE_MOVE_DESTINATION -> {
                if (resultCode == Activity.RESULT_OK) {
                    view?.post {
                        val destinationFolder = data!!.getParcelableExtra<PutioFile>(DestinationFolderActivity.RESULT_EXTRA_FOLDER)
                        PutioApplication.get(context).putioUtils.restInterface.moveFile(
                                PutioUtils.longsToString(*filesAdapter!!.checkedIds.toLongArray()), destinationFolder.id)
                                .bindToLifecycle(this@FolderFragment)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ fileChangingResponse ->
                                    folderLoader!!.refreshFolder()
                                }, { error ->
                                    error.printStackTrace();
                                });

                    }
                    Toast.makeText(activity, getString(R.string.filemoved), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_FILES, files)
        outState.putLongArray(STATE_CHECKED_IDS, filesAdapter!!.checkedIds.toLongArray())
    }

    inner class ActionModeHelper() {
        val useFloating = (Build.VERSION.SDK_INT >= 21)

        var supportActionMode: ActionMode? = null

        fun hasActionMode(): Boolean {
            if (useFloating) {
                return getSelectionFragment() != null
            } else {
                return (supportActionMode != null)
            }
        }

        fun invalidateActionMode() {
            if (useFloating) {
                val count = filesAdapter!!.checkedCount()
                if (count == 0) {
                    childFragmentManager.beginTransaction()
                            .remove(getSelectionFragment())
                            .commitNow()
                    callbacks?.onSelectionEnded()
                } else {
                    getSelectionFragment()!!.amountSelected.onNext(count)
                }
            } else {
                supportActionMode!!.invalidate()
            }
        }

        fun startFloatingActionMode() {
            callbacks?.onSelectionStarted()
            if (useFloating) {
                val selectionFragment = Fragment.instantiate(context, FileSelectionFragment::class.java.name) as FileSelectionFragment
                childFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.folder_selection_holder, selectionFragment, FRAGTAG_SELECTION)
                        .commitNow()
            } else {
                supportActionMode = (activity as AppCompatActivity).startSupportActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                        if (filesAdapter!!.isInCheckMode()) {
                            actionMode.menuInflater.inflate(R.menu.context_files, menu)
                            return true
                        } else {
                            return false
                        }
                    }

                    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                        if (filesAdapter!!.isInCheckMode()) {
                            actionMode.title = getString(R.string.x_selected, filesAdapter!!.checkedCount())
                        } else {
                            actionMode.finish()
                        }
                        return true
                    }

                    override fun onActionItemClicked(actionMode: ActionMode, item: MenuItem): Boolean {
//                        val checkedPositions = filesAdapter!!.getCheckedPositions()
//
//                        when (item.itemId) {
//                            R.id.context_download -> {
//                                initDownloadFiles(*checkedPositions)
//                                return true
//                            }
//                            R.id.context_copydownloadlink -> {
//                                initCopyFileDownloadLink(*checkedPositions)
//                                return true
//                            }
//                            R.id.context_rename -> {
//                                initRenameFile(checkedPositions[0])
//                                return true
//                            }
//                            R.id.context_delete -> {
//                                initDeleteFile(*checkedPositions)
//                                return true
//                            }
//                            R.id.context_move -> {
//                                initMoveFile(*checkedPositions)
//                                return true
//                            }
//                        }

                        return true
                    }

                    override fun onDestroyActionMode(actionMode: ActionMode) {
                        filesAdapter!!.clearChecked()
                        callbacks?.onSelectionEnded()
                        supportActionMode = null
                    }
                })
            }
        }
    }

    interface Callbacks {
        fun onFileSelected(file: PutioFile)
        fun onBackSelected()
        fun onSelectionStarted()
        fun onSelectionEnded()
    }
}