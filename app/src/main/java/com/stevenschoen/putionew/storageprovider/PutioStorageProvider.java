package com.stevenschoen.putionew.storageprovider;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.files.PutioFileData;

import org.apache.commons.io.FileUtils;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class PutioStorageProvider extends DocumentsProvider {

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_ICON,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };

    private OkHttpClient client;
    private PutioUtils utils;

    @Override
    public boolean onCreate() {
        try {
            utils = new PutioUtils(getContext());
        } catch (PutioUtils.NoTokenException e) {
            e.printStackTrace();
            return false;
        }

        client = new OkHttpClient();

        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(resolveRootProjection(projection));

        if (utils == null) {
            return cursor;
        }

        final MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Root.COLUMN_ROOT_ID, "root");
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.putio));
        row.add(Root.COLUMN_SUMMARY, getContext().getString(R.string.files));
        row.add(Root.COLUMN_FLAGS,
                Root.FLAG_SUPPORTS_SEARCH);
        row.add(Root.COLUMN_DOCUMENT_ID, "0");
        row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);

        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));

        PutioFileData file = utils.getRestInterface().file(Integer.valueOf(documentId)).getFile();
        includeFile(cursor, file);

        return cursor;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));

        List<PutioFileData> files = utils.getRestInterface().files(Integer.valueOf(parentDocumentId)).getFiles();
        for (PutioFileData file : files) {
            includeFile(cursor, file);
        }

        return cursor;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        PutioFileData file = utils.getRestInterface().file(Integer.valueOf(documentId)).getFile();

        long dlId = utils.download(getContext(), file.id, false, file.name, Uri.parse(utils.getFileDownloadUrl(file.id)));

        DownloadManager dm = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(dlId);

        boolean done = false;

        while (!done) {
            if (signal != null && signal.isCanceled()) {
                break;
            }
            Cursor downloadCursor = dm.query(query);
            if (downloadCursor.moveToFirst()) {
                int indexStatus = downloadCursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = downloadCursor.getInt(indexStatus);

                if (status == DownloadManager.STATUS_SUCCESSFUL ||
                        status == DownloadManager.STATUS_FAILED) {
                    done = true;
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        return dm.openDownloadedFile(dlId);
                    }
                } else {
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        utils.getRestInterface().deleteFile(documentId);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        PutioFileData file = utils.getRestInterface().file(Integer.valueOf(documentId)).getFile();
        String url = file.icon;
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            File cacheFile = new File(getContext().getCacheDir() + "icon_" + file.id);
            FileUtils.copyInputStreamToFile(response.body().byteStream(), cacheFile);
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);

            return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    private void includeFile(MatrixCursor result, PutioFileData file)
            throws FileNotFoundException {

        int flags = 0;

        flags |= Document.FLAG_SUPPORTS_DELETE;

        if (file.icon != null) {
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        long createdAt = -1;
        createdAt = ISODateTimeFormat.localDateOptionalTimeParser().parseDateTime(file.createdAt).getMillis();

        String contentType = file.contentType;
        if (file.isFolder()) {
            contentType = Document.MIME_TYPE_DIR;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, file.id);
        row.add(Document.COLUMN_DISPLAY_NAME, file.name);
        row.add(Document.COLUMN_SIZE, file.size);
        if (createdAt != -1) {
            row.add(Document.COLUMN_LAST_MODIFIED, createdAt);
        } else {
            row.add(Document.COLUMN_LAST_MODIFIED, null);
        }
        row.add(Document.COLUMN_MIME_TYPE, contentType);
        row.add(Document.COLUMN_FLAGS, flags);
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }
}
