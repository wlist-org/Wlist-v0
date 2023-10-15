package com.xuxiaocheng.WList.Commons.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.constructor.ConstructYamlNull;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.schema.FailsafeSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @see ServerConfiguration#parse(InputStream) for example.
 */
public final class YamlHelper {
    private YamlHelper() {
        super();
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Object> normalizeMapNode(final @Nullable @UnmodifiableView Map<?, ?> config) {
        if (config == null)
            return Map.of();
        final Map<String, Object> map = new LinkedHashMap<>(config.size());
        for (final Map.Entry<?, ?> entry: config.entrySet())
            if (entry.getKey() != null && entry.getValue() != null)
                map.put(entry.getKey().toString(), entry.getValue());
        return Collections.unmodifiableMap(map);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Object> loadYaml(final @NotNull InputStream stream) throws IOException {
        final Object yaml = new Load(LoadSettings.builder().setParseComments(true).setSchema(new FailsafeSchema())
                .setTagConstructors(Map.of(Tag.NULL, new ConstructYamlNull())).build()).loadFromInputStream(stream);
        if (yaml == null)
            return Map.of();
        if (!(yaml instanceof LinkedHashMap<?, ?> map))
            throw new IOException("Invalid yaml root class." + ParametersMap.create().add("real", yaml.getClass().getName()).add("expected", (Supplier<String>) () -> "LinkedHashMap<?,?>"));
        return YamlHelper.normalizeMapNode(map);
    }

    public static void dumpYaml(final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Object> config, final @NotNull OutputStream stream) throws IOException {
        final Dump dumper = new Dump(DumpSettings.builder().setDumpComments(true).setDefaultFlowStyle(FlowStyle.BLOCK).build());
        stream.write(dumper.dumpToString(config).getBytes(StandardCharsets.UTF_8));
    }

    public static void throwErrors(final @NotNull Collection<? extends Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors) throws IOException {
        if (!errors.isEmpty()) {
            final StringBuilder builder = new StringBuilder().append('\n');
            for (final Pair.ImmutablePair<String, String> pair: errors)
                builder.append("In '").append(pair.getFirst()).append("': ").append(pair.getSecond()).append('\n');
            throw new IOException(builder.deleteCharAt(builder.length() - 1).toString());
        }
    }

    public static <T> @NotNull T getConfig(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> config, final @NotNull String key,
                                           final @NotNull T defaultValue, final @NotNull Function<@NotNull Object, ? extends @Nullable T> transfer) {
        final Object value = config.get(key);
        if (value == null)
            return defaultValue;
        final T transferred = transfer.apply(value);
        if (transferred == null)
            return defaultValue;
        return transferred;
    }

    public static <T> @NotNull T getConfig(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> config, final @NotNull String key,
                                           final @NotNull Supplier<? extends @NotNull T> defaultValue, final @NotNull Function<@NotNull Object, @Nullable T> transfer) {
        final Object value = config.get(key);
        if (value == null)
            return defaultValue.get();
        final T transferred = transfer.apply(value);
        if (transferred == null)
            return defaultValue.get();
        return transferred;
    }

    public static <T> @Nullable T getConfigNullable(final @NotNull @UnmodifiableView Map<? super @NotNull String, @NotNull Object> config, final @NotNull String key,
                                                    final @NotNull Function<@NotNull Object, @Nullable T> transfer) {
        final Object value = config.get(key);
        if (value == null || "null".equals(value))
            return null;
        return transfer.apply(value);
    }

    public static @Nullable String transferString(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot) {
        if (obj == null) return null;
        if (!(obj instanceof String str)) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require normal string." + ParametersMap.create().add("obj", obj)));
            return null;
        }
        return str;
    }

    public static @Nullable Boolean transferBooleanFromStr(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot) {
        final String str = YamlHelper.transferString(obj, errors, slot);
        if (str == null) return null;
        return Boolean.parseBoolean(str);
    }

    public static final @NotNull BigInteger IntegerMin = BigInteger.valueOf(Integer.MIN_VALUE);
    public static final @NotNull BigInteger IntegerMax = BigInteger.valueOf(Integer.MAX_VALUE);
    public static final @NotNull BigInteger LongMin = BigInteger.valueOf(Long.MIN_VALUE);
    public static final @NotNull BigInteger LongMax = BigInteger.valueOf(Long.MAX_VALUE);

    public static @Nullable BigInteger transferIntegerFromStr(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot, final @Nullable BigInteger min, final @Nullable BigInteger max) {
        final String str = YamlHelper.transferString(obj, errors, slot);
        if (str == null) return null;
        final BigInteger num;
        try {
            num = new BigInteger(str);
        } catch (final NumberFormatException exception) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require normal integer." + ParametersMap.create().add("str", str).add("exception", exception.getLocalizedMessage())));
            return null;
        }
        if (max != null && num.compareTo(max) > 0) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Exceed maximum limit." + ParametersMap.create().add("num", num).add("max", max)));
            return null;
        }
        if (min != null && num.compareTo(min) < 0) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Exceed minimum limit." + ParametersMap.create().add("num", num).add("min", min)));
            return null;
        }
        return num;
    }

    public static <E extends Enum<E>> @Nullable E transferEnumFromStr(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot, final @NotNull Class<E> enumClass) {
        final String str = YamlHelper.transferString(obj, errors, slot);
        if (str == null) return null;
        final E e;
        try {
            e = Enum.valueOf(enumClass, str);
        } catch (final IllegalArgumentException exception) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Unrecognized enum name." + ParametersMap.create().add("str", str).add("enum", enumClass.getName())));
            return null;
        }
        return e;
    }

    public static @Nullable ZonedDateTime transferDateTimeFromStr(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot, final @NotNull DateTimeFormatter formatter) {
        final String str = YamlHelper.transferString(obj, errors, slot);
        if (str == null) return null;
        try {
            return ZonedDateTime.parse(str, formatter);
        } catch (final DateTimeParseException exception) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require formatted data time." + ParametersMap.create().add("str", str).add("exception", exception.getLocalizedMessage()).add("formatter", formatter)));
            return null;
        }
    }

    public static @Nullable @UnmodifiableView Map<@NotNull String, @NotNull Object> transferMapNode(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot) {
        if (obj == null) return null;
        if (!(obj instanceof Map<?, ?> map)) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require map node." + ParametersMap.create().add("real", obj.getClass()).add("obj", obj)));
            return null;
        }
        return YamlHelper.normalizeMapNode(map);
    }

    public static @Nullable @UnmodifiableView List<@NotNull Object> transferListNode(final @Nullable Object obj, final @NotNull Collection<? super Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> errors, final @NotNull String slot) {
        if (obj == null) return null;
        if (!(obj instanceof ArrayList<?> list)) {
            errors.add(Pair.ImmutablePair.makeImmutablePair(slot, "Require list node." + ParametersMap.create().add("real", obj.getClass()).add("obj", obj)));
            return null;
        }
        return Collections.unmodifiableList(list);
    }
}
