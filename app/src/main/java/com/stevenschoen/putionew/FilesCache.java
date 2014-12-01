package com.stevenschoen.putionew;

import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stevenschoen.putionew.model.responses.CachedFilesListResponse;
import com.stevenschoen.putionew.model.responses.FilesListResponse;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FilesCache {
	private Context context;

	private File filesCacheDir;

	static Gson gson;
	static {
		gson = new GsonBuilder()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.create();
	}

	public FilesCache(Context context) {
		this.context = context;
		this.filesCacheDir = new File(context.getCacheDir() + File.separator + "filesCache");
		filesCacheDir.mkdirs();
	}

	public void cache(FilesListResponse response, long parentId) {
		File file = getFile(parentId);
		try {
			FileUtils.writeStringToFile(file, gson.toJson(response));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CachedFilesListResponse getCached(long parentId) {
		File file = getFile(parentId);
		try {
			return gson.fromJson(FileUtils.readFileToString(file), CachedFilesListResponse.class);
		} catch (FileNotFoundException e) {
//			Not cached yet, no problem
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private File getFile(long parentId) {
		return new File(filesCacheDir + File.separator + parentId + ".json");
	}
}
