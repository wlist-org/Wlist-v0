package com.xuxiaocheng.WList.Driver.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.DataAccessObjects.FileInformation;
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
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class DriverSqlHelper {
    private DriverSqlHelper() {
        super();
    }

    // Util

    private static @NotNull String getTableName(final @NotNull String name) {
        return "Driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_');
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

    private static @NotNull @UnmodifiableView List<@NotNull FileInformation> createFilesInfo(final @NotNull ResultSet result) throws SQLException {
        final List<FileInformation> list = new LinkedList<>();
        FileInformation info = DriverSqlHelper.createNextFileInfo(result);
        while (info != null) {
            list.add(info);
            info = DriverSqlHelper.createNextFileInfo(result);
        }
        return Collections.unmodifiableList(list);
    }

    // Initiate

    public static void initiate(final @NotNull String driverName) throws SQLException {
        try (final Connection connection = DataBaseUtil.getIndexInstance().getConnection()) {
            connection.setAutoCommit(true);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id           INTEGER NOT NULL
                                             PRIMARY KEY AUTOINCREMENT
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

    /* Please try not to use this method and use {@code insertFiles} instead */
    public static void insertFilesIgnoreId(final @NotNull String driverName, final @NotNull Collection<FileInformation> infoList, final @Nullable Connection _connection) throws SQLException {
        if (infoList.isEmpty())
            return;
        final String table = DriverSqlHelper.getTableName(driverName);
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement updater = connection.prepareStatement(String.format("""
                    UPDATE %s SET
                        is_directory = ?, size = ?,
                        create_time = ?, update_time = ?,
                        tag = ?, others = ?
                    WHERE parent_path == ? AND name == ?;
                """, table))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    INSERT INTO %s (parent_path, name, is_directory, size, create_time, update_time, tag, others)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                    """, table))) {
                for (final FileInformation info: infoList) {
                    final String create = DriverSqlHelper.serializeTime(info.createTime());
                    final String update = DriverSqlHelper.serializeTime(info.updateTime());
                    updater.setBoolean(1, info.is_dir());
                    updater.setLong(2, info.size());
                    updater.setString(3, create);
                    updater.setString(4, update);
                    updater.setString(5, info.tag());
                    updater.setString(6, info.others());
                    updater.setString(7, info.path().getParentPath());
                    updater.setString(8, info.path().getName());
                    final int row = updater.executeUpdate();
                    if (row <= 0) {
                        statement.setString(1, info.path().getParentPath());
                        statement.setString(2, info.path().getName());
                        statement.setBoolean(3, info.is_dir());
                        statement.setLong(4, info.size());
                        statement.setString(5, create);
                        statement.setString(6, update);
                        statement.setString(7, info.tag());
                        statement.setString(8, info.others());
                        statement.executeUpdate();
                    }
                }
            }
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    /* Please try not to use this method and use {@code insertFile} instead */
    public static void insertFileIgnoreId(final @NotNull String driverName, final @NotNull FileInformation info, final @Nullable Connection _connection) throws SQLException {
        DriverSqlHelper.insertFilesIgnoreId(driverName, List.of(info), _connection);
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
            final List<FileInformation> list = new LinkedList<>();
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
            final List<FileInformation> list = new LinkedList<>();
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
            final List<List<FileInformation>> list = new LinkedList<>();
            for (final DrivePath parentPath: parentPathList) {
                statement.setString(1, parentPath.getPath());
                try (final ResultSet result = statement.executeQuery()) {
                    list.add(DriverSqlHelper.createFilesInfo(result));
                }
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
            final List<List<FileInformation>> list = new LinkedList<>();
            for (final String tag: tagList) {
                statement.setString(1, tag);
                try (final ResultSet result = statement.executeQuery()) {
                    list.add(DriverSqlHelper.createFilesInfo(result));
                }
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

    public static Pair.@NotNull ImmutablePair<@NotNull Integer, @UnmodifiableView List<@NotNull FileInformation>> getFileByParentPathS(final @NotNull String driverName, final @NotNull DrivePath parentPath, final int limit, final int offset, final @NotNull OrderDirection direction, final @NotNull OrderPolicy policy, final @Nullable Connection _connection) throws SQLException {
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE parent_path == ? ORDER BY ? %s LIMIT ? OFFSET ?;", DriverSqlHelper.getTableName(driverName),
                switch (direction) {case ASCEND -> "ASC";case DESCEND -> "DESC";}))) {
            if (_connection == null)
                connection.setAutoCommit(false);
            statement.setString(1, parentPath.getPath());
            statement.setString(2, switch (policy) {
                case FileName -> "name";
                case Size -> "size";
                case CreateTime -> "create_time";
                case UpdateTime -> "update_time";
//                default -> {throw new IllegalParametersException("Unsupported policy.", policy);}
            });
            statement.setInt(3, limit);
            statement.setLong(4, offset);
            final List<FileInformation> list;
            try (final ResultSet result = statement.executeQuery()) {
                list = DriverSqlHelper.createFilesInfo(result);
            }
            try (final PreparedStatement counter = connection.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM %s WHERE parent_path == ?;", DriverSqlHelper.getTableName(driverName)))) {
                counter.setString(1, parentPath.getPath());
                try (final ResultSet result = counter.executeQuery()) {
                    result.next();
                    return Pair.ImmutablePair.makeImmutablePair(result.getInt(1), list);
                }
            }
        } finally {
            if (_connection == null)
                connection.close();
        }
    }

    // Search
}
