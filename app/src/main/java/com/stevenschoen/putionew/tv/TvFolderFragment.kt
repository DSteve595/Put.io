package com.stevenschoen.putionew.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v17.leanback.app.VerticalGridSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.OnItemViewClickedListener
import android.support.v17.leanback.widget.VerticalGridPresenter
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.UIUtils
import com.stevenschoen.putionew.files.FolderLoader
import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioFile
import io.reactivex.android.schedulers.AndroidSchedulers

class TvFolderFragment : VerticalGridSupportFragment() {

  companion object {
    private const val EXTRA_FOLDER = "folder"

    private const val NUM_COLUMNS = 3

    fun newInstance(context: Context, folder: PutioFile): TvFolderFragment {
      return Fragment.instantiate(context, TvFolderFragment::class.java.name, Bundle().apply {
        putParcelable(EXTRA_FOLDER, folder)
      }) as TvFolderFragment
    }
  }

  var onFolderSelected: ((folder: PutioFile) -> Unit)? = null
  var requestFocusWhenPossible: Boolean = false
    set(value) {
      if (value && isVisible) {
        view!!.apply {
          post {
            requestFocus()
          }
        }
      } else {
        field = value
      }
    }

  private var arrayAdapter: ArrayObjectAdapter? = null
  private val folder by lazy { arguments!!.getParcelable<PutioFile>(EXTRA_FOLDER)!! }
  lateinit private var loader: FolderLoader

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (UIUtils.hasLollipop()) {
      enterTransition = Slide(Gravity.END)
      exitTransition = Slide(Gravity.END)
    }

    loader = FolderLoader.get(loaderManager, context!!, folder)
    loader.refreshFolder(onlyIfStaleOrEmpty = true, cache = false)
    loader.folder()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          when (it) {
            is FolderLoader.FolderResponse -> populateGrid(it.files)
            is ResponseOrError.NetworkError -> Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
          }
        }, { error ->
          PutioUtils.getRxJavaThrowable(error).printStackTrace()
          Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
        })

    title = folder.name

    gridPresenter = GridPresenter()

    arrayAdapter = ArrayObjectAdapter(TvPutioFileCardPresenter())
    adapter = arrayAdapter

    onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
      if (item is PutioFile) {
        if (item.isFolder) {
          onFolderSelected?.invoke(item)
        } else {
          playVideo(item)
        }
      }
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return super.onCreateView(inflater, container, savedInstanceState)!!.apply {
      setBackgroundColor(ContextCompat.getColor(context, R.color.putio_tv_background))

      if (requestFocusWhenPossible) {
        post {
          requestFocus()
        }
        requestFocusWhenPossible = false
      }
    }
  }

  private fun playVideo(file: PutioFile) {
    startActivity(Intent(context, TvPlayerActivity::class.java).apply {
      putExtra(TvPlayerActivity.EXTRA_VIDEO, file)
      putExtra(TvPlayerActivity.EXTRA_USE_MP4, file.isMp4Available)
    })
  }

  private fun populateGrid(files: List<PutioFile>) {
    arrayAdapter!!.clear()
    arrayAdapter!!.addAll(0, files.filter { it.isFolder || it.isMedia })
  }

  inner class GridPresenter : VerticalGridPresenter() {
    init {
      numberOfColumns = NUM_COLUMNS
    }
  }
}
