package com.xuxiaocheng.WListClient.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class MiscellaneousUtil {
    private MiscellaneousUtil() {
        super();
    }

    public static final @NotNull Pattern md5Pattern = Pattern.compile("^[a-z0-9]{32}$");

    public static @NotNull MessageDigest getMd5Digester() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
    }

    public static @NotNull String getMd5(final @NotNull MessageDigest digester) {
        final BigInteger i = new BigInteger(1, digester.digest());
        return String.format("%32s", i.toString(16)).replace(' ', '0');
    }

    public static @NotNull String getMd5(final byte @NotNull [] source) {
        final MessageDigest md5 = MiscellaneousUtil.getMd5Digester();
        md5.update(source);
        return MiscellaneousUtil.getMd5(md5);
    }

    public static @NotNull String getSha256(final byte @NotNull [] source) {
        final MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        sha256.update(source);
        final BigInteger i = new BigInteger(1, sha256.digest());
        return String.format("%64s", i.toString(16)).replace(' ', '0');
    }

    public static void updateMessageDigest(final @NotNull MessageDigest digester, final @NotNull InputStream source) throws IOException {
        int remaining = source.available();
        final int size = Math.min(1 << 20, remaining);
        int nr;
        for (final byte[] buffer = new byte[size]; remaining > 0L; remaining -= nr) {
            nr = source.read(buffer, 0, Math.min(size, remaining));
            if (nr < 0)
                break;
            digester.update(buffer, 0, nr);
        }
    }

    public static <T> @NotNull Iterator<T> getEmptyIterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public @NotNull T next() {
                throw new NoSuchElementException();
            }
        };
    }

    public static <K, V> @NotNull K randomKeyAndPut(final @NotNull Map<? super @NotNull K, V> map, final @NotNull Supplier<? extends @NotNull K> randomKey, final V value) {
        K k;
        while (true) {
            k = randomKey.get();
            final boolean[] flag = {false};
            final K finalK = k;
            map.computeIfAbsent(finalK, (i) -> {
                flag[0] = true;
                return value;
            });
            if (flag[0])
                break;
        }
        return k;
    }

    public static Pair.@NotNull ImmutablePair<byte[], Integer> getCircleBytes(final byte @NotNull [] bytes, final int offset, final int len) {
        if (bytes.length < len)
            throw new IllegalStateException("Too short bytes." + ParametersMap.create().add("length", bytes.length).add("require", len));
        if (offset < bytes.length - len)
            return Pair.ImmutablePair.makeImmutablePair(bytes, offset);
        final int length = bytes.length - offset;
        final byte[] result = new byte[len];
        System.arraycopy(bytes, offset, result, 0, length);
        System.arraycopy(bytes, 0, result, length, len - length);
        return Pair.ImmutablePair.makeImmutablePair(result, 0);
    }

    public static @NotNull String bin(final byte b) {
        return String.format("0b%8s", Integer.toString(b < 0 ? b + 0xff : b, 2)).replace(' ', '0');
    }

    public static int calculatePartCount(final long total, final int limit) {
        //noinspection NumericCastThatLosesPrecision
        return (int) Math.ceil(((double) total) / limit);
    }
}
