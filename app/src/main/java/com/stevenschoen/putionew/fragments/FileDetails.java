package com.stevenschoen.putionew.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;
import com.stevenschoen.putionew.FlushedInputStream;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.cast.CastService.CastCallbacks;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFileData;
import com.stevenschoen.putionew.model.files.PutioMp4Status;
import com.stevenschoen.putionew.model.responses.FileResponse;
import com.stevenschoen.putionew.model.responses.Mp4StatusResponse;

import org.apache.http.util.ByteArrayBuffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

public class FileDetails extends Fragment {
    private PutioFileData origFileData;
    private PutioFileData newFileData;

    private static final String MP4_NOT_AVAILABLE = "NOT_AVAILABLE";
    private static final String MP4_AVAILABLE = "COMPLETED";
    private static final String MP4_IN_QUEUE = "IN_QUEUE";
    private static final String MP4_CONVERTING = "CONVERTING";
    private PutioMp4Status mp4Status;

	private TextView mp4Convert;
	private View mp4Available;
	private View mp4Converting;
    private TextView textPercent;

    private Bitmap imagePreviewBitmap;

    public interface Callbacks {
        public void onFDCancelled();
        public void onFDFinished();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onFDCancelled() { }
        @Override
        public void onFDFinished() { }
    };

    private static CastCallbacks sDummyCastCallbacks = new CastCallbacks() {
        @Override
		public void load(PutioFileData file, String url, PutioUtils utils) { }
    };

    private Callbacks mCallbacks = sDummyCallbacks;
    private CastCallbacks mCastCallbacks = sDummyCastCallbacks;

	private PutioUtils utils;

    private Handler handler;
	private Runnable updateMp4StatusRunnable = new Runnable() {
		@Override
		public void run() {
			utils.getJobManager().addJobInBackground(new PutioRestInterface.GetMp4StatusJob(
					utils, getFileId()));
		}
	};
	private boolean startedMp4StatusCheck = false;

    private TextView textFileCreatedDate;
    private TextView textFileCreatedTime;

    private EditText textFileName;
    private ImageView imagePreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            origFileData = savedInstanceState.getParcelable("origFileData");
            newFileData = savedInstanceState.getParcelable("newFileData");
        } else {
            origFileData = getArguments().getParcelable("fileData");
            newFileData = origFileData;
        }

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();

		utils.getEventBus().register(this);

		handler = new Handler();
	}

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int fileDetailsLayoutId = R.layout.filedetails;
		if (!UIUtils.hasHoneycomb()) {
			if (PutioUtils.dpFromPx(getActivity(), getResources().getDisplayMetrics().heightPixels) < 400) {
				fileDetailsLayoutId = R.layout.filedetailsgbhori;
			} else if (PutioUtils.dpFromPx(getActivity(), getResources().getDisplayMetrics().heightPixels) >= 400) {
				fileDetailsLayoutId = R.layout.filedetailsgbvert;
			} else {
				fileDetailsLayoutId = R.layout.filedetailsgbvert;
			}
		}

        final View view = inflater.inflate(fileDetailsLayoutId, container, false);

        if (UIUtils.isTablet(getActivity())) {
            view.setBackgroundResource(R.drawable.card_bg_r8);

            view.post(new Runnable() {
                @Override
                public void run() {
                    if (PutioUtils.dpFromPx(getActivity(), view.getHeight()) > 560) {
                        view.getLayoutParams().height =
                                (int) PutioUtils.pxFromDp(getActivity(), 560);
                    }

                    if (PutioUtils.dpFromPx(getActivity(), view.getWidth()) > 400) {
                        view.getLayoutParams().width =
                                (int) PutioUtils.pxFromDp(getActivity(), 400);
                    }

                    View parent = (View) view.getParent();
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) parent.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    parent.setLayoutParams(params);
                }
            });
        }

        textFileName = (EditText) view.findViewById(R.id.editText_fileName);
        textFileName.setText(origFileData.name);

        ImageButton btnUndoName = (ImageButton) view.findViewById(R.id.button_undoName);
        btnUndoName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textFileName.setText(origFileData.name);
            }

        });

        String[] created = PutioUtils.separateIsoTime(origFileData.createdAt);
        TextView textFileCreatedCreated = (TextView) view.findViewById(R.id.text_fileDetailsCreatedStatic);
        textFileCreatedCreated.setText(getString(R.string.created) + " ");

        textFileCreatedDate = (TextView) view.findViewById(R.id.text_fileDetailsCreatedDate);
        textFileCreatedDate.setText(created[0]);

        TextView textFileCreatedAt = (TextView) view.findViewById(R.id.text_fileDetailsCreatedStaticAt);
        textFileCreatedAt.setText(" " + getString(R.string.at) + " ");

        textFileCreatedTime = (TextView) view.findViewById(R.id.text_fileDetailsCreatedTime);
        textFileCreatedTime.setText(created[1]);

        Button btnApply = (Button) view.findViewById(R.id.button_filedetails_apply);
        btnApply.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                newFileData.name = textFileName.getText().toString();
                renameAndFinish();
            }
        });

		utils.getJobManager().addJobInBackground(new PutioRestInterface.GetFileJob(utils, getFileId()));

        Button btnCancel = (Button) view.findViewById(R.id.button_filedetails_cancel);
        btnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!UIUtils.isTablet(getActivity())) {
                    getActivity().finish();
                } else {
                    mCallbacks.onFDCancelled();
                }
            }

        });

        Button btnDownload = (Button) view.findViewById(R.id.button_filepreview_download);
        btnDownload.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                initActionFile(PutioUtils.ACTION_NOTHING);
            }
        });

        Button btnOpen = (Button) view.findViewById(R.id.button_filepreview_open);
        OnClickListener playMediaListener = new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String streamOrStreamMp4;
				if (mp4Status == null) {
					if (origFileData.isMp4Available) {
						streamOrStreamMp4 = "/mp4/stream";
					} else {
						streamOrStreamMp4 = "/stream";
					}
				} else {
					if (mp4Status.getStatus().equals(MP4_AVAILABLE)) {
						streamOrStreamMp4 = "/mp4/stream";
					} else {
						streamOrStreamMp4 = "/stream";
					}
				}

                String url = PutioUtils.baseUrl + "/files/"
                        + getFileId() + streamOrStreamMp4 + utils.tokenWithStuff;
                mCastCallbacks.load(newFileData, url, utils);
            }
        };

        OnClickListener openFileListener = new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                initActionFile(PutioUtils.ACTION_OPEN);
            }
        };

        final View filePreviewBox = view.findViewById(R.id.filedetailspreview);
        filePreviewBox.post(new Runnable() {

            @Override
            public void run() {
                if (PutioUtils.dpFromPx(getActivity(), filePreviewBox.getWidth()) > 460) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            (int) PutioUtils.pxFromDp(getActivity(), 460),
                            filePreviewBox.getHeight());
                    params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    filePreviewBox.setLayoutParams(params);
                }
            }
        });

        imagePreview = (ImageView) view.findViewById(R.id.image_filepreview_image);

        class getPreviewTask extends AsyncTask<Void, Void, Bitmap> {
            @Override
            protected Bitmap doInBackground(Void... nothing) {
                URL url = null;
                try {
                    url = new URL(newFileData.screenshot);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                ByteArrayBuffer baf = null;
                try {
                    URLConnection connection = url.openConnection();
                    FlushedInputStream fis = new FlushedInputStream(connection.getInputStream());
                    baf = new ByteArrayBuffer(100);
                    int current = 0;
                    while ((current = fis.read()) != -1) {
                        baf.append((byte) current);
                    }
                    fis.close();
                } catch (FileNotFoundException e) {
                    try {
                        url = new URL(origFileData.screenshot.replace(".jpg", "%3D%3D.jpg"));
                        URLConnection connection = url.openConnection();
                        FlushedInputStream fis = new FlushedInputStream(connection.getInputStream());
                        baf = new ByteArrayBuffer(100);
                        int current = 0;
                        while ((current = fis.read()) != -1) {
                            baf.append((byte) current);
                        }
                        fis.close();
                    } catch (FileNotFoundException ee) {
                        return null;
                    } catch (IOException ee) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return BitmapFactory.decodeByteArray(baf.toByteArray(), 0, baf.length());
            }

            @Override
            public void onPostExecute(final Bitmap bitmap) {
                if (bitmap != null) {
                    changeImagePreview(bitmap, true);
                }
                imagePreviewBitmap = bitmap;
            }
        }
        if (newFileData.screenshot != null && !newFileData.screenshot.equals("null")) {
            if (savedInstanceState != null && savedInstanceState.containsKey("imagePreviewBitmap")) {
                imagePreviewBitmap = savedInstanceState.getParcelable("imagePreviewBitmap");
                changeImagePreview(imagePreviewBitmap, false);
            } else {
                new getPreviewTask().execute();
            }
        }

        boolean isMedia = false;
        for (int i = 0; i < PutioUtils.streamingMediaTypes.length; i++) {
            if (newFileData.contentType.contains(PutioUtils.streamingMediaTypes[i])) {
                isMedia = true;

                btnOpen.setOnClickListener(playMediaListener);
                btnOpen.setText(getString(R.string.play));
            }
        }
        if (!isMedia) {
            btnOpen.setOnClickListener(openFileListener);
        }

		mp4Convert = (TextView) view.findViewById(R.id.button_filepreview_convert);
		mp4Convert.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				utils.getJobManager().addJobInBackground(new PutioRestInterface.PostConvertToMp4Job(
						utils, getFileId()));
				v.setEnabled(false);

				if (!startedMp4StatusCheck) {
					handler.post(updateMp4StatusRunnable);
					startedMp4StatusCheck = true;
				}
			}
		});

		mp4Available = view.findViewById(R.id.holder_filepreview_available);
		mp4Converting = view.findViewById(R.id.holder_filepreview_converting);

		utils.getJobManager().addJobInBackground(new PutioRestInterface.GetMp4StatusJob(
				utils, getFileId()));

        return view;
    }

    private void initActionFile(final int mode) {
        if (PutioUtils.idIsDownloaded(getFileId())) {
            final Dialog dialog = PutioUtils.PutioDialog(getActivity(), getString(R.string.redownloadtitle), R.layout.dialog_redownload);
            dialog.show();

            TextView textBody = (TextView) dialog.findViewById(R.id.text_redownloadbody);
            switch (mode) {
                case PutioUtils.ACTION_NOTHING:
                    textBody.setText(getString(R.string.redownloadfordlbody));
                    break;
                case PutioUtils.ACTION_OPEN:
                    textBody.setText(getString(R.string.redownloadforopenbody));
                    break;
				case PutioUtils.ACTION_SHARE:
					textBody.setText(getString(R.string.redownloadforsharebody));
					break;
            }

            Button buttonOpen = (Button) dialog.findViewById(R.id.button_redownload_open);
            if (mode == PutioUtils.ACTION_OPEN) {
                buttonOpen.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        PutioUtils.openDownloadedId(getFileId(), getActivity());
                        dialog.dismiss();
                    }
                });
            } else {
                buttonOpen.setVisibility(View.GONE);
            }

            Button buttonRedownload = (Button) dialog.findViewById(R.id.button_redownload_download);
            buttonRedownload.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    PutioUtils.deleteId(getFileId());
					utils.downloadFile(getActivity(), mode, newFileData);
                    dialog.dismiss();
                }
            });

            Button buttonCancel = (Button) dialog.findViewById(R.id.button_redownload_cancel);
            buttonCancel.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    dialog.cancel();
                }
            });
        } else {
			utils.downloadFile(getActivity(), mode, newFileData);
        }
    }

    private void initShareFile() {
        if (PutioUtils.idIsDownloaded(getFileId())) {
            PutioUtils.shareDownloadedId(getFileId(), getActivity());
        } else {
            utils.downloadFile(getActivity(), PutioUtils.ACTION_SHARE, newFileData);
        }
    }

    private void initDeleteFile() {
        utils.showDeleteFilesDialog(getActivity(), !UIUtils.isTablet(getActivity()), newFileData);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filedetails, menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_share:
				initShareFile();
				return true;
			case R.id.menu_delete:
				initDeleteFile();
				return true;
		}

		return false;
	}

	private void updateMp4Bar() {
        if (isAdded()) {
            switch (mp4Status.getStatus()) {
                case MP4_AVAILABLE:
                    mp4Convert.setVisibility(View.INVISIBLE);
                    mp4Available.setVisibility(View.VISIBLE);
                    mp4Converting.setVisibility(View.INVISIBLE);
                    break;
                case MP4_CONVERTING:
					mp4Convert.setVisibility(View.INVISIBLE);
					mp4Available.setVisibility(View.INVISIBLE);
					mp4Converting.setVisibility(View.VISIBLE);
                    break;
                case MP4_IN_QUEUE:
					mp4Convert.setVisibility(View.INVISIBLE);
					mp4Available.setVisibility(View.INVISIBLE);
					mp4Converting.setVisibility(View.VISIBLE);
                    break;
                case MP4_NOT_AVAILABLE:
					mp4Convert.setVisibility(View.VISIBLE);
					mp4Available.setVisibility(View.INVISIBLE);
					mp4Converting.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }

    private void changeImagePreview(final Bitmap bitmap, boolean animate) {
        if (animate) {
            animate(imagePreview).setDuration(250).rotationX(90f);
            imagePreview.postDelayed(new Runnable() {

                @Override
                public void run() {
                    changeImagePreview(bitmap, false);
                    ViewHelper.setRotationX(imagePreview, 270f);
                    animate(imagePreview).setDuration(250).rotationXBy(90f);
                    imagePreviewBitmap = bitmap;
                }

            }
                    , 500);
        } else {
            imagePreview.setScaleType(ScaleType.CENTER);
            imagePreview.setImageBitmap(bitmap);
        }
    }

    public void updatePercent(int percent) {
        textPercent.setText(Integer.toString(percent));
    }

	public void onEventMainThread(Mp4StatusResponse result) {
		mp4Status = result.getMp4Status();
		updateMp4Bar();

		if (!startedMp4StatusCheck && shouldCheckForMp4Updates()) {
			handler.postDelayed(updateMp4StatusRunnable, 5000);
			startedMp4StatusCheck = true;
		}
	}

	public void onEventMainThread(FileResponse result) {
		if (result != null) {
			newFileData = result.getFile();

			String[] created = PutioUtils.separateIsoTime(newFileData.createdAt);
			if (!created[0].equals(textFileCreatedDate.getText().toString()) || !created[1].equals(textFileCreatedTime.getText().toString())) {
				textFileCreatedDate.setText(created[0]);
				textFileCreatedTime.setText(created[1]);
			}
		}
	}

	private boolean shouldCheckForMp4Updates() {
		return (mp4Status.getStatus().equals(MP4_IN_QUEUE) ||
						mp4Status.getStatus().equals(MP4_CONVERTING));
	}

    public int getFileId() {
        return origFileData.id;
    }

    public String getOldFilename() {
        return origFileData.name;
    }

    public String getNewFilename() {
        return textFileName.getText().toString();
    }

	public void renameAndFinish() {
		utils.getJobManager().addJobInBackground(new PutioRestInterface.PostRenameFileJob(
				utils, getFileId(), getNewFilename()));
		if (!UIUtils.isTablet(getActivity())) {
			getActivity().finish();
		} else {
			mCallbacks.onFDFinished();
		}
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("origFileData", origFileData);
        outState.putParcelable("newFileData", newFileData);
        if (imagePreviewBitmap != null) {
            outState.putParcelable("imagePreviewBitmap", imagePreviewBitmap);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (UIUtils.isTablet(getActivity())) {
            mCallbacks = (Callbacks) activity;
        }
        mCastCallbacks = (CastCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = sDummyCallbacks;
        mCastCallbacks = sDummyCastCallbacks;
    }

	@Override
	public void onDestroy() {
		utils.getEventBus().unregister(this);
		handler.removeCallbacks(updateMp4StatusRunnable);

		super.onDestroy();
	}
}