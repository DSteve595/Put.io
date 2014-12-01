package com.stevenschoen.putionew.activities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.model.files.PutioFile;

import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;

public class AddTransfers extends FragmentActivity implements DestinationFilesDialog.Callbacks {
    public static final int TYPE_SELECTING = -1;
    public static final int TYPE_URL = 1;
    public static final int TYPE_FILE = 2;

    private int selectedType = TYPE_SELECTING;

    private Uri selectedFileUri;
    private String selectedUrl;

	private SharedPreferences sharedPrefs;

    private Button addButton;

    private View buttonChooseUrl, buttonChooseFile;
    private View holderSelectType, holderUrl, holderFile;
    private ImageButton cancelUrl, cancelFile;

    private EditText textUrl;
    private CheckBox checkBoxExtract;

    private TextView textFilename, textNotATorrent;

    private long destinationFolderId = 0;
    private TextView buttonDestination;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_addtransfer);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		String token = sharedPrefs.getString("token", null);
		if (token == null || token.isEmpty()) {
			Intent putioActivity = new Intent(this, Putio.class);
			startActivity(putioActivity);
			finish();
		}

        TextView textTitle = (TextView) findViewById(R.id.dialog_title);
        textTitle.setText(getString(R.string.add_transfers));

        addButton = (Button) findViewById(R.id.button_addtransfer_add);
        addButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                add();
			}
		});

		Button cancelButton = (Button) findViewById(R.id.button_addtransfer_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

        buttonDestination = (TextView) findViewById(R.id.button_addtransfer_destination);
        buttonDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DestinationFilesDialog destinationDialog = (DestinationFilesDialog)
                        DestinationFilesDialog.instantiate(AddTransfers.this, DestinationFilesDialog.class.getName());
                destinationDialog.show(getSupportFragmentManager(), "dialog");
            }
        });
        destinationFolderId = sharedPrefs.getInt("destinationFolderId", 0);
        String destinationFolderName = sharedPrefs.getString("destinationFolderName", null);
        if (destinationFolderName != null && !destinationFolderName.isEmpty()) {
            buttonDestination.setText(destinationFolderName);
        } else {
            buttonDestination.setText("Your Files");
        }

        buttonChooseUrl = findViewById(R.id.button_addtransfer_chooseurl);
        buttonChooseUrl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = TYPE_URL;
                updateView();
            }
        });

        buttonChooseFile = findViewById(R.id.button_addtransfer_choosefile);
        buttonChooseFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UIUtils.hasKitKat()) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("application/x-bittorrent");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, 0);
                } else {
                    Intent intent = new Intent(AddTransfers.this, FileChooserActivity.class);
                    startActivityForResult(intent, 0);
                }
            }
        });

        holderSelectType = findViewById(R.id.holder_addtransfer_selecttype);
        holderUrl = findViewById(R.id.holder_addtransfer_url);
        holderFile = findViewById(R.id.holder_addtransfer_file);

        cancelUrl = (ImageButton) findViewById(R.id.button_addtransfer_cancelurl);
        cancelUrl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = TYPE_SELECTING;
                updateView();
            }
        });
        cancelFile = (ImageButton) findViewById(R.id.button_addtransfer_cancelfile);
        cancelFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType = TYPE_SELECTING;
                updateView();
            }
        });

        textUrl = (EditText) findViewById(R.id.text_addtransfer_url);
        checkBoxExtract = (CheckBox) findViewById(R.id.checkbox_addtransfer_extract);

        textFilename = (TextView) findViewById(R.id.text_addtransfer_filename);
        textNotATorrent = (TextView) findViewById(R.id.text_addtransfer_notatorrent);

        if (getIntent().getAction() != null) {
            switch (getIntent().getScheme()) {
                case "http":
                case "https":
                case "magnet":
                    selectedType = TYPE_URL;
                    textUrl.setText(getIntent().getDataString());
                    break;
                case "file":
                    selectedType = TYPE_FILE;
                    onActivityResult(0, Activity.RESULT_OK, getIntent());
                    break;
            }
        }

        updateView();
	}

    private void updateView() {
        switch (selectedType) {
            case TYPE_SELECTING:
                addButton.setEnabled(false);
                holderSelectType.setVisibility(View.VISIBLE);
                holderUrl.setVisibility(View.GONE);
                holderFile.setVisibility(View.GONE);
                break;
            case TYPE_URL:
                addButton.setEnabled(true);
                holderSelectType.setVisibility(View.GONE);
                holderUrl.setVisibility(View.VISIBLE);
                holderFile.setVisibility(View.GONE);
                break;
            case TYPE_FILE:
                addButton.setEnabled(true);
                holderSelectType.setVisibility(View.GONE);
                holderUrl.setVisibility(View.GONE);
                holderFile.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void add() {
        switch (selectedType) {
            case TYPE_URL:
                selectedUrl = textUrl.getText().toString();
                addUrl();
                break;
            case TYPE_FILE:
                addFile();
                break;
        }
    }

	private void addUrl() {
		if (!selectedUrl.isEmpty()) {
			Intent addTransferIntent = new Intent(AddTransfers.this, TransfersActivity.class);
			addTransferIntent.putExtra("mode", TYPE_URL);
			addTransferIntent.putExtra("url", selectedUrl);
            addTransferIntent.putExtra("extract", checkBoxExtract.isChecked());
            addTransferIntent.putExtra("saveParentId", destinationFolderId);
			startActivity(addTransferIntent);
			finish();
		} else {
			Toast.makeText(AddTransfers.this, getString(R.string.nothingenteredtofetch), Toast.LENGTH_LONG).show();
		}
	}

	private void addFile() {
		if (selectedFileUri != null) {
            try {
                long size = getContentResolver()
                        .openFileDescriptor(selectedFileUri, "r").getStatSize();

                if (size <= FileUtils.ONE_MB) {
                    Intent addTransferIntent = new Intent(AddTransfers.this, TransfersActivity.class);
                    addTransferIntent.putExtra("mode", TYPE_FILE);
                    addTransferIntent.putExtra("torrenturi", selectedFileUri);
                    addTransferIntent.putExtra("parentId", destinationFolderId);
                    startActivity(addTransferIntent);
                    finish();
                } else {
                    Toast.makeText(AddTransfers.this, getString(R.string.filetoobig), Toast.LENGTH_LONG).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
		} else {
			Toast.makeText(AddTransfers.this, getString(R.string.nothingenteredtofetch), Toast.LENGTH_LONG).show();
		}
	}

    @Override
    public void onDestinationFolderSelected(PutioFile folder) {
        destinationFolderId = folder.id;
        buttonDestination.setText(folder.name);

        sharedPrefs.edit()
                .putLong("destinationFolderId", folder.id)
                .putString("destinationFolderName", folder.name)
                .apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        final Uri uri = data.getData();
                        try {
                            selectedFileUri = uri;
                            textFilename.setText(PutioUtils.getNameFromUri(AddTransfers.this, uri));
                            ContentResolver cr = getContentResolver();
                            String mimetype = cr.getType(uri);
                            if (mimetype == null) {
                                mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                        MimeTypeMap.getFileExtensionFromUrl(uri.getPath()));
                            }
                            if (mimetype.equals("application/x-bittorrent")) {
                                textNotATorrent.animate().alpha(0);
                            } else {
                                textNotATorrent.animate().alpha(1);
                            }
                            selectedType = TYPE_FILE;
                            updateView();
                        } catch (Exception e) {
                            Log.d("asdf", "File select error", e);
                        }
                    }
                }
                break;
        }
    }
}