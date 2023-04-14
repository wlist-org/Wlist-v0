package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Utils.SQLiteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class DriverSQL_123pan {
    private DriverSQL_123pan() {
        super();
    }

    // Tools

    private static @NotNull String getTableName(final @NotNull String name) {
        return "Driver_123pan_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    static void initiate(final @NotNull String driverName) throws SQLException {
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final Statement statement = SQLiteUtil.getIndexInstance().getConnection(null).createStatement()) {
            statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id           INTEGER NOT NULL
                                             UNIQUE,
                        full_path    TEXT    NOT NULL
                                             PRIMARY KEY
                                             UNIQUE,
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

    static void uninitiate(final @NotNull String driverName) throws SQLException {
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final Statement statement = SQLiteUtil.getIndexInstance().getConnection(null).createStatement()) {
            statement.executeUpdate(String.format("DROP TABLE %s;", table));
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    private static @Nullable FileInformation_123pan createFileInfo(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        return new FileInformation_123pan(result.getLong("id"),
                new DrivePath(result.getString("full_path")),
                result.getInt("is_directory"), result.getLong("size"),
                LocalDateTime.parse(result.getString("create_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse(result.getString("update_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                result.getString("s3key"),result.getString("etag"));
    }

    // Insert

    static void insertFiles(final @NotNull String driverName, final @NotNull Collection<FileInformation_123pan> infoList, final @Nullable String connectionName) throws SQLException {
        if (infoList.isEmpty())
            return;
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().getConnection(connectionName)
                .prepareStatement(String.format("""
                INSERT INTO %s (id, full_path, name, is_directory, size, create_time, update_time, s3key, etag)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (full_path) DO
                UPDATE SET
                    id = excluded.id, name = excluded.name,
                    is_directory = excluded.is_directory, size = excluded.size,
                    create_time = excluded.create_time, update_time = excluded.update_time,
                    s3key = excluded.s3key, etag = excluded.etag;
                """, table))) {
            for (final FileInformation_123pan info: infoList) {
                statement.setLong(1, info.id());
                statement.setString(2, info.path().getPath());
                statement.setString(3, info.path().getName());
                statement.setInt(4, info.is_dir());
                statement.setLong(5, info.size());
                statement.setString(6, info.createTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                statement.setString(7, info.updateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                statement.setString(8, info.s3key());
                statement.setString(9, info.etag());
                statement.executeUpdate();
            }
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    static void insertFile(final @NotNull String driverName, final @NotNull FileInformation_123pan info, final @Nullable String connectionName) throws SQLException {
        DriverSQL_123pan.insertFiles(driverName, List.of(info), connectionName);
    }

    // Get

    static @NotNull @UnmodifiableView List<@Nullable FileInformation_123pan> getFiles(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String connectionName) throws SQLException {
        if (pathList.isEmpty())
            return List.of();
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).readLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().getConnection(connectionName)
                .prepareStatement(String.format("SELECT * FROM %s WHERE full_path == ? LIMIT 1;", table))) {
            final List<FileInformation_123pan> list = new ArrayList<>(pathList.size());
            for (final DrivePath path: pathList) {
                statement.setString(1, path.getPath());
                try (final ResultSet result = statement.executeQuery()) {
                    list.add(DriverSQL_123pan.createFileInfo(result));
                }
            }
            return Collections.unmodifiableList(list);
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).readLock().unlock();
        }
    }

    static @Nullable FileInformation_123pan getFile(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String connectionName) throws SQLException {
        return DriverSQL_123pan.getFiles(driverName, List.of(path), connectionName).get(0);
    }

    static @NotNull @UnmodifiableView List<@Nullable FileInformation_123pan> getFilesById(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionName) throws SQLException {
        if (idList.isEmpty())
            return List.of();
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).readLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().getConnection(connectionName)
                .prepareStatement(String.format("SELECT * FROM %s WHERE id == ? LIMIT 1;", table))) {
            final List<FileInformation_123pan> list = new ArrayList<>(idList.size());
            for (final Long id: idList) {
                statement.setLong(1, id.longValue());
                try (final ResultSet result = statement.executeQuery()) {
                    list.add(DriverSQL_123pan.createFileInfo(result));
                }
            }
            return Collections.unmodifiableList(list);
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).readLock().unlock();
        }
    }

    static @Nullable FileInformation_123pan getFileById(final @NotNull String driverName, final long id, final @Nullable String connectionName) throws SQLException {
        return DriverSQL_123pan.getFilesById(driverName, List.of(id), connectionName).get(0);
    }

    // Delete

    static void deleteFiles(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String connectionName) throws SQLException {
        if (pathList.isEmpty())
            return;
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().getConnection(connectionName)
                .prepareStatement(String.format("DELETE FROM %s WHERE full_path == ?;", table))) {
            for (final DrivePath path: pathList) {
                statement.setString(1, path.getPath());
                statement.executeUpdate();
            }
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    static void deleteFile(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String connectionName) throws SQLException {
        DriverSQL_123pan.deleteFiles(driverName, List.of(path), connectionName);
    }

    static void deleteFilesById(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionName) throws SQLException {
        if (idList.isEmpty())
            return;
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().getConnection(connectionName)
                .prepareStatement(String.format("DELETE FROM %s WHERE id == ?;", table))) {
            for (final Long id: idList) {
                statement.setLong(1, id.longValue());
                statement.executeUpdate();
            }
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    static void deleteFileById(final @NotNull String driverName, final long id, final @Nullable String connectionName) throws SQLException {
        DriverSQL_123pan.deleteFilesById(driverName, List.of(id), connectionName);
    }


    static void deleteFilesByParentPath(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable String connectionName) throws SQLException {
        final String table = DriverSQL_123pan.getTableName(driverName);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try (final PreparedStatement statement = SQLiteUtil.getIndexInstance().getConnection(connectionName)
                .prepareStatement(String.format("DELETE FROM %s WHERE full_path REGEXP ?;", table))) {
            if (parentPath.getDepth() == 0)
                statement.setString(1, "^/[^/]+$");
            else
                statement.setString(1, "^" + parentPath.getPath() + "/[^/]+$");
            statement.executeUpdate();
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }
}
