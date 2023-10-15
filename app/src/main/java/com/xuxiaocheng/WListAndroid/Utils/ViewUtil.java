package com.xuxiaocheng.WListAndroid.Utils;

import android.widget.ImageView;
import android.widget.TextView;
import com.xuxiaocheng.WListAndroid.R;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public final class ViewUtil {
    private ViewUtil() {
        super();
    }

    public static @NotNull String getText(final @NotNull TextView textView) {
        return Objects.requireNonNullElse(textView.getText(), "").toString();
    }

    public static void setFileImage(final @NotNull ImageView image, final boolean isDirectory, final @NotNull String name) {
        if (isDirectory) {
            image.setImageResource(R.mipmap.page_file_image_directory);
            return;
        }
        final int index = name.lastIndexOf('.');
        // TODO: cached Drawable.
        image.setImageResource(switch (index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT)) {
            case "bat", "cmd", "sh", "run" -> R.mipmap.page_file_image_bat;
            case "doc", "docx" -> R.mipmap.page_file_image_docx;
            case "exe", "bin" -> R.mipmap.page_file_image_exe;
            case "jpg", "jpeg", "png", "bmp", "psd", "tga" -> R.mipmap.page_file_image_jpg;
            case "mp3", "flac", "wav", "wma", "aac", "ape" -> R.mipmap.page_file_image_mp3;
            case "ppt", "pptx" -> R.mipmap.page_file_image_pptx;
            case "txt", "log" -> R.mipmap.page_file_image_txt;
            case "xls", "xlsx" -> R.mipmap.page_file_image_xlsx;
            case "zip", "7z", "rar", "gz", "tar" -> R.mipmap.page_file_image_zip;
            default -> R.mipmap.page_file_image_file;
        });
    }

    @Contract("null -> null; !null -> !null")
    public static @Nullable String format(final @Nullable ZonedDateTime time) {
        if (time == null) return null;
        return time.toOffsetDateTime().atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()));
    }
}
