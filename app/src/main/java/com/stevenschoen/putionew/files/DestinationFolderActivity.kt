package com.stevenschoen.putionew.files

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.stevenschoen.putionew.R

class DestinationFolderActivity : AppCompatActivity() {

    companion object {
        val RESULT_EXTRA_FOLDER = "folder"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.destination_activity)

        var fragment = getFragment()
        if (fragment == null) {
            fragment = Fragment.instantiate(this, DestinationFolderFragment::class.java.name, Bundle()) as DestinationFolderFragment
            supportFragmentManager.beginTransaction()
                    .add(R.id.destination_activity_root, fragment)
                    .commitNow()
        }

        val toolbarView = findViewById(R.id.destination_toolbar) as Toolbar
        setSupportActionBar(toolbarView)
        toolbarView.setNavigationIcon(R.drawable.ic_toolbarnav_close)
        toolbarView.setNavigationOnClickListener { finish() }

        findViewById(R.id.destination_cancel).setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        findViewById(R.id.destination_choose).setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra(RESULT_EXTRA_FOLDER, fragment!!.currentPage!!.file)
            })
            finish()
        }
    }

    fun getFragment() = supportFragmentManager.findFragmentById(R.id.destination_activity_root) as DestinationFolderFragment?

    override fun onBackPressed() {
        if (!getFragment()!!.goBack()) {
            super.onBackPressed()
        }
    }
}