package com.xuxiaocheng.WList.Databases.TrashedFile;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabaseInterface;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Options;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class TrashedSqlHelper implements TrashedSqlInterface {
    @Contract(pure = true) private static @NotNull String getTableName(final @NotNull String name) {
        return "driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_') + "_trash";
    }

    @Contract(pure = true) private static @NotNull String getOrderPolicy(final Options.@NotNull OrderPolicy policy) {
        return switch (policy) {
            case FileName -> "name";
            case Size -> "size";
            case CreateTime -> "create_time";
            case UpdateTime -> "expire_time";
        };
    }
    @Contract(pure = true) private static @NotNull String getOrderDirection(final Options.@NotNull OrderDirection policy) {
        return switch (policy) {
            case ASCEND -> "ASC";
            case DESCEND -> "DESC";
        };
    }

    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final @NotNull PooledDatabaseInterface database;
    private final @NotNull String driverName;
    private final @NotNull String tableName;

    public TrashedSqlHelper(final @NotNull PooledDatabaseInterface database, final @NotNull String driverName) {
        super();
        this.database = database;
        this.driverName = driverName;
        this.tableName = TrashedSqlHelper.getTableName(driverName);
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }

    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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
                        md5          TEXT    NOT NULL
                                             CHECK (length(md5) == 32 OR md5 == ''),
                        others       TEXT
                    );
                """, this.tableName));
            }
            connection.commit();
        }
    }

    @Override
    public void deleteTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("DROP TABLE %s;", this.tableName));
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull String getDriverName() {
        return this.driverName;
    }

    private static @Nullable TrashedSqlInformation createNextFileInfo(final @NotNull String driverName, final @NotNull ResultSet result) throws SQLException {
        final @NotNull String createTime = result.getString("create_time");
        final @NotNull String trashedTime = result.getString("trashed_time");
        return result.next() ? new TrashedSqlInformation(new FileLocation(driverName, result.getLong("id")), result.getString("name"),
                result.getBoolean("is_directory"), result.getLong("size"),
                createTime.isEmpty() ? null : LocalDateTime.parse(createTime, TrashedSqlHelper.DefaultFormatter),
                trashedTime.isEmpty() ? null : LocalDateTime.parse(trashedTime, TrashedSqlHelper.DefaultFormatter),
                LocalDateTime.parse(result.getString("expire_time"), TrashedSqlHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others")) : null;
    }

    private static @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation> createFilesInfo(final @NotNull String driverName, final @NotNull ResultSet result) throws SQLException {
        final List<TrashedSqlInformation> list = new LinkedList<>();
        while (true) {
            final TrashedSqlInformation info = TrashedSqlHelper.createNextFileInfo(driverName, result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public void insertOrUpdateFiles(final @NotNull Collection<@NotNull TrashedSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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
                    statement.setBoolean(3, inserter.isDirectory());
                    statement.setLong(4, inserter.size());
                    statement.setString(5, inserter.createTime() == null ? "" : inserter.createTime().format(TrashedSqlHelper.DefaultFormatter));
                    statement.setString(6, inserter.trashedTime() == null ? "" : inserter.trashedTime().format(TrashedSqlHelper.DefaultFormatter));
                    statement.setString(7, inserter.expireTime() == null ? "" : inserter.expireTime().format(TrashedSqlHelper.DefaultFormatter));
                    statement.setString(8, inserter.md5());
                    statement.setString(9, inserter.others());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull TrashedSqlInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final Map<Long, TrashedSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE id == ? LIMIT 1;
                """, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(id, TrashedSqlHelper.createNextFileInfo(this.driverName, result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull List<@NotNull TrashedSqlInformation>> selectFilesByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final Map<String, List<TrashedSqlInformation>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE name == ?;
                """, this.tableName))) {
                for (final String name: nameList) {
                    statement.setString(1, name);
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(name, TrashedSqlHelper.createFilesInfo(this.driverName, result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final Map<String, List<TrashedSqlInformation>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE md5 == ? LIMIT 1;
                """, this.tableName))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(md5, TrashedSqlHelper.createFilesInfo(this.driverName, result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public @NotNull @UnmodifiableView Set<@NotNull Long> selectFilesId(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public long selectFileCount(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final long count = this.selectFileCount(connectionId.get());
            if (offset >= count)
                return Pair.ImmutablePair.makeImmutablePair(count, List.of());
            final List<TrashedSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s ORDER BY ? %s LIMIT ? OFFSET ?;
                """, this.tableName, TrashedSqlHelper.getOrderDirection(direction)))) {
                statement.setString(1, TrashedSqlHelper.getOrderPolicy(policy));
                statement.setInt(2, limit);
                statement.setLong(3, offset);
                try (final ResultSet result = statement.executeQuery()) {
                    list = TrashedSqlHelper.createFilesInfo(this.driverName, result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    @Override
    public void deleteFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public void deleteFilesByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        if (nameList.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public void deleteFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public void clear(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    DELETE FROM %s;
                """, this.tableName))) {
                statement.executeUpdate();
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull @UnmodifiableView List<@Nullable TrashedSqlInformation> searchFilesByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        if (limit <= 0)
            return List.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final List<TrashedSqlInformation> list;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE name %s ? ORDER BY abs(length(name) - ?) ASC, id DESC LIMIT ?;
                """, this.tableName, caseSensitive ? "GLOB" : "LIKE"))) {
                statement.setString(1, rule);
                statement.setInt(2, rule.length());
                statement.setInt(3, limit);
                try (final ResultSet result = statement.executeQuery()) {
                    list = TrashedSqlHelper.createFilesInfo(this.driverName, result);
                }
            }
            return list;
        }
    }

    @Override
    public @NotNull String toString() {
        return "TrashedSqlHelper{" +
                "database=" + this.database +
                ", driverName='" + this.driverName + '\'' +
                ", tableName='" + this.tableName + '\'' +
                '}';
    }
}
