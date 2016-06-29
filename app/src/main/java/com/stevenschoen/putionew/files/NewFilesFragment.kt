package com.stevenschoen.putionew.files

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

open class NewFilesFragment : RxFragment(), FolderFragment.Callbacks {

    companion object {
        val STATE_FILES = "files"

        val EXTRA_FOLDER = "folder"

        fun newInstance(folder: PutioFile?): NewFilesFragment {
            val args = Bundle()
            if (folder != null) {
                args.putParcelable(EXTRA_FOLDER, folder)
            }
            return NewFilesFragment().apply {
                arguments = args
            }
        }
    }

    open val canSelect = true
    open val showSearch = true
    open val showCreateFolder = true

    var callbacks: Callbacks? = null

    val files = ArrayList<PutioFile>()
    val filesFragmentsAdapter by lazy { FileFragmentsPagerAdapter() }
    lateinit var pagerView: ViewPager
    val backPageChangeListener = PageChangeListener()
    var isSelecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            files.addAll(savedInstanceState.getParcelableArrayList(STATE_FILES))
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
            pagerView.adapter = filesFragmentsAdapter
            pagerView.addOnPageChangeListener(backPageChangeListener)

            if (UIUtils.hasLollipop()) {
                val folderFrontElevation = resources.getDimension(R.dimen.folder_front_elevation)

                pagerView.setPageTransformer(true) { page, position ->
                    if (position < -1) {
                        page.translationZ = 0f
                    } else {
                        page.translationZ = (position + 1) * folderFrontElevation
                    }
                    if (position < 0) {
                        page.translationX = page.width * (-position * 0.5f)
                    } else {
                        page.translationX = 0f
                    }
                }
            }
        }
    }

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
        pagerView.setCurrentItem(files.lastIndex, true)
    }

    fun getCurrentFolder(): PutioFile {
        return files[pagerView.currentItem]
    }

    fun goBack(): Boolean {
        if (backPageChangeListener.isGoingBack) {
            backPageChangeListener.removeCount++
            pagerView.setCurrentItem(pagerView.currentItem - 1, true)
            return true
        } else if (pagerView.currentItem != files.lastIndex) {
            pagerView.setCurrentItem(files.lastIndex, true)
            return true
        } else if (files.size > 1) {
            backPageChangeListener.isGoingBack = true
            backPageChangeListener.removeCount++
            pagerView.setCurrentItem(files.lastIndex - 1, true)
            return true
        } else {
            return false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_FILES, files)
    }

    inner class FileFragmentsPagerAdapter : FragmentPagerAdapter(childFragmentManager) {

        override fun getItem(position: Int): Fragment {
            val file = files[position]
//            if (file.isFolder) {
            return FolderFragment.newInstance(context, file, canSelect, showSearch, showCreateFolder)
//            } else {
//                return FileDetails.
//            }
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
    }
}