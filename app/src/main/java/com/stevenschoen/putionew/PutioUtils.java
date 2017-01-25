package com.stevenschoen.putionew;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Transformation;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFile;
import com.stevenschoen.putionew.model.files.PutioSubtitle;
import com.stevenschoen.putionew.model.responses.BasePutioResponse;
import com.tbruyelle.rxpermissions.RxPermissions;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;

public class PutioUtils {
	public static final int TYPE_AUDIO = 1;
	public static final int TYPE_VIDEO = 2;
	public static final String[] streamingMediaTypes = new String[]{"audio", "video"};

	public static final int ACTION_NOTHING = 1;
	public static final int ACTION_OPEN = 2;

	private PutioRestInterface putioRestInterface;

	public String token;
	public String tokenWithStuff;

	public static final String baseUrl = "https://api.put.io/v2/";
	public static final String uploadBaseUrl = "https://upload.put.io/v2/";

	private SharedPreferences sharedPrefs;

	public PutioUtils(Context context) throws NoTokenException {
		this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.token = sharedPrefs.getString("token", null);
		if (this.token == null) {
			throw new NoTokenException();
		}
		this.tokenWithStuff = "?oauth_token=" + token;

		this.putioRestInterface = makePutioRestInterface(baseUrl).create(PutioRestInterface.class);
	}

	public Retrofit makePutioRestInterface(String baseUrl) {
		Gson gson = new GsonBuilder()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.create();

		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
				.addInterceptor(new Interceptor() {
					@Override
					public Response intercept(Chain chain) throws IOException {
						Request request = chain.request();
						request = request.newBuilder()
								.url(request.url().newBuilder()
										.addQueryParameter("oauth_token", token)
										.build())
								.build();
						return chain.proceed(request);
					}
				});
		if (BuildConfig.DEBUG) {
			clientBuilder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS));
		}

		return new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(clientBuilder.build())
				.addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
				.addConverterFactory(GsonConverterFactory.create(gson))
				.build();
	}

	public PutioRestInterface getRestInterface() {
		return putioRestInterface;
	}

	public class NoTokenException extends Exception { }

	public Dialog confirmChangesDialog(Context context, String filename) {
		Dialog dialog = new Dialog(context, R.style.Putio_Dialog);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.dialog_confirmchanges);
		dialog.setTitle(context.getString(R.string.applychanges));

		TextView text = (TextView) dialog.findViewById(R.id.text_confirmText);
		text.setText(String.format(context.getString(R.string.applychanges), filename));

		return dialog;
	}

	public static Dialog showPutioDialog(Context context, String title, int contentViewId) {
		return new AlertDialog.Builder(context)
				.setTitle(title)
				.setView(contentViewId)
				.show();
	}

	public static String getNameFromUri(Context context, Uri uri) {
		if (uri.getScheme().equals("file")) {
			return new File(uri.getPath()).getName();
		} else if (uri.getScheme().equals("content")) {
			try (Cursor cursor = context.getContentResolver()
					.query(uri, null, null, null, null, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					return cursor.getString(
							cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
				}
			}
		}

		return null;
	}

	public void downloadFiles(final Activity activity, final int actionWhenDone, final PutioFile... files) {
		List<Observable<Long>> downloadIds = new ArrayList<>(files.length);
		for (PutioFile file : files) {
			if (file.isFolder()) {
				long[] folder = new long[]{file.getId()};
				downloadIds.add(downloadZipWithoutUrl(activity, folder, file.getName()));
			} else {
				downloadIds.add(downloadFileWithoutUrl(activity, file.getId(), file.getName()));
			}
		}

		Observable.zip(downloadIds, new FuncN<long[]>() {
			@Override
			public long[] call(Object... args) {
				long[] downloadIds = new long[args.length];
				for (int i = 0; i < args.length; i++) {
					downloadIds[i] = (long) args[i];
				}
				return downloadIds;
			}
		}).subscribe(new Action1<long[]>() {
			@Override
			public void call(long[] downloadIds) {
				switch (actionWhenDone) {
					case ACTION_OPEN:
						if (files.length > 1) {
							throw new IllegalArgumentException("Download started with ACTION_OPEN but more than one file: " + Arrays.toString(files));
						}
						Intent serviceOpenIntent = new Intent(activity, PutioOpenFileService.class);
						serviceOpenIntent.putExtra(PutioOpenFileService.EXTRA_DOWNLOAD_ID, downloadIds[0]);
						activity.startService(serviceOpenIntent);
						Toast.makeText(activity, activity.getString(R.string.downloadwillopen), Toast.LENGTH_LONG).show();
						break;
					case ACTION_NOTHING:
						Toast.makeText(activity, activity.getString(R.string.downloadstarted), Toast.LENGTH_SHORT).show();
						break;
				}
			}
		}, new Action1<Throwable>() {
			@Override
			public void call(Throwable throwable) {
				throwable.printStackTrace();
			}
		});
	}

	private Observable<Long> downloadFileWithoutUrl(final Activity activity, final long fileId, String filename) {
		Uri uri = Uri.parse(getFileDownloadUrl(fileId));
		return download(activity, fileId, false, filename, uri);
	}

	private Observable<Long> downloadZipWithoutUrl(final Activity activity, final long[] fileId, String filename) {
		Uri uri = Uri.parse(getZipDownloadUrl(fileId));
		return download(activity, 0, true, filename + ".zip", uri);
	}

	private Observable<Long> download(Activity activity, long fileId, boolean isZip, String filename, Uri uri) {
		if (idIsDownloaded(fileId) && !isZip) {
			deleteId(fileId);
		}

		String name;
		if (isZip) {
			name = filename;
		} else {
			name = fileId + File.separator + filename;
		}

		return download(activity, uri, name);
	}

	public static Observable<Long> download(final Activity activity, final Uri uri, final String path) {
		return Observable.create(new Observable.OnSubscribe<Long>() {
			@Override
			public void call(final Subscriber<? super Long> subscriber) {
				new RxPermissions(activity)
						.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
						.subscribe(new Action1<Boolean>() {
							@Override
							public void call(Boolean granted) {
								String subPath = "put.io" + File.separator + path;
								File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + subPath);
								file.getParentFile().mkdirs();

								final DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
								DownloadManager.Request request = new DownloadManager.Request(uri);

								request.setDescription("put.io");
								request.allowScanningByMediaScanner();
								request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
								request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, subPath);

								subscriber.onNext(manager.enqueue(request));
								subscriber.onCompleted();
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								subscriber.onError(throwable);
							}
						});
			}
		});
	}

	public void stream(Context context, PutioFile file, String url, List<PutioSubtitle> subtitles, int type) {
		Intent streamIntent = new Intent();
		streamIntent.setAction(Intent.ACTION_VIEW);
		String typeString;
		if (type == TYPE_AUDIO) {
			typeString = "audio";
		} else if (type == TYPE_VIDEO) {
			typeString = "video";
		} else {
			typeString = "video";
		}

		if (url == null) {
			Toast.makeText(context, context.getString(R.string.streamerror), Toast.LENGTH_LONG).show();
			return;
		}
		streamIntent.setDataAndType(Uri.parse(url), typeString + "/*");

		if (subtitles != null && !subtitles.isEmpty()) {
			Uri[] subtitleUris = new Uri[subtitles.size()];
			String[] subtitleNames = new String[subtitles.size()];

			for (int i = 0; i < subtitles.size(); i++) {
				PutioSubtitle subtitle = subtitles.get(i);
				subtitleUris[i] = Uri.parse(subtitle.getUrl(PutioSubtitle.FORMAT_SRT, file.getId(), tokenWithStuff));
				subtitleNames[i] = subtitle.getLanguage();
			}

			streamIntent.putExtra("subs", subtitleUris);
			streamIntent.putExtra("subs.name", subtitleNames);
		}

			try {
			context.startActivity(streamIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, context.getString(R.string.noactivityfound), Toast.LENGTH_LONG).show();
		}
	}

	public void getStreamUrlAndPlay(final Context context, final PutioFile file, String url) {
		class GetStreamUrlAndPlay extends AsyncTask<String, Void, GetStreamUrlAndPlayResult> {
			Dialog gettingStreamDialog;

			@Override
			public void onPreExecute() {
				gettingStreamDialog = PutioUtils.showPutioDialog(context,
						context.getString(R.string.gettingstreamurltitle),
						R.layout.dialog_loading);
				gettingStreamDialog.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						GetStreamUrlAndPlay.this.cancel(true);
					}
				});
				gettingStreamDialog.show();
			}

			@Override
			protected GetStreamUrlAndPlayResult doInBackground(String... params) {
				GetStreamUrlAndPlayResult result = new GetStreamUrlAndPlayResult();

				String finalUrl = params[0];
				try {
					finalUrl = resolveRedirect(params[0]);
				} catch (IOException e) {
					// No redirect, not a problem
				}
				result.url = finalUrl;

				try {
					result.subtitles = getRestInterface().subtitles(file.getId()).toBlocking().first().getSubtitles();
				} catch (RuntimeException e) {
					Throwable cause = e.getCause();
					if (cause instanceof UnknownHostException) {
						// No subtitles, not a problem
					} else {
						e.printStackTrace();
						Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show();
					}
				}

				return result;
			}

			@Override
			public void onPostExecute(GetStreamUrlAndPlayResult result) {
				try {
					if (gettingStreamDialog.isShowing()) {
						gettingStreamDialog.dismiss();
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
				int type;
				if (file.getContentType().contains("audio")) {
					type = PutioUtils.TYPE_AUDIO;
				} else if (file.getContentType().contains("video")) {
					type = PutioUtils.TYPE_VIDEO;
				} else {
					type = PutioUtils.TYPE_VIDEO;
				}

				stream(context, file, result.url, result.subtitles, type);
			}
		}

		new GetStreamUrlAndPlay().execute(url);
	}

	private static class GetStreamUrlAndPlayResult {
		String url;
		List<PutioSubtitle> subtitles;
	}

	public void copyDownloadLink(final Context context, PutioFile file) {
		String url = baseUrl + "files/" + file.getId() + "/download" + tokenWithStuff;
		copy(context, "Download link", url);
		Toast.makeText(context, context.getString(R.string.readytopaste),
				Toast.LENGTH_SHORT).show();
	}

	public void copyZipDownloadLink(Context context, PutioFile... files) {
		long[] fileIds = new long[files.length];
		for (int i = 0; i < files.length; i++) {
			fileIds[i] = files[i].getId();
		}
		String url = getZipDownloadUrl(fileIds);
		copy(context, "Download link", url);
		Toast.makeText(context, context.getString(R.string.readytopaste),
				Toast.LENGTH_SHORT).show();
	}

	public void copy(Context context, String label, String text) {
		android.content.ClipboardManager clip =
				(android.content.ClipboardManager) context.getSystemService(
						Context.CLIPBOARD_SERVICE);
		clip.setPrimaryClip(ClipData.newPlainText(label, text));
	}

	public static String resolveRedirect(String url) throws IOException {
		OkHttpClient client = new OkHttpClient.Builder()
				.followRedirects(true)
				.followSslRedirects(true)
				.build();
		Response response = client.newCall(new Request.Builder()
				.url(url).build()).execute();
		if (response.code() == 302) {
			return response.header("Location");
		}

		return url;
	}

	public String getFileDownloadUrl(long id) {
		return baseUrl + "files/" + id + "/download" + tokenWithStuff;
	}

	public String getZipDownloadUrl(long... ids) {
		String idsString = longsToString(ids);
		return baseUrl + "files/zip?file_ids=" + idsString + tokenWithStuff.replace("?", "&"); // TODO hacky
	}

	public static boolean idIsDownloaded(long id) {
		String path = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
				+ File.separator + "put.io" + File.separator + id;
		File file = new File(path);
		if (file.exists()) {
			return file.list().length > 0;
		} else {
			return false;
		}
	}

	public static void deleteId(long id) {
		String path = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
				+ File.separator + "put.io" + File.separator + id;
		File file = new File(path);
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void open(Uri uri, Context context) {
		String typename = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(typename);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(uri, type);
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, context.getString(R.string.cantopenbecausetype), Toast.LENGTH_LONG).show();
		}
	}

	public static void openDownloadedId(long id, Context context) {
		if (idIsDownloaded(id)) {
			String path = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
					+ File.separator + "put.io" + File.separator + id;
			File file = new File(path).listFiles()[0];
			Uri uri = Uri.fromFile(file);
			open(uri, context);
		} else {
			Toast.makeText(context, context.getString(R.string.filenotfound), Toast.LENGTH_LONG).show();
		}
	}

	public interface CreateFolderCallback {
		void onCreateFolderClicked();
		void onCreateFolderFinished();
	}

	public Dialog removeTransferDialog(final Context context, final Subscriber<BasePutioResponse> subscriber, final long... idsToDelete) {
		final Dialog removeDialog = showPutioDialog(context, context.getString(R.string.removetransfertitle), R.layout.dialog_removetransfer);

		Button removeRemove = (Button) removeDialog.findViewById(R.id.button_removetransfer_remove);
		removeRemove.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Observable<BasePutioResponse> cancelObservable = getRestInterface()
						.cancelTransfer(PutioUtils.longsToString(idsToDelete))
						.observeOn(AndroidSchedulers.mainThread());
				if (subscriber != null) {
					cancelObservable.subscribe(subscriber);
				} else {
					cancelObservable.subscribe();
				}
				Toast.makeText(context, context.getString(R.string.transferremoved), Toast.LENGTH_SHORT).show();
				removeDialog.dismiss();
			}
		});

		Button cancelRemove = (Button) removeDialog.findViewById(R.id.button_removetransfer_cancel);
		cancelRemove.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				removeDialog.cancel();
			}
		});

		return removeDialog;
	}

	public boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnected();
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = ("KMGTPE").charAt(exp - 1)
				+ "";
		return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static String longsToString(long... longs) {
		StringBuilder string = new StringBuilder();
		for (int i = 0; i < longs.length; i++) {
			long aLong = longs[i];
			string.append(aLong);
			if (i + 1 < longs.length) string.append(",");
		}

		return string.toString();
	}

	public static String intsToString(int... ints) {
		long[] longs = new long[ints.length];
		for (int i = 0; i < ints.length; i++) {
			longs[i] = ints[i];
		}

		return longsToString(longs);
	}

	public static String[] parseIsoTime(Context context, String isoTime) {
		String[] result = new String[2];

		DateTime created = new DateTime(isoTime);
		result[0] = DateFormat.getDateFormat(context).format(created.toDate());
		result[1] = DateFormat.getTimeFormat(context).format(created.toDate());

		return result;
	}

	public static float dpFromPx(Context context, float px) {
		return px / context.getResources().getDisplayMetrics().density;
	}

	public static float pxFromDp(Context context, float dp) {
		return dp * context.getResources().getDisplayMetrics().density;
	}

	public static void padForFab(View viewToPad) {
		Resources res = viewToPad.getResources();
		viewToPad.setPadding(
				viewToPad.getPaddingLeft(),
				viewToPad.getPaddingTop(),
				viewToPad.getPaddingRight(),
				(int) (res.getDimension(R.dimen.fabSize) + (res.getDimension(R.dimen.fabMargin)) * 1.5f));
	}

	public static class BlurTransformation implements Transformation {
		private Context context;
		private float radius;

		public BlurTransformation(Context context, float radius) {
			this.context = context;
			this.radius = radius;
		}

		@Override
		public Bitmap transform(Bitmap source) {
			if (Build.VERSION.SDK_INT < 17) {
				return source;
			}

			RenderScript rs = RenderScript.create(context);
			Allocation input = Allocation.createFromBitmap(rs, source, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
			Allocation output = Allocation.createTyped(rs, input.getType());
			ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
			script.setRadius(radius);
			script.setInput(input);
			script.forEach(output);
			output.copyTo(source);
			return source;
		}

		@Override
		public String key() {
			return BlurTransformation.class.getCanonicalName() + "-" + radius;
		}
	}
}