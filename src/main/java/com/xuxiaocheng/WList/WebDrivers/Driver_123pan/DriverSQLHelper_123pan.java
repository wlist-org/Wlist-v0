package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Utils.SQLiteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

public final class DriverSQLHelper_123pan {
    private DriverSQLHelper_123pan() {
        super();
    }

    private static @NotNull String getTableName(final @NotNull String name) {
        return "Driver_123pan_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    static void init(final @NotNull String driverName) throws SQLException {
        final String table = DriverSQLHelper_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final Statement statement = SQLiteUtil.getIndexInstance().createStatement()) {
            statement.executeUpdate(String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    id           INTEGER NOT NULL,
                    parent_path  TEXT    NOT NULL,
                    name         TEXT    NOT NULL,
                    is_directory INTEGER NOT NULL
                                         DEFAULT (0),
                    size         INTEGER NOT NULL
                                         DEFAULT (0),
                    create_time  TEXT    NOT NULL,
                    update_time  TEXT    NOT NULL,
                    s3key        TEXT    NOT NULL,
                    etag         TEXT    NOT NULL
                );
                """, table));
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    static void delete(final @NotNull String driverName) throws SQLException {
        final String table = DriverSQLHelper_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final Statement statement = SQLiteUtil.getIndexInstance().createStatement()) {
            statement.executeUpdate(String.format("DROP TABLE %s;", table));
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    public record FileInformation(long id, @NotNull DrivePath path, int is_dir, long size, long createTime, long updateTime, @NotNull String s3key, @NotNull String etag) {
    }

    private static @Nullable FileInformation createFileInformation(final @NotNull ResultSet result) throws SQLException {
        try (result) {
            if (!result.next())
                return null;
            return new FileInformation(result.getLong("id"),
                    new DrivePath(result.getString("parent_path")).child(result.getString("name")),
                    result.getInt("is_directory"), result.getLong("size"),
                    result.getLong("create_time"),result.getLong("update_time"),
                    result.getString("s3key"),result.getString("etag"));
        }
    }

    static @Nullable FileInformation getFile(final @NotNull String driverName, final long id) throws SQLException {
        final String table = DriverSQLHelper_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).readLock().lock();
        try (final Statement statement = SQLiteUtil.getIndexInstance().createStatement()) {
            return DriverSQLHelper_123pan.createFileInformation(statement.executeQuery(String.format("SELECT * FROM %s WHERE id == %d LIMIT 1;", table, id)));
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).readLock().unlock();
        }
    }

    static @Nullable FileInformation getFile(final @NotNull String driverName, final @NotNull DrivePath path) throws SQLException {
        final String table = DriverSQLHelper_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).readLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().prepareStatement(String.format("SELECT * FROM %s WHERE parent_path == ? AND name == ? LIMIT 1;", table))) {
            statement.setString(1, path.getParent().getPath());
            statement.setString(2, path.getName());
            return DriverSQLHelper_123pan.createFileInformation(statement.executeQuery());
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).readLock().unlock();
        }
    }

    static void insertFile(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @NotNull JSONObject info) throws SQLException, IllegalParametersException {
        final String table = DriverSQLHelper_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().prepareStatement(String.format("""
                INSERT INTO %s (id, parent_path, name, is_directory, size, create_time, update_time, s3key, etag)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """, table))) {
            statement.setLong(1, info.getLongValue("FileId"));
            statement.setString(2, parentPath.getPath());
            statement.setString(3, info.getString("FileName"));
            statement.setInt(4, info.getIntValue("Type"));
            statement.setLong(5, info.getLongValue("Size"));
            statement.setLong(6, DriverHelper_123pan.parseServerTimeWithZone(info.getString("CreateAt")));
            statement.setLong(7, DriverHelper_123pan.parseServerTimeWithZone(info.getString("UpdateAt")));
            statement.setString(8, info.getString("S3KeyFlag"));
            statement.setString(9, info.getString("Etag"));
            statement.executeUpdate();
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    static void updateFile(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @NotNull JSONObject info) throws SQLException, IllegalParametersException {
        if (DriverSQLHelper_123pan.getFile(driverName, parentPath.getChild(info.getString("FileName"))) == null) {
            DriverSQLHelper_123pan.insertFile(driverName, parentPath, info);
            return;
        }
        final String table = DriverSQLHelper_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().prepareStatement(String.format("""
                UPDATE %s SET id = ?, is_directory = ?, size = ?, create_time = ?, update_time = ?, s3key = ?, etag = ?
                WHERE parent_path == ? AND name == ?;
                """, table))) {
            statement.setLong(1, info.getLongValue("FileId"));
            statement.setInt(2, info.getIntValue("Type"));
            statement.setLong(3, info.getLongValue("Size"));
            statement.setLong(4, DriverHelper_123pan.parseServerTimeWithZone(info.getString("CreateAt")));
            statement.setLong(5, DriverHelper_123pan.parseServerTimeWithZone(info.getString("UpdateAt")));
            statement.setString(6, info.getString("S3KeyFlag"));
            statement.setString(7, info.getString("Etag"));
            statement.setString(8, parentPath.getPath());
            statement.setString(9, info.getString("FileName"));
            statement.executeUpdate();
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    public static int countPath(final @NotNull String name, final @NotNull DrivePath parentPath) throws SQLException {
        final String table = DriverSQLHelper_123pan.getTableName(name);
        SQLiteUtil.getIndexInstance().getLock(table).readLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().prepareStatement(String.format("SELECT COUNT(*) FROM %s WHERE parent_path == ?;", table))) {
            statement.setString(1, parentPath.getPath());
            try (final ResultSet result = statement.executeQuery()) {
                return result.getInt(1);
            }
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).readLock().unlock();
        }
    }

}
