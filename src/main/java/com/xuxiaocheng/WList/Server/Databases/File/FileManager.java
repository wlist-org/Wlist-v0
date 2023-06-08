package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


    public static void insertOrUpdateFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull FileSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).insertOrUpdateFiles(inserters, _connectionId);
    }

    public static void insertOrUpdateFile(final @NotNull String driverName, final @NotNull FileSqlInformation inserter, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertOrUpdateFiles(driverName, List.of(inserter), _connectionId);
    }

    public static void deleteFilesRecursively(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).deleteFilesRecursively(idList, _connectionId);
    }

    public static void deleteFileRecursively(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesRecursively(driverName, List.of(id),_connectionId);
    }

    public static void deleteFilesByPathRecursively(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).deleteFilesByPathRecursively(pathList, _connectionId);
    }

    public static void deleteFileByPathRecursively(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesByPathRecursively(driverName, List.of(path), _connectionId);
    }

    public static void deleteFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).deleteFilesByMd5(md5List, _connectionId);
    }

    public static void deleteFileByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesByMd5(driverName, List.of(md5), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> selectFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFiles(idList, _connectionId);
    }

    public static @Nullable FileSqlInformation selectFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFiles(driverName, List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull FileSqlInformation> selectFilesByPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesByPath(pathList, _connectionId);
    }

    public static @Nullable FileSqlInformation selectFileByPath(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesByPath(driverName, List.of(path), _connectionId).get(path);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesByMd5(md5List, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@NotNull FileSqlInformation> selectFilesByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesByMd5(driverName, List.of(md5), _connectionId).get(md5);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Set<@NotNull Long>> selectFilesIdByParentPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesIdByParentPath(parentPathList, _connectionId);
    }

    public static @NotNull Set<@NotNull Long> selectFileIdByParentPath(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesIdByParentPath(driverName, List.of(parentPath), _connectionId).get(parentPath);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Set<@NotNull Long>> selectFilesIdByParentPathRecursively(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> pathList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesIdByParentPathRecursively(pathList, _connectionId);
    }

    public static @NotNull Set<@NotNull Long> selectFileIdByParentPathRecursively(final @NotNull String driverName, final @NotNull DrivePath path, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesIdByParentPathRecursively(driverName, List.of(path), _connectionId).get(path);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Long> selectFilesCountByParentPath(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesCountByParentPath(parentPathList, _connectionId);
    }

    public static long selectFileCountByParentPath(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesCountByParentPath(driverName, List.of(parentPath), _connectionId).get(parentPath).longValue();
    }

    public static @NotNull @UnmodifiableView Map<@NotNull DrivePath, @NotNull Long> selectFilesCountByParentPathRecursively(final @NotNull String driverName, final @NotNull Collection<? extends @NotNull DrivePath> parentPathList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesCountByParentPathRecursively(parentPathList, _connectionId);
    }

    public static long selectFileCountByParentPathRecursively(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesCountByParentPathRecursively(driverName, List.of(parentPath), _connectionId).get(parentPath).longValue();
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentPathInPage(final @NotNull String driverName, final @NotNull DrivePath parentPath, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesByParentPathInPage(parentPath, limit, offset, direction, policy, _connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentPathInPageRequireGroup(final @NotNull String driverName, final @NotNull DrivePath parentPath, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final long groupId, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectFilesByParentPathInPageRequireGroup(parentPath, limit, offset, direction, policy, groupId, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathRecursivelyLimitedRequireGroup(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final long groupId, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).searchFilesByNameInParentPathRecursivelyLimitedRequireGroup(parentPath, rule, caseSensitive, limit, groupId, _connectionId);
    }


    public static void insertGroupsForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).insertGroupsForEachFile(idList, groups, _connectionId);
    }

    public static void insertGroupsForFile(final @NotNull String driverName, final long id, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertGroupsForEachFile(driverName, List.of(id), groups, _connectionId);
    }

    public static void insertGroupForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertGroupsForEachFile(driverName, idList, List.of(group), _connectionId);
    }

    public static void insertGroupForFile(final @NotNull String driverName, final long id, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertGroupsForEachFile(driverName, List.of(id), List.of(group), _connectionId);
    }

    public static void deleteGroupsForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileSqlHelper.getInstance(driverName).deleteGroupsForEachFile(idList, groups, _connectionId);
    }

    public static void deleteGroupsForFile(final @NotNull String driverName, final long id, final @NotNull Collection<@NotNull Long> groups, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteGroupsForEachFile(driverName, List.of(id), groups, _connectionId);
    }

    public static void deleteGroupForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteGroupsForEachFile(driverName, idList, List.of(group), _connectionId);
    }

    public static void deleteGroupForFile(final @NotNull String driverName, final long id, final long group, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteGroupsForEachFile(driverName, List.of(id), List.of(group), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Set<@NotNull Long>> selectGroupsForEachFile(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return FileSqlHelper.getInstance(driverName).selectGroupsForEachFile(idList, _connectionId);
    }

    public @NotNull Set<@NotNull Long> selectGroupsForFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectGroupsForEachFile(driverName, List.of(id), _connectionId).get(id);
    }
}
