package com.stevenschoen.putio;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.TextView;

import com.stevenschoen.putio.activities.Putio;

public class PutioFileUtils {
	public final String baseUrl = "https://api.put.io/v2/";

	private String token;
	private String tokenWithStuff;

	public PutioFileUtils(String token) {
		this.token = token;
		this.tokenWithStuff = "?oauth_token=" + token;
	}

	public boolean postRename(int id, String newName) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		URI uri;
		InputStream data = null;
		try {
			uri = new URI(baseUrl + "files/rename" + tokenWithStuff);
			HttpPost method = new HttpPost(uri);

			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("file_id", Integer
					.toString(id)));
			nameValuePairs.add(new BasicNameValuePair("name", newName));

			method.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpClient.execute(method);
			data = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		try {
			JSONObject jsonResponse = new JSONObject(
					convertStreamToString(data));
			String responseCode = jsonResponse.getString("status");

			if (responseCode.matches("OK")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			return false;
		}
	}

	public void saveFileToServer(final Context context, final int id,
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

		if (hasUpdates) {
			Intent invalidateListIntent = new Intent(Putio.CUSTOM_INTENT1);
			context.sendBroadcast(invalidateListIntent);
		}

		class saveFileTask extends AsyncTask<Boolean[], Void, Boolean> {
			protected Boolean doInBackground(Boolean[]... updates) {
				Boolean saved = postRename(id, newName);
				return saved;
			}
		}
		new saveFileTask().execute(updates);
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

	public String convertStreamToString(InputStream is) {
		try {
			return new java.util.Scanner(is).useDelimiter("\\A").next();
		} catch (java.util.NoSuchElementException e) {
			return "";
		}
	}
	
	public InputStream getListJsonData(String url, int id) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		URI uri;
		InputStream data = null;
		try {
			if (id != 0) {
				uri = new URI(url + "&parent_id=" + id);
			} else {
				uri = new URI(url);
			}
			HttpGet method = new HttpGet(uri);
			
			HttpResponse response = httpClient.execute(method);
			data = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public InputStream getFileJsonData(String url) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		URI uri;
		InputStream data = null;
		try {
			uri = new URI(url);
			HttpGet method = new HttpGet(uri);
			HttpResponse response = httpClient.execute(method);
			data = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	@TargetApi(11)
	public void downloadFile(Context context, int id, String filename, String url) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
		request.setDescription("put.io");
		if (UIUtils.hasHoneycomb()) {
		    request.allowScanningByMediaScanner();
		    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationInExternalPublicDir(
				Environment.DIRECTORY_DOWNLOADS,
				"put.io" + File.separator
				+ id + File.separator
				+ filename);

		// get download service and enqueue file
		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
	}
	
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
				+ (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public Boolean stringToBooleanHack(String value) {
		if (value.matches("true")) {
			return true;
		} else if (value.matches("false")) {
			return false;
		}
		return false;
	};
}