package com.xuxiaocheng.WList.Databases.File;

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
import java.util.stream.Collectors;

final class FileSqlHelper {
    private static final @NotNull ConcurrentMap<@NotNull String, @NotNull FileSqlHelper> instances = new ConcurrentHashMap<>();

    public static void initialize(final @NotNull String driverName, final long rootId, final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        final boolean[] flag = {true};
        try {
            FileSqlHelper.instances.computeIfAbsent(driverName, HExceptionWrapper.wrapFunction(k -> {
                flag[0] = false;
                return new FileSqlHelper(driverName, rootId, database, _connectionId);
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
        if (flag[0])
            throw new IllegalStateException("File sql helper for (" + driverName + ") is initialized.");
    }

    public static @NotNull FileSqlHelper getInstance(final @NotNull String driverName) {
        final FileSqlHelper helper = FileSqlHelper.instances.get(driverName);
        if (helper == null)
            throw new IllegalStateException("File sql helper for (" + driverName + ") is not initialized.");
        return helper;
    }

    private final @NotNull String tableName;
    private final long rootId;
    private final @NotNull DatabaseUtil database;

    private FileSqlHelper(final @NotNull String driverName, final long rootId, final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        super();
        this.tableName = FileSqlHelper.getTableName(driverName);
        this.rootId = rootId;
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
                        parent_id    INTEGER ,
                        name         TEXT    NOT NULL,
                        is_directory INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (is_directory == 1 OR is_directory == 0),
                        size         INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (size >= -1),
                        create_time  TEXT    NOT NULL,
                        update_time  TEXT    NOT NULL,
                        md5          TEXT    NOT NULL
                                             CHECK (len(md5) == 32),
                        others       TEXT
                    );
                """, this.tableName));
                statement.executeUpdate(String.format("""
                    CREATE INDEX IF NOT EXISTS %s_location ON %s (name, parent_id);
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
                statement.executeUpdate(String.format("DROP TABLE IF EXISTS %s;", helper.tableName));
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull String toString() {
        return "FileSqlHelper{" +
                "tableName='" + this.tableName + '\'' +
                ", database=" + this.database +
                '}';
    }

    static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    static @NotNull String getTableName(final @NotNull String name) {
        return "driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_');
    }

    static @NotNull String serializeTime(final @Nullable LocalDateTime time) {
        return Objects.requireNonNullElseGet(time, LocalDateTime::now).withNano(0).format(FileSqlHelper.DefaultFormatter);
    }

    private static @Nullable FileSqlInformation createNextFileInfo(final @NotNull ResultSet result) throws SQLException {
        return result.next() ? new FileSqlInformation(result.getLong("id"),
                result.getLong("parent_id"), result.getString("name"),
                result.getBoolean("is_directory"), result.getLong("size"),
                LocalDateTime.parse(result.getString("create_time"), FileSqlHelper.DefaultFormatter),
                LocalDateTime.parse(result.getString("update_time"), FileSqlHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others")) : null;
    }

    private static @NotNull @UnmodifiableView Set<@NotNull FileSqlInformation> createFilesInfo(final @NotNull ResultSet result) throws SQLException {
        final Set<FileSqlInformation> set = new HashSet<>();
        while (true) {
            final FileSqlInformation info = FileSqlHelper.createNextFileInfo(result);
            if (info == null)
                break;
            set.add(info);
        }
        return Collections.unmodifiableSet(set);
    }

    private static @NotNull @UnmodifiableView List<@NotNull FileSqlInformation> createFilesInfoInOrder(final @NotNull ResultSet result) throws SQLException {
        final List<FileSqlInformation> list = new LinkedList<>();
        while (true) {
            final FileSqlInformation info = FileSqlHelper.createNextFileInfo(result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }

    @Contract(pure = true) static @NotNull String getOrderPolicy(final Options.@NotNull OrderPolicy policy) {
        return switch (policy) {
            case FileName -> "name";
            case Size -> "size";
            case CreateTime -> "create_time";
            case UpdateTime -> "update_time";
        };
    }
    @Contract(pure = true) static @NotNull String getOrderDirection(final Options.@NotNull OrderDirection policy) {
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
                    INSERT INTO %s (id, parent_id, name, is_directory, size, create_time, update_time, md5, others)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        parent_id = excluded.parent_id, name = excluded.name,
                        is_directory = excluded.is_directory, size = excluded.size,
                        create_time = excluded.create_time, update_time = excluded.update_time,
                        md5 = excluded.md5, others = excluded.others;
                """, this.tableName))) {
                for (final FileSqlInformation inserter: inserters) {
                    statement.setLong(1, inserter.id());
                    statement.setLong(2, inserter.parentId());
                    statement.setString(3, inserter.name());
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

    @Deprecated
    public @Nullable FileSqlInformation selectFileByPath(final @NotNull DrivePath path, final @Nullable String _connectionId) throws SQLException {
        FileSqlInformation information = this.selectFiles(List.of(this.rootId), _connectionId).get(this.rootId);
        if (path.getDepth() == 0 || information == null)
            return information;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_id == ? AND name == ? LIMIT 1;
                """, this.tableName))) {
                for (final String name: path) {
                    statement.setLong(1, information.id());
                    statement.setString(2, name);
                    try (final ResultSet result = statement.executeQuery()) {
                        information = FileSqlHelper.createNextFileInfo(result);
                        if (information == null)
                            return null;
                    }
                }
            }
            return information;
        }
    }

    public @Nullable FileSqlInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final FileSqlInformation information;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_id == ? AND name == ? LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, parentId);
                statement.setString(2, name);
                try (final ResultSet result = statement.executeQuery()) {
                    information = FileSqlHelper.createNextFileInfo(result);
                }
            }
            return information;
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, Set<FileSqlInformation>> map = new HashMap<>();
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

    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        if (parentIdList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, Set<Long>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT id FROM %s WHERE parent_id == ?;
                """, this.tableName))) {
                for (final Long parentId: parentIdList) {
                    statement.setLong(1, parentId.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        final Set<Long> set = new HashSet<>();
                        while (result.next())
                            set.add(result.getLong("id"));
                        map.put(parentId, set);
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        if (parentIdList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, Long> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT COUNT(*) FROM %s WHERE parent_id == ?;
                """, this.tableName))) {
                for (final Long parentId: parentIdList) {
                    statement.setLong(1, parentId.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        map.put(parentId, result.getLong(1));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentIdInPage(final long parentId, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final long count = this.selectFilesCountByParentId(List.of(parentId), connectionId.get()).get(parentId).longValue();
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<FileSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_id == ? ORDER BY ? ? LIMIT ? OFFSET ?;
                """, this.tableName))) {
                statement.setLong(1, parentId);
                statement.setString(2, FileSqlHelper.getOrderPolicy(policy));
                statement.setString(3, FileSqlHelper.getOrderDirection(direction));
                statement.setInt(4, limit);
                statement.setLong(5, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = FileSqlHelper.createFilesInfoInOrder(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    public void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Collection<Long> leave = new HashSet<>();
            final Collection<Long> set = new HashSet<>(idList);
            while (!set.isEmpty()) {
                final Map<Long, Set<Long>> maps = this.selectFilesIdByParentId(set, connectionId.get());
                set.clear();
                maps.forEach((key, value) -> {
                    leave.add(key);
                    set.addAll(value);
                });
            }
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s WHERE id == ?;
                """, this.tableName))) {
                for (final Long id: leave) {
                    statement.setLong(1, id.longValue());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteFilesByMd5Recursively(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            this.deleteFilesRecursively(this.selectFilesByMd5(md5List, connectionId.get()).values().stream().filter(Objects::nonNull)
                    .flatMap(Set::stream).map(FileSqlInformation::id).toList(), connectionId.get());
            connection.commit();
        }
    }

    // TODO Better Search

    public @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathLimited(final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<FileSqlInformation> set;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_path == ? AND name %s ? ORDER BY abs(length(name) - ?) ASC, id DESC LIMIT ?;
                """, this.tableName, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, parentPath.getPath());
                statement.setString(2, rule);
                statement.setInt(3, rule.length());
                statement.setInt(4, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    set = FileSqlHelper.createFilesInfoInOrder(result);
                }
            }
            return set;
        }
    }

    public @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathRecursivelyLimited(final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<FileSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_path GLOB ? AND name %s ? ORDER BY abs(length(name) - ?) ASC, id DESC LIMIT ?;
                """, this.tableName, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, parentPath.getPath() + "/*");
                statement.setString(2, rule);
                statement.setInt(3, rule.length());
                statement.setInt(4, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = FileSqlHelper.createFilesInfoInOrder(result);
                }
            }
            return list;
        }
    }
}
