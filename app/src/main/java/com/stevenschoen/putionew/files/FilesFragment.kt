package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.UIUtils
import com.stevenschoen.putionew.analytics
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp
import com.trello.rxlifecycle2.components.support.RxFragment
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import retrofit2.HttpException
import java.util.*

open class FilesFragment : RxFragment() {

  companion object {
    const val STATE_PAGES = "pages"
    const val STATE_CURRENT_PAGE = "current_page"

    const val EXTRA_FOLDER = "folder"

    fun newInstance(context: Context, folder: PutioFile?): FilesFragment {
      val args = Bundle()
      if (folder != null) {
        args.putParcelable(EXTRA_FOLDER, folder)
      }
      return Fragment.instantiate(context, FilesFragment::class.java.name, args) as FilesFragment
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
  val fileListFragmentsAdapter by lazy { PageFragmentsPagerAdapter() }
  var pagerView: ViewPager? = null
  val pageChangeListener = PageChangeListener()
  var isSelecting = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setHasOptionsMenu(true)

    when {
      savedInstanceState != null -> {
        pages.addAll(savedInstanceState.getParcelableArrayList(STATE_PAGES))
        if (savedInstanceState.containsKey(STATE_CURRENT_PAGE)) {
          currentPage = savedInstanceState.getParcelable(STATE_CURRENT_PAGE)
        }
      }
      arguments?.containsKey(EXTRA_FOLDER) == true -> {
        pages.add(Page.File(arguments!!.getParcelable(EXTRA_FOLDER)))
      }
      else -> {
        pages.add(Page.File(PutioFile.makeRootFolder(resources)))
      }
    }
    fileListFragmentsAdapter.notifyDataSetChanged()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.files, container, false).apply {
      pagerView = findViewById(R.id.files_pager)
      pagerView!!.adapter = fileListFragmentsAdapter
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

  fun findPageWithIndexForFragment(fragment: FileListFragment<*>): IndexedValue<Page>? {
    val idToCompare = when (fragment) {
      is FolderFragment -> Page.File(fragment.folder).uniqueId
      is SearchFragment -> Page.Search(fragment.query, fragment.parentFolder).uniqueId
      else -> throw RuntimeException()
    }

    pages.forEachIndexed { index, page ->
      if (page.uniqueId == idToCompare) {
        return IndexedValue(index, page)
      }
    }
    return null
  }

  fun removePagesAfterIndex(position: Int, notifyAdapterIfChanged: Boolean) {
    var changed = false
    while (pages.lastIndex > position) {
      pages.removeAt(pages.lastIndex)
      changed = true
    }

    if (notifyAdapterIfChanged && changed) {
      fileListFragmentsAdapter.notifyDataSetChanged()
    }
  }

  fun addFile(file: PutioFile) {
    removePagesAfterIndex(pagerView!!.currentItem, false)

    val iter = pages.listIterator()
    var foundParentIndex = -1
    for (existingPage in iter) {
      if (existingPage is Page.File) {
        if (existingPage.file.id == file.parentId) {
          foundParentIndex = iter.previousIndex()
          break
        }
      }
    }
    if (foundParentIndex != -1) {
      pages.add(foundParentIndex + 1, Page.File(file))
      removePagesAfterIndex(foundParentIndex + 1, false)
    } else {
      pages.add(Page.File(file))
    }

    fileListFragmentsAdapter.notifyDataSetChanged()
    pagerView!!.setCurrentItem(pages.lastIndex, true)

    if (file.isFolder) {
      analytics.logBrowsedToFolder(file)
    } else {
      analytics.logViewedFile(file)
    }
  }

  fun addSearch(query: String) {
    removePagesAfterIndex(pagerView!!.currentItem, false)
    pages.add(Page.Search(query, (pages.last() as Page.File).file))

    fileListFragmentsAdapter.notifyDataSetChanged()
    pagerView!!.setCurrentItem(pages.lastIndex, true)

    analytics.logSearched(query)
  }

  fun goToFile(parentId: Long, id: Long) {
    var found = false
    var parentIndex = 0
    pages.listIterator().apply {
      loop@ while (hasNext()) {
        val nextPage = next()
        when (nextPage) {
          is Page.Search -> {
            removePagesAfterIndex(previousIndex() - 1, false)
            break@loop
          }
          is Page.File -> {
            if (nextPage.file.id == parentId) {
              parentIndex = previousIndex()
              var targetIndex = parentIndex
              if (hasNext()) {
                val childPage = next() as Page.File
                if (childPage.file.id == id) {
                  found = true
                  targetIndex = previousIndex()
                }
              }
              removePagesAfterIndex(targetIndex, false)
              break@loop
            }
          }
        }
      }
    }
    if (!found) {
      removePagesAfterIndex(parentIndex, false)
      putioApp.putioUtils!!.restInterface.file(id)
          .bindToLifecycle(this)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({ result ->
            addFile(result.file)
          }, { error ->
            if (error is HttpException && error.code() == 404) {
              Toast.makeText(context, R.string.filenotfound, Toast.LENGTH_LONG).show()
            }
          })
    }

    fileListFragmentsAdapter.notifyDataSetChanged()
    pagerView!!.setCurrentItem(pages.lastIndex, true)
  }

  fun goBack(goToLastPageIfNotSelected: Boolean): Boolean {
    return when {
      pageChangeListener.isGoingBack -> {
        if (pagerView!!.currentItem == 0) {
          false
        } else {
          pageChangeListener.removeCount++
          pagerView!!.setCurrentItem(pagerView!!.currentItem - 1, true)
          true
        }
      }
      goToLastPageIfNotSelected && pagerView!!.currentItem != pages.lastIndex -> {
        pagerView!!.setCurrentItem(pages.lastIndex, true)
        true
      }
      pages.size > 1 -> {
        pageChangeListener.isGoingBack = true
        pageChangeListener.removeCount++
        pagerView!!.setCurrentItem(pages.lastIndex - 1, true)
        true
      }
      else -> false
    }
  }

  fun goBackToRoot(): Boolean {
    return when {
      pages.size == 1 -> false
      pageChangeListener.isGoingBack -> {
        pagerView!!.setCurrentItem(0, true)
        pageChangeListener.removeCount = (pages.size - 1)
        true
      }
      else -> {
        while (pageChangeListener.removeCount < (pages.size - 1)) {
          goBack(true)
        }
        true
      }
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
            val pageWithIndex = findPageWithIndexForFragment(childFragment)
            pageWithIndex?.let {
              removePagesAfterIndex(it.index, true)
            }
            goBack(false)
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
      is FileDetailsFragment -> {
        childFragment.onBackPressed = {
          goBack(false)
        }
        childFragment.castCallbacks = activity as PutioApplication.CastCallbacks
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelableArrayList(STATE_PAGES, pages)
    outState.putParcelable(STATE_CURRENT_PAGE, currentPage)
  }

  inner class PageFragmentsPagerAdapter : FragmentPagerAdapter(childFragmentManager) {

    private val fragments = SparseArray<Fragment>()

    override fun getItem(position: Int): Fragment {
      val page = pages[position]
      return when (page) {
        is Page.File -> {
          val file = page.file
          when {
            file.isFolder -> FolderFragment.newInstance(context!!, file, padForFab, canSelect, showSearch, showCreateFolder)
            else -> FileDetailsFragment.newInstance(context!!, file)
          }
        }
        is Page.Search -> {
          SearchFragment.newInstance(context!!, page.searchQuery, page.parentFolder, canSelect)
        }
      }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
      val fragment = super.instantiateItem(container, position) as Fragment
      fragments.put(position, fragment)

      return fragment
    }

    override fun getItemPosition(obj: Any): Int {
      when (obj) {
        is FolderFragment -> {
          for ((index, page) in pages.withIndex()) {
            if (page is Page.File && page.file == obj.folder) {
              return index
            }
          }
          return POSITION_NONE
        }
        is SearchFragment -> {
          for ((index, page) in pages.withIndex()) {
            if (page is Page.Search &&
                page.searchQuery == obj.query && page.parentFolder == obj.parentFolder) {
              return index
            }
          }
          return POSITION_NONE
        }
        else -> return POSITION_NONE
      }
    }

    override fun getItemId(position: Int): Long {
      return pages[position].uniqueId
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
      super.destroyItem(container, position, `object`)
      if (position >= count) {
        childFragmentManager.beginTransaction()
            .remove(`object` as Fragment)
            .commitNowAllowingStateLoss()
      }
      fragments.removeAt(position)
    }

    override fun getCount() = pages.size

    fun getFragmentAt(position: Int) = fragments[position]
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
          fileListFragmentsAdapter.notifyDataSetChanged()
        }
      }
    }
  }

  sealed class Page : Parcelable {

    abstract val uniqueId: Long

    @Parcelize
    class File(val file: PutioFile) : Page() {
      @IgnoredOnParcel
      override val uniqueId = file.id
    }

    @Parcelize
    class Search(val searchQuery: String, val parentFolder: PutioFile) : Page() {
      @IgnoredOnParcel
      override val uniqueId = Objects.hash(searchQuery, parentFolder.id).toLong()
    }

  }

  interface Callbacks {
    fun onSelectionStarted()
    fun onSelectionEnded()
    fun onCurrentFileChanged()
  }
}
