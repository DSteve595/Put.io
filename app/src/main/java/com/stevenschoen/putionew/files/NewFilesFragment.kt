package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.UIUtils
import com.stevenschoen.putionew.model.files.PutioFile
import com.trello.rxlifecycle.components.support.RxFragment
import java.util.*

open class NewFilesFragment : RxFragment() {

    companion object {
        val STATE_FILES = "files"
        val STATE_CURRENT_FILE = "current_file"

        val EXTRA_FOLDER = "folder"

        fun newInstance(context: Context, folder: PutioFile?): NewFilesFragment {
            val args = Bundle()
            if (folder != null) {
                args.putParcelable(EXTRA_FOLDER, folder)
            }
            return Fragment.instantiate(context, NewFilesFragment::class.java.name, args) as NewFilesFragment
        }
    }

    open val canSelect = true
    open val showSearch = true
    open val showCreateFolder = true
    open val padForFab = true

    var callbacks: Callbacks? = null

    val files = ArrayList<PutioFile>()
    var currentFile: PutioFile? = null
        get() = if (pagerView != null) {
            files[pagerView!!.currentItem]
        } else {
            field
        }
    val filesFragmentsAdapter by lazy { FileFragmentsPagerAdapter() }
    var pagerView: ViewPager? = null
    val pageChangeListener = PageChangeListener()
    var isSelecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            files.addAll(savedInstanceState.getParcelableArrayList(STATE_FILES))
            if (savedInstanceState.containsKey(STATE_CURRENT_FILE)) {
                currentFile = savedInstanceState.getParcelable(STATE_CURRENT_FILE)
            }
        } else if (arguments.containsKey(EXTRA_FOLDER)) {
            files.add(arguments.getParcelable(EXTRA_FOLDER))
        } else {
            files.add(PutioFile.makeRootFolder(resources))
        }
        filesFragmentsAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.newfiles, container, false).apply {
            pagerView = findViewById(R.id.newfiles_pager) as ViewPager
            pagerView!!.adapter = filesFragmentsAdapter
            pagerView!!.addOnPageChangeListener(pageChangeListener)

            if (UIUtils.hasLollipop()) {
                val folderFrontElevation = resources.getDimension(R.dimen.folder_front_elevation)

                pagerView!!.setPageTransformer(true) { page, position ->
                    if (position < -1) {
                        page.translationZ = 0f
                    } else {
                        page.translationZ = (position + 1) * folderFrontElevation
                    }
                    if (position < 0) {
                        if (position == -1f) {
                            // To prevent the previous page from being clickable through the current page
                            page.translationX == 0f
                        } else {
                            page.translationX = page.width * (-position * 0.5f)
                        }
                    } else {
                        page.translationX = 0f
                    }
                }
            }
        }
    }

    fun addFile(file: PutioFile) {
        val iter = files.listIterator()
        var foundParentIndex = -1
        for (existingFile in iter) {
            if (existingFile.id == file.parentId) {
                foundParentIndex = iter.previousIndex()
                break
            }
        }
        if (foundParentIndex != -1) {
            files.add(foundParentIndex + 1, file)
            while (files.size > (foundParentIndex + 2)) {
                files.removeAt(files.lastIndex)
            }
        } else {
            files.add(file)
        }
        filesFragmentsAdapter.notifyDataSetChanged()
        pagerView!!.setCurrentItem(files.lastIndex, true)
    }

    fun goBack(): Boolean {
        if (pageChangeListener.isGoingBack) {
            if (pagerView!!.currentItem == 0) {
                return false
            } else {
                pageChangeListener.removeCount++
                pagerView!!.setCurrentItem(pagerView!!.currentItem - 1, true)
                return true
            }
        } else if (pagerView!!.currentItem != files.lastIndex) {
            pagerView!!.setCurrentItem(files.lastIndex, true)
            return true
        } else if (files.size > 1) {
            pageChangeListener.isGoingBack = true
            pageChangeListener.removeCount++
            pagerView!!.setCurrentItem(files.lastIndex - 1, true)
            return true
        } else {
            return false
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)

        when (childFragment) {
            is FolderFragment -> childFragment.callbacks = object : FolderFragment.Callbacks {
                override fun onFileSelected(file: PutioFile) = addFile(file)
                override fun onBackSelected() {
                    goBack()
                }
                override fun onSelectionStarted() {
                    isSelecting = true
                    callbacks?.onSelectionStarted()
                }
                override fun onSelectionEnded() {
                    isSelecting = false
                    callbacks?.onSelectionEnded()
                }
            }
            is FileDetailsFragment -> childFragment.callbacks = object : FileDetailsFragment.Callbacks {
                override fun onFileDetailsClosed() {
                    goBack()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_FILES, files)
        if (pagerView != null) {
            outState.putParcelable(STATE_CURRENT_FILE, currentFile)
        }
    }

    inner class FileFragmentsPagerAdapter : FragmentPagerAdapter(childFragmentManager) {

        override fun getItem(position: Int): Fragment {
            val file = files[position]
            if (file.isFolder) {
                return FolderFragment.newInstance(context, file, padForFab, canSelect, showSearch, showCreateFolder)
            } else {
                return FileDetailsFragment.newInstance(context, file)
            }
        }

        override fun getItemPosition(obj: Any): Int {
            if (obj is FolderFragment) {
                val index = files.indexOf(obj.folder)
                if (index != -1) {
                    return index
                } else {
                    return POSITION_NONE
                }
            }
            return POSITION_NONE
        }

        override fun getItemId(position: Int): Long {
            return files[position].id
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            super.destroyItem(container, position, `object`)
            if (position >= count) {
                childFragmentManager.beginTransaction()
                        .remove(`object` as Fragment)
                        .commit()
            }
        }

        override fun getCount() = files.size
    }

    inner class PageChangeListener : ViewPager.SimpleOnPageChangeListener() {
        var removeCount = 0
        var isGoingBack = false

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            callbacks?.onCurrentFileChanged()
        }

        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                if (isGoingBack) {
                    isGoingBack = false
                    while (removeCount > 0) {
                        files.removeAt(files.lastIndex)
                        removeCount--
                    }
                    filesFragmentsAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    interface Callbacks {
        fun onSelectionStarted()
        fun onSelectionEnded()
        fun onCurrentFileChanged()
    }
}