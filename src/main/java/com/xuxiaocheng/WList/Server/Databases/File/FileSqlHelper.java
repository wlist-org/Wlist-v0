package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.Contract;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("ClassHasNoToStringMethod")
final class FileSqlHelper {
    private static final @NotNull ConcurrentMap<@NotNull String, @NotNull FileSqlHelper> instances = new ConcurrentHashMap<>();

    public static void initialize(final @NotNull String driverName, final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        final FileSqlHelper helper;
        try {
            helper = FileSqlHelper.instances.computeIfAbsent(driverName, HExceptionWrapper.wrapFunction(k -> new FileSqlHelper(driverName, database, _connectionId)));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
        if (helper != null)
            throw new IllegalStateException("File sql helper for (" + driverName + ") is initialized. instance: " + helper);
    }

    public static @NotNull FileSqlHelper getInstance(final @NotNull String driverName) {
        final FileSqlHelper helper = FileSqlHelper.instances.get(driverName);
        if (helper == null)
            throw new IllegalStateException("File sql helper for (" + driverName + ") is not initialized.");
        return helper;
    }

    private final @NotNull String tableName;
    private final @NotNull DatabaseUtil database;

    private FileSqlHelper(final @NotNull String driverName, final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        super();
        this.tableName = FileSqlHelper.getTableName(driverName);
        this.database = database;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
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
                """, this.tableName));
                statement.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS %s_permissions (
                        rule_id      INTEGER PRIMARY KEY AUTOINCREMENT
                                             UNIQUE
                                             NOT NULL,
                        identifier   TEXT    UNIQUE
                                             NOT NULL,
                        id           INTEGER NOT NULL,
                        group_id     INTEGER NOT NULL
                    );
                """, this.tableName));
                statement.executeUpdate(String.format("""
                    CREATE TRIGGER IF NOT EXISTS %s_deleter AFTER delete ON %s FOR EACH ROW
                    BEGIN
                        DELETE FROM %s_permissions WHERE id == old.id;
                        DELETE FROM %s WHERE parent_path == old.parent_path || '/' || old.name;
                    END;
                """, this.tableName, this.tableName, this.tableName, this.tableName));
                statement.executeUpdate(String.format("""
                    CREATE TRIGGER IF NOT EXISTS %s_updater AFTER update OF is_directory ON %s FOR EACH ROW
                    BEGIN
                        DELETE FROM %s WHERE new.is_directory == 0 AND parent_path == old.parent_path || '/' || old.name;
                    END;
                """, this.tableName, this.tableName, this.tableName));
                statement.executeUpdate(String.format("""
                    CREATE TRIGGER IF NOT EXISTS %s_group_deleter AFTER delete ON groups FOR EACH ROW
                    BEGIN
                        DELETE FROM %s_permissions WHERE group_id == old.group_id;
                    END;
                """, this.tableName, this.tableName));
            }
            connection.commit();
        }
    }

    public static void uninitialize(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        final FileSqlHelper helper = FileSqlHelper.getInstance(driverName);
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = helper.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("DROP TABLE %s;", helper.tableName));
                statement.executeUpdate(String.format("DROP TABLE %s_permissions;", helper.tableName));
                statement.executeUpdate(String.format("DROP TRIGGER %s_deleter;", helper.tableName));
                statement.executeUpdate(String.format("DROP TRIGGER %s_updater;", helper.tableName));
                statement.executeUpdate(String.format("DROP TRIGGER %s_group_deleter;", helper.tableName));
            }
            connection.commit();
        }
    }


    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static @NotNull String getTableName(final @NotNull String name) {
        return "driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_');
    }

    private static @NotNull String serializeTime(final @Nullable LocalDateTime time) {
        return Objects.requireNonNullElseGet(time, LocalDateTime::now).withNano(0).format(FileSqlHelper.DefaultFormatter);
    }

    private static @Nullable FileSqlInformation createNextFileInfo(final @NotNull ResultSet result) throws SQLException {
        return result.next() ? new FileSqlInformation(result.getLong("id"),
                new DrivePath(result.getString("parent_path")).child(result.getString("name")),
                result.getBoolean("is_directory"), result.getLong("size"),
                LocalDateTime.parse(result.getString("create_time"), FileSqlHelper.DefaultFormatter),
                LocalDateTime.parse(result.getString("update_time"), FileSqlHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others")) : null;
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

    private static Pair.@NotNull ImmutablePair<@NotNull FileSqlInformationWithGroups, @NotNull Boolean> createFileInfoWithGroups(final @NotNull ResultSet result) throws SQLException {
        final FileSqlInformation information = new FileSqlInformation(result.getLong("id"),
                new DrivePath(result.getString("parent_path")).child(result.getString("name")),
                result.getBoolean("is_directory"), result.getLong("size"),
                LocalDateTime.parse(result.getString("create_time"), FileSqlHelper.DefaultFormatter),
                LocalDateTime.parse(result.getString("update_time"), FileSqlHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others"));
        final Set<Long> groups = new HashSet<>();
        boolean hasNext;
        while (true) {
            hasNext = result.next();
            if (hasNext && result.getLong("id") == information.id())
                groups.add(result.getLong("group_id"));
            else
                break;
        }
        return Pair.ImmutablePair.makeImmutablePair(new FileSqlInformationWithGroups(information, groups), hasNext);
    }

    private static @NotNull @UnmodifiableView List<@NotNull FileSqlInformationWithGroups> createFilesInfoWithGroups(final @NotNull ResultSet result) throws SQLException {
        final List<FileSqlInformationWithGroups> list = new LinkedList<>();
        while (true) {
            final Pair.ImmutablePair<FileSqlInformationWithGroups, Boolean> info = FileSqlHelper.createFileInfoWithGroups(result);
            list.add(info.getFirst());
            if (!info.getSecond().booleanValue())
                break;
        }
        return Collections.unmodifiableList(list);
    }

    @Contract(pure = true) private static @NotNull String getOrderPolicy(final Options.@NotNull OrderPolicy policy) {
        return switch (policy) {
            case FileName -> "name";
            case Size -> "size";
            case CreateTime -> "create_time";
            case UpdateTime -> "update_time";
        };
    }
    @Contract(pure = true) private static @NotNull String getOrderDirection(final Options.@NotNull OrderDirection policy) {
        return switch (policy) {
            case ASCEND -> "ASC";
            case DESCEND -> "DESC";
        };
    }


    public void insertOrUpdateFiles(final @NotNull Collection<@NotNull FileSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    INSERT INTO %s (id, parent_path, name, is_directory, size, create_time, update_time, md5, others)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        parent_path = excluded.parent_path, name = excluded.name,
                        is_directory = excluded.is_directory, size = excluded.size,
                        create_time = excluded.create_time, update_time = excluded.update_time,
                        md5 = excluded.md5, others = excluded.others;
                """, this.tableName))) {
                for (final FileSqlInformation inserter: inserters) {
                    statement.setLong(1, inserter.id());
                    statement.setString(2, inserter.path().getParentPath());
                    statement.setString(3, inserter.path().getName());
                    statement.setBoolean(4, inserter.isDir());
                    statement.setLong(5, inserter.size());
                    statement.setString(6, FileSqlHelper.serializeTime(inserter.createTime()));
                    statement.setString(7, FileSqlHelper.serializeTime(inserter.updateTime()));
                    statement.setString(8, inserter.md5());
                    statement.setString(9, inserter.others());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s WHERE id == ?;
                """, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteFilesByPathRecursively(final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        if (pathList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s WHERE parent_path == ? AND NAME == ?;
                """, this.tableName))) {
                for (final DrivePath path: pathList) {
                    statement.setString(1, path.getParentPath());
                    statement.setString(2, path.getName());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s WHERE md5 == ?;
                """, this.tableName))) {
                for (final String md5: md5List) {
                    if (md5.isEmpty())
                        continue;
                    statement.setString(1, md5);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, FileSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE id == ? LIMIT 1;
                """, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(id, FileSqlHelper.createNextFileInfo(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformationWithGroups> selectFilesWithGroups(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, FileSqlInformationWithGroups> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s NATURAL JOIN %s_permissions WHERE id == ? LIMIT 1;
                """, this.tableName, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        if (result.next())
                            map.put(id, FileSqlHelper.createFileInfoWithGroups(result).getFirst());
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull FileSqlInformation> selectFilesByPath(final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        if (pathList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<DrivePath, FileSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_path == ? AND name == ? LIMIT 1;
                """, this.tableName))) {
                for (final DrivePath path: pathList) {
                    statement.setString(1, path.getParentPath());
                    statement.setString(2, path.getName());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(path, FileSqlHelper.createNextFileInfo(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull FileSqlInformationWithGroups> selectFilesByPathWithGroups(final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        if (pathList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<DrivePath, FileSqlInformationWithGroups> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s NATURAL JOIN %s_permissions WHERE parent_path == ? AND name == ? LIMIT 1;
                """, this.tableName, this.tableName))) {
                for (final DrivePath path: pathList) {
                    statement.setString(1, path.getParentPath());
                    statement.setString(2, path.getName());
                    try (final ResultSet result = statement.executeQuery()) {
                        if (result.next())
                            map.put(path, FileSqlHelper.createFileInfoWithGroups(result).getFirst());
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, List<FileSqlInformation>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE md5 == ? LIMIT 1;
                """, this.tableName))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(md5, FileSqlHelper.createFilesInfo(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull FileSqlInformationWithGroups>> selectFilesByMd5WithGroups(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, List<FileSqlInformationWithGroups>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s NATURAL JOIN %s_permissions WHERE md5 == ? LIMIT 1;
                """, this.tableName, this.tableName))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    try (final ResultSet result = statement.executeQuery()) {
                        if (result.next())
                            map.put(md5, FileSqlHelper.createFilesInfoWithGroups(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Set<@NotNull Long>> selectFilesIdByParentPath(final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String _connectionId) throws SQLException {
        if (parentPathList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<DrivePath, Set<Long>> map = new HashMap<>(parentPathList.size());
            for (final DrivePath parentPath: parentPathList)
                map.put(parentPath, new HashSet<>());
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT id FROM %s WHERE parent_path == ?;
                """, this.tableName))) {
                for (final DrivePath parentPath: parentPathList) {
                    statement.setString(1, parentPath.getPath());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.get(parentPath).add(result.getLong("id"));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Set<@NotNull Long>> selectFilesIdByParentPathRecursively(final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String _connectionId) throws SQLException {
        if (parentPathList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<DrivePath, Set<Long>> map = new HashMap<>(parentPathList.size());
            for (final DrivePath parentPath: parentPathList)
                map.put(parentPath, new HashSet<>());
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT id FROM %s WHERE parent_path == ?;
                """, this.tableName))) {
                for (final DrivePath parentPath: parentPathList) {
                    statement.setString(1, parentPath.getPath());
                    try (final ResultSet result = statement.executeQuery()) {
                        while (result.next())
                            map.get(parentPath).add(result.getLong("id"));
                    }
                }
            }
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT id FROM %s WHERE parent_path GLOB ?;
                """, this.tableName))) {
                for (final DrivePath parentPath: parentPathList) {
                    statement.setString(1, parentPath.getPath() + "/*");
                    try (final ResultSet result = statement.executeQuery()) {
                        while (result.next())
                            map.get(parentPath).add(result.getLong("id"));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Long> selectFilesCountByParentPath(final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String _connectionId) throws SQLException {
        if (parentPathList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<DrivePath, Long> map = new HashMap<>(parentPathList.size());
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT COUNT(*) FROM %s WHERE parent_path == ?;
                """, this.tableName))) {
                for (final DrivePath parentPath: parentPathList) {
                    statement.setString(1, parentPath.getPath());
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        map.put(parentPath, result.getLong(1));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Long> selectFilesCountByParentPathRecursively(final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String _connectionId) throws SQLException {
        if (parentPathList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<DrivePath, Long> source = this.selectFilesCountByParentPath(parentPathList, connectionId.get());
            final Map<DrivePath, Long> map = new HashMap<>(parentPathList.size());
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT COUNT(*) FROM %s WHERE parent_path GLOB ?;
                """, this.tableName))) {
                for (final DrivePath parentPath: parentPathList) {
                    statement.setString(1, parentPath.getPath() + "/*");
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        map.put(parentPath, source.get(parentPath).longValue() + result.getLong(1));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentPathInPage(final @NotNull DrivePath parentPath, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final long count = this.selectFilesCountByParentPath(List.of(parentPath), connectionId.get()).get(parentPath).longValue();
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<FileSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_path == ? ORDER BY ? %s LIMIT ? OFFSET ?;
                """, this.tableName, FileSqlHelper.getOrderDirection(direction)))) {
                statement.setString(1, parentPath.getPath());
                statement.setString(2, FileSqlHelper.getOrderPolicy(policy));
                statement.setInt(3, limit);
                statement.setLong(4, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = FileSqlHelper.createFilesInfo(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformationWithGroups>> selectFilesByParentPathInPageWithGroups(final @NotNull DrivePath parentPath, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final long count = this.selectFilesCountByParentPath(List.of(parentPath), connectionId.get()).get(parentPath).longValue();
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<FileSqlInformationWithGroups> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s NATURAL JOIN %s_permissions WHERE parent_path == ? ORDER BY ? %s LIMIT ? OFFSET ?;
                """, this.tableName, this.tableName, FileSqlHelper.getOrderDirection(direction)))) {
                statement.setString(1, parentPath.getPath());
                statement.setString(2, FileSqlHelper.getOrderPolicy(policy));
                statement.setInt(3, limit);
                statement.setLong(4, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = FileSqlHelper.createFilesInfoWithGroups(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    public @NotNull @UnmodifiableView List<@Nullable FileSqlInformationWithGroups> searchFilesByNameInParentPathRecursivelyLimitedWithGroups(final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<FileSqlInformationWithGroups> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s NATURAL JOIN %s_permissions WHERE parent_path GLOB ? AND name %s ?
                    ORDER BY abs(length(name) - ?) ASC, id DESC LIMIT ?;
                """, this.tableName, this.tableName, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, parentPath.getPath() + "/*");
                statement.setString(2, rule);
                statement.setInt(3, rule.length());
                statement.setInt(4, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = FileSqlHelper.createFilesInfoWithGroups(result);
                }
            }
            return list;
        }
    }


    public void insertGroupsForEachFile(final @NotNull Collection<@NotNull Long> idList, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty() || groups.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    INSERT OR IGNORE INTO %s_permissions (identifier, id, group_id)
                        VALUES (?, ?, ?);
                """, this.tableName))) {
                for (final Long id: idList)
                    for (final Long groupId: groups) {
                        statement.setString(1, String.format("%d %d", id.longValue(), groupId.longValue()));
                        statement.setLong(2, id.longValue());
                        statement.setLong(3, groupId.longValue());
                        statement.executeUpdate();
                    }
            }
        }
    }

    public void deleteGroupsForEachFile(final @NotNull Collection<@NotNull Long> idList, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty() || groups.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s_permissions WHERE id == ? AND group_id == ?;
                """, this.tableName))) {
                for (final Long id: idList)
                    for (final Long groupId: groups) {
                        statement.setLong(1, id.longValue());
                        statement.setLong(2, groupId.longValue());
                        statement.executeUpdate();
                    }
            }
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Set<@NotNull Long>> selectGroupsForEachFile(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, Set<Long>> map = new HashMap<>(idList.size());
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT group_id FROM %s_permissions WHERE id == ?;
                """, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        final Set<Long> set = new HashSet<>();
                        while (result.next())
                            set.add(result.getLong("1"));
                        map.put(id, set);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }
}
