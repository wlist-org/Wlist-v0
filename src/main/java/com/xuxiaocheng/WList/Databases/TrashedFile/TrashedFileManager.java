package com.xuxiaocheng.WList.Databases.TrashedFile;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Initializer.HMultiInitializers;
import com.xuxiaocheng.WList.Driver.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrashedFileManager {
    private TrashedFileManager() {
        super();
    }

    public static final @NotNull HMultiInitializers<String, TrashedSqlInterface> sqlInstances = new HMultiInitializers<>("TrashedSqlInstances");

    public static void quicklyInitialize(final @NotNull TrashedSqlInterface sqlInstance) {
        TrashedFileManager.sqlInstances.initializeIfNot(sqlInstance.getDriverName(), () -> sqlInstance);
    }

    public static boolean quicklyUninitialize(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        final TrashedSqlInterface sqlInstance = TrashedFileManager.sqlInstances.uninitialize(driverName);
        if (sqlInstance != null) sqlInstance.deleteTable(_connectionId);
        return sqlInstance != null;
    }

    public static void insertOrUpdateFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull TrashedSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.sqlInstances.getInstance(driverName).insertOrUpdateFiles(inserters, _connectionId);
    }

    public static void insertOrUpdateFile(final @NotNull String driverName, final @NotNull TrashedSqlInformation inserter, final @Nullable String _connectionId) throws SQLException {
        TrashedFileManager.insertOrUpdateFiles(driverName, List.of(inserter), _connectionId);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull TrashedSqlInformation> selectFiles(final @NotNull String driverName, final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFiles(idList, _connectionId);
    }

    public static @Nullable TrashedSqlInformation selectFile(final @NotNull String driverName, final long id, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.selectFiles(driverName, List.of(id), _connectionId).get(id);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull List<@NotNull TrashedSqlInformation>> selectFilesByName(final @NotNull String driverName, final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFilesByName(nameList, _connectionId);
    }

    public static @NotNull List<@NotNull TrashedSqlInformation> selectFileByName(final @NotNull String driverName, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.selectFilesByName(driverName, List.of(name), _connectionId).get(name);
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesByMd5(final @NotNull String driverName, final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFilesByMd5(md5List, _connectionId);
    }

    public static @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation> selectFilesByMd5(final @NotNull String driverName, final @NotNull String md5, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.selectFilesByMd5(driverName, List.of(md5), _connectionId).get(md5);
    }

    public static @NotNull @UnmodifiableView Set<@NotNull Long> selectFilesId(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFilesId(_connectionId);
    }

    public static long selectFileCount(final @NotNull String driverName, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).selectFileCount(_connectionId);
    }

    public static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesInPage(final @NotNull String driverName, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException {
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

    public static @NotNull @UnmodifiableView List<@Nullable TrashedSqlInformation> searchFilesByNameLimited(final @NotNull String driverName, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException {
        return TrashedFileManager.sqlInstances.getInstance(driverName).searchFilesByNameLimited(rule, caseSensitive, limit, _connectionId);
    }
}
