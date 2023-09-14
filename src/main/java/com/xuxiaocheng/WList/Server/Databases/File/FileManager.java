package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
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



    @Override
    public Pair.@NotNull ImmutablePair<@Nullable FileInformation, @Nullable FileInformation> selectFile(final long id, @Nullable final String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectFile(id, _connectionId);
    }

    @Override
    public @Nullable FileInformation selectFile(final long id, final boolean isDirectory, @Nullable final String _connectionId) throws SQLException {
        return this.innerSqlInstance.selectFile(id, isDirectory, _connectionId);
    }
}
