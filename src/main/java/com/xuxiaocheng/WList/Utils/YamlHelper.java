package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @see com.xuxiaocheng.WList.Server.GlobalConfiguration#init(java.io.File) for example.
 */
public final class YamlHelper {
    private YamlHelper() {
        super();
    }

    public static <T> @NotNull T checkConfig(final @NotNull Map<? super @NotNull String, @NotNull Object> config, final @NotNull String key, final @NotNull Object defaultValue, final @NotNull Function<@NotNull Object, @Nullable T> transfer) {
        final Object value = MiscellaneousUtil.resetNonNull(config, key, defaultValue);
        final T res = transfer.apply(value);
        if (res == null)
            return Objects.requireNonNull(transfer.apply(defaultValue));
        return res;
    }

    public static @Nullable String getString(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot) {
        if (!(obj instanceof String str)) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require normal text. obj: " + obj));
            return null;
        }
        return str;
    }

    public static @Nullable Boolean getBooleanFromStr(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot) {
        final String str = YamlHelper.getString(obj, errors, slot);
        if (str == null)
            return null;
        return Boolean.parseBoolean(str);
    }

    public static @Nullable Integer getIntegerFromStr(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot, final @Nullable Integer min, final @Nullable Integer max) {
        final String str = YamlHelper.getString(obj, errors, slot);
        if (str == null)
            return null;
        final int i;
        try {
            i = Integer.parseInt(str);
        } catch (final NumberFormatException exception) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require normal integer. str: '" + str + "' NumberFormatException: " + exception.getLocalizedMessage()));
            return null;
        }
        if (max != null && i > max.intValue()) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Exceed maximum limit. integer: " + i + " limit: " + max));
            return null;
        }
        if (min != null && i < min.intValue()) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Exceed minimum limit. integer: " + i + " limit: " + min));
            return null;
        }
        return i;
    }

    public static @Nullable Long getLongFromStr(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot, final @Nullable Long min, final @Nullable Long max) {
        final String str = YamlHelper.getString(obj, errors, slot);
        if (str == null)
            return null;
        final long l;
        try {
            l = Long.parseLong(str);
        } catch (final NumberFormatException exception) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require normal integer. str: '" + str + "' NumberFormatException: " + exception.getLocalizedMessage()));
            return null;
        }
        if (max != null && l > max.longValue()) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Exceed maximum limit. integer: " + l + " limit: " + max));
            return null;
        }
        if (min != null && l < min.longValue()) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Exceed minimum limit. integer: " + l + " limit: " + min));
            return null;
        }
        return l;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> @Nullable Map<K, V> getMap(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot) {
        if (!(obj instanceof Map<?, ?> map)) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require map node. obj: " + obj));
            return null;
        }
        return (Map<K, V>) map;
    }
}
