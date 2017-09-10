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
import android.widget.Toast;

import com.akaita.java.rxjava2debug.RxJava2Debug;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Transformation;
import com.stevenschoen.putionew.files.FileDownload;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.ResponseOrError;
import com.stevenschoen.putionew.model.files.PutioFile;
import com.stevenschoen.putionew.model.files.PutioSubtitle;
import com.stevenschoen.putionew.model.responses.CreateZipResponse;
import com.stevenschoen.putionew.model.responses.ZipResponse;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

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
				.addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
				.addConverterFactory(GsonConverterFactory.create(gson))
				.build();
	}

	public PutioRestInterface getRestInterface() {
		return putioRestInterface;
	}

	public class NoTokenException extends Exception { }

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
		List<Single<Long>> downloadIds = new ArrayList<>(files.length);
		for (PutioFile file : files) {
			if (file.isFolder()) {
				downloadIds.add(downloadZipWithoutUrl(activity, file.getName() + ".zip", file.getId()));
			} else {
				downloadIds.add(downloadFileWithoutUrl(activity, file.getId(), file.getName()));
			}
		}

		Single.zip(downloadIds, new Function<Object[], long[]>() {
			@Override
			public long[] apply(@NonNull Object[] args) throws Exception {
				long[] downloadIds = new long[args.length];
				for (int i = 0; i < args.length; i++) {
					downloadIds[i] = (long) args[i];
				}
				return downloadIds;
			}
		}).subscribe(new Consumer<long[]>() {
			@Override
			public void accept(@NonNull long[] downloadIds) throws Exception {
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
		}, new Consumer<Throwable>() {
			@Override
			public void accept(@NonNull Throwable throwable) throws Exception {
				throwable.printStackTrace();
			}
		});
	}

	private Single<Long> downloadFileWithoutUrl(final Activity activity, final long fileId, String filename) {
		Uri uri = Uri.parse(getFileDownloadUrl(fileId));
		return download(activity, fileId, false, filename, uri);
	}

	private Single<Long> downloadZipWithoutUrl(final Activity activity, final String filename, final long... fileIds) {
		return getZipUrl(fileIds)
				.observeOn(AndroidSchedulers.mainThread())
				.flatMap(new Function<String, SingleSource<? extends Long>>() {
					@Override
					public SingleSource<? extends Long> apply(@NonNull String zipUrl) throws Exception {
						Toast.makeText(activity, R.string.downloading_zip, Toast.LENGTH_LONG).show();
						return download(activity, Uri.parse(zipUrl), filename);
					}
				});
	}

	private Single<Long> download(Activity activity, long fileId, boolean isZip, String filename, Uri uri) {
		String name;
		if (isZip) {
			name = filename;
		} else {
			name = fileId + File.separator + filename;
		}

		return download(activity, uri, name, fileId);
	}

	public static Single<Long> download(final Activity activity, final Uri uri, final String path) {
		return download(activity, uri, path, -1);
	}

	public static Single<Long> download(final Activity activity, final Uri uri, final String path, final long fileId) {
		return new RxPermissions(activity)
				.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
				.firstOrError()
				.map(new Function<Boolean, Long>() {
					@Override
					public Long apply(@NonNull Boolean granted) throws Exception {
						if (!granted) {
							throw new RuntimeException("Permission not granted");
						}

						String subPath = "put.io" + File.separator + path;
						File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + subPath);
						file.getParentFile().mkdirs();

						final DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
						DownloadManager.Request request = new DownloadManager.Request(uri);

						request.setDescription("put.io");
						request.allowScanningByMediaScanner();
						request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
						request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, subPath);

						final long downloadId = manager.enqueue(request);

						if (fileId != -1) {
							AsyncTask.execute(new Runnable() {
								@Override
								public void run() {
									PutioApplicationKt.putioApp(activity).getFileDownloadDatabase()
											.fileDownloadsDao()
											.insert(new FileDownload(fileId, downloadId,
													FileDownload.Status.InProgress, null));
								}
							});
						}

						return downloadId;
					}
				});
	}

	public Single<String> getZipUrl(long... fileIds) {
		return getRestInterface().createZip(longsToString(fileIds))
				.map(new Function<CreateZipResponse, Long>() {
					@Override
					public Long apply(@NonNull CreateZipResponse response) throws Exception {
						return response.getZipId();
					}
				}).flatMapPublisher(new Function<Long, Publisher<ZipResponse>>() {
					@Override
					public Publisher<ZipResponse> apply(@NonNull final Long zipId) throws Exception {
						return Single.defer(new Callable<SingleSource<ZipResponse>>() {
							@Override
							public SingleSource<ZipResponse> call() throws Exception {
								return getRestInterface().getZip(zipId);
							}
						}).repeatWhen(new Function<Flowable<Object>, Publisher<?>>() {
							@Override
							public Publisher<?> apply(@NonNull Flowable<Object> flowable) throws Exception {
								return flowable.delay(1, TimeUnit.SECONDS);
							}
						});
					}
				}).filter(new Predicate<ZipResponse>() {
					@Override
					public boolean test(@NonNull ZipResponse zipResponse) throws Exception {
						return zipResponse.getUrl() != null;
					}
				}).map(new Function<ZipResponse, String>() {
					@Override
					public String apply(@NonNull ZipResponse zipResponse) throws Exception {
						return zipResponse.getUrl();
					}
				}).firstOrError();
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
					result.subtitles = getRestInterface().subtitles(file.getId()).blockingGet().getSubtitles();
				} catch (RuntimeException e) {
					Throwable cause = e.getCause();
					if (cause instanceof UnknownHostException) {
						// No subtitles, not a problem
					} else {
						e.printStackTrace();
						if (context != null && context instanceof Activity) {
							((Activity) context).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show();
								}
							});
						}
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

	public void copyZipDownloadLink(final Context context, PutioFile... files) {
		long[] fileIds = new long[files.length];
		for (int i = 0; i < fileIds.length; i++) {
			fileIds[i] = files[i].getId();
		}
		getZipUrl(fileIds)
				.subscribe(new Consumer<String>() {
					@Override
					public void accept(String zipUrl) throws Exception {
						if (context != null) {
							copy(context, "Download link", zipUrl);
							Toast.makeText(context, context.getString(R.string.readytopaste),
									Toast.LENGTH_SHORT).show();
						}
					}
				});
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

	public Dialog removeTransferDialog(
			final Context context, final SingleObserver<ResponseOrError.BasePutioResponse> observer,
			final long... idsToDelete) {
		final Dialog removeDialog = showPutioDialog(context, context.getString(R.string.removetransfertitle), R.layout.dialog_removetransfer);

		Button removeRemove = (Button) removeDialog.findViewById(R.id.button_removetransfer_remove);
		removeRemove.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Single<ResponseOrError.BasePutioResponse> cancelObservable = getRestInterface()
						.cancelTransfer(PutioUtils.longsToString(idsToDelete))
						.observeOn(AndroidSchedulers.mainThread());
				if (observer != null) {
					cancelObservable.subscribe(observer);
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

	public static Throwable getRxJavaThrowable(Throwable throwable) {
		try {
			return RxJava2Debug.getEnhancedStackTrace(throwable);
		} catch (NullPointerException e) {
			e.printStackTrace();
			return throwable;
		}
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