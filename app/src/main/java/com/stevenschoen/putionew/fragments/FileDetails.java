package com.stevenschoen.putionew.fragments;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.stevenschoen.putionew.FlushedInputStream;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioApplication.CastCallbacks;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
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

public class FileDetails extends Fragment {
    private PutioFileData origFileData;
    private PutioFileData newFileData;

    private static final String MP4_NOT_AVAILABLE = "NOT_AVAILABLE";
    private static final String MP4_AVAILABLE = "COMPLETED";
    private static final String MP4_IN_QUEUE = "IN_QUEUE";
    private static final String MP4_CONVERTING = "CONVERTING";
    private PutioMp4Status mp4Status;

    private boolean isMedia = false;

    private Toolbar toolbar;
    private TextView textTitle;

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

        for (int i = 0; i < PutioUtils.streamingMediaTypes.length; i++) {
            if (newFileData.contentType.contains(PutioUtils.streamingMediaTypes[i])) {
                isMedia = true;
            }
        }

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();

		utils.getEventBus().register(this);

		handler = new Handler();
	}

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.filedetails, container, false);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_filedetails);
        if (isAdded()) {
            if (UIUtils.isTablet(getActivity())) {
                toolbar.inflateMenu(R.menu.filedetails);
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return onOptionsItemSelected(item);
                    }
                });
            } else {
                ActionBarActivity activity = (ActionBarActivity) getActivity();
                activity.setSupportActionBar(toolbar);
                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }

        if (UIUtils.hasLollipop()) {
            View preview = view.findViewById(R.id.filedetailspreview);
            preview.setElevation(PutioUtils.pxFromDp(getActivity(), 2));
            toolbar.setElevation(getResources().getDimension(R.dimen.actionBarElevation));

            if (UIUtils.isTablet(getActivity())) {
                view.setElevation(getResources().getDimension(R.dimen.fileDetailsElevation));
            }
        }

        textTitle = (TextView) view.findViewById(R.id.text_filepreview_title);
        textTitle.setText(getOldFilename());
        textTitle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                utils.renameFileDialog(getActivity(), newFileData).show();
            }
        });

        if (UIUtils.isTablet(getActivity())) {
            if (UIUtils.hasLollipop()) {
                view.setBackgroundColor(Color.WHITE);
            } else {
                view.setBackgroundResource(R.drawable.card_bg_r8);
            }

//            view.post(new Runnable() {
//                @Override
//                public void run() {
//                    if (PutioUtils.dpFromPx(getActivity(), view.getHeight()) > 560) {
//                        view.getLayoutParams().height =
//                                (int) PutioUtils.pxFromDp(getActivity(), 560);
//                    }
//
//                    if (PutioUtils.dpFromPx(getActivity(), view.getWidth()) > 400) {
//                        view.getLayoutParams().width =
//                                (int) PutioUtils.pxFromDp(getActivity(), 400);
//                    }
//
//                    View parent = (View) view.getParent();
//                    ViewGroup.LayoutParams params = parent.getLayoutParams();
//                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//                    parent.setLayoutParams(params);
//                }
//            });
        }

        ViewGroup holderInfo = (ViewGroup) view.findViewById(R.id.holder_fileinfo);

        View infoMp4Available = holderInfo.findViewById(R.id.holder_fileinfo_mp4);
        if (newFileData.isVideo()) {
            TextView textMp4Available = (TextView) infoMp4Available.findViewById(R.id.text_fileinfo_mp4);
//            TODO mp4 status
        } else {
            infoMp4Available.setVisibility(View.GONE);
        }

        View infoAccessed = holderInfo.findViewById(R.id.holder_fileinfo_accessedat);
        TextView textAccessed = (TextView) infoAccessed.findViewById(R.id.text_fileinfo_accessedat);
        String[] accessed = PutioUtils.parseIsoTime(getActivity(), newFileData.firstAccessedAt);
        textAccessed.setText(getString(R.string.accessed_on_x_at_x, accessed[0], accessed[1]));

        final View infoCreated = holderInfo.findViewById(R.id.holder_fileinfo_createdat);
        TextView textCreated = (TextView) infoCreated.findViewById(R.id.text_fileinfo_createdat);
        String[] created = PutioUtils.parseIsoTime(getActivity(), newFileData.createdAt);
        textCreated.setText(getString(R.string.created_on_x_at_x, created[0], created[1]));

        final View infoSize = holderInfo.findViewById(R.id.holder_fileinfo_size);
        TextView textSize = (TextView) infoSize.findViewById(R.id.text_fileinfo_size);
        textSize.setText(PutioUtils.humanReadableByteCount(newFileData.size, false));

        final View infoMore = holderInfo.findViewById(R.id.holder_fileinfo_more);
        if (isMedia) {
            infoMore.setVisibility(View.VISIBLE);

            infoCreated.setVisibility(View.GONE);
            infoSize.setVisibility(View.GONE);

            infoMore.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    infoMore.setVisibility(View.GONE);

                    infoCreated.setVisibility(View.VISIBLE);
                    infoSize.setVisibility(View.VISIBLE);
                }
            });
        } else {
            infoMore.setVisibility(View.GONE);
        }

		utils.getJobManager().addJobInBackground(new PutioRestInterface.GetFileJob(utils, getFileId()));

        ImageButton buttonClose = (ImageButton) view.findViewById(R.id.button_filedetails_close);
        buttonClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (UIUtils.isTablet(getActivity())) {
                    mCallbacks.onFDCancelled();
                } else {
                    getActivity().finish();
                }
            }

        });

        View buttonPlay = view.findViewById(R.id.button_filedetails_play);
        PutioUtils.setupFab(buttonPlay);
        buttonPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });

        ImageView imagePreviewStock = (ImageView) view.findViewById(R.id.image_filepreview_stock);
        Picasso.with(getActivity())
                .load(origFileData.icon)
                .transform(new PutioUtils.BlurTransformation(getActivity(), 4))
                .into(imagePreviewStock);

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
                    setImagePreview(bitmap, true);
                }
                imagePreviewBitmap = bitmap;
            }
        }
        if (newFileData.screenshot != null && !newFileData.screenshot.equals("null")) {
            if (savedInstanceState != null && savedInstanceState.containsKey("imagePreviewBitmap")) {
                imagePreviewBitmap = savedInstanceState.getParcelable("imagePreviewBitmap");
                setImagePreview(imagePreviewBitmap, false);
            } else {
                new getPreviewTask().execute();
            }
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (!UIUtils.isTablet(getActivity())) {
            inflater.inflate(R.menu.filedetails, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_download:
                initActionFile(PutioUtils.ACTION_NOTHING);
                return true;
            case R.id.menu_share:
                initShareFile();
                return true;
            case R.id.menu_delete:
                initDeleteFile();
                return true;
        }
        return false;
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

    private void setImagePreview(final Bitmap bitmap, boolean animate) {
        if (isAdded()) {
            Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Palette.Swatch vibrant = palette.getVibrantSwatch();
                    if (vibrant != null) {
                        if (UIUtils.hasLollipop() && !UIUtils.isTablet(getActivity())) {
                            ValueAnimator statusBarAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                                    getActivity().getWindow().getStatusBarColor(), vibrant.getRgb());
                            statusBarAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    getActivity().getWindow().setStatusBarColor((Integer) animation.getAnimatedValue());
                                }
                            });
                            statusBarAnim.start();
                        }
                    }
                }
            });
            if (animate) {
                setImagePreview(bitmap, false);
                if (UIUtils.hasLollipop()) {
                    Animator anim = ViewAnimationUtils.createCircularReveal(imagePreview,
                            imagePreview.getWidth() / 2, imagePreview.getHeight() / 2,
                            0, Math.max(imagePreview.getWidth(), imagePreview.getHeight()));
                    anim.start();
                } else {
                    imagePreview.setAlpha(0f);
                    imagePreview.animate().alpha(1);
                }
            } else {
                imagePreview.setImageBitmap(bitmap);
            }
        }
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
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
		if (result != null && result.getFile().id == origFileData.id) {
			newFileData = result.getFile();
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
        return newFileData.name;
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