package com.xuxiaocheng.WList.Internal.Server.Helper;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Internal.Utils.SQLiteUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class TokenHelper {
    private TokenHelper() {
        super();
    }

    private static final long ExpiredTime = TimeUnit.DAYS.toMillis(1);
    //                                username                                                  token            create_time
    private static final @NotNull Map<@NotNull String, @NotNull Set<Pair.@NotNull ImmutablePair<@NotNull String, @NotNull Long>>> TokensCache = new ConcurrentHashMap<>();

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
        TokenHelper.getTokens(username).add(Pair.ImmutablePair.makeImmutablePair(token, time));
        TokenHelper.insertToken(token, username, time);
    }

    public static @NotNull Set<Pair.@NotNull ImmutablePair<@NotNull String, @NotNull Long>> getTokens(final @NotNull String username) throws SQLException {
        try {
            final Set<Pair.ImmutablePair<String, Long>> tokens = TokenHelper.TokensCache.computeIfAbsent(username, username1 -> {
                try {
                    return Collections.synchronizedSet(TokenHelper.selectTokens(username1));
                } catch (final SQLException exception) {
                    throw new RuntimeException(exception);
                }
            });
            tokens.removeIf(p -> p.getSecond().longValue() < System.currentTimeMillis() - TokenHelper.ExpiredTime);
            return tokens;
        } catch (final RuntimeException exception) {
            if (exception.getCause() instanceof SQLException sqlException)
                throw sqlException;
            throw exception;
        }
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
        TokenHelper.TokensCache.forEach((n, ts) -> ts.removeIf(token -> token.getSecond().longValue() < time));
        TokenHelper.TokensCache.entrySet().removeIf(e -> !e.getValue().isEmpty());
    }

    private static @NotNull Set<Pair.@NotNull ImmutablePair<@NotNull String, @NotNull Long>> selectTokens(final @NotNull String username) throws SQLException {
        final Set<Pair.ImmutablePair<String, Long>> sets = new HashSet<>();
        SQLiteUtil.getDataInstance().getLock("tokens").readLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getDataInstance().prepareStatement("SELECT token, time FROM tokens WHERE user == ?;")) {
            statement.setString(1, username);
            try (final ResultSet tokens = statement.executeQuery()) {
                while (tokens.next())
                    sets.add(Pair.ImmutablePair.makeImmutablePair(tokens.getString(1), tokens.getLong(2)));
            }
        } finally {
            SQLiteUtil.getDataInstance().getLock("tokens").readLock().unlock();
        }
        return sets;
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
