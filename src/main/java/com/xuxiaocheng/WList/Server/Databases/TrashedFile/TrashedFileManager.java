package com.xuxiaocheng.WList.Server.Databases.TrashedFile;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WList.Commons.Options.OrderPolicies;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
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

public final class TrashedFileManager {
    private TrashedFileManager() {
        super();
    }

    private static final @NotNull HMultiInitializers<String, TrashedSqlInterface> sqlInstances = new HMultiInitializers<>("TrashedSqlInstances");

    public static void quicklyInitialize(final @NotNull TrashedSqlInterface sqlInstance, final @Nullable String _connectionId) throws SQLException {
        try {
            TrashedFileManager.sqlInstances.initializeIfNot(sqlInstance.getDriverName(), HExceptionWrapper.wrapSupplier(() -> {
                sqlInstance.createTable(_connectionId);
                return sqlInstance;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static boolean quicklyUninitializeReserveTable(final @NotNull String driverName) {
        return TrashedFileManager.sqlInstances.uninitializeNullable(driverName) != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        final TrashedSqlInterface sqlInstance = TrashedFileManager.sqlInstances.uninitializeNullable(driverName);
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }

    public static @NotNull Connection getConnection(final @NotNull String driverName, final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).getConnection(_connectionId, connectionId);
    }

    public static void insertOrUpdateFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull TrashedFileInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.sqlInstances.getInstance(driverName).insertOrUpdateFiles(inserters, _connectionId);
    }

    public static void insertOrUpdateFile(final @NotNull String driverName, final @NotNull TrashedFileInformation inserter, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.insertOrUpdateFiles(driverName, List.of(inserter), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull TrashedFileInformation> selectFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFiles(idList, _connectionId);
    }

    public static @Nullable TrashedFileInformation selectFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.selectFiles(driverName, List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull List<@NotNull TrashedFileInformation>> selectFilesByName(final @NotNull String driverName, final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFilesByName(nameList, _connectionId);
    }

    public static @NotNull List<@NotNull TrashedFileInformation> selectFileByName(final @NotNull String driverName, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.selectFilesByName(driverName, List.of(name), _connectionId).get(name);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull TrashedFileInformation>> selectFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFilesByMd5(md5List, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@NotNull TrashedFileInformation> selectFilesByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.selectFilesByMd5(driverName, List.of(md5), _connectionId).get(md5);
    }

    public static @NotNull @UnmodifiableView Set<@NotNull Long> selectFilesId(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFilesId(_connectionId);
    }

    public static long selectFileCount(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFileCount(_connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedFileInformation>> selectFilesInPage(final @NotNull String driverName, final int limit, final long offset, final @NotNull OrderDirection direction, final OrderPolicies.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFilesInPage(limit, offset, direction, policy, _connectionId);
    }

    public static void deleteFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.sqlInstances.getInstance(driverName).deleteFiles(idList, _connectionId);
    }

    public static void deleteFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.deleteFiles(driverName, List.of(id),_connectionId);
    }

    public static void deleteFilesByName(final @NotNull String driverName, final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.sqlInstances.getInstance(driverName).deleteFilesByName(nameList, _connectionId);
    }

    public static void deleteFileByName(final @NotNull String driverName, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.deleteFilesByName(driverName, List.of(name), _connectionId);
    }

    public static void deleteFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.sqlInstances.getInstance(driverName).deleteFilesByMd5(md5List, _connectionId);
    }

    public static void deleteFileByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.deleteFilesByMd5(driverName, List.of(md5), _connectionId);
    }

    public static void clear(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.sqlInstances.getInstance(driverName).clear(_connectionId);
    }

    public static @NotNull @UnmodifiableView List<@Nullable TrashedFileInformation> searchFilesByNameLimited(final @NotNull String driverName, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).searchFilesByNameLimited(rule, caseSensitive, limit, _connectionId);
    }
}
