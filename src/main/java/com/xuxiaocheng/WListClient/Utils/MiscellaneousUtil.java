package com.xuxiaocheng.WListClient.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.random.RandomGenerator;

public final class MiscellaneousUtil {
    private MiscellaneousUtil() {
        super();
    }

    public static @NotNull String getMd5(final byte @NotNull [] source) {
        final MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        md5.update(source);
        final BigInteger i = new BigInteger(1, md5.digest());
        return i.toString(16);
    }

    public static @NotNull String getMd5(final @NotNull InputStream source) throws IOException {
        final MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        int remaining = source.available();
        final int size = Math.min(2048, remaining);
        int nr;
        for (final byte[] buffer = new byte[size]; remaining > 0L; remaining -= nr) {
            nr = source.read(buffer, 0, Math.min(size, remaining));
            if (nr < 0)
                break;
            md5.update(buffer);
        }
        final BigInteger i = new BigInteger(1, md5.digest());
        return i.toString(16);
    }

    public static void generateRandomByteArray(final @NotNull BigInteger seed, final byte @NotNull [] bytes) {
        final BigInteger[] div = seed.divideAndRemainder(BigInteger.valueOf(Long.MAX_VALUE<<1));
        new Random(div[0].longValue() ^ Integer.reverseBytes(div[1].intValue()) ^ div[0].bitLength()).nextBytes(bytes);
        final RandomGenerator random = new Random(div[1].longValue() ^ seed.getLowestSetBit() ^ Integer.reverseBytes(seed.bitLength()));
        for (int i = 0; i < bytes.length; ++i)
            bytes[i] = (byte) (bytes[i] ^ (byte) random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE));
    }
}
