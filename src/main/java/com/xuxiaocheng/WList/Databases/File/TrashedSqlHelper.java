package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class TrashedSqlHelper {
    private static final @NotNull ConcurrentMap<@NotNull String, @NotNull TrashedSqlHelper> instances = new ConcurrentHashMap<>();

    public static void initialize(final @NotNull String driverName, final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        final boolean[] flag = {true};
        try {
            TrashedSqlHelper.instances.computeIfAbsent(driverName, HExceptionWrapper.wrapFunction(k -> {
                flag[0] = false;
                return new TrashedSqlHelper(driverName, database, _connectionId);
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
        if (flag[0])
            throw new IllegalStateException("Trashed file sql helper for (" + driverName + ") is initialized.");
    }

    public static @NotNull TrashedSqlHelper getInstance(final @NotNull String driverName) {
        final TrashedSqlHelper helper = TrashedSqlHelper.instances.get(driverName);
        if (helper == null)
            throw new IllegalStateException("Trashed file sql helper for (" + driverName + ") is not initialized.");
        return helper;
    }

    private final @NotNull String tableName;
    private final @NotNull DatabaseUtil database;

    private TrashedSqlHelper(final @NotNull String driverName, final @NotNull DatabaseUtil database, final @Nullable String _connectionId) throws SQLException {
        super();
        this.tableName = TrashedSqlHelper.getTableName(driverName);
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
                        name         TEXT    NOT NULL,
                        is_directory INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (is_directory == 1 OR is_directory == 0),
                        size         INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (size >= -1),
                        create_time  TEXT    NOT NULL,
                        trashed_time TEXT    NOT NULL,
                        expire_time  TEXT    NOT NULL,
                        md5          TEXT    NOT NULL,
                        others       TEXT
                    );
                """, this.tableName));
            }
            connection.commit();
        }
    }

    public static void uninitialize(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        final TrashedSqlHelper helper = TrashedSqlHelper.getInstance(driverName);
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = helper.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("DROP TABLE %s;", helper.tableName));
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull String toString() {
        return "TrashedSqlHelper{" +
                "tableName='" + this.tableName + '\'' +
                ", database=" + this.database +
                '}';
    }


    private static @NotNull String getTableName(final @NotNull String name) {
        return FileSqlHelper.getTableName(name) + "_trash";
    }

    private static @Nullable TrashedSqlInformation createNextFileInfo(final @NotNull ResultSet result) throws SQLException {
        return result.next() ? new TrashedSqlInformation(result.getLong("id"), result.getString("name"),
                result.getBoolean("is_directory"), result.getLong("size"),
                LocalDateTime.parse(result.getString("create_time"), FileSqlHelper.DefaultFormatter),
                LocalDateTime.parse(result.getString("trashed_time"), FileSqlHelper.DefaultFormatter),
                LocalDateTime.parse(result.getString("expire_time"), FileSqlHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others")) : null;
    }

    private static @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation> createFilesInfo(final @NotNull ResultSet result) throws SQLException {
        final List<TrashedSqlInformation> list = new LinkedList<>();
        while (true) {
            final TrashedSqlInformation info = TrashedSqlHelper.createNextFileInfo(result);
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
            case UpdateTime -> "expire_time";
        };
    }


    public void insertOrUpdateFiles(final @NotNull Collection<@NotNull TrashedSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    INSERT INTO %s (id, name, is_directory, size, create_time, trashed_time, expire_time, md5, others)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        name = excluded.name, is_directory = excluded.is_directory,
                        size = excluded.size, create_time = excluded.create_time,
                        trashed_time = excluded.trashed_time, expire_time = excluded.expire_time,
                        md5 = excluded.md5, others = excluded.others;
                """, this.tableName))) {
                for (final TrashedSqlInformation inserter: inserters) {
                    statement.setLong(1, inserter.id());
                    statement.setString(2, inserter.name());
                    statement.setBoolean(3, inserter.isDir());
                    statement.setLong(4, inserter.size());
                    statement.setString(5, FileSqlHelper.serializeTime(inserter.createTime()));
                    statement.setString(6, FileSqlHelper.serializeTime(inserter.trashedTime()));
                    statement.setString(7, FileSqlHelper.serializeTime(inserter.expireTime()));
                    statement.setString(8, inserter.md5());
                    statement.setString(9, inserter.others());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    public void deleteFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
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

    public void deleteFilesByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s WHERE name == ?;
                """, this.tableName))) {
                for (final String name: nameList) {
                    statement.setString(1, name);
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

    public void clear(final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s;
                """, this.tableName))) {
                statement.executeUpdate();
            }
            connection.commit();
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull TrashedSqlInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<Long, TrashedSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE id == ? LIMIT 1;
                """, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(id, TrashedSqlHelper.createNextFileInfo(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull List<@NotNull TrashedSqlInformation>> selectFilesByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, List<TrashedSqlInformation>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE name == ?;
                """, this.tableName))) {
                for (final String name: nameList) {
                    statement.setString(1, name);
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(name, TrashedSqlHelper.createFilesInfo(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return Map.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, List<TrashedSqlInformation>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE md5 == ? LIMIT 1;
                """, this.tableName))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(md5, TrashedSqlHelper.createFilesInfo(result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    public @NotNull @UnmodifiableView Set<@NotNull Long> selectFilesId(final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Set<Long> set = new HashSet<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT id FROM %s;
                """, this.tableName))) {
                try (final ResultSet result = statement.executeQuery()) {
                    while (result.next())
                        set.add(result.getLong("id"));
                }
            }
            return Collections.unmodifiableSet(set);
        }
    }

    public long selectFileCount(final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT COUNT(*) FROM %s;
                """, this.tableName))) {
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    return result.getLong(1);
                }
            }
        }
    }

    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final long count = this.selectFileCount(connectionId.get());
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<TrashedSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s ORDER BY ? %s LIMIT ? OFFSET ?;
                """, this.tableName, FileSqlHelper.getOrderDirection(direction)))) {
                statement.setString(1, FileSqlHelper.getOrderPolicy(policy));
                statement.setInt(2, limit);
                statement.setLong(3, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = TrashedSqlHelper.createFilesInfo(result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    public @NotNull @UnmodifiableView List<@Nullable TrashedSqlInformation> searchFilesByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.database.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final List<TrashedSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE name %s ? ORDER BY abs(length(name) - ?) ASC, id DESC LIMIT ?;
                """, this.tableName, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, rule);
                statement.setInt(2, rule.length());
                statement.setInt(3, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = TrashedSqlHelper.createFilesInfo(result);
                }
            }
            return list;
        }
    }
}
