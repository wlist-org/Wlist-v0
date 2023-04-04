package com.xuxiaocheng.WList.Server.Helper;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Utils.SQLiteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class TokenHelper {
    private TokenHelper() {
        super();
    }

    private static final long ExpiredTime = TimeUnit.DAYS.toMillis(1);
    //                                token                                        username         create_time
    private static final @NotNull Map<@NotNull String, Pair.@NotNull ImmutablePair<@NotNull String, @NotNull Long>> TokensCache = new ConcurrentHashMap<>();

    public static void init() throws SQLException {
        SQLiteUtil.getDataInstance().getLock("tokens").writeLock().lock();
        try (final Statement statement = SQLiteUtil.getDataInstance().createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tokens (
                        token TEXT    PRIMARY KEY
                                      UNIQUE
                                      NOT NULL,
                        user  TEXT    REFERENCES users (username)
                                      NOT NULL,
                        time  INTEGER NOT NULL
                    );
                    """);
        } finally {
            SQLiteUtil.getDataInstance().getLock("tokens").writeLock().unlock();
        }
        TokenHelper.clearExpiredTokens();
    }

    public static void addToken(final @NotNull String token, final @NotNull String username, final long time) throws SQLException {
        final Pair.ImmutablePair<String, Long> old = TokenHelper.TokensCache.putIfAbsent(token, Pair.ImmutablePair.makeImmutablePair(token, time));
        if (old != null && !username.equals(old.getFirst()))
            throw new SQLException("Token is existed!");
        TokenHelper.insertToken(token, username, time);
    }

    public static @Nullable String getUsername(final @NotNull String token) throws SQLException {
        final Pair.ImmutablePair<String, Long> user;
        try {
            user = TokenHelper.TokensCache.computeIfAbsent(token, k -> {
                try {
                    return TokenHelper.selectToken(token);
                } catch (final SQLException exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (final RuntimeException exception) {
            if (exception.getCause() instanceof SQLException sqlException)
                throw sqlException;
            throw exception;
        }
        if (user == null || user.getSecond().longValue() < System.currentTimeMillis() - TokenHelper.ExpiredTime)
            return null;
        return user.getFirst();
    }

    public static void clearExpiredTokens() throws SQLException {
        final long time = System.currentTimeMillis() - TokenHelper.ExpiredTime;
        SQLiteUtil.getDataInstance().getLock("tokens").writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getDataInstance().prepareStatement("DELETE FROM tokens WHERE time < ?;")) {
            statement.setLong(1, time);
            statement.executeUpdate();
        } finally {
            SQLiteUtil.getDataInstance().getLock("tokens").writeLock().unlock();
        }
        TokenHelper.TokensCache.entrySet().removeIf(e -> e.getValue().getSecond().longValue() < time);
    }

    private static Pair.@Nullable ImmutablePair<@NotNull String, @NotNull Long> selectToken(final @NotNull String token) throws SQLException {
        SQLiteUtil.getDataInstance().getLock("tokens").readLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getDataInstance().prepareStatement("SELECT user, time FROM tokens WHERE token == ? LIMIT 1;")) {
            statement.setString(1, token);
            try (final ResultSet tokens = statement.executeQuery()) {
                if (tokens.next())
                    return Pair.ImmutablePair.makeImmutablePair(tokens.getString(1), tokens.getLong(2));
                return null;
            }
        } finally {
            SQLiteUtil.getDataInstance().getLock("tokens").readLock().unlock();
        }
    }

    private static void insertToken(final @NotNull String token, final @NotNull String username, final long time) throws SQLException {
        SQLiteUtil.getDataInstance().getLock("tokens").writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getDataInstance().prepareStatement("INSERT INTO tokens (token, user, time) VALUES (?, ?, ?);")) {
            statement.setString(1, token);
            statement.setString(2, username);
            statement.setLong(3, time);
            statement.executeUpdate();
        } finally {
            SQLiteUtil.getDataInstance().getLock("tokens").writeLock().unlock();
        }
    }
}
