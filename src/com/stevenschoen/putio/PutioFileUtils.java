package com.stevenschoen.putio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.stevenschoen.putio.activities.Putio;

public class PutioFileUtils {
	public static final int TYPE_AUDIO = 1;
	public static final int TYPE_VIDEO = -1;
	public static final String[] streamingMediaTypes = new String[] { "audio", "video" };
	
	public final String baseUrl = "https://api.put.io/v2/";

	private String token;
	private String tokenWithStuff;

	private SharedPreferences sharedPrefs;
	
	public PutioFileUtils(String token, SharedPreferences sharedPrefs) {
		this.token = token;
		this.tokenWithStuff = "?oauth_token=" + token;
		
		this.sharedPrefs = sharedPrefs;
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
	
	public boolean postDelete(Integer... ids) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		URI uri;
		InputStream data = null;
		try {
			uri = new URI(baseUrl + "files/delete" + tokenWithStuff);
			HttpPost method = new HttpPost(uri);

			ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			String idsString = null;
			for (int i = 0; i < ids.length; i++) {
				if (i == 0) {
					idsString = Integer.toString(ids[i]);
				} else {
					idsString = idsString + ", " + ids[i];
				}
			}
			nameValuePairs.add(new BasicNameValuePair("file_ids", idsString));

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

		class saveFileTask extends AsyncTask<Boolean[], Void, Boolean> {
			protected Boolean doInBackground(Boolean[]... updates) {
				Boolean saved = postRename(id, newName);
				return saved;
			}
		}
		new saveFileTask().execute(updates);
		
		if (hasUpdates) {
			Intent invalidateListIntent = new Intent(Putio.CUSTOM_INTENT1);
			context.sendBroadcast(invalidateListIntent);
		}
	}
	
	public void deleteFile(Context context, final Integer... ids) {
		class deleteFileTask extends AsyncTask<Void, Void, Boolean> {
			protected Boolean doInBackground(Void... nothing) {
				Boolean saved = postDelete(ids);
				return null;
			}
		}
		new deleteFileTask().execute();
		
		Intent invalidateListIntent = new Intent(Putio.CUSTOM_INTENT1);
		context.sendBroadcast(invalidateListIntent);
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
	public long downloadFile(final Context context, final int id, String filename) {
		final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(getFileDownloadUrl(id)));
		request.setDescription("put.io");
		if (UIUtils.hasHoneycomb()) {
		    request.allowScanningByMediaScanner();
		    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		
		String path = "put.io" + File.separator + id + File.separator + filename;
		File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + path);
		boolean made = file.mkdirs();
		
		request.setDestinationInExternalPublicDir(
				Environment.DIRECTORY_DOWNLOADS,
				path);

		// get download service and enqueue file
		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		long downloadId = manager.enqueue(request);
		
		sharedPrefs.edit().putLong("downloadId" + id, downloadId);
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
		streamIntent.setDataAndType(Uri.parse(url), typeString + "/*");
		
		context.startActivity(streamIntent);
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
//		try {
//			return resolveRedirect(baseUrl + "files/" + id + "/download" + tokenWithStuff);
//		} catch (ClientProtocolException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
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