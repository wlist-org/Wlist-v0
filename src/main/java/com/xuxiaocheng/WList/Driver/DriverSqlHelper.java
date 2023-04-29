package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.WList.Utils.DataBaseUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
import java.util.Objects;

public final class DriverSqlHelper {
    private DriverSqlHelper() {
        super();
    }

    // Util

    private static @NotNull String getTableName(final @NotNull String name) {
        return "Driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull String serializeTime(final @Nullable LocalDateTime time) {
        return Objects.requireNonNullElseGet(time, LocalDateTime::now).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static @Nullable FileInformation createNextFileInfo(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        return new FileInformation(result.getLong("id"),
                new DrivePath(result.getString("parent_path")).child(result.getString("name")),
                result.getBoolean("is_directory"), result.getLong("size"),
                LocalDateTime.parse(result.getString("create_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                LocalDateTime.parse(result.getString("update_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                result.getString("tag"), result.getString("others"));
    }

    // Initiate

    public static void initiate(final @NotNull String driverName) throws SQLException {
        try (final Connection connection = DataBaseUtil.getIndexInstance().getConnection()) {
            connection.setAutoCommit(true);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id           INTEGER NOT NULL
                                             PRIMARY KEY
                                             UNIQUE,
                        parent_path    TEXT  NOT NULL,
                        name         TEXT    NOT NULL,
                        is_directory INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (is_directory == 1 OR
                                                is_directory == 0),
                        size         INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (size >= -1),
                        create_time  TEXT    NOT NULL,
                        update_time  TEXT    NOT NULL,
                        tag          TEXT    NOT NULL,
                        others       TEXT
                    );
                    """, DriverSqlHelper.getTableName(driverName)));
            }
        }
    }

    public static void uninitiate(final @NotNull String driverName) throws SQLException {
        try (final Connection connection = DataBaseUtil.getIndexInstance().getConnection()) {
            connection.setAutoCommit(true);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("DROP TABLE %s;", DriverSqlHelper.getTableName(driverName)));
            }
        }
    }

    // Insert

    public static void insertFiles(final @NotNull String driverName, final @NotNull Collection<FileInformation> infoList, final @Nullable Connection _connection) throws SQLException {
        if (infoList.isEmpty())
            return;
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                INSERT INTO %s (id, parent_path, name, is_directory, size, create_time, update_time, tag, others)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    parent_path = excluded.parent_path, name = excluded.name,
                    is_directory = excluded.is_directory, size = excluded.size,
                    create_time = excluded.create_time, update_time = excluded.update_time,
                    tag = excluded.tag, others = excluded.others;
                """, DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            for (final FileInformation info: infoList) {
                statement.setLong(1, info.id());
                statement.setString(2, info.path().getParentPath());
                statement.setString(3, info.path().getName());
                statement.setBoolean(4, info.is_dir());
                statement.setLong(5, info.size());
                statement.setString(6, DriverSqlHelper.serializeTime(info.createTime()));
                statement.setString(7, DriverSqlHelper.serializeTime(info.updateTime()));
                statement.setString(8, info.tag());
                statement.setString(9, info.others());
                statement.executeUpdate();
            }
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static void insertFile(final @NotNull String driverName, final @NotNull FileInformation info, final @Nullable Connection _connection) throws SQLException {
        DriverSqlHelper.insertFiles(driverName, List.of(info), _connection);
    }

    // Delete

    public static void deleteFiles(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final Connection _connection) throws SQLException {
        if (pathList.isEmpty())
            return;
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "DELETE FROM %s WHERE parent_path == ? AND NAME == ?;", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            for (final DrivePath path: pathList) {
                statement.setString(1, path.getParentPath());
                statement.setString(2, path.getName());
                statement.executeUpdate();
            }
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static void deleteFile(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable Connection _connection) throws SQLException {
        DriverSqlHelper.deleteFiles(driverName, List.of(path), _connection);
    }

    public static void deleteFilesById(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable Connection _connection) throws SQLException {
        if (idList.isEmpty())
            return;
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "DELETE FROM %s WHERE id == ?;", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            for (final Long id: idList) {
                statement.setLong(1, id.longValue());
                statement.executeUpdate();
            }
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static void deleteFileById(final @NotNull String driverName, final long id, final @Nullable Connection _connection) throws SQLException {
        DriverSqlHelper.deleteFilesById(driverName, List.of(id), _connection);
    }

    public static void deleteFilesByParentPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable Connection _connection) throws SQLException {
        if (parentPathList.isEmpty())
            return;
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "DELETE FROM %s WHERE parent_path == ? AND name GLOB '?*';", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            for (final DrivePath parentPath: parentPathList) {
                statement.setString(1, parentPath.getPath());
                statement.executeUpdate();
            }
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static void deleteFileByParentPath(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable Connection _connection) throws SQLException {
        DriverSqlHelper.deleteFilesByParentPath(driverName, List.of(parentPath), _connection);
    }

    public static void deleteFilesByTag(final @NotNull String driverName, final @NotNull Collection<@NotNull String> tagList, final @Nullable Connection _connection) throws SQLException {
        if (tagList.isEmpty())
            return;
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "DELETE FROM %s WHERE tag == ?;", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            for (final String tag: tagList) {
                statement.setString(1, tag);
                statement.executeUpdate();
            }
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static void deleteFileByTag(final @NotNull String driverName, final @NotNull String tag, final @Nullable Connection _connection) throws SQLException {
        DriverSqlHelper.deleteFilesByTag(driverName, List.of(tag), _connection);
    }

    // Get

    public static @NotNull @UnmodifiableView List<@Nullable FileInformation> getFiles(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable Connection _connection) throws SQLException {
        if (pathList.isEmpty())
            return List.of();
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE parent_path == ? AND name == ? LIMIT 1;", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            final List<FileInformation> list = new ArrayList<>(pathList.size());
            for (final DrivePath path: pathList) {
                statement.setString(1, path.getParentPath());
                statement.setString(2, path.getName());
                try (final ResultSet result = statement.executeQuery()) {
                    list.add(DriverSqlHelper.createNextFileInfo(result));
                }
            }
            return Collections.unmodifiableList(list);
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static @Nullable FileInformation getFile(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable Connection _connection) throws SQLException {
        return DriverSqlHelper.getFiles(driverName, List.of(path), _connection).get(0);
    }

    public static @NotNull @UnmodifiableView List<@Nullable FileInformation> getFilesById(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable Connection _connection) throws SQLException {
        if (idList.isEmpty())
            return List.of();
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE id == ? LIMIT 1;", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            final List<FileInformation> list = new ArrayList<>(idList.size());
            for (final Long id: idList) {
                statement.setLong(1, id.longValue());
                try (final ResultSet result = statement.executeQuery()) {
                    list.add(DriverSqlHelper.createNextFileInfo(result));
                }
            }
            return Collections.unmodifiableList(list);
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static @Nullable FileInformation getFileById(final @NotNull String driverName, final long id, final @Nullable Connection _connection) throws SQLException {
        return DriverSqlHelper.getFilesById(driverName, List.of(id), _connection).get(0);
    }

    public static @NotNull @UnmodifiableView List<@NotNull @UnmodifiableView List<@NotNull FileInformation>> getFilesByParentPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable Connection _connection) throws SQLException {
        if (parentPathList.isEmpty())
            return List.of();
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE parent_path == ?;", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            final List<List<FileInformation>> list = new ArrayList<>(parentPathList.size());
            for (final DrivePath parentPath: parentPathList) {
                statement.setString(1, parentPath.getPath());
                final List<FileInformation> insideList = new ArrayList<>();
                try (final ResultSet result = statement.executeQuery()) {
                    FileInformation info = DriverSqlHelper.createNextFileInfo(result);
                    while (info != null) {
                        insideList.add(info);
                        info = DriverSqlHelper.createNextFileInfo(result);
                    }
                }
                list.add(Collections.unmodifiableList(insideList));
            }
            return Collections.unmodifiableList(list);
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static @NotNull @UnmodifiableView List<@NotNull FileInformation> getFileByParentPath(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable Connection _connection) throws SQLException {
        return DriverSqlHelper.getFilesByParentPath(driverName, List.of(parentPath), _connection).get(0);
    }

    public static @NotNull @UnmodifiableView List<@NotNull @UnmodifiableView List<@NotNull FileInformation>> getFilesByTag(final @NotNull String driverName, final @NotNull Collection<@NotNull String> tagList, final @Nullable Connection _connection) throws SQLException {
        if (tagList.isEmpty())
            return List.of();
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE tag == ?;", DriverSqlHelper.getTableName(driverName)))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            final List<List<FileInformation>> list = new ArrayList<>(tagList.size());
            for (final String tag: tagList) {
                statement.setString(1, tag);
                final List<FileInformation> insideList = new ArrayList<>();
                try (final ResultSet result = statement.executeQuery()) {
                    FileInformation info = DriverSqlHelper.createNextFileInfo(result);
                    while (info != null) {
                        insideList.add(info);
                        info = DriverSqlHelper.createNextFileInfo(result);
                    }
                }
                list.add(Collections.unmodifiableList(insideList));
            }
            return Collections.unmodifiableList(list);
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    public static @NotNull @UnmodifiableView List<@NotNull FileInformation> getFileByTag(final @NotNull String driverName, final @NotNull String tag, final @Nullable Connection _connection) throws SQLException {
        return DriverSqlHelper.getFilesByTag(driverName, List.of(tag), _connection).get(0);
    }

    // Search
}
