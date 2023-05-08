package com.xuxiaocheng.WList.Utils;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class TomlUtil {
    private TomlUtil() {
        super();
    }

    public static <T> @NotNull T getOrSet(final @NotNull CommentedConfig toml, final @NotNull String key, final @NotNull T defaultValue, final @NotNull String defaultComment) {
        if (!toml.contains(key))
            toml.add(key, defaultValue);
        if (!toml.containsComment(key))
            toml.setComment(key, defaultComment);
        return toml.get(key);
    }

    public static int getOrSet(final @NotNull CommentedConfig toml, final @NotNull String key, final int intValue, final @NotNull String defaultComment) throws IOException {
        try {
            return TomlUtil.getOrSet(toml, key, Integer.valueOf(intValue), defaultComment).intValue();
        } catch (final RuntimeException exception) {
            throw new IOException("Need int value. key: " + key);
        }
    }

    // TODO: more types support.
}
