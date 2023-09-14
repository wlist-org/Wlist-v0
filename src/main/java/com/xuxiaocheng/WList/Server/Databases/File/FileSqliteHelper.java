package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AStreams;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqliteHelper;
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

public final class FileSqliteHelper implements FileSqlInterface {
    @Contract(pure = true) private static @NotNull String getTableName(final @NotNull String name) {
        return "driver_" + Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).replace('=', '_');
    }

    @Contract(pure = true) private static @NotNull String getOrderPolicy(final Options.@NotNull OrderPolicy policy) {
        return "name_ordered";
//        return switch (policy) {
//            case FileName -> "name_ordered";
//            case Size -> "size";
//            case CreateTime -> "create_time";
//            case UpdateTime -> "update_time";
//        };
    }
    @Contract(pure = true) private static @NotNull String getOrderDirection(final Options.@NotNull OrderDirection policy) {
        return switch (policy) {
            case ASCEND -> "ASC";
            case DESCEND -> "DESC";
        };
    }

    private static final @NotNull DateTimeFormatter DefaultFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final @NotNull SqlDatabaseInterface database;
    private final @NotNull String driverName;
    private final @NotNull String tableName;
    private final long rootId;

    public FileSqliteHelper(final @NotNull SqlDatabaseInterface database, final @NotNull String driverName, final long rootId) {
        super();
        this.database = database;
        this.driverName = driverName;
        this.tableName = FileSqliteHelper.getTableName(driverName);
        this.rootId = rootId;
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
                        id           INTEGER PRIMARY KEY AUTOINCREMENT
                                             UNIQUE
                                             NOT NULL,
                        parent_id    INTEGER ,
                        name         TEXT    NOT NULL,
                        name_ordered BLOB    NOT NULL,
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

    private static @Nullable FileInformation createNextFileInfo(final @NotNull String driver, final @NotNull ResultSet result) throws SQLException {
        final @NotNull String createTime = result.getString("create_time");
        final @NotNull String updateTime = result.getString("update_time");
        return result.next() ? new FileInformation(new FileLocation(driver, result.getLong("id")),
                result.getLong("parent_id"), result.getString("name"),
                FileSqlInterface.FileSqlType.values()[result.getInt("type")], result.getLong("size"),
                createTime.isEmpty() ? null : LocalDateTime.parse(createTime, FileSqliteHelper.DefaultFormatter),
                updateTime.isEmpty() ? null : LocalDateTime.parse(updateTime, FileSqliteHelper.DefaultFormatter),
                result.getString("md5"), result.getString("others")) : null;
    }

    private static @NotNull @UnmodifiableView Set<@NotNull FileInformation> createFilesInfo(final @NotNull String driver, final @NotNull ResultSet result) throws SQLException {
        final Set<FileInformation> set = new HashSet<>();
        while (true) {
            final FileInformation info = FileSqliteHelper.createNextFileInfo(driver, result);
            if (info == null)
                break;
            set.add(info);
        }
        return Collections.unmodifiableSet(set);
    }

    private static @NotNull @UnmodifiableView List<@NotNull FileInformation> createFilesInfoInOrder(final @NotNull String driver, final @NotNull ResultSet result) throws SQLException {
        final List<FileInformation> list = new LinkedList<>();
        while (true) {
            final FileInformation info = FileSqliteHelper.createNextFileInfo(driver, result);
            if (info == null)
                break;
            list.add(info);
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public void insertFilesForce(final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    INSERT INTO %s (id, parent_id, name, name_ordered, type, size, create_time, update_time, md5, others)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        parent_id = excluded.parent_id, name = excluded.name, name_ordered = excluded.name_ordered,
                        type = excluded.type, size = excluded.size,
                        create_time = excluded.create_time, update_time = excluded.update_time,
                        md5 = excluded.md5, others = excluded.others;
                """, this.tableName))) {
                for (final FileInformation inserter: inserters) {
                    statement.setLong(1, inserter.id());
                    statement.setLong(2, inserter.parentId());
                    statement.setString(3, inserter.name());
                    statement.setBytes(4, SqliteHelper.toOrdered((inserter.isDirectory() ? "d" : "f") + inserter.name()));
                    statement.setInt(5, inserter.type().ordinal());
                    statement.setLong(6, inserter.size());
                    statement.setString(7, inserter.createTime() == null ? "" : inserter.createTime().format(FileSqliteHelper.DefaultFormatter));
                    statement.setString(8, inserter.updateTime() == null ? "" : inserter.updateTime().format(FileSqliteHelper.DefaultFormatter));
                    statement.setString(9, inserter.md5());
                    statement.setString(10, inserter.others());
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    @Override
    public void updateDirectoryType(final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    UPDATE %s SET type = ? %s WHERE id == ?;
                """, this.tableName, empty ? ", size = ?" : ""))) {
                statement.setInt(1, (empty ? FileSqlInterface.FileSqlType.EmptyDirectory : FileSqlInterface.FileSqlType.Directory).ordinal());
                if (empty)
                    statement.setLong(2, 0L);
                statement.setLong(empty ? 3 : 2, id);
                statement.executeUpdate();
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final Map<Long, FileInformation> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE id == ? LIMIT 1;
                """, this.tableName))) {
                for (final Long id: idList) {
                    statement.setLong(1, id.longValue());
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(id, FileSqliteHelper.createNextFileInfo(this.driverName, result));
                    }
                }
            }
            return Collections.unmodifiableMap(map);
        }
    }

    @Override
    public @Nullable FileInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final FileInformation information;
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE parent_id == ? AND name == ? AND parent_id != id LIMIT 1;
                """, this.tableName))) {
                statement.setLong(1, parentId);
                statement.setString(2, name);
                try (final ResultSet result = statement.executeQuery()) {
                    information = FileSqliteHelper.createNextFileInfo(this.driverName, result);
                }
            }
            return information;
        }
    }

    @Override
    public @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return Map.of();
        try (final Connection connection = this.getConnection(_connectionId, null)) {
            final Map<String, Set<FileInformation>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT * FROM %s WHERE md5 == ?;
                """, this.tableName))) {
                for (final String md5: md5List) {
                    statement.setString(1, md5);
                    try (final ResultSet result = statement.executeQuery()) {
                        map.put(md5, FileSqliteHelper.createFilesInfo(this.driverName, result));
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
            final Map<Long, Set<Long>> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT id FROM %s WHERE parent_id == ? AND parent_id != id;
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
            final Map<Long, Long> map = new HashMap<>();
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT COUNT(*) FROM %s WHERE parent_id == ? AND parent_id != id;
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
    public Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> selectFilesByParentIdInPage(final long parentId, final Options.@NotNull FilterPolicy filter, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final long count = this.selectFilesCountByParentId(List.of(parentId), connectionId.get()).get(parentId).longValue();
            final long filterCount;
            if (filter == Options.FilterPolicy.Both)
                filterCount = count;
            else {
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    SELECT COUNT(*) FROM %s WHERE parent_id == ? AND parent_id != id""" + switch (filter) {
                        case OnlyFiles -> " AND type == 0;";
                        case OnlyDirectories -> " AND (type == 1 OR type == 2);";
                        default -> throw new IllegalStateException("Unexpected value: " + filter);
                    }, this.tableName))) {
                    statement.setLong(1, parentId);
                    try (final ResultSet result = statement.executeQuery()) {
                        result.next();
                        filterCount = result.getLong(1);
                    }
                }
            }
//            if (offset >= filterCount || limit <= 0)
                return Triad.ImmutableTriad.makeImmutableTriad(count, filterCount, List.of());
//            final List<FileInformation> list;
//            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
//                    SELECT * FROM %s WHERE parent_id == ? AND parent_id != id""" + switch (filter) {
//                        case OnlyDirectories -> " AND (type == 1 OR type == 2) ";
//                        case OnlyFiles -> " AND type == 0 ";
//                        case Both -> " ";
//                    } + "ORDER BY " + FileSqliteHelper.getOrderPolicy(policy) + " " + FileSqliteHelper.getOrderDirection(direction) +
//                    (policy == Options.OrderPolicy.FileName ? "" : ", " + FileSqliteHelper.getOrderPolicy(Options.OrderPolicy.FileName) + " " +
//                            FileSqliteHelper.getOrderDirection(Options.OrderDirection.ASCEND)) + """
//                    LIMIT ? OFFSET ?;
//                """, this.tableName))) {
//                statement.setLong(1, parentId);
//                statement.setInt(2, limit);
//                statement.setLong(3, offset);
//                try (final ResultSet result = statement.executeQuery()) {
//                    list = FileSqliteHelper.createFilesInfoInOrder(this.driverName, result);
//                }
//            }
//            return Triad.ImmutableTriad.makeImmutableTriad(count, filterCount, list);
        }
    }

    @Override
    public void mergeFiles(final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable Collection<@NotNull Long> mergingUniverse, final @Nullable String _connectionId) throws SQLException {
        if (inserters.isEmpty())
            return;
        final Collection<Long> ids = mergingUniverse == null ? AStreams.streamToList(inserters.stream().map(FileInformation::id)) : mergingUniverse;
        if (ids.isEmpty()) {
            this.insertFilesForce(inserters, _connectionId);
            return;
        }
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final Map<Long, FileInformation> cached = this.selectFiles(ids, connectionId.get());
            this.insertFilesForce(AStreams.streamToList(inserters.stream().map(d -> {
                @Nullable final FileInformation cached1 = cached.get(d.id());
                if (cached1 == null || (cached1.createTime() == null && cached1.updateTime() == null && cached1.md5().isEmpty())
                        || (d.createTime() != null && d.updateTime() != null && !d.md5().isEmpty()))
                    return d;
                return new FileInformation(d.location(), d.parentId(), d.name(), d.type(), d.size(),
                        d.createTime() == null ? cached1.createTime() : d.createTime(),
                        d.updateTime() == null ? cached1.updateTime() : d.updateTime(),
                        d.md5().isEmpty() ? cached1.md5() : d.md5(), d.others());
            })), connectionId.get()); // TODO: No select and update directly.
            connection.commit();
        }
    }

    @Override
    public void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        if (idList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final Collection<Long> leave = new HashSet<>();
            final Collection<Long> set = new HashSet<>(idList);
            set.remove(this.rootId);
            final Collection<Long> universe = new HashSet<>();
            while (!set.isEmpty()) {
                set.removeAll(universe);
                final Map<Long, Set<Long>> maps = this.selectFilesIdByParentId(set, connectionId.get());
                set.clear();
                maps.forEach((key, value) -> {
                    leave.add(key);
                    set.addAll(value);
                });
                universe.addAll(set);
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
            this.deleteFilesRecursively(AStreams.streamToList(this.selectFilesByMd5(md5List, connectionId.get()).values().stream()
                    .filter(Objects::nonNull).flatMap(Set::stream).map(FileInformation::id)), connectionId.get());
            connection.commit();
        }
    }

    @Override // Use 'synchronized' to prevent 'lost update'.
    public synchronized @Nullable Long updateDirectorySize(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final FileInformation directory = this.selectFiles(List.of(directoryId), connectionId.get()).get(directoryId);
            if (directory == null || directory.type() != FileSqlType.Directory)
                return null;
            if (directory.size() < 0)
                return directory.size();
            final long realSize = directory.size() + delta;
            assert realSize >= 0;
            this.updateParentsSize(directoryId, directory, realSize, connectionId.get());
            connection.commit();
            return realSize;
        }
    }

    @Override // Use 'synchronized' to prevent 'lost update'.
    public synchronized @Nullable Long calculateDirectorySizeRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            final FileInformation directory = this.selectFiles(List.of(directoryId), connectionId.get()).get(directoryId);
            if (directory == null || !directory.isDirectory())
                return null;
            long realSize = 0;
            if (directory.type() == FileSqlType.Directory)
                try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                        SELECT id, size, type FROM %s WHERE parent_id == ?;
                    """, this.tableName))) {
                    statement.setLong(1, directoryId);
                    try (final ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            final long id = result.getLong("id");
                            if (id == directoryId)
                                continue;
                            final long size = result.getLong("size");
                            if (size >= 0) {
                                realSize += size;
                                continue;
                            }
                            final FileSqlType type = FileSqlInterface.FileSqlType.values()[result.getInt("type")];
                            if (type == FileSqlType.RegularFile) {
                                realSize = -1;
                                break;
                            }
                            final Long fixedSize = this.calculateDirectorySizeRecursively(id, connectionId.get());
                            if (fixedSize == null || fixedSize.longValue() < 0) {
                                realSize = -1;
                                break;
                            }
                            realSize += fixedSize.longValue();
                        }
                    }
                }
            if (directory.size() == realSize) {
                connection.commit();
                return null;
            }
            this.updateParentsSize(directoryId, directory, realSize, connectionId.get());
            connection.commit();
            return realSize;
        }
    }

    private void updateParentsSize(final long directoryId, final @NotNull FileInformation directory, final long realSize, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = this.getConnection(_connectionId, connectionId)) {
            try (final PreparedStatement statement = connection.prepareStatement(String.format("""
                    UPDATE %s SET size = ? WHERE id == ?;
                """, this.tableName))) {
                statement.setLong(1, realSize);
                statement.setLong(2, directoryId);
                statement.executeUpdate();
            }
            if (directoryId != directory.parentId()) {
                final Long update = this.updateDirectorySize(directory.parentId(), realSize - directory.size(), connectionId.get());
                if (update != null && update.longValue() < 0)
                    this.calculateDirectorySizeRecursively(directory.parentId(), connectionId.get());
            }
            connection.commit();
        }
    }

    @Override
    public @NotNull String toString() {
        return "FileSqliteHelper{" +
                "database=" + this.database +
                ", driverName='" + this.driverName + '\'' +
                ", tableName='" + this.tableName + '\'' +
                '}';
    }
}
