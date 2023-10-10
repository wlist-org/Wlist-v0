package com.xuxiaocheng.WListClientAndroid.Utils;

import android.widget.TextView;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ViewUtil {
    private ViewUtil() {
        super();
    }

    public static @NotNull String getText(final @NotNull TextView textView) {
        return Objects.requireNonNullElse(textView.getText(), "").toString();
    }
}
