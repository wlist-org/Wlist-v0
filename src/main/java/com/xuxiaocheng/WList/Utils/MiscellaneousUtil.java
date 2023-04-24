package com.xuxiaocheng.WList.Utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
