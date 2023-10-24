package com.xuxiaocheng.WListAndroid.Utils;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.UiThread;
import com.xuxiaocheng.WListAndroid.R;
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

    @UiThread
    public static void fadeIn(final @NotNull View view, final long time) {
        if (view.getVisibility() == View.VISIBLE) return;
        final Animation animation = new AlphaAnimation(0.0F, 1.0F);
        animation.setDuration(time);
        view.startAnimation(animation);
        view.setVisibility(View.VISIBLE);
    }

    @UiThread
    public static void fadeOut(final @NotNull View view, final long time) {
        if (view.getVisibility() != View.VISIBLE) return;
        final Animation animation = new AlphaAnimation(1.0F, 0.0F);
        animation.setDuration(time);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final @NotNull Animation animation) {
            }

            @Override
            public void onAnimationEnd(final @NotNull Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(final @NotNull Animation animation) {
            }
        });
        view.startAnimation(animation);
    }


    private static final float SizeFactor = 0.9F;
    private static final @NotNull String @NotNull [] SizeUnits = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    public static @NotNull String formatSize(final long size, final @NotNull String unknown) {
        if (size < 0) return unknown;
        int index = 0;
        float s = size;
        for (; s >= 1024 * ViewUtil.SizeFactor && index < ViewUtil.SizeUnits.length - 1; ++index)
            s /= 1024;
        return String.format(Locale.getDefault(), "%.2f %s", s, ViewUtil.SizeUnits[index]);
    }

    public static @NotNull String formatSizeDetail(final long size, final @NotNull String unknown) {
        return ViewUtil.formatSize(size, unknown) + (size <= 1024 * ViewUtil.SizeFactor ? "" : String.format(Locale.getDefault(), " (%d B)", size));
    }

    public static @NotNull String formatTime(final @Nullable ZonedDateTime time, final @NotNull String unknown) {
        if (time == null) return unknown;
        return time.toOffsetDateTime().atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()));
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
}
