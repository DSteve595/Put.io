package com.stevenschoen.putionew.files

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.Toast
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.trello.rxlifecycle.kotlin.bindToLifecycle
import rx.android.schedulers.AndroidSchedulers

class FolderFragment : FileListFragment<FileListFragment.Callbacks>() {

    val folder by lazy { arguments.getParcelable<PutioFile>(EXTRA_FOLDER) }
    val padForFab by lazy { arguments.getBoolean(EXTRA_PAD_FOR_FAB) }
    val showSearch by lazy { arguments.getBoolean(EXTRA_SHOW_SEARCH) }
    val showCreateFolder by lazy { arguments.getBoolean(EXTRA_SHOW_CREATEFOLDER) }

    var folderLoader: FolderLoader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            currentSearchHolderView.visibility = View.GONE
            if (folder.id != 0L) {
                currentFolderNameView.visibility = View.VISIBLE
                currentFolderNameView.text = folder.name
            } else {
                currentViewHolderView.visibility = View.GONE
            }

            if (padForFab) PutioUtils.padForFab(filesView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_folder, menu)

        val itemSearch = menu.findItem(R.id.menu_search)
        val searchManager = activity.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = MenuItemCompat.getActionView(itemSearch) as SearchView
        searchView.setIconifiedByDefault(true)
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.componentName))

        if (!showSearch) itemSearch.apply {
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
            R.id.menu_createfolder -> {
                CreateFolderFragment.newInstance(context, folder).show(childFragmentManager, FRAGTAG_CREATE_FOLDER)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when (childFragment.tag) {
            FRAGTAG_CREATE_FOLDER -> {
                childFragment as CreateFolderFragment
                childFragment.callbacks = object : CreateFolderFragment.Callbacks {
                    override fun onNameEntered(folderName: String) {
                        PutioApplication.get(context).putioUtils.restInterface
                                .createFolder(folderName, folder.id)
                                .bindToLifecycle(this@FolderFragment)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ response ->
                                    if (response.status == "OK") {
                                        folderLoader!!.refreshFolder(false)
                                    }
                                }, { error ->
                                    error.printStackTrace()
                                    Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                                })
                    }
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        folderLoader = FolderLoader.get(loaderManager, context, folder)
        folderLoader!!.folder()
                .bindToLifecycle(this)
                .subscribe({ response ->
                    files.clear()
                    files.addAll(response.files)
                    filesAdapter!!.notifyDataSetChanged()
                    if (response.fresh) {
                        swipeRefreshView.isRefreshing = false
                    }
                    updateViewState()
                }, { error ->
                    error.printStackTrace()
                    Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                })
        folderLoader!!.publishCachedFileIfNeeded()
        folderLoader!!.refreshFolder(onlyIfStaleOrEmpty = true)
        updateViewState()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisible) {
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
                        PutioApplication.get(context).putioUtils.restInterface
                                .moveFile(PutioUtils.longsToString(*filesAdapter!!.checkedIds.toLongArray()), destinationFolder.id)
                                .bindToLifecycle(this@FolderFragment)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ fileChangingResponse ->
                                    folderLoader!!.refreshFolder()
                                }, { error ->
                                    error.printStackTrace()
                                    Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                                })

                    }
                    Toast.makeText(activity, getString(R.string.filemoved), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun refresh() {
        folderLoader!!.refreshFolder(false)
    }

    override fun isRefreshing(): Boolean {
        return folderLoader!!.isRefreshing()
    }

    companion object {
        const val EXTRA_FOLDER = "folder"
        const val EXTRA_PAD_FOR_FAB = "padforfab"
        const val EXTRA_SHOW_SEARCH = "show_search"
        const val EXTRA_SHOW_CREATEFOLDER = "show_createfolder"

        const val REQUEST_CHOOSE_MOVE_DESTINATION = 1

        const val FRAGTAG_SELECTION = "selection"
        const val FRAGTAG_RENAME = "rename"
        const val FRAGTAG_DOWNLOAD_INDIVIDUALORZIP = "dl_indivorzip"
        const val FRAGTAG_DELETE = "delete"
        const val FRAGTAG_CREATE_FOLDER = "create_folder"

        fun newInstance(context: Context, folder: PutioFile,
                        canSelect: Boolean,
                        padForFab: Boolean, showSearch: Boolean, showCreateFolder: Boolean): FolderFragment {
            if (!folder.isFolder) {
                throw IllegalStateException("FolderFragment created on a file, not a folder: ${folder.name} (ID ${folder.id})")
            }
            val args = Bundle()
            args.putParcelable(EXTRA_FOLDER, folder)
            args.putBoolean(EXTRA_PAD_FOR_FAB, padForFab)
            args.putBoolean(EXTRA_SHOW_SEARCH, showSearch)
            args.putBoolean(EXTRA_SHOW_CREATEFOLDER, showCreateFolder)
            return addArguments(Fragment.instantiate(context, FolderFragment::class.java.name, args) as FolderFragment,
                    canSelect)
        }
    }
}