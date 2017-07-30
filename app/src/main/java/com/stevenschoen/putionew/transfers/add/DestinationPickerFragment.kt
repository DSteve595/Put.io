package com.stevenschoen.putionew.transfers.add

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.files.DestinationFolderActivity
import com.stevenschoen.putionew.model.files.PutioFile
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject

class DestinationPickerFragment : Fragment() {

    private lateinit var destinationSubject: BehaviorSubject<PutioFile>

    val destination: PutioFile
        get() = destinationSubject.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val defaultDestination: PutioFile =
                if (savedInstanceState != null && savedInstanceState.containsKey(STATE_DESTINATION)) {
                    savedInstanceState.getParcelable(STATE_DESTINATION)
                } else {
                    PutioFile.makeRootFolder(resources)
                }
        destinationSubject = BehaviorSubject.create(defaultDestination)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.addtransfer_pick_destination, container, false)

        view.setOnClickListener {
            startActivityForResult(Intent(context, DestinationFolderActivity::class.java), REQUEST_PICK_NEW_DESTINATION)
        }

        val folderNameView = view.findViewById<TextView>(R.id.addtransfer_pick_destination_name)

        fun updateFolderNameView(newDestination: PutioFile) {
            folderNameView.text = newDestination.name
        }

        destinationSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateFolderNameView)

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            destinationSubject.onNext(data!!.extras.getParcelable(DestinationFolderActivity.RESULT_EXTRA_FOLDER))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(STATE_DESTINATION, destinationSubject.value)
    }

    companion object {
        const val REQUEST_PICK_NEW_DESTINATION = 1

        const val STATE_DESTINATION = "dest"
    }
}