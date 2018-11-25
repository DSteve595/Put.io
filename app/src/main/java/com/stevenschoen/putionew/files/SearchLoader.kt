package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.getUniqueLoaderId
import com.stevenschoen.putionew.model.ResponseOrError
import com.stevenschoen.putionew.model.files.PutioFile
import com.stevenschoen.putionew.model.responses.FilesSearchResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class SearchLoader(context: Context, private val parentFolder: PutioFile, val query: String) : PutioBaseLoader(context) {

  private val searchSubject = BehaviorSubject.create<ResponseOrError>()
  fun search() = searchSubject.observeOn(AndroidSchedulers.mainThread())

  var searchSubscription: Disposable? = null
  fun refreshSearch(onlyIfEmpty: Boolean = false) {
    if (onlyIfEmpty && (searchSubject.value is FilesSearchResponse || isRefreshing())) return
    searchSubscription?.dispose()
    // TODO: Annoy put.io about adding parent folder as a param for searches
    searchSubscription = api.searchFiles(query).subscribe({ response ->
      searchSubscription = null
      searchSubject.onNext(response)
    }, { error ->
      searchSubscription = null
      searchSubject.onNext(ResponseOrError.NetworkError(error))
    })
  }

  fun isRefreshing() = searchSubscription != null && !searchSubscription!!.isDisposed

  companion object {
    fun get(loaderManager: LoaderManager, context: Context, parentFolder: PutioFile, query: String): SearchLoader {
      return loaderManager.initLoader(
          getUniqueLoaderId(SearchLoader::class.java), null, object : Callbacks(context) {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
          return SearchLoader(context, parentFolder, query)
        }
      }) as SearchLoader
    }
  }
}
