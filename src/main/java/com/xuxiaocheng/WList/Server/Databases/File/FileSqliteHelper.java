package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlHelper;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class FileSqliteHelper implements FileSqlInterface {
    @Contract(pure = true)
    protected static @NotNull String getTableName(final @NotNull String name) {
        return "provider_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_');
    }

    protected final @NotNull SqlDatabaseInterface database;
    protected final @NotNull String providerName;
    protected final long rootId;
    protected final @NotNull String tableName;
    protected final long doubleRootId;

    public FileSqliteHelper(final @NotNull SqlDatabaseInterface database, final @NotNull String providerName, final long rootId) {
        super();
        this.database = database;
        this.providerName = providerName;
        this.rootId = rootId;
        this.tableName = FileSqliteHelper.getTableName(providerName);
        this.doubleRootId = FileSqlInterface.getDoubleId(this.rootId, true);
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }


    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
    CREATE TABLE IF NOT EXISTS %s (
        double_id   INTEGER     PRIMARY KEY
                                UNIQUE
                                NOT NULL,
        parent_id   INTEGER     NOT NULL,
        name        TEXT        NOT NULL,
        name_order  BLOB        NOT NULL,
        size        INTEGER     NOT NULL
                                DEFAULT (0)
                                CHECK (size >= -1),
        create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
        update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
        others      TEXT        DEFAULT (NULL)
    );
                """, this.tableName));
                statement.executeUpdate(String.format("""
    CREATE INDEX IF NOT EXISTS %s_name ON %s (name);
                """, this.tableName, this.tableName));
                statement.executeUpdate(String.format("""
    CREATE INDEX IF NOT EXISTS %s_parent ON %s (parent_id);
                """, this.tableName, this.tableName));
            }
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT double_id FROM %s WHERE double_id == parent_id LIMIT 1;
                """, this.tableName))) {
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next() && this.doubleRootId != result.getLong(1))
                        throw new IllegalStateException("Duplicate database root id." + ParametersMap.create()
                                .add("existed", FileSqlInterface.getRealId(result.getLong(1))).add("new", this.rootId).add("provider", this.providerName));
                }
            }
            final boolean newer;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT parent_id FROM %s WHERE double_id == ? LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, this.doubleRootId);
                try (final ResultSet result = statement.executeQuery()) {
                    newer = !result.next() || result.getLong(1) != this.doubleRootId;
                }
            }
            if (newer)
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    INSERT INTO %s (double_id, parent_id, name, name_order, size)
        VALUES (?, ?, ?, ?, ?);
                        """, this.tableName))) {
                    statement.setLong(1, this.doubleRootId);
                    statement.setLong(2, this.doubleRootId);
                    statement.setString(3, this.providerName);
                    statement.setBytes(4, SqlHelper.toOrdered(this.providerName));
                    statement.setLong(5, -1);
                    statement.executeUpdate();
                }
            connection.commit();
        }
    }

    @Override
    public void deleteTable(final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate(String.format("""
    DROP TABLE IF EXISTS %s;
                    """, this.tableName));
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull String getProviderName() {
        return this.providerName;
    }

    @Override
    public long getRootId() {
        return this.rootId;
    }


    public static final @NotNull String FileInfoExtra = "double_id, parent_id, name, size, create_time, update_time, others";

    public static @Nullable FileInformation nextFile(final @NotNull ResultSet result) throws SQLException {
        if (!result.next())
            return null;
        final long doubleId = result.getLong("double_id");
        final long doubleParentId = result.getLong("parent_id");
        final boolean directory = FileSqlInterface.isDirectory(doubleId);
        final String name = result.getString("name");
        final long size = result.getLong("size");
        final Timestamp createTime = result.getTimestamp("create_time");
        final Timestamp updateTime = result.getTimestamp("update_time");
        final String others = result.getString("others");
        return new FileInformation(FileSqlInterface.getRealId(doubleId), FileSqlInterface.getRealId(doubleParentId), name, directory, size,
                createTime == null ? null : SqlHelper.toZonedDataTime(createTime),
                updateTime == null ? null : SqlHelper.toZonedDataTime(updateTime),
                others);
    }

    public static @NotNull @Unmodifiable List<@NotNull FileInformation> allFiles(final @NotNull ResultSet result) throws SQLException {
        final List<FileInformation> list = new LinkedList<>();
        while (true) {
            final FileInformation info = FileSqliteHelper.nextFile(result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }


    /* --- Insert --- */

    @Override
    public void insertFileOrDirectory(final @NotNull FileInformation information, final @Nullable String _connectionId) throws SQLException {
        assert information.isDirectory() ? information.size() == 0 : information.size() >= 0;
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    INSERT INTO %s (double_id, parent_id, name, name_order, size, create_time, update_time, others)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                """, this.tableName))) {
                statement.setLong(1, FileSqlInterface.getDoubleId(information.id(), information.isDirectory()));
                statement.setLong(2, FileSqlInterface.getDoubleId(information.parentId(), true));
                statement.setString(3, information.name());
                statement.setBytes(4, SqlHelper.toOrdered(information.name()));
                statement.setLong(5, information.size());
                statement.setTimestamp(6, SqlHelper.getTimestamp(information.createTime()));
                statement.setTimestamp(7, SqlHelper.getTimestamp(information.updateTime()));
                statement.setString(8, information.others());
                statement.executeUpdate();
            }
            if (information.size() > 0)
                this.updateDirectorySizeRecursively(information.parentId(), information.size(), connectionId.get());
            connection.commit();
        }
    }

    @Override
    public void insertIterator(final @NotNull Iterator<@NotNull FileInformation> iterator, final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final long doubleDirectoryId = FileSqlInterface.getDoubleId(directoryId, true);
            boolean success = true;
            long size = 0;
            if (iterator.hasNext()) {
                final Collection<Long> inserted = new HashSet<>();
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    INSERT INTO %s (double_id, parent_id, name, name_order, size, create_time, update_time, others)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                    """, this.tableName))) {
                    while (iterator.hasNext()) {
                        final FileInformation information = iterator.next();
                        assert information.parentId() == directoryId && (information.isDirectory() || information.size() >= 0);
                        final long doubleId = FileSqlInterface.getDoubleId(information.id(), information.isDirectory());
                        if (inserted.contains(doubleId))
                            continue;
                        inserted.add(doubleId);
                        statement.setLong(1, doubleId);
                        statement.setLong(2, doubleDirectoryId);
                        statement.setString(3, information.name());
                        statement.setBytes(4, SqlHelper.toOrdered(information.name()));
                        statement.setLong(5, information.size());
                        statement.setTimestamp(6, SqlHelper.getTimestamp(information.createTime()));
                        statement.setTimestamp(7, SqlHelper.getTimestamp(information.updateTime()));
                        statement.setString(8, information.others());
                        statement.executeUpdate();
                        if (success) {
                            if (information.size() == -1)
                                success = false;
                            size += information.size();
                        }
                    }
                }
            }
            if (success) {
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET size = 0 WHERE double_id == ?;
                    """, this.tableName))) {
                    statement.setLong(1, doubleDirectoryId);
                    statement.executeUpdate();
                } // Prevent calculate size again.
                this.updateDirectorySizeRecursively(directoryId, size, connectionId.get());
            }
            connection.commit();
        }
    }


    /* --- Update --- */

    protected void updateDirectorySizeRecursively(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final long doubleId = FileSqlInterface.getDoubleId(directoryId, true);
            final long size, parentId;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT size, parent_id FROM %s WHERE double_id == ? LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, doubleId);
                try (final ResultSet result = statement.executeQuery()) {
                    if (!result.next())
                        throw new IllegalStateException("No such directory." + ParametersMap.create().add("directoryId", directoryId).add("delta", delta));
                    size = result.getLong("size");
                    parentId = FileSqlInterface.getRealId(result.getLong("parent_id"));
                }
            }
            if (size >= 0) {
                if (size + delta < 0)
                    throw new IllegalArgumentException("Too low delta." + ParametersMap.create().add("directoryId", directoryId).add("size", size).add("delta", delta));
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET size = ? WHERE double_id == ?;
                    """, this.tableName))) {
                    statement.setLong(1, size + delta);
                    statement.setLong(2, doubleId);
                    statement.executeUpdate();
                }
                if (directoryId != this.rootId)
                    this.updateDirectorySizeRecursively(parentId, delta, connectionId.get());
            } else {
                boolean success = true;
                long total = 0;
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT size FROM %s WHERE parent_id == ? AND double_id != ?;
                """, this.tableName))) {
                    statement.setLong(1, doubleId);
                    statement.setLong(2, this.doubleRootId);
                    try (final ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            final long s = result.getLong(1);
                            if (s == -1) {
                                success = false;
                                break;
                            }
                            total += s;
                        }
                    }
                }
                if (success) {
                    try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET size = ? WHERE double_id == ?;
                        """, this.tableName))) {
                        statement.setLong(1, total);
                        statement.setLong(2, doubleId);
                        statement.executeUpdate();
                    }
                    if (directoryId != this.rootId)
                        this.updateDirectorySizeRecursively(parentId, delta, connectionId.get());
                }
            }
            connection.commit();
        }
    }

    protected void unknownDirectorySizeRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final long doubleId = FileSqlInterface.getDoubleId(directoryId, true);
            final long size, parentId;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT size, parent_id FROM %s WHERE double_id == ? LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, doubleId);
                try (final ResultSet result = statement.executeQuery()) {
                    if (!result.next())
                        throw new IllegalStateException("No such directory." + ParametersMap.create().add("directoryId", directoryId));
                    size = result.getLong("size");
                    parentId = FileSqlInterface.getRealId(result.getLong("parent_id"));
                }
            }
            if (size >= 0) {
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET size = -1 WHERE double_id == ?;
                    """, this.tableName))) {
                    statement.setLong(1, doubleId);
                    statement.executeUpdate();
                }
                if (directoryId != this.rootId)
                    this.unknownDirectorySizeRecursively(parentId, connectionId.get());
            }
            connection.commit();
        }
    }

    private boolean isDirectoryIndexed(final long directoryDoubleId, final @NotNull Connection connection) throws SQLException {
        final long size;
        try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT size FROM %s WHERE double_id == ? LIMIT 1;
            """, this.tableName))) {
            statement.setLong(1, directoryDoubleId);
            try (final ResultSet result = statement.executeQuery()) {
                size = result.next() ? result.getLong(1) : -2;
            }
        }
        if (size < -1)
            return false;
        if (size > -1)
            return true;
        try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT 1 FROM %s WHERE parent_id == ? LIMIT 1;
            """, this.tableName))) {
            statement.setLong(1, directoryDoubleId);
            try (final ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    @Override
    public void updateOrInsertFile(final @NotNull FileInformation file, final @Nullable String _connectionId) throws SQLException {
        assert !file.isDirectory() && file.size() >= 0;
        final long parentDoubleId = FileSqlInterface.getDoubleId(file.parentId(), true);
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            if (this.isDirectoryIndexed(parentDoubleId, connection)) {
                final FileInformation old = this.selectInfo(file.id(), false, connectionId.get());
                if (old == null)
                    this.insertFileOrDirectory(file, connectionId.get());
                else {
                    try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET parent_id = ?, name = ?, name_order = ?, size = ?, create_time = ?, update_time = ?, others = ? WHERE double_id == ?;
                        """, this.tableName))) {
                        statement.setLong(1, parentDoubleId);
                        statement.setString(2, file.name());
                        statement.setBytes(3, SqlHelper.toOrdered(file.name()));
                        statement.setLong(4, file.size());
                        statement.setTimestamp(5, SqlHelper.getTimestamp(file.createTime() == null ? old.createTime() : file.createTime()));
                        statement.setTimestamp(6, SqlHelper.getTimestamp(file.updateTime() == null ? old.updateTime() : file.updateTime()));
                        statement.setString(7, file.others());
                        statement.setLong(8, FileSqlInterface.getDoubleId(file.id(), false));
                        statement.executeUpdate();
                    }
                    if (file.parentId() == old.parentId()) {
                        if (file.size() != old.size())
                            this.updateDirectorySizeRecursively(file.parentId(), file.size() - old.size(), connectionId.get());
                    } else {
                        this.updateDirectorySizeRecursively(old.parentId(), -old.size(), connectionId.get());
                        this.updateDirectorySizeRecursively(file.parentId(), file.size(), connectionId.get());
                    }
                }
            } else this.deleteFile(file.id(), connectionId.get());
            connection.commit();
        }
    }

    @Override
    public void updateOrInsertDirectory(final @NotNull FileInformation directory, final @Nullable String _connectionId) throws SQLException {
        assert directory.isDirectory();
        final long parentDoubleId = FileSqlInterface.getDoubleId(directory.parentId(), true);
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            if (this.isDirectoryIndexed(parentDoubleId, connection)) {
                final FileInformation old = this.selectInfo(directory.id(), true, connectionId.get());
                if (old == null) {
                    try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    INSERT INTO %s (double_id, parent_id, name, name_order, size, create_time, update_time, others)
        VALUES (?, ?, ?, ?, -1, ?, ?, ?);
                        """, this.tableName))) {
                        statement.setLong(1, FileSqlInterface.getDoubleId(directory.id(), true));
                        statement.setLong(2, parentDoubleId);
                        statement.setString(3, directory.name());
                        statement.setBytes(4, SqlHelper.toOrdered(directory.name()));
                        statement.setTimestamp(5, SqlHelper.getTimestamp(directory.createTime()));
                        statement.setTimestamp(6, SqlHelper.getTimestamp(directory.updateTime()));
                        statement.setString(7, directory.others());
                        statement.executeUpdate();
                    }
                    this.unknownDirectorySizeRecursively(directory.parentId(), connectionId.get());
                } else {
                    if (directory.id() != old.id() && this.isInDirectoryRecursively(directory.id(), true, old.id(), connectionId.get()))
                        throw new IllegalStateException("Recycle directory tree." + ParametersMap.create().add("directory", directory).add("old", old));
                    try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                            UPDATE %s SET parent_id = ?, name = ?, name_order = ?, create_time = ?, update_time = ?, others = ? WHERE double_id == ?;
                                            """, this.tableName))) {
                        statement.setLong(1, parentDoubleId);
                        statement.setString(2, directory.name());
                        statement.setBytes(3, SqlHelper.toOrdered(directory.name()));
                        statement.setTimestamp(4, SqlHelper.getTimestamp(directory.createTime() == null ? old.createTime() : directory.createTime()));
                        statement.setTimestamp(5, SqlHelper.getTimestamp(directory.updateTime() == null ? old.updateTime() : directory.updateTime()));
                        statement.setString(6, directory.others());
                        statement.setLong(7, FileSqlInterface.getDoubleId(directory.id(), true));
                        statement.executeUpdate();
                    }
                    if (directory.parentId() != old.parentId())
                        if (old.size() == -1) {
                            this.updateDirectorySizeRecursively(old.parentId(), 0, connectionId.get());
                            this.unknownDirectorySizeRecursively(directory.parentId(), connectionId.get());
                        } else {
                            this.updateDirectorySizeRecursively(old.parentId(), -old.size(), connectionId.get());
                            this.updateDirectorySizeRecursively(directory.parentId(), old.size(), connectionId.get());
                        }
                }
            } else this.deleteDirectoryRecursively(directory.id(), connectionId.get());
            connection.commit();
        }
    }

    @Override
    public void calculateDirectorySize(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        this.updateDirectorySizeRecursively(directoryId, 0, _connectionId);
    }


    /* --- Select --- */

    @Contract(pure = true)
    protected static @NotNull String orderBy(@SuppressWarnings("TypeMayBeWeakened") final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, @NotNull OrderDirection> orders) {
        if (orders.isEmpty())
            return "ORDER BY name_order ASC, double_id ASC";
        final StringBuilder builder = new StringBuilder("ORDER BY ");
        for (final Map.Entry<VisibleFileInformation.Order, OrderDirection> order: orders.entrySet()) {
            builder.append(switch (order.getKey()) {
                case Id -> "double_id";
                case Name -> "name_order";
                case Directory -> "(double_id + 1) & 1";
                case Size -> "size";
                case CreateTime -> "create_time";
                case UpdateTime -> "update_time";
            }).append(switch (order.getValue()) {
                case ASCEND -> " ASC";
                case DESCEND -> " DESC";
            }).append(',');
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    @Contract(pure = true)
    protected static @NotNull String whereFilter(final @NotNull FilterPolicy filter) {
        return switch (filter) {
            case Both -> "";
            case OnlyDirectories -> "AND double_id & 1 == 0";
            case OnlyFiles -> "AND double_id & 1 == 1";
        };
    }

    @Override
    public @Nullable FileInformation selectInfo(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException {
        final FileInformation information;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM %s WHERE double_id == ? LIMIT 1;
                """, FileSqliteHelper.FileInfoExtra, this.tableName))) {
                statement.setLong(1, FileSqlInterface.getDoubleId(id, isDirectory));
                try (final ResultSet result = statement.executeQuery()) {
                    information = FileSqliteHelper.nextFile(result);
                }
            }
            connection.commit();
        }
        return information;
    }

    @Override
    public @NotNull FilesListInformation selectInfosInDirectory(final long directoryId, final @NotNull FilterPolicy filter, final @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count, filtered;
        final List<FileInformation> files;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT COUNT(*) FROM %s WHERE parent_id == ? AND double_id != ?;
                """, this.tableName))) {
                statement.setLong(1, FileSqlInterface.getDoubleId(directoryId, true));
                statement.setLong(2, this.doubleRootId);
                try (final ResultSet result = statement.executeQuery()) {
                    result.next();
                    count = result.getLong(1);
                }
            }
            if (count == 0) {
                filtered = 0;
                files = List.of();
            } else {
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT COUNT(*) FROM %s WHERE parent_id == ? %s AND double_id != ?;
                    """, this.tableName, FileSqliteHelper.whereFilter(filter)))) {
                    statement.setLong(1, FileSqlInterface.getDoubleId(directoryId, true));
                    statement.setLong(2, this.doubleRootId);
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        filtered = result.getLong(1);
                    }
                }
                if (position < 0 || filtered <= position || limit <= 0)
                    files = List.of();
                else
                    try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM %s WHERE parent_id == ? AND double_id != ? %s %s LIMIT ? OFFSET ?;
                        """, FileSqliteHelper.FileInfoExtra, this.tableName, FileSqliteHelper.whereFilter(filter), FileSqliteHelper.orderBy(orders)))) {
                        statement.setLong(1, FileSqlInterface.getDoubleId(directoryId, true));
                        statement.setLong(2, this.doubleRootId);
                        statement.setLong(3, limit);
                        statement.setLong(4, position);
                        try (final ResultSet result = statement.executeQuery()) {
                            files = FileSqliteHelper.allFiles(result);
                        }
                    }
            }
            connection.commit();
        }
        return new FilesListInformation(count, filtered, files);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Set<@NotNull Long>, @NotNull Set<@NotNull Long>> selectIdsInDirectory(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final Set<Long> files = new HashSet<>(), directories = new HashSet<>();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT double_id FROM %s WHERE parent_id == ?;
                """, this.tableName))) {
                statement.setLong(1, FileSqlInterface.getDoubleId(directoryId, true));
                try (final ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        final long doubleId = result.getLong(1);
                        (FileSqlInterface.isDirectory(doubleId) ? directories : files).add(FileSqlInterface.getRealId(doubleId));
                    }
                }
            }
        }
        directories.remove(this.rootId);
        return Pair.ImmutablePair.makeImmutablePair(files, directories);
    }

    @Override
    public @Nullable FileInformation selectInfoInDirectoryByName(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        final FileInformation information;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM %s WHERE parent_id == ? AND name == ? AND double_id != ? LIMIT 1;
                """, FileSqliteHelper.FileInfoExtra, this.tableName))) {
                statement.setLong(1, FileSqlInterface.getDoubleId(parentId, true));
                statement.setString(2, name);
                statement.setLong(3, this.doubleRootId);
                try (final ResultSet result = statement.executeQuery()) {
                    information = FileSqliteHelper.nextFile(result);
                }
            }
        }
        return information;
    }

    @Override
    public boolean isInDirectoryRecursively(final long id, final boolean isDirectory, final long directoryId, final @Nullable String _connectionId) throws SQLException {
        if ((isDirectory && id == directoryId) || directoryId == this.rootId)
            return true;
        final boolean success;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            long fileDoubleParent;
            final long directoryDoubleId = FileSqlInterface.getDoubleId(directoryId, true);
            final long fileSize, directorySize;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT parent_id, size FROM %s WHERE double_id == ? LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, FileSqlInterface.getDoubleId(id, isDirectory));
                try (final ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        connection.commit();
                        return false;
                    }
                    fileDoubleParent = result.getLong(1);
                    if (fileDoubleParent == directoryDoubleId) {
                        connection.commit();
                        return true;
                    }
                    fileSize = result.getLong(2);
                }
                statement.setLong(1, directoryDoubleId);
                try (final ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        connection.commit();
                        return true;
                    }
                    directorySize = result.getLong(2);
                }
            }
            if (directorySize >= 0 && (fileSize > directorySize || (!isDirectory && fileSize == -1)))
                success = false;
            else
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT parent_id FROM %s WHERE double_id == ? LIMIT 1;
                    """, this.tableName))) {
                    final Collection<Long> book = new HashSet<>();
                    while (true) {
                        if (book.contains(fileDoubleParent))
                            throw new IllegalStateException("Recycling files tree." + ParametersMap.create().add("id", id).add("isDirectory", isDirectory).add("parentId", FileSqlInterface.getRealId(fileDoubleParent)));
                        book.add(fileDoubleParent);
                        statement.setLong(1, fileDoubleParent);
                        try (final ResultSet result = statement.executeQuery()) {
                            if (!result.next())
                                throw new IllegalStateException("Illegal files tree." + ParametersMap.create().add("id", id).add("isDirectory", isDirectory).add("parentId", FileSqlInterface.getRealId(fileDoubleParent)));
                            fileDoubleParent = result.getLong(1);
                        }
                        if (fileDoubleParent == this.doubleRootId) {
                            success = false;
                            break;
                        }
                        if (fileDoubleParent == directoryDoubleId) {
                            success = true;
                            break;
                        }
                    }
                }
            connection.commit();
        }
        return success;
    }


    /* --- Delete --- */

    @Override
    public boolean deleteFile(final long fileId, final @Nullable String _connectionId) throws SQLException {
        final boolean success;
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            long size = 0, parentId = 0;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT size, parent_id FROM %s WHERE double_id == ? LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, FileSqlInterface.getDoubleId(fileId, false));
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        size = result.getLong(1);
                        parentId = FileSqlInterface.getRealId(result.getLong(2));
                        success = true;
                    } else
                        success = false;
                }
            }
            if (success) {
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    DELETE FROM %s WHERE double_id == ?;
                    """, this.tableName))) {
                    statement.setLong(1, FileSqlInterface.getDoubleId(fileId, false));
                    statement.executeUpdate();
                }
                this.updateDirectorySizeRecursively(parentId, -size, connectionId.get());
            }
            connection.commit();
        }
        return success;
    }

    @Override
    public boolean deleteDirectoryRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final boolean success;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            if (directoryId == this.rootId) {
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    DELETE FROM %s WHERE double_id != ?;
                    """, this.tableName))) {
                    statement.setLong(1, this.doubleRootId);
                    statement.executeUpdate();
                }
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET size = 0 WHERE double_id == ?;
                    """, this.tableName))) {
                    statement.setLong(1, this.doubleRootId);
                    statement.executeUpdate();
                }
                success = true;
            } else {
                long size = 0, parentId = 0;
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT size, parent_id FROM %s WHERE double_id == ? LIMIT 1;
                    """, this.tableName))) {
                    statement.setLong(1, FileSqlInterface.getDoubleId(directoryId, true));
                    try (final ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            size = result.getLong(1);
                            parentId = FileSqlInterface.getRealId(result.getLong(2));
                            success = true;
                        } else
                            success = false;
                    }
                }
                if (success) {
                    final Collection<Long> files = new HashSet<>();
                    final Collection<Long> directories = new HashSet<>();
                    directories.add(directoryId);
                    final Queue<Long> processingDirectories = new ArrayDeque<>(directories);
                    final Collection<Long> book = new HashSet<>();
                    while (!processingDirectories.isEmpty()) {
                        final Long current = processingDirectories.remove();
                        book.add(current);
                        final Pair.ImmutablePair<Set<Long>, Set<Long>> children = this.selectIdsInDirectory(current.longValue(), connectionId.get());
                        files.addAll(children.getFirst());
                        directories.addAll(children.getSecond());
                        processingDirectories.addAll(children.getSecond());
                        processingDirectories.removeAll(book);
                    }
                    try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    DELETE FROM %s WHERE double_id == ?;
                        """, this.tableName))) {
                        for (final Long id: files) {
                            statement.setLong(1, FileSqlInterface.getDoubleId(id.longValue(), false));
                            statement.executeUpdate();
                        }
                        for (final Long id: directories) {
                            statement.setLong(1, FileSqlInterface.getDoubleId(id.longValue(), true));
                            statement.executeUpdate();
                        }
                        statement.setLong(1, FileSqlInterface.getDoubleId(directoryId, true));
                        statement.executeUpdate();
                    }
                    this.updateDirectorySizeRecursively(parentId, size == -1 ? 0 : -size, connectionId.get());
                }
            }
            connection.commit();
        }
        return success;
    }


    /* --- Search --- */


    @Override
    public @NotNull String toString() {
        return "FileSqliteHelper{" +
                "database=" + this.database +
                ", providerName='" + this.providerName + '\'' +
                ", rootId=" + this.rootId +
                ", tableName='" + this.tableName + '\'' +
                '}';
    }
}
