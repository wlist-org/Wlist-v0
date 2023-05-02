package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

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

    public static @NotNull KeyPair generateRsaKeyPair(final int keySize) {
        final KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        generator.initialize(keySize, (SecureRandom) HRandomHelper.RANDOM);
        return generator.generateKeyPair();
    }

    public static @NotNull Key generateAesKey(final long seed) {
        final byte[] key = new byte[32];
        new Random(seed).nextBytes(key);
        return new SecretKeySpec(key, "AES");
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

    /**
     * Automatically get not null connection with DataBaseUtil.
     * Standard code: @code {
     *  final Connection connection = MiscellaneousUtil.requireConnection(_connection, databaseUtil);
     *  try {
     *      // use connection.
     *  } finally {
     *      if (_connection == null)
     *          connection.close();
     *  }
     * }
     */
    public static @NotNull Connection requireConnection(final @Nullable Connection _connection, final @NotNull DataBaseUtil util) throws SQLException {
        if (_connection != null)
            return _connection;
        return util.getConnection();
    }
}
