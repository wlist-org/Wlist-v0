package com.xuxiaocheng.WListAndroid.Utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

final class FileUtil {
    private FileUtil() {
        super();
    }

    public static @Nullable File uri2file(final @NotNull Context context, final @NotNull Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            switch (uri.getAuthority()) {
                //noinspection SpellCheckingInspection
                case "com.android.externalstorage.documents" -> {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("primary:"))
                        return new File(Environment.getExternalStorageDirectory(), id.substring("primary:".length()));
                }
                case "com.android.providers.media.documents" -> {
                    final String id = DocumentsContract.getDocumentId(uri);
                    String path = null;
                    if (id.startsWith("image:"))
                        path = FileUtil.getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "_id=?", new String[]{id.substring("image:".length())});
                    if (id.startsWith("video:"))
                        path = FileUtil.getDataColumn(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "_id=?", new String[]{id.substring("video:".length())});
                    if (id.startsWith("audio:"))
                        path = FileUtil.getDataColumn(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "_id=?", new String[]{id.substring("audio:".length())});
                    if (id.startsWith("document:"))
                        path = FileUtil.getDataColumn(context, MediaStore.Files.getContentUri("external"), "_id=?", new String[]{id.substring("document:".length())});
                    if (path != null)
                        return new File(path);
                }
                case "com.android.providers.downloads.documents" -> {
                    final String id = DocumentsContract.getDocumentId(uri);
//                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/all_downloads"), Long.parseLong(id));
//                    final String path = FileUtil.getDataColumn(context, contentUri, null, null);
//                    if (path != null)
//                        return new File(path);
                }
            }
        }
        return null;
    }

    private static @Nullable String getDataColumn(final @NotNull Context context, final @NotNull Uri uri, final @Nullable String selection, final @NotNull String @Nullable [] selectionArgs) {
        final String column = MediaStore.MediaColumns.DATA;
        try (final Cursor cursor = context.getContentResolver().query(uri, new String[]{column}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndex(column);
                if (columnIndex >= 0)
                    return cursor.getString(columnIndex);
            }
        } catch (final IllegalArgumentException exception) {
            HLogManager.getInstance("DefaultLogger").log(HLogLevel.WARN, exception.getLocalizedMessage());
        }
        return null;
    }
}
