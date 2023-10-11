package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

public record FileManager(@NotNull FileSqlInterface innerSqlInstance) implements FileSqlInterface {
    private static final @NotNull HMultiInitializers<@NotNull String, @NotNull FileManager> ManagerInstances = new HMultiInitializers<>("FileManagers");

    @Deprecated
    public static @NotNull HMultiInitializers<@NotNull String, @NotNull FileManager> getManagerInstances() {
        return FileManager.ManagerInstances;
    }

    public static final @NotNull HInitializer<Function<@NotNull SqlDatabaseInterface, @NotNull BiFunction<@NotNull String, @NotNull Long, @NotNull FileSqlInterface>>> SqlMapper = new HInitializer<>("FileSqlInstanceMapper", d -> (providerName, rootId) -> {
        if (!"Sqlite".equals(d.sqlLanguage()))
            throw new IllegalStateException("Invalid sql language when initializing FileManager." + ParametersMap.create().add("require", "Sqlite").add("real", d.sqlLanguage()));
        return new FileSqliteHelper(d, providerName, rootId.longValue());
    });

    public static void quicklyInitialize(final @NotNull String name, final @NotNull SqlDatabaseInterface database, final long rootId, final @Nullable String _connectionId) throws SQLException {
        try {
            FileManager.ManagerInstances.initializeIfNot(name, HExceptionWrapper.wrapSupplier(() -> {
                final FileSqlInterface instance = FileManager.SqlMapper.getInstance().apply(database).apply(name, rootId);
                instance.createTable(_connectionId);
                return new FileManager(instance);
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitializeReserveTable(final @NotNull String name) {
        return FileManager.ManagerInstances.uninitializeNullable(name) != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        final FileSqlInterface sqlInstance = FileManager.ManagerInstances.uninitializeNullable(name);
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }


    public static @NotNull FileManager getInstance(final @NotNull String providerName) {
        return FileManager.ManagerInstances.getInstance(providerName);
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return this.innerSqlInstance.getConnection(_connectionId, connectionId);
    }

    @Deprecated
    @Override
    public void createTable(final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.createTable(_connectionId);
    }

    @Deprecated
    @Override
    public void deleteTable(final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.deleteTable(_connectionId);
    }

    @Override
    public @NotNull String getProviderName() {
        return this.innerSqlInstance.getProviderName();
    }

    @Override
    public long getRootId() {
        return this.innerSqlInstance.getRootId();
    }


    /* --- Insert --- */

    @Override
    public void insertFileOrDirectory(final @NotNull FileInformation information, final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.insertFileOrDirectory(information, _connectionId);
    }

    @Override
    public void insertIterator(final @NotNull Iterator<@NotNull FileInformation> iterator, final long directoryId, final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.insertIterator(iterator, directoryId, _connectionId);
    }

    /* --- Update --- */

    @Override
    public void updateOrInsertFile(final @NotNull FileInformation file, final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.updateOrInsertFile(file, _connectionId);
    }

    @Override
    public void updateOrInsertDirectory(final @NotNull FileInformation directory, final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.updateOrInsertDirectory(directory, _connectionId);
    }

    @Override
    public void updateOrInsertFileOrDirectory(final @NotNull FileInformation information, final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.updateOrInsertFileOrDirectory(information, _connectionId);
    }

    @Override
    public void calculateDirectorySize(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        this.innerSqlInstance.calculateDirectorySize(directoryId, _connectionId);
    }

    /* --- Select --- */

    @Override
    public @Nullable FileInformation selectInfo(final long id, final boolean isDirectory, @Nullable final String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectInfo(id, isDirectory, _connectionId);
    }

    @Override
    public @NotNull FilesListInformation selectInfosInDirectory(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectInfosInDirectory(directoryId, filter, orders, position, limit, _connectionId);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Set<@NotNull Long>, @NotNull Set<@NotNull Long>> selectIdsInDirectory(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectIdsInDirectory(directoryId, _connectionId);
    }

    @Override
    public @Nullable FileInformation selectInfoInDirectoryByName(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectInfoInDirectoryByName(parentId, name, _connectionId);
    }

    @Override
    public boolean isInDirectoryRecursively(final long id, final boolean isDirectory, final long directoryId, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.isInDirectoryRecursively(id, isDirectory, directoryId, _connectionId);
    }

    /* --- Delete --- */

    @Override
    public boolean deleteFile(final long fileId, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.deleteFile(fileId, _connectionId);
    }

    @Override
    public boolean deleteDirectoryRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.deleteDirectoryRecursively(directoryId, _connectionId);
    }

    @Override
    public boolean deleteFileOrDirectory(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException {
        return this.innerSqlInstance.deleteFileOrDirectory(id, isDirectory, _connectionId);
    }

    /* --- Search --- */

}
