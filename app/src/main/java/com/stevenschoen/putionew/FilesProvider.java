package com.stevenschoen.putionew;

import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.files.PutioFile;
import com.stevenschoen.putionew.model.responses.FilesListResponse;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class FilesProvider {

	private static final int MAX_CACHED_PER_RECEIVER = 12;

	private Context context;
	private PutioRestInterface restInterface;

	private List<Receiver> receivers = new ArrayList<>(1);
	private LinkedHashMap<Long, CacheEntry> cachedObservables = new LinkedHashMap<>(MAX_CACHED_PER_RECEIVER, 0.75f, true);
	private DiskCache diskCache;

	public FilesProvider(Context context, PutioRestInterface restInterface) {
		this.context = context;
		diskCache = new DiskCache();
		this.restInterface = restInterface;
	}

	public Observable<FilesResponse> getFiles(Receiver receiver, boolean refresh, long id) {
		cleanCache(id);

		CacheEntry mapEntry = cachedObservables.get(id);
		if (mapEntry != null) {
			if (refresh) {
				updateFromNetwork(mapEntry.observable, id);
			}
			return cachedObservables.get(id).observable;
		} else {
			BehaviorSubject<FilesResponse> observable = makeObservable(true, id);
			cachedObservables.put(id, new CacheEntry(observable, receiver));
			return observable;
		}
	}

	private void updateFromNetwork(final BehaviorSubject<FilesResponse> subject, final long id) {
		restInterface.files(id)
				.subscribe(new Action1<FilesListResponse>() {
					@Override
					public void call(FilesListResponse filesListResponse) {
						FilesResponse response = new FilesResponse();
						response.parent = filesListResponse.getParent();
						response.files = filesListResponse.getFiles();
						response.fresh = true;
						subject.onNext(response);
						diskCache.cache(filesListResponse, id);

						CacheEntry cacheEntry = cachedObservables.get(id);
						for (WeakReference<Receiver> receiverRef : cacheEntry.receivers) {
							Receiver receiver = receiverRef.get();
							if (receiver != null && receiver.getCurrentFolder().id == response.parent.id) {
								prefetchFolders(receiver, response.files);
								break;
							}
						}
					}
				});
	}

	private BehaviorSubject<FilesResponse> makeObservable(final boolean forceFetch, final long id) {
		final BehaviorSubject<FilesResponse> subject = BehaviorSubject.create();

		Observable.fromCallable(new Callable<FilesListResponse>() {
			@Override
			public FilesListResponse call() throws Exception {
				return diskCache.getCached(id);
			}
		}).subscribe(new Action1<FilesListResponse>() {
			@Override
			public void call(FilesListResponse filesListResponse) {
				if (filesListResponse != null) {
					FilesResponse response = new FilesResponse();
					response.parent = filesListResponse.getParent();
					response.files = filesListResponse.getFiles();
					response.fresh = false;
					subject.onNext(response);
					if (forceFetch) {
						updateFromNetwork(subject, id);
					}
				} else {
					updateFromNetwork(subject, id);
				}
			}
		});

		return subject;
	}

	private void prefetchFolders(Receiver receiver, List<PutioFile> files) {
		int foldersPrefetched = 0;
		for (int i = 0; i < files.size(); i++) {
			if (foldersPrefetched < 4) {
				PutioFile file = files.get(i);
				if (file.isFolder()) {
					CacheEntry mapEntry = cachedObservables.get(file.id);
					if (mapEntry == null) {
						BehaviorSubject<FilesResponse> observable = makeObservable(false, file.id);
						cachedObservables.put(file.id, new CacheEntry(observable, receiver));
						foldersPrefetched++;
					}
				}
			} else {
				break;
			}
		}
	}

	private void cleanCache() {
		cleanCache(-1);
	}

	private void cleanCache(long idToKeep) {
		receivers.removeAll(Collections.<Receiver>singleton(null));
		int maximum = MAX_CACHED_PER_RECEIVER * receivers.size();

		// Remove entries with no attached receivers
		Iterator<Map.Entry<Long, CacheEntry>> iterator = cachedObservables.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Long, CacheEntry> entry = iterator.next();
			boolean remove = true;
			for (WeakReference<Receiver> receiverRef : entry.getValue().receivers) {
				Receiver receiver = receiverRef.get();
				if (receiver != null) {
					remove = false;
					break;
				}
			}
			if (remove) {
				iterator.remove();
			}
		}

		int difference = cachedObservables.size() - maximum;
		if (difference > 0) {
			List<Long> idsToRemove = new ArrayList<>(difference);

			List<Long> idsInOldestAccessedOrder = new ArrayList<>(cachedObservables.keySet());
			for (long id : idsInOldestAccessedOrder) {
				if (idsToRemove.size() == difference) {
					break;
				} else {
					if (id != idToKeep) {
						boolean idIsInUse = false;
						for (WeakReference<Receiver> receiverRef : cachedObservables.get(id).receivers) {
							Receiver receiver = receiverRef.get();
							if (receiver != null) {
								PutioFile currentFolder = receiver.getCurrentFolder();
								if (id == currentFolder.id || id == currentFolder.parentId) {
									idIsInUse = true;
									break;
								}
							}
						}
						if (!idIsInUse) {
							idsToRemove.add(id);
						}
					}
				}
			}
			for (long idToRemove : idsToRemove) {
				cachedObservables.remove(idToRemove);
			}
		}
	}

	public void register(Receiver receiver) {
		receivers.add(receiver);
	}

	public void unregister(Receiver receiver) {
		receivers.remove(receiver);
		cleanCache();
	}

	public interface Receiver {
		PutioFile getCurrentFolder();
	}

	public static class FilesResponse {
		public boolean fresh;
		public PutioFile parent;
		public List<PutioFile> files;
	}

	public static class CacheEntry {
		public BehaviorSubject<FilesResponse> observable;
		public List<WeakReference<Receiver>> receivers;

		public CacheEntry(BehaviorSubject<FilesResponse> observable, List<WeakReference<Receiver>> receivers) {
			this.observable = observable;
			this.receivers = receivers;
		}

		public CacheEntry(BehaviorSubject<FilesResponse> observable, Receiver receiver) {
			this.observable = observable;
			receivers = new ArrayList<>(1);
			receivers.add(new WeakReference<>(receiver));
		}
	}

	private class DiskCache {
		private File filesCacheDir;

		Gson gson = new GsonBuilder()
					.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
					.create();

		public DiskCache() {
			filesCacheDir = new File(context.getCacheDir() + File.separator + "filesCache");
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

		public boolean isCached(long parentId) {
			return getFile(parentId).exists();
		}

		public FilesListResponse getCached(long parentId) {
			File file = getFile(parentId);
			try {
				return gson.fromJson(FileUtils.readFileToString(file), FilesListResponse.class);
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
}
