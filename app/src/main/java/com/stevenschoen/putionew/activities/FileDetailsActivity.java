package com.stevenschoen.putionew.activities;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.fragments.FileDetails;
import com.stevenschoen.putionew.model.files.PutioFileData;

public class FileDetailsActivity extends BaseCastActivity {

    private FileDetails fileDetailsFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PutioFileData fileData = getIntent().getExtras().getParcelable("fileData");

        setContentView(R.layout.filedetailsphone);

        initCastBar();

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager
                    .beginTransaction();
            Bundle fileDetailsBundle = new Bundle();
            fileDetailsBundle.putParcelable("fileData", fileData);
            fileDetailsFragment = (FileDetails) FileDetails.instantiate(
                    this, FileDetails.class.getName(), fileDetailsBundle);
            fragmentTransaction.add(R.id.DetailsHolder, fileDetailsFragment);
            fragmentTransaction.commit();
        } else {
            fileDetailsFragment = (FileDetails) getFragmentManager().findFragmentById(R.id.DetailsHolder);
        }

        setTitle(fileData.name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                Intent homeIntent = new Intent(getApplicationContext(), Putio.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean shouldUpdateCastContext() {
        return (!getCastManager().hasContext());
    }
}