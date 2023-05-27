package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
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

public final class FileSqlHelper {
    private FileSqlHelper() {
        super();
    }

    public static final @NotNull DatabaseUtil DefaultDatabaseUtil = HExceptionWrapper.wrapSupplier(DatabaseUtil::getInstance).get();

    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Util

    private static @NotNull String getTableName(final @NotNull String name) {
        return "Driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_');
    }

    private static @NotNull String serializeTime(final @Nullable LocalDateTime time) {
        return Objects.requireNonNullElseGet(time, LocalDateTime::now).withNano(0).format(FileSqlHelper.DefaultFormatter);
    }

    private static @Nullable FileSqlInformation createNextFileInfo(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        return new FileSqlInformation(result.getLong("id"),
                new DrivePath(result.getString("parent_path")).child(result.getString("name")),
                result.getBoolean("is_directory"), result.getLong("size"),
                LocalDateTime.parse(result.getString("create_time"), FileSqlHelper.DefaultFormatter),
                LocalDateTime.parse(result.getString("update_time"), FileSqlHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others"));
    }

    private static @NotNull @UnmodifiableView List<@NotNull FileSqlInformation> createFilesInfo(final @NotNull ResultSet result) throws SQLException {
        final List<FileSqlInformation> list = new LinkedList<>();
        while (true) {
            final FileSqlInformation info = FileSqlHelper.createNextFileInfo(result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }

    // Initialize

    public static void initialize(final @NotNull String driverName, final @Nullable String connectionId) throws SQLException {
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(true);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT
                                             UNIQUE
                                             NOT NULL,
                        parent_path  TEXT    NOT NULL,
                        name         TEXT    NOT NULL,
                        is_directory INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (is_directory == 1 OR is_directory == 0),
                        size         INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (size >= -1),
                        create_time  TEXT    NOT NULL,
                        update_time  TEXT    NOT NULL,
                        md5          TEXT    NOT NULL,
                        others       TEXT
                    );
                    """, FileSqlHelper.getTableName(driverName)));
            }
        }
    }

    public static void uninitialize(final @NotNull String driverName, final @Nullable String connectionId) throws SQLException {
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(true);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("DROP TABLE %s;", FileSqlHelper.getTableName(driverName)));
            }
        }
    }

    // Insert or Update

    public static void insertFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull FileSqlInformation> infoList, final @Nullable String connectionId) throws SQLException {
        if (infoList.isEmpty())
            return;
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    INSERT INTO %s (id, parent_path, name, is_directory, size, create_time, update_time, md5, others)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        parent_path = excluded.parent_path, name = excluded.name,
                        is_directory = excluded.is_directory, size = excluded.size,
                        create_time = excluded.create_time, update_time = excluded.update_time,
                        md5 = excluded.md5, others = excluded.others;
                    """, FileSqlHelper.getTableName(driverName)))) {
                for (final FileSqlInformation info: infoList) {
                    statement.setLong(1, info.id());
                    statement.setString(2, info.path().getParentPath());
                    statement.setString(3, info.path().getName());
                    statement.setBoolean(4, info.is_dir());
                    statement.setLong(5, info.size());
                    statement.setString(6, FileSqlHelper.serializeTime(info.createTime()));
                    statement.setString(7, FileSqlHelper.serializeTime(info.updateTime()));
                    statement.setString(8, info.md5());
                    statement.setString(9, info.others());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void insertFile(final @NotNull String driverName, final @NotNull FileSqlInformation info, final @Nullable String connectionId) throws SQLException {
        FileSqlHelper.insertFiles(driverName, List.of(info), connectionId);
    }

    // Delete

    public static void deleteFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                    "DELETE FROM %s WHERE id == ?;", FileSqlHelper.getTableName(driverName)))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void deleteFile(final @NotNull String driverName, final long id, final @Nullable String connectionId) throws SQLException {
        FileSqlHelper.deleteFiles(driverName, List.of(id), connectionId);
    }

    public static void deleteFilesByPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String connectionId) throws SQLException {
        if (pathList.isEmpty())
            return;
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                    "DELETE FROM %s WHERE parent_path == ? AND NAME == ?;", FileSqlHelper.getTableName(driverName)))) {
                for (final DrivePath path: pathList) {
                    statement.setString(1, path.getParentPath());
                    statement.setString(2, path.getName());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void deleteFileByPath(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String connectionId) throws SQLException {
        FileSqlHelper.deleteFilesByPath(driverName, List.of(path), connectionId);
    }

    public static void deleteFilesByParentPathRecursively(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String connectionId) throws SQLException {
        if (parentPathList.isEmpty())
            return;
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                    "DELETE FROM %s WHERE parent_path == ?;", FileSqlHelper.getTableName(driverName)))) {
                for (final DrivePath parentPath: parentPathList) {
                    statement.setString(1, parentPath.getPath());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void deleteFileByParentPathRecursively(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable String connectionId) throws SQLException {
        FileSqlHelper.deleteFilesByParentPathRecursively(driverName, List.of(parentPath), connectionId);
    }

    public static void deleteFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String connectionId) throws SQLException {
        if (md5List.isEmpty())
            return;
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                    "DELETE FROM %s WHERE md5 == ?;", FileSqlHelper.getTableName(driverName)))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public static void deleteFileByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String connectionId) throws SQLException {
        FileSqlHelper.deleteFilesByMd5(driverName, List.of(md5), connectionId);
    }

    // Select

    public static @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> selectFiles(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String connectionId) throws SQLException {
        if (pathList.isEmpty())
            return List.of();
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final List<FileSqlInformation> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE parent_path == ? AND name == ? LIMIT 1;", FileSqlHelper.getTableName(driverName)))) {
                for (final DrivePath path: pathList) {
                    statement.setString(1, path.getParentPath());
                    statement.setString(2, path.getName());
                    try (final ResultSet result = statement.executeQuery()) {
                        list.add(FileSqlHelper.createNextFileInfo(result));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    public static @Nullable FileSqlInformation selectFile(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String connectionId) throws SQLException {
        return FileSqlHelper.selectFiles(driverName, List.of(path), connectionId).get(0);
    }

    public static @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> selectFilesById(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String connectionId) throws SQLException {
        if (idList.isEmpty())
            return List.of();
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final List<FileSqlInformation> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE id == ? LIMIT 1;", FileSqlHelper.getTableName(driverName)))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        list.add(FileSqlHelper.createNextFileInfo(result));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    public static @Nullable FileSqlInformation selectFileById(final @NotNull String driverName, final long id, final @Nullable String connectionId) throws SQLException {
        return FileSqlHelper.selectFilesById(driverName, List.of(id), connectionId).get(0);
    }

    public static @NotNull @UnmodifiableView List<@NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String connectionId) throws SQLException {
        if (parentPathList.isEmpty())
            return List.of();
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final List<List<FileSqlInformation>> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                    "SELECT * FROM %s WHERE parent_path == ?;", FileSqlHelper.getTableName(driverName)))) {
                for (final DrivePath parentPath: parentPathList) {
                    statement.setString(1, parentPath.getPath());
                    try (final ResultSet result = statement.executeQuery()) {
                        list.add(FileSqlHelper.createFilesInfo(result));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    public static @NotNull @UnmodifiableView List<@NotNull FileSqlInformation> selectFileByParentPath(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable String connectionId) throws SQLException {
        return FileSqlHelper.selectFilesByParentPath(driverName, List.of(parentPath), connectionId).get(0);
    }

    public static @NotNull @UnmodifiableView List<@NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String connectionId) throws SQLException {
        if (md5List.isEmpty())
            return List.of();
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final List<List<FileSqlInformation>> list = new LinkedList<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE md5 == ?;", FileSqlHelper.getTableName(driverName)))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    try (final ResultSet result = statement.executeQuery()) {
                        list.add(FileSqlHelper.createFilesInfo(result));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        }
    }

    public static @NotNull @UnmodifiableView List<@NotNull FileSqlInformation> selectFileByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String connectionId) throws SQLException {
        return FileSqlHelper.selectFilesByMd5(driverName, List.of(md5), connectionId).get(0);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFileByParentPathInPage(final @NotNull String driverName, final @NotNull DrivePath parentPath, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String connectionId) throws SQLException {
        try (final Connection connection = FileSqlHelper.DefaultDatabaseUtil.getConnection(connectionId)) {
            connection.setAutoCommit(false);
            final long count;
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                    "SELECT COUNT(*) FROM %s WHERE parent_path == ?;", FileSqlHelper.getTableName(driverName)))) {
                statement.setString(1, parentPath.getPath());
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<FileSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE parent_path == ? ORDER BY ? %s LIMIT ? OFFSET ?;", FileSqlHelper.getTableName(driverName),
                switch (direction) {case ASCEND -> "ASC";case DESCEND -> "DESC";}))) {
                statement.setString(1, parentPath.getPath());
                statement.setString(2, switch (policy) {
                    case FileName -> "name";
                    case Size -> "size";
                    case CreateTime -> "create_time";
                    case UpdateTime -> "update_time";
//                    default -> {throw new IllegalParametersException("Unsupported policy.", policy);}
                });
                statement.setInt(3, limit);
                statement.setLong(4, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = FileSqlHelper.createFilesInfo(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    //TODO Search
}
