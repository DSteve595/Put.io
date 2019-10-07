package com.stevenschoen.putionew.files

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.putioApp
import com.trello.rxlifecycle3.components.support.RxFragment
import com.trello.rxlifecycle3.kotlin.bindToLifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

abstract class FileListFragment<CallbacksClass : FileListFragment.Callbacks> : RxFragment() {

  companion object {
    const val STATE_FILES = "files"
    const val STATE_CHECKED_IDS = "checked_ids"

    const val EXTRA_CAN_SELECT = "can_select"

    const val REQUEST_DOWNLOAD_SELECTED = 1

    fun <T> addArguments(fragment: T, canSelect: Boolean): T where T : FileListFragment<*> {
      fragment.arguments = fragment.arguments!!.apply {
        putBoolean(EXTRA_CAN_SELECT, canSelect)
      }
      return fragment
    }
  }

  val canSelect by lazy { arguments!!.getBoolean(EXTRA_CAN_SELECT) }

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
  val fileDownloadHelper by lazy { FileDownloadHelper(context!!) }

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
        if (!selectionHelper.isShowing) {
          if (filesAdapter!!.isInCheckMode()) {
            selectionHelper.show()
          }
        } else {
          selectionHelper.invalidate()
        }
      }
    })

    if (savedInstanceState != null) {
      files.addAll(savedInstanceState.getParcelableArrayList(STATE_FILES)!!)
      filesAdapter!!.addCheckedIds(*savedInstanceState.getLongArray(STATE_CHECKED_IDS)!!)
    }
    getSelectionFragment()?.amountSelected?.onNext(filesAdapter!!.checkedCount())
    if (filesAdapter!!.isInCheckMode()) {
      callbacks?.onSelectionStarted()
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.file_list, container, false).apply {
      currentViewHolderView = findViewById(R.id.files_currentview_holder)
      currentViewBackView = currentViewHolderView.findViewById(R.id.file_title_bar_back)
      currentViewBackView.setOnClickListener {
        callbacks?.onBackSelected()
      }
      TooltipCompat.setTooltipText(currentViewBackView, getString(R.string.go_back))
      currentFolderNameView = currentViewHolderView.findViewById(R.id.file_title_bar_name)
      currentSearchHolderView = currentViewHolderView.findViewById(R.id.file_title_bar_search_holder)
      currentSearchQueryView = currentSearchHolderView.findViewById(R.id.file_title_bar_search_query)
      currentSearchFolderView = currentSearchHolderView.findViewById(R.id.file_title_bar_search_parent)

      loadingView = findViewById(R.id.files_loading)
      emptySubfolderView = findViewById(R.id.files_empty_subfolder)

      filesView = findViewById(R.id.folder_list)
      filesView.adapter = filesAdapter
      filesView.layoutManager = LinearLayoutManager(context)
      swipeRefreshView = findViewById(R.id.folder_swiperefresh)
      swipeRefreshView.setColorSchemeResources(R.color.putio_accent)
      swipeRefreshView.setOnRefreshListener {
        refresh()
      }

      getSelectionFragment()?.let { selectionHelper.setViewProperties() }
    }
  }

  fun updateViewState() {
    if (files.isEmpty()) {
      if (isRefreshing) {
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
    val renameFragment = RenameFragment.newInstance(context!!, getCheckedFiles().first())
    renameFragment.show(childFragmentManager, FolderFragment.FRAGTAG_RENAME)
  }

  private fun selectionDownloadFiles() {
    if (fileDownloadHelper.hasPermission()) {
      val checkedFiles = getCheckedFiles()
      if (checkedFiles.size > 1) {
        val downloadFragment = Fragment.instantiate(context!!, DownloadIndividualOrZipFragment::class.java.name) as DownloadIndividualOrZipFragment
        downloadFragment.show(childFragmentManager, FolderFragment.FRAGTAG_DOWNLOAD_INDIVIDUALORZIP)
      } else if (checkedFiles.size == 1) {
        fileDownloadHelper.downloadFile(checkedFiles.first()).subscribe()
        filesAdapter!!.clearChecked()
        Toast.makeText(
            context, getString(R.string.downloadstarted),
            Toast.LENGTH_SHORT
        ).show()
      } else {
        throw IllegalStateException("Download started with no file IDs!")
      }
    } else {
      requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_DOWNLOAD_SELECTED)
    }
  }

  private fun selectionCopyLinks() {
    val checkedFiles = getCheckedFiles()
    val utils = putioApp.putioUtils!!
    if (checkedFiles.size == 1) {
      val file = checkedFiles[0]
      if (file.isFolder) {
        fileDownloadHelper.copyZipLink(file.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
              if (isVisible) {
                Toast.makeText(
                    context, getString(R.string.readytopaste),
                    Toast.LENGTH_SHORT
                ).show()
              }
            }
      } else {
        PutioUtils.copy(context, "Download link", file.getDownloadUrl(utils))
        Toast.makeText(
            context, getString(R.string.readytopaste),
            Toast.LENGTH_SHORT
        ).show()
      }
    } else if (checkedFiles.isNotEmpty()) {
      fileDownloadHelper.copyZipLink(*checkedFiles.map { it.id }.toLongArray())
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            if (isVisible) {
              Toast.makeText(
                  context, getString(R.string.readytopaste),
                  Toast.LENGTH_SHORT
              ).show()
            }
          }
    }
  }

  private fun selectionMove() {
    startActivityForResult(Intent(context, DestinationFolderActivity::class.java), FolderFragment.REQUEST_CHOOSE_MOVE_DESTINATION)
  }

  private fun selectionDelete() {
    val deleteFragment = ConfirmDeleteFragment.newInstance(context!!, filesAdapter!!.checkedCount())
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
          override fun onRenameSelected() = selectionRename()
          override fun onDownloadSelected() = selectionDownloadFiles()
          override fun onCopyLinkSelected() {
            selectionCopyLinks()
            filesAdapter!!.clearChecked()
          }

          override fun onMoveSelected() = selectionMove()
          override fun onDeleteSelected() = selectionDelete()
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
            putioApp.putioUtils!!.restInterface
                .renameFile(getCheckedFiles().first().id, newName)
                .bindToLifecycle(this@FileListFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                  filesAdapter!!.clearChecked()
                  refresh()
                }, { error ->
                  PutioUtils.getRxJavaThrowable(error).printStackTrace()
                  Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                })
          }
        }
      }
      FolderFragment.FRAGTAG_DOWNLOAD_INDIVIDUALORZIP -> {
        childFragment as DownloadIndividualOrZipFragment
        childFragment.callbacks = object : DownloadIndividualOrZipFragment.Callbacks {
          override fun onIndividualSelected() {
            getCheckedFiles().forEach {
              fileDownloadHelper.downloadFile(it).subscribe()
            }
            Toast.makeText(
                context, getString(R.string.downloadstarted),
                Toast.LENGTH_SHORT
            ).show()
            filesAdapter!!.clearChecked()
          }

          override fun onZipSelected() {
            val checkedFiles = getCheckedFiles()
            val filename = checkedFiles.take(5).joinToString(separator = ", ", transform = { it.name }) + ".zip"

            Toast.makeText(context, R.string.downloading_zip, Toast.LENGTH_LONG).show()
            fileDownloadHelper
                .getZipUrl(*checkedFiles.map { it.id }.toLongArray())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { fileDownloadHelper.download(it, filename) }
                .subscribe({ _ ->
                  Toast.makeText(context, getString(R.string.downloadstarted), Toast.LENGTH_SHORT).show()
                }, { error ->
                  error.printStackTrace()
                  PutioUtils.getRxJavaThrowable(error).printStackTrace()
                  Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                })
            filesAdapter!!.clearChecked()
          }

          override fun onCanceled() {}
        }
      }
      FolderFragment.FRAGTAG_DELETE -> {
        childFragment as ConfirmDeleteFragment
        childFragment.callbacks = object : ConfirmDeleteFragment.Callbacks {
          override fun onDeleteSelected() {
            putioApp.putioUtils!!.restInterface
                .deleteFile(PutioUtils.longsToString(*filesAdapter!!.checkedIds.toLongArray()))
                .bindToLifecycle(this@FileListFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                  filesAdapter!!.clearChecked()
                  refresh()
                }, { error ->
                  PutioUtils.getRxJavaThrowable(error).printStackTrace()
                  Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
                })
          }
        }
      }
    }
  }

  fun getCheckedFiles(): List<PutioFile> {
    return filesAdapter!!.checkedIds.map { id -> files.find { it.id == id }!! }
  }

  override fun setUserVisibleHint(isVisibleToUser: Boolean) {
    super.setUserVisibleHint(isVisibleToUser)
    view?.post {
      if (!userVisibleHint && filesAdapter!!.isInCheckMode()) {
        filesAdapter!!.clearChecked()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putParcelableArrayList(STATE_FILES, files)
    outState.putLongArray(STATE_CHECKED_IDS, filesAdapter!!.checkedIds.toLongArray())
  }

  override fun onRequestPermissionsResult(
      requestCode: Int, permissions: Array<out String>,
      grantResults: IntArray
  ) {
    when (requestCode) {
      REQUEST_DOWNLOAD_SELECTED -> {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          selectionDownloadFiles()
        }
      }
    }
  }

  abstract fun refresh()
  abstract val isRefreshing: Boolean

  inner class SelectionHelper() {
    val isShowing
      get() = getSelectionFragment() != null

    fun invalidate() {
      val count = filesAdapter!!.checkedCount()
      if (count == 0) {
        childFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            .remove(getSelectionFragment()!!)
            .commitNow()
        callbacks?.onSelectionEnded()
      } else {
        getSelectionFragment()!!.amountSelected.onNext(count)
      }
    }

    fun show() {
      callbacks?.onSelectionStarted()
      val selectionFragment = Fragment.instantiate(context!!, FileSelectionFragment::class.java.name) as FileSelectionFragment
      childFragmentManager.beginTransaction()
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .add(R.id.file_list_root, selectionFragment, FolderFragment.FRAGTAG_SELECTION)
          .commitNow()
      setViewProperties()
    }

    fun setViewProperties() {
      val selectionFragment = getSelectionFragment()!!
      selectionFragment.onSetupLayout = {
        selectionFragment.view!!.apply {
          (layoutParams as RelativeLayout.LayoutParams).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            addRule(RelativeLayout.CENTER_HORIZONTAL)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.file_list_selection_bottom_margin)
          }
        }
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
