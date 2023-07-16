package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializer.HMultiInitializers;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class FileManager {
    private FileManager() {
        super();
    }

    public static final @NotNull HMultiInitializers<String, FileSqlInterface> sqlInstances = new HMultiInitializers<>("FileSqlInstances");

    public static void quicklyInitialize(final @NotNull FileSqlInterface sqlInstance) {
        FileManager.sqlInstances.initializeIfNot(sqlInstance.getDriverName(), () -> sqlInstance);
    }

    public static boolean quicklyUninitialize(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        final FileSqlInterface sqlInstance = FileManager.sqlInstances.uninitialize(driverName);
        if (sqlInstance != null) sqlInstance.deleteTable(_connectionId);
        return sqlInstance != null;
    }

    public static void insertOrUpdateFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull FileSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).insertOrUpdateFiles(inserters, _connectionId);
    }

    public static void insertOrUpdateFile(final @NotNull String driverName, final @NotNull FileSqlInformation inserter, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertOrUpdateFiles(driverName, List.of(inserter), _connectionId);
    }

    public static void updateDirectoryType(final @NotNull String driverName, final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).updateDirectoryType(id, empty, _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> selectFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFiles(idList, _connectionId);
    }

    public static @Nullable FileSqlInformation selectFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFiles(driverName, List.of(id), _connectionId).get(id);
    }

    @Deprecated
    public static @NotNull @UnmodifiableView Map<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull String>, @NotNull FileSqlInformation> selectFilesInDirectory(final @NotNull String driverName, final @NotNull Collection<? extends Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull String>> pairList, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.sqlInstances.getInstance(driverName).getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            return pairList.stream().collect(Collectors.toMap(UnaryOperator.identity(), HExceptionWrapper.wrapFunction(p ->
                    FileManager.selectFileInDirectory(driverName, p.getFirst().longValue(), p.getSecond(), connectionId.get()))));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static @Nullable FileSqlInformation selectFileInDirectory(final @NotNull String driverName, final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFileInDirectory(parentId, name, _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesByMd5(md5List, _connectionId);
    }

    public static @Nullable @UnmodifiableView Set<@NotNull FileSqlInformation> selectFilesByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesByMd5(driverName, List.of(md5), _connectionId).get(md5);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesIdByParentId(parentIdList, _connectionId);
    }

    public static @NotNull Set<@NotNull Long> selectFileIdByParentId(final @NotNull String driverName, final long parentId, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesIdByParentId(driverName, List.of(parentId), _connectionId).get(parentId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesCountByParentId(parentIdList, _connectionId);
    }

    public static long selectFileCountByParentId(final @NotNull String driverName, final long parentId, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesCountByParentId(driverName, List.of(parentId), _connectionId).get(parentId).longValue();
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentIdInPage(final @NotNull String driverName, final long parentId, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesByParentIdInPage(parentId, limit, offset, direction, policy, _connectionId);
    }

    public static void deleteFilesRecursively(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).deleteFilesRecursively(idList, _connectionId);
    }

    public static void deleteFileRecursively(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesRecursively(driverName, List.of(id),_connectionId);
    }

    public static void deleteFilesByMd5Recursively(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).deleteFilesByMd5Recursively(md5List, _connectionId);
    }

    public static void deleteFileByMd5Recursively(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesByMd5Recursively(driverName, List.of(md5), _connectionId);
    }

    @Deprecated
    public static @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathLimited(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).searchFilesByNameInParentPathLimited(parentPath, rule, caseSensitive, limit, _connectionId);
    }

    @Deprecated
    public static @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathRecursivelyLimited(final @NotNull String driverName, final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).searchFilesByNameInParentPathRecursivelyLimited(parentPath, rule, caseSensitive, limit, _connectionId);
    }
}
