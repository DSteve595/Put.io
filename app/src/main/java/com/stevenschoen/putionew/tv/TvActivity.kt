package com.stevenschoen.putionew.tv

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import com.stevenschoen.putionew.LoginActivity
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile
import rx.subjects.BehaviorSubject
import java.util.*

class TvActivity : FragmentActivity() {

    companion object {
        fun makeFolderFragTag(folder: PutioFile) = "folder_${folder.id}"
    }

    val displayedFolders = ArrayList<PutioFile>()
    val folders by lazy { BehaviorSubject.create<List<PutioFile>>(listOf(PutioFile.makeRootFolder(resources))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = application as PutioApplication
        if (application.isLoggedIn) {
            init()
        } else {
            val setupIntent = Intent(this, LoginActivity::class.java)
            startActivity(setupIntent)
            finish()
            return
        }
    }

    fun init() {
        setContentView(R.layout.tv_activity)

        val stackView = findViewById(R.id.tv_stack) as HorizontalStackLayout

        fun addFolder(folder: PutioFile) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.tv_stack, TvFolderFragment.newInstance(this@TvActivity, folder), makeFolderFragTag(folder))
                    .commitNow()
            displayedFolders.add(folder)
        }

        fun removeLastFolder() {
            val lastFolder = displayedFolders.last()
            supportFragmentManager.beginTransaction()
                    .remove(supportFragmentManager.findFragmentByTag(makeFolderFragTag(lastFolder)))
                    .commitNow()
            displayedFolders.removeAt(displayedFolders.lastIndex)
        }

        addFolder(PutioFile.makeRootFolder(resources))

        folders.subscribe { newFolders ->
            while (displayedFolders.size > newFolders.size) {
                removeLastFolder()
            }
            for ((index, newFolder) in newFolders.withIndex()) {
                if (index <= displayedFolders.lastIndex) {
                    val displayedFolder = displayedFolders[index]
                    if (newFolder.id != displayedFolder.id) {
                        (index..displayedFolders.lastIndex).forEach { removeLastFolder() }
                    }
                } else {
                    addFolder(newFolder)
                }
            }
            getLastFolderFragment().requestFocusWhenPossible = true
        }
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        if (fragment is TvFolderFragment) {
            fragment.onFolderSelected = { folder ->
                folders.onNext(folders.value.plus(folder))
            }
        }
    }

    fun getLastFolderFragment() = supportFragmentManager.findFragmentByTag(makeFolderFragTag(displayedFolders.last())) as TvFolderFragment

    override fun onBackPressed() {
        if (displayedFolders.size > 1) {
            val lastFolderFragment = supportFragmentManager.findFragmentByTag(makeFolderFragTag(displayedFolders.last()))
            if (lastFolderFragment != null && lastFolderFragment is TvFolderFragment) {
                folders.onNext(folders.value.dropLast(1))
            }
        } else {
            super.onBackPressed()
        }
    }
}
