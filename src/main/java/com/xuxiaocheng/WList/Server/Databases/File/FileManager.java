package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WList.Commons.Options.Options;
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

    private static final @NotNull HMultiInitializers<String, FileSqlInterface> sqlInstances = new HMultiInitializers<>("FileSqlInstances");

    public static void quicklyInitialize(final @NotNull FileSqlInterface sqlInstance, final @Nullable String _connectionId) throws SQLException {
        try {
            FileManager.sqlInstances.initializeIfNot(sqlInstance.getDriverName(), HExceptionWrapper.wrapSupplier(() -> {
                sqlInstance.createTable(_connectionId);
                return sqlInstance;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static boolean quicklyUninitializeReserveTable(final @NotNull String driverName) {
        return FileManager.sqlInstances.uninitializeNullable(driverName) != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        final FileSqlInterface sqlInstance = FileManager.sqlInstances.uninitializeNullable(driverName);
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }

    public static @NotNull Connection getConnection(final @NotNull String driverName, final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).getConnection(_connectionId, connectionId);
    }

    public static void insertFilesForce(final @NotNull String driverName, final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).insertFilesForce(inserters, _connectionId);
    }

    public static void insertFileForce(final @NotNull String driverName, final @NotNull FileInformation inserter, final @Nullable String _connectionId) throws SQLException {
        FileManager.insertFilesForce(driverName, List.of(inserter), _connectionId);
    }

    public static void updateDirectoryType(final @NotNull String driverName, final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).updateDirectoryType(id, empty, _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileInformation> selectFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFiles(idList, _connectionId);
    }

    public static @Nullable FileInformation selectFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFiles(driverName, List.of(id), _connectionId).get(id);
    }

    @Deprecated
    public static @NotNull @UnmodifiableView Map<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull String>, @NotNull FileInformation> selectFilesInDirectory(final @NotNull String driverName, final @NotNull Collection<? extends Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull String>> pairList, final @Nullable String _connectionId) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.sqlInstances.getInstance(driverName).getConnection(_connectionId, connectionId)) {
            return pairList.stream().collect(Collectors.toMap(UnaryOperator.identity(), HExceptionWrapper.wrapFunction(p ->
                    FileManager.selectFileInDirectory(driverName, p.getFirst().longValue(), p.getSecond(), connectionId.get()))));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static @Nullable FileInformation selectFileInDirectory(final @NotNull String driverName, final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFileInDirectory(parentId, name, _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileInformation>> selectFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesByMd5(md5List, _connectionId);
    }

    public static @Nullable @UnmodifiableView Set<@NotNull FileInformation> selectFilesByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesByMd5(driverName, List.of(md5), _connectionId).get(md5);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesIdByParentId(parentIdList, _connectionId);
    }

    public static @NotNull @UnmodifiableView Set<@NotNull Long> selectFileIdByParentId(final @NotNull String driverName, final long parentId, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesIdByParentId(driverName, List.of(parentId), _connectionId).get(parentId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesCountByParentId(parentIdList, _connectionId);
    }

    public static long selectFileCountByParentId(final @NotNull String driverName, final long parentId, final @Nullable String _connectionId) throws SQLException {
        return FileManager.selectFilesCountByParentId(driverName, List.of(parentId), _connectionId).get(parentId).longValue();
    }

    public static Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> selectFilesByParentIdInPage(final @NotNull String driverName, final long parentId, final Options.@NotNull FilterPolicy filter, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).selectFilesByParentIdInPage(parentId, filter, limit, offset, direction, policy, _connectionId);
    }

    public static void mergeFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable Collection<@NotNull Long> mergingUniverse, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).mergeFiles(inserters, mergingUniverse, _connectionId);
    }

    public static void mergeFile(final @NotNull String driverName, final @NotNull FileInformation inserter, final @Nullable String _connectionId) throws SQLException {
        FileManager.mergeFiles(driverName, List.of(inserter), List.of(inserter.id()), _connectionId);
    }

    public static void deleteFilesRecursively(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).deleteFilesRecursively(idList, _connectionId);
    }

    public static void deleteFileRecursively(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesRecursively(driverName, List.of(id), _connectionId);
    }

    public static void deleteFilesByMd5Recursively(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        FileManager.sqlInstances.getInstance(driverName).deleteFilesByMd5Recursively(md5List, _connectionId);
    }

    public static void deleteFileByMd5Recursively(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        FileManager.deleteFilesByMd5Recursively(driverName, List.of(md5), _connectionId);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static @Nullable Long updateDirectorySize(final @NotNull String driverName, final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).updateDirectorySize(directoryId, delta, _connectionId);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static @Nullable Long calculateDirectorySizeRecursively(final @NotNull String driverName, final long directoryId, @Nullable final String _connectionId) throws SQLException {
        return FileManager.sqlInstances.getInstance(driverName).calculateDirectorySizeRecursively(directoryId, _connectionId);
    }
}
