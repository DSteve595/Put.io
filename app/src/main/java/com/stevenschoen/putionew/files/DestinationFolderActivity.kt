package com.stevenschoen.putionew.files

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.stevenschoen.putionew.R

class DestinationFolderActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.destination_activity)

    val fragment = getFragment() ?: (Fragment.instantiate(
        this,
        DestinationFolderFragment::class.java.name
    ) as DestinationFolderFragment).also {
      supportFragmentManager.beginTransaction()
          .add(R.id.destination_activity_root, it)
          .commitNow()
    }

    val toolbarView = findViewById<Toolbar>(R.id.destination_toolbar)
    setSupportActionBar(toolbarView)
    toolbarView.setNavigationIcon(R.drawable.ic_toolbar_nav_close)
    toolbarView.setNavigationOnClickListener { finish() }

    findViewById<View>(R.id.destination_cancel).setOnClickListener {
      setResult(RESULT_CANCELED)
      finish()
    }

    findViewById<View>(R.id.destination_choose).setOnClickListener {
      setResult(RESULT_OK, Intent().apply {
        putExtra(RESULT_EXTRA_FOLDER, (fragment.currentPage as FilesFragment.Page.File).file)
      })
      finish()
    }
  }

  private fun getFragment() = supportFragmentManager.findFragmentById(R.id.destination_activity_root) as DestinationFolderFragment?

  override fun onBackPressed() {
    if (!getFragment()!!.goBack(true)) {
      super.onBackPressed()
    }
  }

  companion object {
    const val RESULT_EXTRA_FOLDER = "folder"
  }
}
