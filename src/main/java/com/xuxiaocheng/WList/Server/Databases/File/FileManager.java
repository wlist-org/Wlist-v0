package com.xuxiaocheng.WList.Server.Databases.File;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FileManager {
    private FileManager() {
        super();
    }

    public static @NotNull DatabaseUtil getDatabaseUtil() throws SQLException {
        return DatabaseUtil.getInstance();
    }

    public static void initialize(final @NotNull String driverName) throws SQLException {
        FileSqlHelper.initialize(driverName, FileManager.getDatabaseUtil(), "initialize");
    }

    public static void uninitialize(final @NotNull String driverName) throws SQLException {
        FileSqlHelper.uninitialize(driverName, "uninitialize");
    }

    // TODO cache lock. enhance cache.
    private static final @NotNull com.github.benmanes.caffeine.cache.Cache<@NotNull Long, @NotNull FileSqlInformation> Cache = Caffeine.newBuilder()
            .maximumSize(GlobalConfiguration.getInstance().maxCacheSize())
            .softValues().build();

    public static void insertOrUpdateFiles(final @NotNull String driverName, final @NotNull Collection<FileSqlInformation.@NotNull Inserter> inserters, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).insertOrUpdateFiles(inserters, _connectionId);
    }

    public static void insertOrUpdateFile(final @NotNull String driverName, final FileSqlInformation.@NotNull Inserter inserter, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertOrUpdateFiles(driverName, List.of(inserter), _connectionId);
    }

    public static void deleteFilesRecursively(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).deleteFilesRecursively(idList, _connectionId);
        FileManager.Cache.invalidateAll(idList);
    }

    public static void deleteFileRecursively(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesRecursively(driverName, List.of(id),_connectionId);
    }

    public static void deleteFilesByPathRecursively(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        if (pathList.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<DrivePath, Set<Long>> map = FileSqlHelper.getInstance(driverName).selectAllFilesIdByPathRecursively(pathList, connectionId.get());
            FileSqlHelper.getInstance(driverName).deleteFilesByPathRecursively(pathList, connectionId.get());
            connection.commit();
            FileManager.Cache.invalidateAll(map.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        }
    }

    public static void deleteFileByPathRecursively(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesByPathRecursively(driverName, List.of(path), _connectionId);
    }

    public static void deleteFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        if (md5List.isEmpty())
            return;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            final Map<String, List<FileSqlInformation>> map = FileSqlHelper.getInstance(driverName).selectFilesByMd5(md5List, connectionId.get());
            FileSqlHelper.getInstance(driverName).deleteFilesByMd5(md5List, connectionId.get());
            connection.commit();
            FileManager.Cache.invalidateAll(map.values().stream().flatMap(List::stream).map(FileSqlInformation::id).collect(Collectors.toSet()));
        }
    }

    public static void deleteFileByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesByMd5(driverName, List.of(md5), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> selectFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        final Map<Long, FileSqlInformation> cached = FileManager.Cache.getAllPresent(idList);
        final Collection<Long> rest = new HashSet<>(idList);
        rest.removeAll(cached.keySet());
        final Map<Long, FileSqlInformation> required = FileSqlHelper.getInstance(driverName).selectFiles(rest, _connectionId);
        FileManager.Cache.putAll(required);
        cached.putAll(required);
        return cached;
    }

    public static @Nullable FileSqlInformation selectFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFiles(driverName, List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull FileSqlInformation> selectFilesByPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        final Map<DrivePath, FileSqlInformation> map = FileSqlHelper.getInstance(driverName).selectFilesByPath(pathList, _connectionId);
        FileManager.Cache.putAll(map.values().stream().collect(Collectors.toMap(FileSqlInformation::id, Function.identity())));
        return map;
    }

    public static @Nullable FileSqlInformation selectFileByPath(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesByPath(driverName, List.of(path), _connectionId).get(path);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        final Map<String, List<FileSqlInformation>> map = FileSqlHelper.getInstance(driverName).selectFilesByMd5(md5List, _connectionId);
        FileManager.Cache.putAll(map.values().stream().flatMap(List::stream).collect(Collectors.toMap(FileSqlInformation::id, Function.identity())));
        return map;
    }

    public static @NotNull @UnmodifiableView List<@NotNull FileSqlInformation> selectFileByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesByMd5(driverName, List.of(md5), _connectionId).get(md5);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull DrivePath, @Nullable Set<@NotNull Long>> selectAllFilesIdByPathRecursively(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectAllFilesIdByPathRecursively(pathList, _connectionId);
    }

    public static @NotNull @UnmodifiableView Set<@NotNull Long> selectAllFileIdByPathRecursively(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String _connectionId) throws SQLException {
        return Collections.unmodifiableSet(Objects.requireNonNullElseGet(FileManager.selectAllFilesIdByPathRecursively(driverName, List.of(path), _connectionId).get(path), HashSet::new));
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFileByParentPathInPage(final @NotNull String driverName, final @NotNull DrivePath parentPath, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> list = FileSqlHelper.getInstance(driverName).selectFilesByParentPathInPage(parentPath, limit, offset, direction, policy, _connectionId);
        FileManager.Cache.putAll(list.getSecond().stream().collect(Collectors.toMap(FileSqlInformation::id, Function.identity())));
        return list;
    }

    public static @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathRecursivelyLimited(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        final List<FileSqlInformation> list = FileSqlHelper.getInstance(driverName).searchFilesByNameInParentPathRecursivelyLimited(parentPath, rule, caseSensitive, limit, _connectionId);
        FileManager.Cache.putAll(list.stream().collect(Collectors.toMap(FileSqlInformation::id, Function.identity())));
        return list;
    }

    public static void insertPermissionsForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).insertPermissionsForEachFile(idList, groups, _connectionId);
        FileManager.Cache.getAllPresent(idList).values().forEach(i -> i.availableForGroup().addAll(groups));
    }

    public static void insertPermissionsForFile(final @NotNull String driverName, final long id, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertPermissionsForEachFile(driverName, List.of(id), groups, _connectionId);
    }

    public static void insertPermissionForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertPermissionsForEachFile(driverName, idList, List.of(group), _connectionId);
    }

    public static void insertPermissionForFile(final @NotNull String driverName, final long id, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertPermissionsForEachFile(driverName, List.of(id), List.of(id), _connectionId);
    }

    public static void deletePermissionsForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).deletePermissionsForEachFile(idList, groups, _connectionId);
        FileManager.Cache.getAllPresent(idList).values().forEach(i -> i.availableForGroup().removeAll(groups));
    }

    public static void deletePermissionsForFile(final @NotNull String driverName, final long id, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileManager.deletePermissionsForEachFile(driverName, List.of(id), groups, _connectionId);
    }

    public static void deletePermissionForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.deletePermissionsForEachFile(driverName, idList, List.of(group), _connectionId);
    }

    public static void deletePermissionForFile(final @NotNull String driverName, final long id, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.deletePermissionsForEachFile(driverName, List.of(id), List.of(id), _connectionId);
    }
}
