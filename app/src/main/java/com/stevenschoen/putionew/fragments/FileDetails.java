package com.stevenschoen.putionew.fragments;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioApplication.CastCallbacks;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFileData;
import com.stevenschoen.putionew.model.files.PutioMp4Status;
import com.stevenschoen.putionew.model.responses.BasePutioResponse;
import com.stevenschoen.putionew.model.responses.FileResponse;
import com.stevenschoen.putionew.model.responses.Mp4StatusResponse;

import java.io.IOException;

public class FileDetails extends Fragment {
    private PutioFileData origFileData;
    private PutioFileData newFileData;

    private static final String MP4_NOT_AVAILABLE = "NOT_AVAILABLE";
    private static final String MP4_AVAILABLE = "COMPLETED";
    private static final String MP4_IN_QUEUE = "IN_QUEUE";
    private static final String MP4_CONVERTING = "CONVERTING";
    private static final String MP4_ALREADY = "internal_ALREADY";
    private PutioMp4Status mp4Status;

    private Toolbar toolbar;
    private TextView textTitle;

    private View infoMp4Already;
    private View infoMp4Available;
    private TextView textMp4Available;
    private CheckBox checkBoxMp4Available;
    private View infoMp4NotAvailable;
	private View infoMp4Converting;

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

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();

		utils.getEventBus().register(this);

		handler = new Handler();
	}

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.filedetails, container, false);

        if (UIUtils.isTablet(getActivity())) {
            ViewTreeObserver vto = view.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (isAdded()) {
                        int maxHeight = (int) getResources().getDimension(R.dimen.fileDetailsMaxHeight);
                        if (view.getHeight() > maxHeight) {
                            ViewGroup.LayoutParams params = view.getLayoutParams();
                            params.height = maxHeight;
                            view.setLayoutParams(params);
                        }
                    }
                }
            });
        }

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_filedetails);
        if (isAdded()) {
            if (UIUtils.isTablet(getActivity())) {
                toolbar.inflateMenu(R.menu.filedetails);
                if (newFileData.isMedia()) {
                    MenuItem itemOpen = toolbar.getMenu().findItem(R.id.menu_open);
                    itemOpen.setVisible(false);
                    itemOpen.setEnabled(false);
                }
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
                utils.renameFileDialog(getActivity(), newFileData, new PutioUtils.RenameCallback() {
                    @Override
                    public void onRename(PutioFileData file, String newName) {
                        newFileData.name = newName;
                        textTitle.setText(newName);
                    }
                }).show();
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

        infoMp4Already = holderInfo.findViewById(R.id.holder_fileinfo_mp4_already);
        infoMp4Available = holderInfo.findViewById(R.id.holder_fileinfo_mp4_available);
        infoMp4NotAvailable = holderInfo.findViewById(R.id.holder_fileinfo_mp4_notavailable);
        infoMp4Converting = holderInfo.findViewById(R.id.holder_fileinfo_mp4_converting);
        if (newFileData.isVideo()) {
            checkBoxMp4Available = (CheckBox) infoMp4Available.findViewById(R.id.checkbox_fileinfo_mp4);
            checkBoxMp4Available.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    refreshMp4View();
                }
            });
            textMp4Available = (TextView) infoMp4Available.findViewById(R.id.text_fileinfo_mp4);
            infoMp4NotAvailable.setOnClickListener(new OnClickListener() {
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
            refreshMp4View();
        } else {
            infoMp4Already.setVisibility(View.GONE);
            infoMp4Available.setVisibility(View.GONE);
            infoMp4NotAvailable.setVisibility(View.GONE);
            infoMp4Converting.setVisibility(View.GONE);
        }

        View infoAccessed = holderInfo.findViewById(R.id.holder_fileinfo_accessedat);
        TextView textAccessed = (TextView) infoAccessed.findViewById(R.id.text_fileinfo_accessedat);
        if (newFileData.isAccessed()) {
            String[] accessed = PutioUtils.parseIsoTime(getActivity(), newFileData.firstAccessedAt);
            textAccessed.setText(getString(R.string.accessed_on_x_at_x, accessed[0], accessed[1]));
        } else {
            textAccessed.setText(getString(R.string.never_accessed));
        }

        final View infoCreated = holderInfo.findViewById(R.id.holder_fileinfo_createdat);
        TextView textCreated = (TextView) infoCreated.findViewById(R.id.text_fileinfo_createdat);
        String[] created = PutioUtils.parseIsoTime(getActivity(), newFileData.createdAt);
        textCreated.setText(getString(R.string.created_on_x_at_x, created[0], created[1]));

        final View infoSize = holderInfo.findViewById(R.id.holder_fileinfo_size);
        TextView textSize = (TextView) infoSize.findViewById(R.id.text_fileinfo_size);
        textSize.setText(PutioUtils.humanReadableByteCount(newFileData.size, false));

        final View infoMore = holderInfo.findViewById(R.id.holder_fileinfo_more);
        if (newFileData.isMedia()) {
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
        if (newFileData.isMedia()) {
            PutioUtils.setupFab(buttonPlay);
            buttonPlay.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean mp4 = false;
                    if (mp4Status != null) {
                        if (mp4Status.getStatus().equals(MP4_AVAILABLE)) {
                            mp4 = checkBoxMp4Available.isChecked();
                        }
                    } else if (newFileData.isMp4Available) {
                        mp4 = checkBoxMp4Available.isChecked();
                    }

                    String url = newFileData.getStreamUrl(utils, mp4);
                    mCastCallbacks.load(newFileData, url, utils);
                }
            });
        } else {
            buttonPlay.setEnabled(false);
            buttonPlay.setVisibility(View.GONE);
        }

        ImageView imagePreviewStock = (ImageView) view.findViewById(R.id.image_filepreview_stock);
        Picasso.with(getActivity())
                .load(origFileData.icon)
                .transform(new PutioUtils.BlurTransformation(getActivity(), 4))
                .into(imagePreviewStock);

        imagePreview = (ImageView) view.findViewById(R.id.image_filepreview_image);

        class getPreviewTask extends AsyncTask<Void, Void, Bitmap> {
            @Override
            protected Bitmap doInBackground(Void... nothing) {
                try {
                    return Picasso.with(getActivity()).load(newFileData.screenshot).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            public void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    imagePreviewBitmap = bitmap;
                    refreshImagePreview(true);
                }
            }
        }
        if (newFileData.screenshot != null && !newFileData.screenshot.equals("null")) {
            if (savedInstanceState != null && savedInstanceState.containsKey("imagePreviewBitmap")) {
                imagePreviewBitmap = savedInstanceState.getParcelable("imagePreviewBitmap");
                refreshImagePreview(false);
            } else {
                new getPreviewTask().execute();
            }
        }

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
            case R.id.menu_open:
                initActionFile(PutioUtils.ACTION_OPEN);
                return true;
            case R.id.menu_download:
                initActionFile(PutioUtils.ACTION_NOTHING);
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

    private void initDeleteFile() {
        utils.showDeleteFilesDialog(getActivity(), !UIUtils.isTablet(getActivity()), newFileData);
    }

    private void refreshMp4View() {
        if (mp4Status != null) {
            switch (mp4Status.getStatus()) {
                case MP4_ALREADY: {
                    infoMp4Already.setVisibility(View.VISIBLE);
                    infoMp4Available.setVisibility(View.GONE);
                    infoMp4Converting.setVisibility(View.GONE);
                    infoMp4NotAvailable.setVisibility(View.GONE);
                    break;
                }
                case MP4_AVAILABLE: {
                    infoMp4Already.setVisibility(View.GONE);
                    infoMp4Available.setVisibility(View.VISIBLE);
                    infoMp4Converting.setVisibility(View.GONE);
                    infoMp4NotAvailable.setVisibility(View.GONE);

                    if (checkBoxMp4Available.isChecked()) {
                        textMp4Available.setText(getString(R.string.use_mp4));
                    } else {
                        textMp4Available.setText(getString(R.string.dont_use_mp4));
                    }
                    break;
                }
                case MP4_CONVERTING:
                case MP4_IN_QUEUE: {
                    infoMp4Already.setVisibility(View.GONE);
                    infoMp4Available.setVisibility(View.GONE);
                    infoMp4Converting.setVisibility(View.VISIBLE);
                    infoMp4NotAvailable.setVisibility(View.GONE);
                    break;
                }
                case MP4_NOT_AVAILABLE: {
                    infoMp4Already.setVisibility(View.GONE);
                    infoMp4Available.setVisibility(View.GONE);
                    infoMp4Converting.setVisibility(View.GONE);
                    infoMp4NotAvailable.setVisibility(View.VISIBLE);
                    break;
                }
            }
        } else {
            String status;
            if (newFileData.isMp4()) {
                status = MP4_ALREADY;
            } else if (newFileData.isMp4Available) {
                status = MP4_AVAILABLE;
            } else {
                status = MP4_NOT_AVAILABLE;
            }
            mp4Status = new PutioMp4Status(status);
            refreshMp4View();
        }
    }

    private void refreshImagePreview(boolean animate) {
        if (isAdded()) {
            Palette.generateAsync(imagePreviewBitmap, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Palette.Swatch darkMuted = palette.getDarkMutedSwatch();
                    if (darkMuted != null) {
                        if (UIUtils.hasLollipop() && !UIUtils.isTablet(getActivity())) {
                            ValueAnimator statusBarAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                                    getActivity().getWindow().getStatusBarColor(), darkMuted.getRgb());
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
                refreshImagePreview(false);
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
                imagePreview.setImageBitmap(imagePreviewBitmap);
            }
        }
    }

	public void onEventMainThread(Mp4StatusResponse result) {
		mp4Status = result.getMp4Status();
        if (newFileData.isMp4()) {
            mp4Status.setStatus(MP4_ALREADY);
        }
		refreshMp4View();

		if (!startedMp4StatusCheck && shouldCheckForMp4Updates()) {
			handler.postDelayed(updateMp4StatusRunnable, 5000);
			startedMp4StatusCheck = true;
		}
	}

	public void onEventMainThread(FileResponse result) {
		if (result != null && result.getFile().id == newFileData.id) {
			newFileData = result.getFile();
            textTitle.setText(newFileData.name);
		}
	}

    public void onEventMainThread(BasePutioResponse.FileChangingResponse result) {
        utils.getJobManager().addJobInBackground(new PutioRestInterface.GetFileJob(utils, getFileId()));
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