package com.stevenschoen.putionew;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.path.android.jobqueue.JobManager;
import com.squareup.okhttp.OkHttpClient;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFileData;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import de.greenrobot.event.EventBus;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

public class PutioUtils {
    public static final int TYPE_AUDIO = 1;
    public static final int TYPE_VIDEO = 2;
    public static final String[] streamingMediaTypes = new String[]{"audio", "video"};

    public static final int ACTION_NOTHING = -1;
    public static final int ACTION_OPEN = 1;
    public static final int ACTION_SHARE = 2;

    public static final int ADDTRANSFER_FILE = 1;
    public static final int ADDTRANSFER_URL = 2;

    public static final String CAST_APPLICATION_ID = "E5977464"; // Styled media receiver
//    public static final String CAST_APPLICATION_ID = "C18ACC9E";
//	public static final String CAST_APPLICATION_ID = "2B3BFF06"; // Put.io's

	private PutioRestInterface putioRestInterface;
	private FilesCache filesCache;
	private JobManager jobManager;
	private EventBus eventBus;

    public String token;
    public String tokenWithStuff;

	public static final String baseUrl = "https://api.put.io/v2";

    private SharedPreferences sharedPrefs;

    public PutioUtils(Context context) throws NoTokenException {
		this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.token = sharedPrefs.getString("token", null);
		if (this.token == null) {
			throw new NoTokenException();
		}
        this.tokenWithStuff = "?oauth_token=" + token;

		Gson gson = new GsonBuilder()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.create();

		RestAdapter.Builder restAdapterBuilder = new RestAdapter.Builder()
				.setEndpoint(baseUrl)
//				.setLogLevel(RestAdapter.LogLevel.FULL)
				.setConverter(new GsonConverter(gson))
				.setClient(new OkClient(new OkHttpClient()))
				.setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						request.addQueryParam("oauth_token", token);
					}
				});
		RestAdapter restAdapter = restAdapterBuilder.build();
		this.putioRestInterface = restAdapter.create(PutioRestInterface.class);

		this.filesCache = new FilesCache(context);
		this.jobManager = new JobManager(context);
		this.eventBus = new EventBus();
	}

	public PutioRestInterface getRestInterface() {
		return putioRestInterface;
	}

	public FilesCache getFilesCache() {
		return filesCache;
	}

	public JobManager getJobManager() {
		return jobManager;
	}

	public EventBus getEventBus() {
		return eventBus;
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

    public static Dialog PutioDialog(Context context, String title, int contentViewId) {
        Dialog dialog = new Dialog(context, R.style.Putio_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(contentViewId);
        TextView textTitle = (TextView) dialog.findViewById(R.id.dialog_title);
        textTitle.setText(title);

        return dialog;
    }

    public static String convertStreamToString(InputStream is) {
        try {
            return new java.util.Scanner(is).useDelimiter("\\A").next();
        } catch (java.util.NoSuchElementException e) {
            return "";
        }
    }

    public static String getNameFromUri(Context context, Uri uri) {
        if (uri.getScheme().equals("file")) {
            return new File(uri.getPath()).getName();
        } else if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    public InputStream getNotificationsJsonData() throws SocketTimeoutException {
        URL url = null;
        try {
            url = new URL("http://stevenschoen.com/putio/notifications2.json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);

            return connection.getInputStream();
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException();
        } catch (IOException e) {
//			e.printStackTrace();
        }
        return null;
    }

    public void downloadFile(final Context context, final int actionWhenDone, final PutioFileData... files) {
        class downloadFileTaskCompat extends AsyncTask<Void, Integer, Long> {
            private boolean resolveRedirect = false;
            private Dialog dialog;

            @Override
            protected Long doInBackground(Void... params) {
				for (PutioFileData file : files) {
					long dlId;
					if (UIUtils.hasHoneycomb()) {
						if (file.isFolder()) {
							int[] folder = new int[]{file.id};
							dlId = downloadZipWithoutUrl(context, folder, file.name);
						} else {
							dlId = downloadFileWithoutUrl(context, file.id, file.name);
							return dlId;
						}
					} else {
						publishProgress(0);
						if (file.isFolder()) {
							int[] folder = new int[]{file.id};
							dlId = downloadZipWithUrl(context, folder, file.name,
									getZipDownloadUrl(folder).replace("https://", "http://"));
							return dlId;
						} else {
							try {
								dlId = downloadFileWithUrl(context, file.id, file.name,
										resolveRedirect(getFileDownloadUrl(file.id).replace("https://", "http://")));
								return dlId;
							} catch (IOException ee) {
								ee.printStackTrace();
							}
						}
					}
				}
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... nothing) {
                resolveRedirect = true;

                dialog = PutioDialog(context, context.getString(R.string.downloadpreparing), R.layout.dialog_loading);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }

            @Override
            protected void onPostExecute(Long dlId) {
                if (resolveRedirect) {
                    dialog.dismiss();
                }

                switch (actionWhenDone) {
                    case ACTION_OPEN:
                        Intent serviceOpenIntent = new Intent(context, PutioOpenFileService.class);
                        serviceOpenIntent.putExtra("downloadId", (long) dlId);
                        serviceOpenIntent.putExtra("id", files[0].id);
                        serviceOpenIntent.putExtra("filename", files[0].name);
                        serviceOpenIntent.putExtra("mode", actionWhenDone);
                        context.startService(serviceOpenIntent);
                        Toast.makeText(context, context.getString(R.string.downloadwillopen),
                                Toast.LENGTH_LONG).show();
                        break;
                    case ACTION_SHARE:
                        Intent serviceShareIntent = new Intent(context, PutioOpenFileService.class);
                        serviceShareIntent.putExtra("downloadId", (long) dlId);
                        serviceShareIntent.putExtra("id", files[0].id);
                        serviceShareIntent.putExtra("filename", files[0].name);
                        serviceShareIntent.putExtra("mode", actionWhenDone);
                        context.startService(serviceShareIntent);
                        Toast.makeText(context, context.getString(R.string.downloadwillshare), Toast.LENGTH_LONG).show();
                        break;
                    case ACTION_NOTHING:
                        Toast.makeText(context, context.getString(R.string.downloadstarted), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
        new downloadFileTaskCompat().execute();
    }

    private long downloadFileWithoutUrl(final Context context, final int id, String filename) {
        Uri uri = Uri.parse(getFileDownloadUrl(id));
        return download(context, id, false, filename, uri);
    }

    private long downloadFileWithUrl(final Context context, final int id, String filename, String url) {
        Uri uri = Uri.parse(url.replace("https://", "http://"));
        return download(context, id, false, filename, uri);
    }

    private long downloadZipWithoutUrl(final Context context, final int[] id, String filename) {
        Uri uri = Uri.parse(getZipDownloadUrl(id));
        return download(context, 0, true, filename + ".zip", uri);
    }

    private long downloadZipWithUrl(final Context context, final int[] id, String filename, String url) {
        Uri uri = Uri.parse(url.replace("https://", "http://"));
        return download(context, 0, true, filename + ".zip", uri);
    }

    @TargetApi(11)
    private long download(Context context, int id, boolean isZip, String filename, Uri uri) {
        if (idIsDownloaded(id) && !isZip) {
            deleteId(id);
        }

        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        String path = "put.io" + File.separator;
        if (isZip) {
            path += filename;
        } else {
            path += id + File.separator + filename;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + path);
        file.getParentFile().mkdirs();

        request.setDescription("put.io");
        if (UIUtils.hasHoneycomb()) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                path);

		return manager.enqueue(request);
    }

    public static void stream(Context context, String url, Uri[] subtitles, int type) {
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
		if (subtitles != null && subtitles.length > 0) {
			streamIntent.putExtra("subs", subtitles);
		}

        try {
            context.startActivity(streamIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.noactivityfound), Toast.LENGTH_LONG).show();
        }
    }

    public void getStreamUrlAndPlay(final Context context, final PutioFileData file, String url) {
        class GetStreamUrlAndPlay extends AsyncTask<String, Void, String> {
            Dialog gettingStreamDialog;

            @Override
            public void onPreExecute() {
                gettingStreamDialog = PutioUtils.PutioDialog(context,
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
            protected String doInBackground(String... params) {
                try {
                    return PutioUtils.resolveRedirect(params[0]);
                } catch (IOException e) {
//                    No redirect
					return params[0];
                }
            }

            @Override
            public void onPostExecute(String finalUrl) {
                if (gettingStreamDialog.isShowing()) {
                    gettingStreamDialog.dismiss();
                }
                int type;
                if (file.contentType.contains("audio")) {
                    type = PutioUtils.TYPE_AUDIO;
                } else if (file.contentType.contains("video")) {
                    type = PutioUtils.TYPE_VIDEO;
                } else {
                    type = PutioUtils.TYPE_VIDEO;
                }

				String subtitleUrl = PutioUtils.baseUrl + "/files/" + file.id +
						"/subtitles/default" + tokenWithStuff;
				Uri[] subtitles = new Uri[] { Uri.parse(subtitleUrl) };

                PutioUtils.stream(context, finalUrl, subtitles, type);
            }
        }

        new GetStreamUrlAndPlay().execute(url);
    }

    public InputStream getDefaultSubtitleData(int id) throws SocketTimeoutException {
        URL url = null;
        try {
            url = new URL(baseUrl + "/files/" + id + "/subtitles/default" + tokenWithStuff + "&format=webvtt");
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);

            return connection.getInputStream();
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void copyDownloadLink(final Context context, int id) {
        class GetDlLinkTask extends AsyncTask<Integer, Void, String> {
            Dialog dialog;

            @Override
            protected void onPreExecute() {
                dialog = PutioUtils.PutioDialog(context, context.getString(R.string.copyingdownloadlink), R.layout.dialog_loading);
                dialog.show();
            }

            @Override
            protected String doInBackground(Integer... fileId) {
                try {
                    return PutioUtils.resolveRedirect(baseUrl + "/files/" + fileId[0] + "/download"
                            + tokenWithStuff);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @SuppressLint({"NewApi", "ServiceCast"})
            @Override
            protected void onPostExecute(String result) {
                dialog.dismiss();
                if (result != null) {
                    if (UIUtils.hasHoneycomb()) {
                        android.content.ClipboardManager clip =
                                (android.content.ClipboardManager) context.getSystemService(
                                        Context.CLIPBOARD_SERVICE);
                        clip.setPrimaryClip(ClipData.newPlainText("Download link", result));
                    } else {
                        android.text.ClipboardManager clip =
                                (android.text.ClipboardManager) context.getSystemService(
                                        Context.CLIPBOARD_SERVICE);
                        clip.setText(result);
                    }
                    Toast.makeText(context, context.getString(R.string.readytopaste),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.couldntgetdownloadlink),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new GetDlLinkTask().execute(id);
    }

    public static String resolveRedirect(String url) throws IOException {
        HttpParams httpParameters = new BasicHttpParams();
        HttpClientParams.setRedirecting(httpParameters, false);

        HttpClient httpClient = new DefaultHttpClient(httpParameters);
        HttpGet httpget = new HttpGet(url);
        HttpContext context = new BasicHttpContext();

        HttpResponse response = httpClient.execute(httpget, context);

        // If we didn't get a '302 Found' we aren't being redirected.
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY)
            throw new IOException(response.getStatusLine().toString());

        org.apache.http.Header[] loc = response.getHeaders("Location");
        return loc.length > 0 ? loc[0].getValue() : null;
    }

    public String getFileDownloadUrl(int id) {
        return baseUrl + "/files/" + id + "/download" + tokenWithStuff;
    }

    public String getZipDownloadUrl(int[] id) {
        String ids = Integer.toString(id[0]);
        for (int i = 1; i < id.length; i++) {
            ids += "," + Integer.toString(id[i]);
        }
        return baseUrl + "/files/zip?file_ids=" + ids + tokenWithStuff.replace("?", "&"); // TODO hacky
    }

    public static boolean idIsDownloaded(int id) {
        String path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                + File.separator + "put.io" + File.separator + id;
        File file = new File(path);
        if (file.exists()) {
            if (file.list().length > 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void deleteId(int id) {
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

    public static void openDownloadedId(int id, Context context) {
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

    private static void share(Uri uri, Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setDataAndType(uri, "application/octet-stream");
        try {
            context.startActivity(Intent.createChooser(intent, uri.getLastPathSegment()));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.cantopenbecausetype), Toast.LENGTH_LONG).show();
        }
    }

    public static void shareDownloadedId(int id, Context context) {
        if (idIsDownloaded(id)) {
            String path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                    + File.separator + "put.io" + File.separator + id;
            File file = new File(path).listFiles()[0];
            Uri uri = Uri.fromFile(file);
            share(uri, context);
        } else {
            Toast.makeText(context, context.getString(R.string.filenotfound), Toast.LENGTH_LONG).show();
        }
    }

    public static void openDownloadedUri(Uri uri, Context context) {
        open(uri, context);
    }

    public static void shareDownloadedUri(Uri uri, Context context) {
        share(uri, context);
    }

    public void showDeleteFilesDialog(final Context context, final boolean finish, final PutioFileData... filesToDelete) {
        final Dialog deleteDialog = PutioDialog(context,
				context.getResources().getQuantityString(
						R.plurals.deletetitle, filesToDelete.length),
				R.layout.dialog_delete);

		TextView textDeleteBody = (TextView) deleteDialog.findViewById(R.id.text_delete_body);
		textDeleteBody.setText(context.getResources()
				.getQuantityString(R.plurals.deletebody, filesToDelete.length));

        deleteDialog.show();

        Button deleteDelete = (Button) deleteDialog.findViewById(R.id.button_delete_delete);
        deleteDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
				int[] idsToDelete = new int[filesToDelete.length];
				for (int i = 0; i < filesToDelete.length; i++) {
					idsToDelete[i] = filesToDelete[i].id;
				}
				getJobManager().addJobInBackground(new PutioRestInterface.PostDeleteFilesJob(
						PutioUtils.this, idsToDelete));
                Toast.makeText(context, context.getString(R.string.filedeleted), Toast.LENGTH_SHORT).show();
                deleteDialog.dismiss();

                if (finish) {
                    ((Activity) context).finish();
                }
            }
        });

        Button cancelDelete = (Button) deleteDialog.findViewById(R.id.button_delete_cancel);
        cancelDelete.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                deleteDialog.cancel();
            }
        });
    }

    public void showRemoveTransferDialog(final Context context, final int... idsToDelete) {
        final Dialog removeDialog = PutioDialog(context, context.getString(R.string.removetransfertitle), R.layout.dialog_removetransfer);
        removeDialog.show();

        Button removeRemove = (Button) removeDialog.findViewById(R.id.button_removetransfer_remove);
        removeRemove.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getJobManager().addJobInBackground(new PutioRestInterface.PostCancelTransferJob(
						PutioUtils.this, idsToDelete));
                Toast.makeText(context, context.getString(R.string.transferremoved), Toast.LENGTH_SHORT).show();
                removeDialog.dismiss();
            }
        });

        Button cancelRemove = (Button) removeDialog.findViewById(R.id.button_removetransfer_cancel);
        cancelRemove.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                removeDialog.cancel();
            }
        });
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
//		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
        String pre = ("KMGTPE").charAt(exp - 1)
//				+ (si ? "" : "i");
                + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String[] humanReadableByteCountArray(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return new String[]{Long.toString(bytes), "B"};
        int exp = (int) (Math.log(bytes) / Math.log(unit));
//		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
        String pre = ("KMGTPE").charAt(exp - 1)
//				+ (si ? "" : "i");
                + "";
        String one = String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
        return one.split(" ");
    }

    public static Boolean stringToBooleanHack(String value) {
        if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        }
        return false;
	}

	public static String intsToString(int... ints) {
		StringBuilder string = new StringBuilder();
		for (int i = 0; i < ints.length; i++) {
			int anInt = ints[i];
			string.append(anInt);
			if (i + 1 < ints.length) string.append(",");
		}

		return string.toString();
	}

    public static String[] separateIsoTime(String isoTime) {
        return isoTime.split("T");
    }

    public static float dpFromPx(Context context, float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static float pxFromDp(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}