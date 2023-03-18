package com.xuxiaocheng.WList.Internal.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Internal.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Internal.Utils.SQLiteUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class TokenManager {
    private TokenManager() {
        super();
    }

    private static final @NotNull String ServerPasswordSlat = "Server SALT***#WListServer%CreateBy@XXC#***TokenMD5&SALT";
    private static final long ExpiredTime = TimeUnit.DAYS.toMillis(1);

    private static boolean initialized = false;
    public static void init() throws SQLException {
        if (TokenManager.initialized)
            return;
        SQLiteUtil.getDataInstance().getLock("users").writeLock().lock();
        try (final Statement statement = SQLiteUtil.getDataInstance().createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id          INTEGER    PRIMARY KEY
                                               UNIQUE
                                               NOT NULL,
                        username    TEXT       UNIQUE
                                               NOT NULL,
                        password    TEXT       NOT NULL,
                        permission  TEXT       NOT NULL
                                               DEFAULT "[]"
                    );
                    """);//TODO empty permission
            try (final ResultSet admins = statement.executeQuery("SELECT 1 FROM users WHERE permission == 100 LIMIT 1;")) {
                if (!admins.next()) {
                    final char[] word = new char[8];
                    HRandomHelper.setArray(HRandomHelper.RANDOM, word, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.".toCharArray());
                    final String password = new String(word);
                    statement.executeUpdate("DELETE FROM users WHERE username == \"admin\";");
                    statement.executeUpdate(String.format("""
                            INSERT INTO users (username, password, permission)
                            VALUES ("admin", "%s", 100);
                            """, TokenManager.getMd5(password + TokenManager.ServerPasswordSlat)));//TODO full permission
                    HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Reset admin user, password: ", password);
                }
            }
        } finally {
            SQLiteUtil.getDataInstance().getLock("users").writeLock().unlock();
        }
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
            statement.executeUpdate(String.format("DELETE FROM tokens WHERE time < %d;",
                    System.currentTimeMillis() - TokenManager.ExpiredTime));
        } finally {
            SQLiteUtil.getDataInstance().getLock("tokens").writeLock().unlock();
        }
        TokenManager.initialized = true;
    }

    private static final @NotNull Map<@NotNull String, @NotNull Set<@NotNull String>> tokens = new ConcurrentHashMap<>();
    private static @NotNull String generateNewToken(final @NotNull String username) throws SQLException {
        final long time = System.currentTimeMillis();
        final String token = Base64.getEncoder().encodeToString(("@MD5-Username:\n\t" + TokenManager.getMd5(username)).getBytes(StandardCharsets.UTF_8)) + '.'
                + Base64.getEncoder().encodeToString(HRandomHelper.getRandomUUID(HRandomHelper.RANDOM).toString().getBytes(StandardCharsets.UTF_8)) + '_'
                + TokenManager.getMd5(Long.toString(time, 8));
        try {
            TokenManager.tokens.computeIfAbsent(username, k -> {
                final Set<String> sets = Collections.synchronizedSet(new HashSet<>());
                try {
                    SQLiteUtil.getDataInstance().getLock("tokens").readLock().lock();
                    try (final Statement statement = SQLiteUtil.getDataInstance().createStatement()) {
                        try (final ResultSet tokens = statement.executeQuery(String.format("SELECT token FROM tokens WHERE user == \"%s\";", username))) {
                            while (tokens.next())
                                sets.add(tokens.getString("token"));
                        }
                    } finally {
                        SQLiteUtil.getDataInstance().getLock("tokens").readLock().unlock();
                    }
                } catch (final SQLException exception) {
                    throw new RuntimeException(exception);
                }
                return sets;
            }).add(token);
        } catch (final RuntimeException exception) {
            if (exception.getCause() instanceof SQLException sql)
                throw sql;
            throw new SQLException(exception);
        }
        SQLiteUtil.executor.submit((Callable<?>) () -> {
            SQLiteUtil.getDataInstance().getLock("tokens").writeLock().lock();
            try (final Statement statement = SQLiteUtil.getDataInstance().createStatement()) {
                statement.executeUpdate(String.format("""
                    INSERT INTO tokens (token, user, time)
                    VALUES ("%s", "%s", %s);
                    """, token, username, time));
            } finally {
                SQLiteUtil.getDataInstance().getLock("tokens").writeLock().unlock();
            }
            //noinspection ReturnOfNull
            return null;
        });
        return token;
    }

    private static Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String> readUserPair(final @NotNull ByteBuf buf) throws IOException {
        final String username = ByteBufIOUtil.readUTF(buf);
        if (username.contains("\""))
            throw new IllegalNetworkDataException("Invalid username.");
        final String password = ByteBufIOUtil.readUTF(buf);
        if (password.contains("\""))
            throw new IllegalNetworkDataException("Invalid password!");
        return Pair.ImmutablePair.makeImmutablePair(username, TokenManager.getMd5(password + TokenManager.ServerPasswordSlat));
    }

    public static @NotNull String doRegister(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final Pair<String, String> userPair = TokenManager.readUserPair(buf);
        SQLiteUtil.getDataInstance().getLock("users").writeLock().lock();
        try (final Statement statement = SQLiteUtil.getDataInstance().createStatement()) {
            try (final ResultSet user = statement.executeQuery(String.format("SELECT 1 FROM users WHERE username == \"%s\" LIMIT 1;", userPair.getFirst()))) {
                if (user.next())
                    throw new IllegalNetworkDataException("The same username has existed.");
            }
            statement.executeUpdate(String.format("""
                    INSERT INTO users (username, password)
                    VALUES ("%s", "%s");
                    """, userPair.getFirst(), userPair.getSecond()));
        } finally {
            SQLiteUtil.getDataInstance().getLock("users").writeLock().unlock();
        }
        return TokenManager.generateNewToken(userPair.getFirst());
    }

    public static @NotNull String doLoginIn(final @NotNull ByteBuf buf) throws IOException, SQLException {
        final Pair<String, String> userPair = TokenManager.readUserPair(buf);
        SQLiteUtil.getDataInstance().getLock("users").readLock().lock();
        try (final Statement statement = SQLiteUtil.getDataInstance().createStatement()) {
            try (final ResultSet user = statement.executeQuery(String.format("SELECT * FROM users WHERE username == \"%s\" LIMIT 1;", userPair.getFirst()))) {
                if (!user.next() || !userPair.getSecond().equals(user.getString("password")))
                    throw new IllegalNetworkDataException("The username or password is wrong.");
            }
        } finally {
            SQLiteUtil.getDataInstance().getLock("users").readLock().unlock();
        }
        return TokenManager.generateNewToken(userPair.getFirst());
    }

    public static @NotNull String doLoginOut(final @NotNull ByteBuf ignoredBuf) {
        return "";
    }

    public static @NotNull String getMd5(final String source) {
        final MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        md5.update(source.getBytes(StandardCharsets.UTF_8));
        final BigInteger i = new BigInteger(1, md5.digest());
        return i.toString(16);
    }
}
