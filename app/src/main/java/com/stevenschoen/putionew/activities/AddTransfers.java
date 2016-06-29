package com.stevenschoen.putionew.activities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
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

import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.files.DestinationFolderActivity;
import com.stevenschoen.putionew.model.files.PutioFile;

import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;

public class AddTransfers extends AppCompatActivity {
    public static final int TYPE_SELECTING = -1;
    public static final int TYPE_URL = 1;
    public static final int TYPE_FILE = 2;

	public static final int REQUEST_DESTINATION_FOLDER = 1;
	public static final int REQUEST_CHOOSE_FILE = 2;

	public static final String EXTRA_STARTING_FOLDER = "starting_folder";

	private static final String STATE_DESTINATION_FOLDER = "dest_folder";

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

    private PutioFile destinationFolder;
    private TextView buttonDestination;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		String token = sharedPrefs.getString("token", null);
		if (token == null || token.isEmpty()) {
			Intent putioActivity = new Intent(this, Putio.class);
			startActivity(putioActivity);
			finish();
            return;
		}

        setContentView(R.layout.dialog_addtransfer);

		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_DESTINATION_FOLDER)) {
			destinationFolder = savedInstanceState.getParcelable(STATE_DESTINATION_FOLDER);
		} else if (getIntent().hasExtra(EXTRA_STARTING_FOLDER)) {
			destinationFolder = getIntent().getParcelableExtra(EXTRA_STARTING_FOLDER);
		}
		if (destinationFolder == null) {
			destinationFolder = PutioFile.makeRootFolder(getResources());
		}

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
				Intent destinationFolderIntent = new Intent(AddTransfers.this, DestinationFolderActivity.class);
				startActivityForResult(destinationFolderIntent, REQUEST_DESTINATION_FOLDER);
            }
        });
		buttonDestination.setText(destinationFolder.name);

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
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/x-bittorrent");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_CHOOSE_FILE);
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
            addTransferIntent.putExtra("saveParentId", destinationFolder.id);
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
                    addTransferIntent.putExtra("parentId", destinationFolder.id);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
			case REQUEST_DESTINATION_FOLDER: {
				if (resultCode == RESULT_OK && data != null) {
					PutioFile folder = data.getParcelableExtra(DestinationFolderActivity.Companion.getRESULT_EXTRA_FOLDER());
					destinationFolder = folder;
					buttonDestination.setText(folder.name);
				}
			} break;
            case REQUEST_CHOOSE_FILE: {
				if (resultCode == RESULT_OK && data != null) {
					final Uri uri = data.getData();
					try {
						selectedFileUri = uri;
						String filename = PutioUtils.getNameFromUri(AddTransfers.this, uri);
						textFilename.setText(filename);
						ContentResolver cr = getContentResolver();
						String mimetype = cr.getType(uri);
						if (mimetype == null) {
							mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
									MimeTypeMap.getFileExtensionFromUrl(uri.getPath()));
						}
						if ((mimetype != null && mimetype.equals("application/x-bittorrent")) || filename.endsWith("torrent")) {
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
			} break;
        }
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_DESTINATION_FOLDER, destinationFolder);
	}
}