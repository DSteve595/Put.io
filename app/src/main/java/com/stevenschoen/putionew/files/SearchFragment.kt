package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.trello.rxlifecycle.kotlin.bindToLifecycle

class SearchFragment : FileListFragment<FileListFragment.Callbacks>() {

    companion object {
        val EXTRA_QUERY = "query"
        val EXTRA_PARENT_FOLDER = "parent_folder"

        fun newInstance(context: Context, query: String, parentFolder: PutioFile,
                        canSelect: Boolean): SearchFragment {
            if (!parentFolder.isFolder) {
                throw IllegalStateException("SearchFragment created with parent as a file, not a folder: ${parentFolder.name} (ID ${parentFolder.id})")
            }
            val args = Bundle()
            args.putString(EXTRA_QUERY, query)
            args.putParcelable(EXTRA_PARENT_FOLDER, parentFolder)
            return addArguments(Fragment.instantiate(context, SearchFragment::class.java.name, args) as SearchFragment,
                    canSelect)
        }
    }

    val query by lazy { arguments.getString(EXTRA_QUERY) }
    val parentFolder by lazy { arguments.getParcelable<PutioFile>(EXTRA_PARENT_FOLDER) }

    var searchLoader: SearchLoader? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            currentFolderNameView.visibility = View.GONE
            currentSearchQueryView.text = query
            currentSearchFolderView.text = getString(R.string.in_x, parentFolder.name)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        searchLoader = SearchLoader.get(loaderManager, context, parentFolder, query)
        searchLoader!!.search()
                .bindToLifecycle(this)
                .subscribe({ response ->
                    files.clear()
                    files.addAll(response)
                    filesAdapter!!.notifyDataSetChanged()
                    swipeRefreshView.isRefreshing = false
                    updateViewState()
                }, { error ->
                    error.printStackTrace()
                    Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                })
        searchLoader!!.refreshSearch(onlyIfEmpty = true)
        updateViewState()
    }

    override fun refresh() {
        searchLoader!!.refreshSearch(false)
    }

    override fun isRefreshing(): Boolean {
        return searchLoader!!.isRefreshing()
    }
}