package com.stevenschoen.putionew.files

import android.content.Context
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.util.Log
import com.stevenschoen.putionew.PutioBaseLoader
import com.stevenschoen.putionew.model.files.PutioFile
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject

class SearchLoader(context: Context, private val parentFolder: PutioFile, val query: String) : PutioBaseLoader(context) {

    private val searchSubject = BehaviorSubject.create<List<PutioFile>>()
    fun search() = searchSubject.observeOn(AndroidSchedulers.mainThread())

    var searchSubscription: Subscription? = null
    fun refreshSearch(onlyIfEmpty: Boolean = false) {
        if (onlyIfEmpty && (searchSubject.hasValue() || isRefreshing())) return
        searchSubscription?.unsubscribe()
        // TODO: Annoy put.io about adding parent folder as a param for searches
        searchSubscription = api.searchFiles(query).subscribe({ response ->
            searchSubscription = null
            searchSubject.onNext(response.files)
        }, { error ->
            searchSubscription = null
            Log.d("asdf", "error: $error")
        })
    }

    fun isRefreshing(): Boolean {
        if (searchSubscription != null && !searchSubscription!!.isUnsubscribed) {
            return true
        } else {
            return false
        }
    }

    companion object {
        private val LOADER_ID = 372895

        fun get(loaderManager: LoaderManager, context: Context, parentFolder: PutioFile, query: String): SearchLoader {
            return loaderManager.initLoader(
                    LOADER_ID, null, object : Callbacks(context) {
                override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
                    return SearchLoader(context, parentFolder, query)
                }
            }) as SearchLoader
        }
    }
}