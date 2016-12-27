package com.stevenschoen.putionew.files

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.stevenschoen.putionew.R

class FileFinishedActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_URI = "uri"
        const val EXTRA_MEDIA_TYPE = "type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_filefinished)

        val name = intent.extras.getString(EXTRA_NAME)

        val messageView = findViewById(R.id.text_downloadfinished_body) as TextView
        messageView.text = String.format(getString(R.string.downloadfinishedbody), name)

        val openView = findViewById(R.id.button_filefinished_action) as Button
        openView.setOnClickListener {
            val uri = intent.extras.getParcelable<Uri>(EXTRA_URI)
            val type = intent.extras.getString(EXTRA_MEDIA_TYPE)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, type)
            }
            startActivity(Intent.createChooser(intent, null))

            finish()
        }

        val okView = findViewById(R.id.button_filefinished_ok) as Button
        okView.setOnClickListener { finish() }
    }
}