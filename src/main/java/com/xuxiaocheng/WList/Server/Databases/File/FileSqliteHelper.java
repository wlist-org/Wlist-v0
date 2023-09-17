package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqliteHelper;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        this.doubleRootId = FileSqliteHelper.getDoubleId(this.rootId, true);
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.database.getConnection(_connectionId, connectionId);
    }


    @Contract(pure = true)
    public static long getDoubleId(final long id, final boolean isDirectory) {
        return (id << 1) + (isDirectory ? 0 : 1);
    }
    @Contract(pure = true)
    public static long getRealId(final long doubleId) {
        return doubleId >> 1;
    }
    @Contract(pure = true)
    public static boolean isDirectory(final long doubleId) {
        return (doubleId & 1) == 0;
    }

    @Contract(pure = true)
    public static byte @NotNull [] getNameOrder(final @NotNull String name, final boolean isDirectory) {
        return SqliteHelper.toOrdered((isDirectory ? 'd' : 'f') + name);
    }

    @Contract(pure = true)
    public static @Nullable Timestamp getTimestamp(final @Nullable ZonedDateTime time) {
        assert time == null || ZoneOffset.UTC.equals(time.getZone());
        return time == null ? null : Timestamp.valueOf(time.toLocalDateTime());
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
                    statement.setBytes(4, FileSqliteHelper.getNameOrder(this.providerName, true));
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
        final long parentId = result.getLong("parent_id");
        final boolean directory = FileSqliteHelper.isDirectory(doubleId);
        final String name = result.getString("name");
        final long size = result.getLong("size");
        final Timestamp createTime = result.getTimestamp("create_time");
        final Timestamp updateTime = result.getTimestamp("update_time");
        final String others = result.getString("others");
        return new FileInformation(FileSqliteHelper.getRealId(doubleId), parentId, name, directory, size,
                createTime == null ? null : ZonedDateTime.of(createTime.toLocalDateTime(), ZoneOffset.UTC),
                updateTime == null ? null : ZonedDateTime.of(updateTime.toLocalDateTime(), ZoneOffset.UTC),
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
                statement.setLong(1, FileSqliteHelper.getDoubleId(information.id(), information.isDirectory()));
                statement.setLong(2, FileSqliteHelper.getDoubleId(information.parentId(), true));
                statement.setString(3, information.name());
                statement.setBytes(4, FileSqliteHelper.getNameOrder(information.name(), information.isDirectory()));
                statement.setLong(5, information.size());
                statement.setTimestamp(6, FileSqliteHelper.getTimestamp(information.createTime()));
                statement.setTimestamp(7, FileSqliteHelper.getTimestamp(information.updateTime()));
                statement.setString(8, information.others());
                statement.executeUpdate();
            }
            if (information.size() > 0)
                this.updateDirectorySizeRecursively(information.parentId(), information.size(), connectionId.get());
            connection.commit();
        }
    }

    @Override
    public void insertFilesSameDirectory(final @NotNull Iterator<@NotNull FileInformation> iterator, final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final long doubleDirectoryId = FileSqliteHelper.getDoubleId(directoryId, true);
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET size = 0 WHERE double_id == ?;
                    """, this.tableName))) {
                statement.setLong(1, doubleDirectoryId);
                statement.executeUpdate();
            }
            boolean success = true;
            long size = 0;
            if (iterator.hasNext()) {
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    INSERT INTO %s (double_id, parent_id, name, name_order, size, create_time, update_time, others)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                    """, this.tableName))) {
                    while (iterator.hasNext()) {
                        final FileInformation information = iterator.next();
                        assert information.parentId() == directoryId;
                        assert information.isDirectory() || information.size() >= 0;
                        statement.setLong(1, FileSqliteHelper.getDoubleId(information.id(), information.isDirectory()));
                        statement.setLong(2, doubleDirectoryId);
                        statement.setString(3, information.name());
                        statement.setBytes(4, FileSqliteHelper.getNameOrder(information.name(), information.isDirectory()));
                        statement.setLong(5, information.size());
                        statement.setTimestamp(6, FileSqliteHelper.getTimestamp(information.createTime()));
                        statement.setTimestamp(7, FileSqliteHelper.getTimestamp(information.updateTime()));
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
            if (success)
                this.updateDirectorySizeRecursively(directoryId, size, connectionId.get());
            connection.commit();
        }
    }

    /* --- Update --- */

    @Override
    public void updateDirectorySizeRecursively(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>(_connectionId);
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final long doubleId = FileSqliteHelper.getDoubleId(directoryId, true);
            final long size, parentId;
            try (final PreparedStatement statement1 = connection.prepareStatement(String.format("""
    SELECT size, parent_id FROM %s WHERE double_id == ? LIMIT 1;
                """, this.tableName))) {
                statement1.setLong(1, doubleId);
                try (final ResultSet result = statement1.executeQuery()) {
                    if (!result.next())
                        throw new IllegalStateException("No such directory." + ParametersMap.create().add("directoryId", directoryId).add("delta", delta));
                    size = result.getLong("size");
                    parentId = result.getLong("parent_id");
                }
            }
            if (size >= 0) {
                if (size + delta < 0)
                    throw new IllegalArgumentException("Too low delta." + ParametersMap.create().add("directoryId", directoryId).add("size", size).add("delta", delta));
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    UPDATE %s SET size = ? WHERE double_id == ?;
                    """, this.tableName))) {
                    statement.setLong(1, size + delta);
                    statement.setLong(2, FileSqliteHelper.getDoubleId(directoryId, true));
                    statement.executeUpdate();
                }
                if (directoryId != this.rootId)
                    this.updateDirectorySizeRecursively(parentId, delta, connectionId.get());
            }
            connection.commit();
        }
    }


    /* --- Select --- */

    protected static @NotNull String orderBy(@SuppressWarnings("TypeMayBeWeakened") final @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders) {
        if (orders.isEmpty())
            return "ORDER BY name_order ASC, double_id ASC";
        final StringBuilder builder = new StringBuilder("ORDER BY ");
        for (final Map.Entry<VisibleFileInformation.Order, Options.OrderDirection> order: orders.entrySet()) {
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

    protected static @NotNull String whereFilter(final Options.@NotNull FilterPolicy filter) {
        return switch (filter) {
            case Both -> "";
            case OnlyDirectories -> "AND double_id & 1 == 0";
            case OnlyFiles -> "AND double_id & 1 == 1";
        };
    }

    @Override
    public @Nullable FileInformation selectFile(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException {
        final FileInformation information;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT %s FROM %s WHERE double_id == ? LIMIT 1;
                """, FileSqliteHelper.FileInfoExtra, this.tableName))) {
                statement.setLong(1, FileSqliteHelper.getDoubleId(id, isDirectory));
                try (final ResultSet result = statement.executeQuery()) {
                    information = FileSqliteHelper.nextFile(result);
                }
            }
            connection.commit();
        }
        return information;
    }

    @Override
    public @NotNull FilesListInformation selectFilesInDirectory(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        final long count, filtered;
        final List<FileInformation> files;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    SELECT COUNT(*) FROM %s WHERE parent_id == ? AND double_id != ?;
                """, this.tableName))) {
                statement.setLong(1, FileSqliteHelper.getDoubleId(directoryId, true));
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
                    statement.setLong(1, FileSqliteHelper.getDoubleId(directoryId, true));
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
    SELECT %s FROM %s WHERE parent_id == ? %s AND double_id != ? %s LIMIT ? OFFSET ?;
                        """, FileSqliteHelper.FileInfoExtra, this.tableName, FileSqliteHelper.whereFilter(filter), FileSqliteHelper.orderBy(orders)))) {
                        statement.setLong(1, FileSqliteHelper.getDoubleId(directoryId, true));
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


    /* --- Delete --- */

    @Override
    public boolean deleteFile(final long fileId, final @Nullable String _connectionId) throws SQLException {
        final boolean success;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    DELETE FROM %s WHERE double_id == ?;
                """, this.tableName))) {
                statement.setLong(1, FileSqliteHelper.getDoubleId(fileId, false));
                success = statement.executeUpdate() == 1;
            }
            connection.commit();
        }
        if (success) {
            // TODO
//            updateDirectorySizeRecursively();
        }
        return success;
    }

    @Override
    public long deleteDirectoryRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final long success;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
    DELETE FROM %s WHERE double_id == ?;
                """, this.tableName))) {
                statement.setLong(1, FileSqliteHelper.getDoubleId(directoryId, true));
                success = statement.executeLargeUpdate();
            }
            connection.commit();
        }
        if (success > 0) {
            // TODO
//            updateDirectorySizeRecursively();
        }
        return success;
    }

    //    @Override
//    public @Nullable FileInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
//        try (final Connection connection = this.getConnection(_connectionId, null)) {
//            final FileInformation information;
//            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                    SELECT * FROM %s WHERE parent_id == ? AND name == ? AND parent_id != id LIMIT 1;
//                """, this.tableName))) {
//                statement.setLong(1, parentId);
//                statement.setString(2, name);
//                try (final ResultSet result = statement.executeQuery()) {
//                    information = FileSqliteHelper.createNextFileInfo(this.driverName, result);
//                }
//            }
//            return information;
//        }
//    }
//
//    @Override
//    public @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
//        if (md5List.isEmpty())
//            return Map.of();
//        try (final Connection connection = this.getConnection(_connectionId, null)) {
//            final Map<String, Set<FileInformation>> map = new HashMap<>();
//            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                    SELECT * FROM %s WHERE md5 == ?;
//                """, this.tableName))) {
//                for (final String md5: md5List) {
//                    statement.setString(1, md5);
//                    try (final ResultSet result = statement.executeQuery()) {
//                        map.put(md5, FileSqliteHelper.createFilesInfo(this.driverName, result));
//                    }
//                }
//            }
//            return Collections.unmodifiableMap(map);
//        }
//    }
//
//    @Override
//    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
//        if (parentIdList.isEmpty())
//            return Map.of();
//        try (final Connection connection = this.getConnection(_connectionId, null)) {
//            final Map<Long, Set<Long>> map = new HashMap<>();
//            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                    SELECT id FROM %s WHERE parent_id == ? AND parent_id != id;
//                """, this.tableName))) {
//                for (final Long parentId: parentIdList) {
//                    statement.setLong(1, parentId.longValue());
//                    try (final ResultSet result = statement.executeQuery()) {
//                        final Set<Long> set = new HashSet<>();
//                        while (result.next())
//                            set.add(result.getLong("id"));
//                        map.put(parentId, set);
//                    }
//                }
//            }
//            return Collections.unmodifiableMap(map);
//        }
//    }
//
//
//    @Override
//    public void mergeFiles(final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable Collection<@NotNull Long> mergingUniverse, final @Nullable String _connectionId) throws SQLException {
//        if (inserters.isEmpty())
//            return;
//        final Collection<Long> ids = mergingUniverse == null ? AStreams.streamToList(inserters.stream().map(FileInformation::id)) : mergingUniverse;
//        if (ids.isEmpty()) {
//            this.insertFilesForce(inserters, _connectionId);
//            return;
//        }
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
//            final Map<Long, FileInformation> cached = this.selectFiles(ids, connectionId.get());
//            this.insertFilesForce(AStreams.streamToList(inserters.stream().map(d -> {
//                @Nullable final FileInformation cached1 = cached.get(d.id());
//                if (cached1 == null || (cached1.createTime() == null && cached1.updateTime() == null && cached1.md5().isEmpty())
//                        || (d.createTime() != null && d.updateTime() != null && !d.md5().isEmpty()))
//                    return d;
//                return new FileInformation(d.location(), d.parentId(), d.name(), d.type(), d.size(),
//                        d.createTime() == null ? cached1.createTime() : d.createTime(),
//                        d.updateTime() == null ? cached1.updateTime() : d.updateTime(),
//                        d.md5().isEmpty() ? cached1.md5() : d.md5(), d.others());
//            })), connectionId.get()); // TODO: No select and update directly.
//            connection.commit();
//        }
//    }
//
//    @Override
//    public void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
//        if (idList.isEmpty())
//            return;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
//            final Collection<Long> leave = new HashSet<>();
//            final Collection<Long> set = new HashSet<>(idList);
//            set.remove(this.rootId);
//            final Collection<Long> universe = new HashSet<>();
//            while (!set.isEmpty()) {
//                set.removeAll(universe);
//                final Map<Long, Set<Long>> maps = this.selectFilesIdByParentId(set, connectionId.get());
//                set.clear();
//                maps.forEach((key, value) -> {
//                    leave.add(key);
//                    set.addAll(value);
//                });
//                universe.addAll(set);
//            }
//            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                    DELETE FROM %s WHERE id == ?;
//                """, this.tableName))) {
//                for (final Long id: leave) {
//                    statement.setLong(1, id.longValue());
//                    statement.executeUpdate();
//                }
//            }
//            connection.commit();
//        }
//    }
//
//    @Override
//    public void deleteFilesByMd5Recursively(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
//        if (md5List.isEmpty())
//            return;
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
//            this.deleteFilesRecursively(AStreams.streamToList(this.selectFilesByMd5(md5List, connectionId.get()).values().stream()
//                    .filter(Objects::nonNull).flatMap(Set::stream).map(FileInformation::id)), connectionId.get());
//            connection.commit();
//        }
//    }
//
//    @Override // Use 'synchronized' to prevent 'lost update'.
//    public synchronized @Nullable Long updateDirectorySize(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException {
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
//            final FileInformation directory = this.selectFiles(List.of(directoryId), connectionId.get()).get(directoryId);
//            if (directory == null || directory.type() != FileSqlType.Directory)
//                return null;
//            if (directory.size() < 0)
//                return directory.size();
//            final long realSize = directory.size() + delta;
//            assert realSize >= 0;
//            this.updateParentsSize(directoryId, directory, realSize, connectionId.get());
//            connection.commit();
//            return realSize;
//        }
//    }
//
//    @Override // Use 'synchronized' to prevent 'lost update'.
//    public synchronized @Nullable Long calculateDirectorySizeRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException {
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
//            final FileInformation directory = this.selectFiles(List.of(directoryId), connectionId.get()).get(directoryId);
//            if (directory == null || !directory.isDirectory())
//                return null;
//            long realSize = 0;
//            if (directory.type() == FileSqlType.Directory)
//                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                        SELECT id, size, type FROM %s WHERE parent_id == ?;
//                    """, this.tableName))) {
//                    statement.setLong(1, directoryId);
//                    try (final ResultSet result = statement.executeQuery()) {
//                        while (result.next()) {
//                            final long id = result.getLong("id");
//                            if (id == directoryId)
//                                continue;
//                            final long size = result.getLong("size");
//                            if (size >= 0) {
//                                realSize += size;
//                                continue;
//                            }
//                            final FileSqlType type = FileSqlInterface.FileSqlType.values()[result.getInt("type")];
//                            if (type == FileSqlType.RegularFile) {
//                                realSize = -1;
//                                break;
//                            }
//                            final Long fixedSize = this.calculateDirectorySizeRecursively(id, connectionId.get());
//                            if (fixedSize == null || fixedSize.longValue() < 0) {
//                                realSize = -1;
//                                break;
//                            }
//                            realSize += fixedSize.longValue();
//                        }
//                    }
//                }
//            if (directory.size() == realSize) {
//                connection.commit();
//                return null;
//            }
//            this.updateParentsSize(directoryId, directory, realSize, connectionId.get());
//            connection.commit();
//            return realSize;
//        }
//    }
//
//    private void updateParentsSize(final long directoryId, final @NotNull FileInformation directory, final long realSize, final @Nullable String _connectionId) throws SQLException {
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
//            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                    UPDATE %s SET size = ? WHERE id == ?;
//                """, this.tableName))) {
//                statement.setLong(1, realSize);
//                statement.setLong(2, directoryId);
//                statement.executeUpdate();
//            }
//            if (directoryId != directory.parentId()) {
//                final Long update = this.updateDirectorySize(directory.parentId(), realSize - directory.size(), connectionId.get());
//                if (update != null && update.longValue() < 0)
//                    this.calculateDirectorySizeRecursively(directory.parentId(), connectionId.get());
//            }
//            connection.commit();
//        }
//    }


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
