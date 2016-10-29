package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
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
        val STATE_PAGES = "pages"
        val STATE_CURRENT_PAGE = "current_page"

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
    open val choosingFolder = false
    open val showSearch = true
    open val showCreateFolder = true
    open val padForFab = true

    var callbacks: Callbacks? = null

    val pages = ArrayList<Page>()
    var currentPage: Page? = null
        get() = if (pagerView != null) {
            pages[pagerView!!.currentItem]
        } else {
            field
        }
    val filesFragmentsAdapter by lazy { PageFragmentsPagerAdapter() }
    var pagerView: ViewPager? = null
    val pageChangeListener = PageChangeListener()
    var isSelecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        if (savedInstanceState != null) {
            pages.addAll(savedInstanceState.getParcelableArrayList(STATE_PAGES))
            if (savedInstanceState.containsKey(STATE_CURRENT_PAGE)) {
                currentPage = savedInstanceState.getParcelable(STATE_CURRENT_PAGE)
            }
        } else if (arguments.containsKey(EXTRA_FOLDER)) {
            pages.add(Page(arguments.getParcelable<PutioFile>(EXTRA_FOLDER)))
        } else {
            pages.add(Page(PutioFile.makeRootFolder(resources)))
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
                    page.visibility = View.VISIBLE
                    if (position <= -1) {
                        page.translationZ = 0f
                    } else {
                        page.translationZ = (position + 1) * folderFrontElevation
                    }
                    if (position < 0) {
                        if (position == -1f) {
                            // To prevent the previous page from being clickable through the current page
                            page.visibility = View.INVISIBLE
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
        val iter = pages.listIterator()
        var foundParentIndex = -1
        for (existingPage in iter) {
            if (existingPage.type == Page.Type.File) {
                if (existingPage.file!!.id == file.parentId) {
                    foundParentIndex = iter.previousIndex()
                    break
                }
            }
        }
        if (foundParentIndex != -1) {
            pages.add(foundParentIndex + 1, Page(file))
            while (pages.size > (foundParentIndex + 2)) {
                pages.removeAt(pages.lastIndex)
            }
        } else {
            pages.add(Page(file))
        }

        filesFragmentsAdapter.notifyDataSetChanged()
        pagerView!!.setCurrentItem(pages.lastIndex, true)
    }

    fun addSearch(query: String) {
        val currentIndex = pagerView!!.currentItem
        while (pages.lastIndex > currentIndex) {
            pages.removeAt(pages.lastIndex)
        }
        pages.add(Page(query, pages.last().file!!))

        filesFragmentsAdapter.notifyDataSetChanged()
        pagerView!!.setCurrentItem(pages.lastIndex, true)
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
        } else if (pagerView!!.currentItem != pages.lastIndex) {
            pagerView!!.setCurrentItem(pages.lastIndex, true)
            return true
        } else if (pages.size > 1) {
            pageChangeListener.isGoingBack = true
            pageChangeListener.removeCount++
            pagerView!!.setCurrentItem(pages.lastIndex - 1, true)
            return true
        } else {
            return false
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)

        when (childFragment) {
            is FolderFragment, is SearchFragment -> {
                childFragment as FileListFragment<FileListFragment.Callbacks>
                childFragment.callbacks = object : FileListFragment.Callbacks {
                    override fun onFileSelected(file: PutioFile) {
                        if (!choosingFolder || file.isFolder) {
                            addFile(file)
                        }
                    }
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
        outState.putParcelableArrayList(STATE_PAGES, pages)
        outState.putParcelable(STATE_CURRENT_PAGE, currentPage)
    }

    inner class PageFragmentsPagerAdapter : FragmentPagerAdapter(childFragmentManager) {

        override fun getItem(position: Int): Fragment {
            val page = pages[position]
            when (page.type) {
                Page.Type.File -> {
                    val file = page.file!!
                    if (file.isFolder) {
                        return FolderFragment.newInstance(context, file, padForFab, canSelect, showSearch, showCreateFolder)
                    } else {
                        return FileDetailsFragment.newInstance(context, file)
                    }
                }
                Page.Type.Search -> {
                    return SearchFragment.newInstance(context, page.searchQuery!!, page.parentFolder!!, canSelect)
                }
            }
        }

        override fun getItemPosition(obj: Any): Int {
            when (obj) {
                is FolderFragment -> {
                    for ((index, page) in pages.withIndex()) {
                        if (page.type == Page.Type.File && page.file!! == obj.folder) {
                            return index
                        }
                    }
                    return POSITION_NONE
                }
                is SearchFragment -> {
                    for ((index, page) in pages.withIndex()) {
                        if (page.type == Page.Type.Search &&
                                page.searchQuery!! == obj.query && page.parentFolder!! == obj.parentFolder) {
                            return index
                        }
                    }
                    return POSITION_NONE
                }
                else -> return POSITION_NONE
            }
        }

        override fun getItemId(position: Int): Long {
            val page = pages[position]
            when (page.type) {
                Page.Type.File -> return page.file!!.id
                Page.Type.Search -> return page.searchQuery!!.hashCode() + page.parentFolder!!.id
            }
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            super.destroyItem(container, position, `object`)
            if (position >= count) {
                childFragmentManager.beginTransaction()
                        .remove(`object` as Fragment)
                        .commit()
            }
        }

        override fun getCount() = pages.size
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
                        pages.removeAt(pages.lastIndex)
                        removeCount--
                    }
                    filesFragmentsAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    class Page : Parcelable {

        val type: Type
        var file: PutioFile? = null
        var searchQuery: String? = null
        var parentFolder: PutioFile? = null

        constructor(file: PutioFile) {
            this.type = Type.File
            this.file = file
        }

        constructor(searchQuery: String, parentFolder: PutioFile) {
            this.type = Type.Search
            this.searchQuery = searchQuery
            this.parentFolder = parentFolder
        }

        constructor(source: Parcel) {
            this.type = Type.values()[source.readInt()]
            when (type) {
                Type.File -> this.file = source.readParcelable(PutioFile::class.java.classLoader)
                Type.Search -> {
                    this.searchQuery = source.readString()
                    this.parentFolder = source.readParcelable(PutioFile::class.java.classLoader)
                }
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(type.ordinal)
            when (type) {
                Type.File -> dest.writeParcelable(file, flags)
                Type.Search -> {
                    dest.writeString(searchQuery)
                    dest.writeParcelable(parentFolder, flags)
                }
            }
        }

        override fun describeContents(): Int {
            return 0
        }

        enum class Type {
            File, Search
        }

        companion object {
            @JvmField val CREATOR: Parcelable.Creator<Page> = object : Parcelable.Creator<Page> {
                override fun createFromParcel(source: Parcel): Page {
                    return Page(source)
                }

                override fun newArray(size: Int): Array<Page?> {
                    return arrayOfNulls(size)
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