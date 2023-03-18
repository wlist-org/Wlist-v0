package com.xuxiaocheng.WList.Internal.Server.Helper;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Internal.Server.Operation;
import com.xuxiaocheng.WList.Internal.Utils.SQLiteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UserHelper {
    private UserHelper() {
        super();
    }

    public static final @NotNull List<@NotNull Byte> FullPermission = Stream.of(Operation.Permission.values()).filter(p -> p != Operation.Permission.Undefined).map(Operation.Permission::getId).sorted().toList();
    public static final @NotNull List<@NotNull Byte> DefaultPermission = Stream.of(Operation.Permission.Undefined).map(Operation.Permission::getId).sorted().toList();

    public static void init() throws SQLException {
        SQLiteUtil.getDataInstance().getLock("users").writeLock().lock();
        try (final Statement statement = SQLiteUtil.getDataInstance().createStatement()) {
            statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS users (
                        id          INTEGER    PRIMARY KEY
                                               UNIQUE
                                               NOT NULL,
                        username    TEXT       UNIQUE
                                               NOT NULL,
                        password    TEXT       NOT NULL,
                        permission  TEXT       NOT NULL
                                               DEFAULT "%s"
                    );
                    """, UserHelper.DefaultPermission));
            try (final ResultSet admins = statement.executeQuery(String.format("SELECT 1 FROM users WHERE permission == \"%s\" LIMIT 1;", UserHelper.FullPermission))) {
                if (!admins.next()) {
                    final String password = UserHelper.generateRandomPassword();
                    statement.executeUpdate("DELETE FROM users WHERE username == \"admin\";");
                    statement.executeUpdate(String.format("""
                            INSERT INTO users (username, password, permission)
                                VALUES ("admin", "%s", "%s");
                            """, UserHelper.encryptPassword(password), UserHelper.FullPermission));
                    HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "Reset admin user, password: ", password);
                }
            }
        } finally {
            SQLiteUtil.getDataInstance().getLock("users").writeLock().unlock();
        }
    }

    public static void addUser(final @NotNull String username, final @NotNull String password) throws SQLException {
        UserHelper.insertUser(username, password);
    }

    public static @Nullable Pair<@NotNull String, @NotNull Set<Operation.@NotNull Permission>> getUser(final @NotNull String username) throws SQLException {
        return UserHelper.selectUser(username);
    }

    private static @Nullable Pair<@NotNull String, @NotNull Set<Operation.@NotNull Permission>> selectUser(final @NotNull String username) throws SQLException {
        SQLiteUtil.getDataInstance().getLock("users").readLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getDataInstance().prepareStatement("SELECT password, permission FROM users WHERE username == ? LIMIT 1;")) {
            statement.setString(1, username);
            try (final ResultSet user = statement.executeQuery()) {
                if (!user.next())
                    return null;
                return Pair.makePair(user.getString(1),
                        JSON.parseArray(user.getString(2), Byte.class)
                                .stream().map(Operation::getPermission).collect(Collectors.toSet()));
            }
        } finally {
            SQLiteUtil.getDataInstance().getLock("users").readLock().unlock();
        }
    }

    private static void insertUser(final @NotNull String username, final @NotNull String password) throws SQLException {
        SQLiteUtil.getDataInstance().getLock("users").writeLock().lock();
        try (final PreparedStatement updateStatement = SQLiteUtil.getDataInstance().prepareStatement("INSERT INTO users (username, password) VALUES (?, ?);")) {
            updateStatement.setString(1, username);
            updateStatement.setString(2, password);
            updateStatement.executeUpdate();
        } finally {
            SQLiteUtil.getDataInstance().getLock("users").writeLock().unlock();
        }
    }

    public static @NotNull String generateRandomPassword() {
        final char[] word = new char[8];
        HRandomHelper.setArray(HRandomHelper.RANDOM, word, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_".toCharArray());
        return new String(word);
    }

    public static @NotNull String generateRandomToken(final @NotNull String username, final long time) {
        return Base64.getEncoder().encodeToString(("@MD5-Username:\n\t" + UserHelper.getMd5(username)).getBytes(StandardCharsets.UTF_8)) + '.'
                + Base64.getEncoder().encodeToString(HRandomHelper.getRandomUUID(HRandomHelper.RANDOM).toString().getBytes(StandardCharsets.UTF_8)) + '_'
                + UserHelper.getMd5(Long.toString(time, 8));
    }

    private static final @NotNull String ServerPasswordSlat = "Server SALT***#WListServer%CreateBy@XXC#***TokenMD5&SALT";
    public static @NotNull String encryptPassword(final @NotNull String password) {
        return UserHelper.getMd5(password + UserHelper.ServerPasswordSlat);
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
