package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AStreams;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class FileSqlHelper implements FileSqlInterface {
    @Contract(pure = true) private static @NotNull String getTableName(final @NotNull String name) {
        return "driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_');
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

    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final @NotNull PooledDatabaseInterface database;
    private final @NotNull String driverName;
    private final @NotNull String tableName;
    private final long rootId;

    public FileSqlHelper(final @NotNull PooledDatabaseInterface database, final @NotNull String driverName, final long rootId) {
        super();
        this.database = database;
        this.driverName = driverName;
        this.tableName = FileSqlHelper.getTableName(driverName);
        this.rootId = rootId;
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
                        parent_id    INTEGER ,
                        name         TEXT    NOT NULL,
                        type         INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (type == 0 OR type == 1 OR type == 2),
                        size         INTEGER NOT NULL
                                             DEFAULT (0)
                                             CHECK (size >= -1),
                        create_time  TEXT    NOT NULL,
                        update_time  TEXT    NOT NULL,
                        md5          TEXT    NOT NULL
                                             CHECK (length(md5) == 32 OR md5 == ''),
                        others       TEXT
                    );
                """, this.tableName));
                statement.executeUpdate(String.format("""
                    CREATE INDEX IF NOT EXISTS %s_location ON %s (name, parent_id);
                """, this.tableName, this.tableName));
                statement.executeUpdate(String.format("""
                    CREATE TRIGGER IF NOT EXISTS %s_inserter AFTER insert ON %s FOR EACH ROW
                    BEGIN
                        UPDATE %s SET type = %d WHERE id == new.parent_id;
                    END;
                """, this.tableName, this.tableName, this.tableName, FileSqlInterface.FileSqlType.Directory.ordinal()));
            }
            connection.commit();
        }
    }

    @Override
    public void deleteTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("DROP TABLE IF EXISTS %s;", this.tableName));
                statement.executeUpdate(String.format("DROP TRIGGER IF EXISTS %s_inserter;", this.tableName));
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull String getDriverName() {
        return this.driverName;
    }

    @Override
    public long getRootId() {
        return this.rootId;
    }

    private static @Nullable FileSqlInformation createNextFileInfo(final @NotNull String driver, final @NotNull ResultSet result) throws SQLException {
        final @NotNull String createTime = result.getString("create_time");
        final @NotNull String updateTime = result.getString("update_time");
        return result.next() ? new FileSqlInformation(new FileLocation(driver, result.getLong("id")),
                result.getLong("parent_id"), result.getString("name"),
                FileSqlInterface.FileSqlType.values()[result.getInt("type")], result.getLong("size"),
                createTime.isEmpty() ? null : LocalDateTime.parse(createTime, FileSqlHelper.DefaultFormatter),
                updateTime.isEmpty() ? null : LocalDateTime.parse(updateTime, FileSqlHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others")) : null;
    }

    private static @NotNull @UnmodifiableView Set<@NotNull FileSqlInformation> createFilesInfo(final @NotNull String driver, final @NotNull ResultSet result) throws SQLException {
        final Set<FileSqlInformation> set = new HashSet<>();
        while (true) {
            final FileSqlInformation info = FileSqlHelper.createNextFileInfo(driver, result);
            if (info == null)
                break;
            set.add(info);
        }
        return Collections.unmodifiableSet(set);
    }

    private static @NotNull @UnmodifiableView List<@NotNull FileSqlInformation> createFilesInfoInOrder(final @NotNull String driver, final @NotNull ResultSet result) throws SQLException {
        final List<FileSqlInformation> list = new LinkedList<>();
        while (true) {
            final FileSqlInformation info = FileSqlHelper.createNextFileInfo(driver, result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public void insertOrUpdateFiles(final @NotNull Collection<@NotNull FileSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    INSERT INTO %s (id, parent_id, name, type, size, create_time, update_time, md5, others)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        parent_id = excluded.parent_id, name = excluded.name,
                        type = excluded.type, size = excluded.size,
                        create_time = excluded.create_time, update_time = excluded.update_time,
                        md5 = excluded.md5, others = excluded.others;
                """, this.tableName))) {
                for (final FileSqlInformation inserter: inserters) {
                    statement.setLong(1, inserter.id());
                    statement.setLong(2, inserter.parentId());
                    statement.setString(3, inserter.name());
                    statement.setInt(4, inserter.type().ordinal());
                    statement.setLong(5, inserter.size());
                    statement.setString(6, inserter.createTime() == null ? "" : inserter.createTime().format(FileSqlHelper.DefaultFormatter));
                    statement.setString(7, inserter.updateTime() == null ? "" : inserter.updateTime().format(FileSqlHelper.DefaultFormatter));
                    statement.setString(8, inserter.md5());
                    statement.setString(9, inserter.others());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    @Override
    public void updateDirectoryType(final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    UPDATE %s SET type = ? WHERE id == ?;
                """, this.tableName))) {
                statement.setInt(1, (empty ? FileSqlInterface.FileSqlType.EmptyDirectory : FileSqlInterface.FileSqlType.Directory).ordinal());
                statement.setLong(2, id);
                statement.executeUpdate();
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final Map<Long, FileSqlInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE id == ? LIMIT 1;
                """, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(id, FileSqlHelper.createNextFileInfo(this.driverName, result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public @Nullable FileSqlInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final FileSqlInformation information;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_id == ? AND name == ? LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, parentId);
                statement.setString(2, name);
                try (final ResultSet result = statement.executeQuery()) {
                    information = FileSqlHelper.createNextFileInfo(this.driverName, result);
                }
            }
            return information;
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            connection.setAutoCommit(false);
            final Map<String, Set<FileSqlInformation>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE md5 == ? LIMIT 1;
                """, this.tableName))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(md5, FileSqlHelper.createFilesInfo(this.driverName, result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        if (parentIdList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        if (parentIdList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
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

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentIdInPage(final long parentId, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
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
                    list = FileSqlHelper.createFilesInfoInOrder(this.driverName, result);
                }
            }
            return Pair.ImmutablePair.makeImmutablePair(count, list);
        }
    }

    @Override
    public void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
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

    @Override
    public void deleteFilesByMd5Recursively(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            this.deleteFilesRecursively(AStreams.streamToList(this.selectFilesByMd5(md5List, connectionId.get()).values().stream()
                    .filter(Objects::nonNull).flatMap(Set::stream).map(FileSqlInformation::id)), connectionId.get());
            connection.commit();
        }
    }

    @Override
    public @NotNull String toString() {
        return "FileSqlHelper{" +
                "database=" + this.database +
                ", driverName='" + this.driverName + '\'' +
                ", tableName='" + this.tableName + '\'' +
                ", rootId=" + this.rootId +
                '}';
    }
}
