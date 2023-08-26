package com.xuxiaocheng.WListClientAndroid.Utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;

import java.io.File;

public final class UriHelper {
    private UriHelper() {
        super();
    }

    @NonNull public static File uri2File(@NonNull final Context context, @NonNull final Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            switch (uri.getAuthority()) {
                case "com.android.providers.media.documents" -> {
//                    final String[] split = DocumentsContract.getDocumentId(uri).split(":");
//                    return UriHelper.query(context, switch (split[0]) {
//                        case "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//                        case "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
//                        case "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//                        default -> null;
//                    }, BaseColumns._ID + "=?", new String[]{split[1]});
                }
                //noinspection SpellCheckingInspection
                case "com.android.externalstorage.documents" -> { // External Storage
                    final String[] split = DocumentsContract.getDocumentId(uri).split(":");
                    assert split.length == 2;
                    if ("primary".equals(split[0]))
                        return new File(Environment.getExternalStorageDirectory(), split[1]);
                    return new File("storage", split[0] + "/" + split[1]); // TODO: check
                }
                case "com.android.providers.downloads.documents" -> { // Downloads

                }
            }
        }
        return UriHelper.query(context, uri, null, null);
    }

    @NonNull private static File query(@NonNull final Context context, @NonNull final Uri uri, @Nullable final String selection, @Nullable final String[] selectionArgs) {
        String data = null;
        try (final Cursor cursor = context.getContentResolver().query(uri, new String[] {MediaStore.MediaColumns.DATA}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (index > -1)
                    data = cursor.getString(index);
            }
        }
        if (data == null)
            throw new IllegalStateException("Filed to get file path from Uri." + ParametersMap.create().add("uri", uri));
        return new File(data);
    }
}
