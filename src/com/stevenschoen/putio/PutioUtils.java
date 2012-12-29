package com.stevenschoen.putio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.stevenschoen.putio.activities.Putio;

public class PutioUtils {
	public static final int TYPE_AUDIO = 1;
	public static final int TYPE_VIDEO = -1;
	public static final String[] streamingMediaTypes = new String[] { "audio", "video" };
	
	public static final int ACTION_NOTHING = -1;
	public static final int ACTION_OPEN = 1;
	public static final int ACTION_SHARE = -2;
	
	public final static String baseUrl = "https://api.put.io/v2/";

	private String token;
	private static String tokenWithStuff;

	private SharedPreferences sharedPrefs;
	
	public PutioUtils(String token, SharedPreferences sharedPrefs) {
		this.token = token;
		this.tokenWithStuff = "?oauth_token=" + token;
		
		this.sharedPrefs = sharedPrefs;
	}

	private boolean postRename(int id, String newName) {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/rename" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			connection.setDoOutput(true);
			
			OutputStreamWriter output = new OutputStreamWriter(connection.getOutputStream());
		    output.write("file_id=" + id + "&name=" + URLEncoder.encode(newName, "UTF-8"));
		    output.flush();
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpsURLConnection.HTTP_OK) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private static boolean postDelete(Integer... ids) {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/delete" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			connection.setDoOutput(true);
			
			OutputStreamWriter output = new OutputStreamWriter(connection.getOutputStream());
			String idsString = null;
			for (int i = 0; i < ids.length; i++) {
				if (i == 0) {
					idsString = Integer.toString(ids[i]);
				} else {
					idsString = idsString + ", " + ids[i];
				}
			}
		    output.write("file_ids=" + idsString);
		    output.flush();
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpsURLConnection.HTTP_OK) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private static boolean postRemoveTransfer(Integer... ids) {
		URL url = null;
		try {
			url = new URL(baseUrl + "transfers/cancel" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			connection.setDoOutput(true);
			
			OutputStreamWriter output = new OutputStreamWriter(connection.getOutputStream());
			String idsString = null;
			for (int i = 0; i < ids.length; i++) {
				if (i == 0) {
					idsString = Integer.toString(ids[i]);
				} else {
					idsString = idsString + ", " + ids[i];
				}
			}
		    output.write("transfer_ids=" + idsString);
		    output.flush();
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpsURLConnection.HTTP_OK) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean postAddTransferByUrl(String urls) {
		URL url = null;
		try {
			url = new URL(baseUrl + "transfers/add" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			connection.setDoOutput(true);
			
			OutputStreamWriter output = new OutputStreamWriter(connection.getOutputStream());
		    output.write("url=" + urls);
		    output.flush();
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpsURLConnection.HTTP_OK) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean postAddTransferByFile(String filePath) {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/upload" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url.toString());
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("file", new FileBody(new File(filePath)));

			httppost.setEntity(reqEntity);

			HttpResponse response = httpclient.execute(httppost);
			
			int responseCode = response.getStatusLine().getStatusCode();
			if (responseCode == HttpsURLConnection.HTTP_OK) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean postConvert(int id) {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/" + id + "/mp4" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			connection.setDoOutput(true);
			
			connection.connect();
			
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpsURLConnection.HTTP_OK) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void applyFileToServer(final Context context, final int id,
			String origName, final String newName) {
		boolean updateName = false;
		if (!newName.matches(origName)) {
			updateName = true;
		}

		Boolean[] updates = new Boolean[] { Boolean.valueOf(updateName) };

		boolean hasUpdates = false;
		for (int i = 0; i < updates.length; i++) {
			if (updates[i]) {
				hasUpdates = true;
			}
		}

		class saveFileTask extends AsyncTask<Boolean[], Void, Boolean> {
			protected Boolean doInBackground(Boolean[]... updates) {
				Boolean saved = postRename(id, newName);
				return saved;
			}
		}
		new saveFileTask().execute(updates);
		
		if (hasUpdates) {
			Intent invalidateListIntent = new Intent(Putio.invalidateListIntent);
			context.sendBroadcast(invalidateListIntent);
		}
	}
	
	public static void deleteFileAsync(Context context, final Integer... ids) {
		class deleteFileTask extends AsyncTask<Void, Void, Boolean> {
			protected Boolean doInBackground(Void... nothing) {
				Boolean saved = postDelete(ids);
				return null;
			}
		}
		new deleteFileTask().execute();
		
		Intent invalidateListIntent = new Intent(Putio.invalidateListIntent);
		context.sendBroadcast(invalidateListIntent);
	}
	
	public static void removeTransferAsync(Context context, final Integer... ids) {
		class RemoveTransferTask extends AsyncTask<Void, Void, Boolean> {
			protected Boolean doInBackground(Void... nothing) {
				Boolean saved = postRemoveTransfer(ids);
				return null;
			}
		}
		new RemoveTransferTask().execute();
	}
	
	public void addTransfersByUrlAsync(final String urls) {
		class addTransferTask extends AsyncTask<Void, Void, Boolean> {
			protected Boolean doInBackground(Void... nothing) {
				Boolean saved = postAddTransferByUrl(urls);
				return null;
			}
		}
		new addTransferTask().execute();
	}
	
	public void addTransfersByFileAsync(final String filePath) {
		class addTransferTask extends AsyncTask<Void, Void, Boolean> {
			protected Boolean doInBackground(Void... nothing) {
				Boolean saved = postAddTransferByFile(filePath);
				return null;
			}
		}
		new addTransferTask().execute();
	}
	
	public void convertToMp4Async(int id) {
		class convertToMp4 extends AsyncTask<Integer, Void, Void> {
			
			@Override
			protected Void doInBackground(Integer... id) {
				postConvert(id[0]);
				return null;
			}
		}
		
		new convertToMp4().execute(id);
	}
	
	public Dialog confirmChangesDialog(Context context, String filename) {
		Dialog dialog = new Dialog(context, R.style.Putio_Dialog);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.dialog_confirmchanges);
		dialog.setTitle("Apply changes?");
		
		TextView text = (TextView) dialog.findViewById(R.id.text_confirmText);
		text.setText(String.format(context.getString(R.string.applychanges), filename));
		
		return dialog;
	}
	
	public static Dialog PutioDialog(Context context, String title, int contentViewId) {
		Typeface robotoLight = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
		
		Dialog dialog = new Dialog(context, R.style.Putio_Dialog);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(contentViewId);
		TextView textTitle = (TextView) dialog.findViewById(R.id.dialog_title);
		textTitle.setText(title);
		textTitle.setTypeface(robotoLight);
		
		return dialog;
	}

	public String convertStreamToString(InputStream is) {
		try {
			return new java.util.Scanner(is).useDelimiter("\\A").next();
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}
	
	public InputStream getFilesListJsonData(int id) throws SocketTimeoutException {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/list" + tokenWithStuff);
			if (id != 0) {
				url = new URL(url + "&parent_id=" + id);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			
			return connection.getInputStream();
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public InputStream getFilesSearchJsonData(String query)
			throws UnsupportedEncodingException, SocketTimeoutException {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/search/" + URLEncoder.encode(query, "UTF-8") + "/page/-1" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			
			return connection.getInputStream();
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public InputStream getFileJsonData(int id) throws SocketTimeoutException {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/" + id + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			
			return connection.getInputStream();
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public InputStream getNotificationsJsonData() throws SocketTimeoutException {
		URL url = null;
		try {
			url = new URL("http://stevenschoen.com/putio/notifications.json");
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
			e.printStackTrace();
		}
		return null;
	}
	
	public void downloadFile(final Context context, final int id, final String filename, final int actionWhenDone) {
		class downloadFileTaskCompat extends AsyncTask<Void, Integer, Long> {
			private boolean resolveRedirect = false;
			private Dialog dialog;
			
			@Override
			protected Long doInBackground(Void... params) {
				long dlId;
				if (UIUtils.hasHoneycomb()) {
					dlId = downloadFileWithoutUrl(context, id, filename);
					return dlId;
				} else {
					publishProgress(0);
					try {
						dlId = downloadFileWithUrl(context, id, filename,
								resolveRedirect(getFileDownloadUrl(id).replace("https://", "http://")));
						return dlId;
					} catch (ClientProtocolException ee) {
						ee.printStackTrace();
					} catch (IOException ee) {
						ee.printStackTrace();
					}
				}
				return null;
			}
			
			@Override
			protected void onProgressUpdate(Integer... nothing) {
				resolveRedirect = true;
				
				dialog = PutioDialog(context, "Preparing to download", R.layout.dialog_loading);
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
					serviceOpenIntent.putExtra("id", id);
					serviceOpenIntent.putExtra("filename", filename);
					serviceOpenIntent.putExtra("mode", actionWhenDone);
					context.startService(serviceOpenIntent);
					Toast.makeText(context, "Your file will open as soon as it is finished downloading.", Toast.LENGTH_LONG).show();
					break;
				case ACTION_SHARE:
					Intent serviceShareIntent = new Intent(context, PutioOpenFileService.class);
					serviceShareIntent.putExtra("downloadId", (long) dlId);
					serviceShareIntent.putExtra("id", id);
					serviceShareIntent.putExtra("filename", filename);
					serviceShareIntent.putExtra("mode", actionWhenDone);
					context.startService(serviceShareIntent);
					Toast.makeText(context, "Your file will be shared as soon as it is finished downloading.", Toast.LENGTH_LONG).show();
					break;
				case ACTION_NOTHING:
					Toast.makeText(context, "Download started.", Toast.LENGTH_SHORT).show();
					break;					
				}
			}
		}
		new downloadFileTaskCompat().execute();
	}
	
	private long downloadFileWithoutUrl(final Context context, final int id, String filename) {
		Uri uri = Uri.parse(getFileDownloadUrl(id));
		return download(context, id, filename, uri);
	}
	
	private long downloadFileWithUrl(final Context context, final int id, String filename, String url) {		
		Uri uri = Uri.parse(url.replace("https://", "http://"));
		return download(context, id, filename, uri);
	}
	
	@TargetApi(11)
	private long download(Context context, int id, String filename, Uri uri) {
		if (idIsDownloaded(id)) {
			deleteId(id);
		}
		
		final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request = new DownloadManager.Request(uri);
		
		String path = "put.io" + File.separator + id + File.separator + filename;
		File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + path);
		boolean made = file.getParentFile().mkdirs();
		
		request.setDescription("put.io");
		if (UIUtils.hasHoneycomb()) {
		    request.allowScanningByMediaScanner();
		    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationInExternalPublicDir(
				Environment.DIRECTORY_DOWNLOADS,
				path);

		long downloadId = manager.enqueue(request);
		return downloadId;
	}
	
	public void stream(Context context, String url, int type) {
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
		
		context.startActivity(streamIntent);
	}
	
	public InputStream getTransfersListJsonData() throws SocketTimeoutException {
		URL url = null;
		try {
			url = new URL(baseUrl + "transfers/list" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			
			return connection.getInputStream();
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public InputStream getMp4JsonData(int id) throws SocketTimeoutException {
		URL url = null;
		try {
			url = new URL(baseUrl + "files/" + id + "/mp4" + tokenWithStuff);
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
	
	public static String resolveRedirect(String url) throws ClientProtocolException, IOException {
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
		return baseUrl + "files/" + id + "/download" + tokenWithStuff;
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
			Toast.makeText(context, "The file could not be found. Was it deleted?", Toast.LENGTH_LONG).show();
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
			Toast.makeText(context, "The file could not be found. Was it deleted?", Toast.LENGTH_LONG).show();
		}
	}
	
	public static void openDownloadedUri(Uri uri, Context context) {
		open(uri, context);
	}
	
	public static void shareDownloadedUri(Uri uri, Context context) {
		share(uri, context);
	}
	
	public static void showDeleteFileDialog(final Context context, final int idToDelete) {
		final Dialog deleteDialog = PutioDialog(context, context.getString(R.string.deletetitle), R.layout.dialog_delete);
		deleteDialog.show();
		
		Button deleteDelete = (Button) deleteDialog.findViewById(R.id.button_delete_delete);
		deleteDelete.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				deleteFileAsync(context, idToDelete);
				Toast.makeText(context, context.getString(R.string.filedeleted), Toast.LENGTH_SHORT).show();
				deleteDialog.dismiss();
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
	
	public static void showRemoveTransferDialog(final Context context, final int idToDelete) {
		final Dialog removeDialog = PutioDialog(context, context.getString(R.string.removetransfertitle), R.layout.dialog_removetransfer);
		removeDialog.show();
		
		Button removeRemove = (Button) removeDialog.findViewById(R.id.button_removetransfer_remove);
		removeRemove.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				removeTransferAsync(context, idToDelete);
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
	
	public PutioAccountInfo getAccountInfo() throws SocketTimeoutException {
		URL url = null;
		try {
			url = new URL(baseUrl + "account/info" + tokenWithStuff);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(8000);
			
			String string = convertStreamToString(connection.getInputStream());
			try {
				JSONObject json = new JSONObject(string).getJSONObject("info");
				JSONObject disk = json.getJSONObject("disk");
				String username = null;
				String mail = null;
				long avail = 0;
				long used = 0;
				long size = 0;
				try {
					username = json.getString("username");
				} catch (JSONException e) {
				}
				try {
					mail = json.getString("mail");
				} catch (JSONException e) {
				}
				try {
					avail = disk.getLong("avail");
				} catch (JSONException e) {
				}
				try {
					used = disk.getLong("used");
				} catch (JSONException e) {
				}
				try {
					size = disk.getLong("size");
				} catch (JSONException e) {
				}
				return new PutioAccountInfo(
						username,
						mail,
						avail,
						used,
						size);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork == null) {
			return false;
		}
		return activeNetwork.isConnected();
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
			return new String[] { Long.toString(bytes), "B" };
		int exp = (int) (Math.log(bytes) / Math.log(unit));
//		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
		String pre = ("KMGTPE").charAt(exp - 1)
//				+ (si ? "" : "i");
				+ "";
		String one = String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
		return one.split(" ");
	}
	
	public Boolean stringToBooleanHack(String value) {
		if (value.matches("true")) {
			return true;
		} else if (value.matches("false")) {
			return false;
		}
		return false;
	};
	
	public static String[] separateIsoTime(String isoTime) {
		return isoTime.split("T");
	}
	
	public static float dpFromPx(Context context, float px)
	{
	    return px / context.getResources().getDisplayMetrics().density;
	}


	public static float pxFromDp(Context context, float dp)
	{
	    return dp * context.getResources().getDisplayMetrics().density;
	}
}