package com.xuxiaocheng.WList.Server.Databases.Constant;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ConstantManager {
    private ConstantManager() {
        super();
    }

    public static final @NotNull HInitializer<ConstantSqlInterface> sqlInstance = new HInitializer<>("ConstantSqlInstance");

    public static final @NotNull HInitializer<Function<@NotNull SqlDatabaseInterface, @NotNull ConstantSqlInterface>> Mapper = new HInitializer<>("ConstantSqlInstanceMapper", d -> {
        if (!"Sqlite".equals(d.sqlLanguage()))
            throw new IllegalStateException("Invalid sql language when initializing ConstantManager." + ParametersMap.create().add("require", "Sqlite").add("real", d.sqlLanguage()));
        return new ConstantSqliteHelper(d);
    });

    public static void quicklyInitialize(final @NotNull SqlDatabaseInterface database, final @Nullable String _connectionId) throws SQLException {
        try {
            ConstantManager.sqlInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                final ConstantSqlInterface instance = ConstantManager.Mapper.getInstance().apply(database);
                instance.createTable(_connectionId);
                return instance;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static boolean quicklyUninitializeReserveTable() {
        return ConstantManager.sqlInstance.uninitializeNullable() != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean quicklyUninitialize(final @Nullable String _connectionId) throws SQLException {
        final ConstantSqlInterface sqlInstance = ConstantManager.sqlInstance.uninitializeNullable();
        if (sqlInstance == null)
            return false;
        sqlInstance.deleteTable(_connectionId);
        return true;
    }

    public static @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        return ConstantManager.sqlInstance.getInstance().getConnection(_connectionId, connectionId);
    }

    // Each constant should call this method only once and then save the value in static fields.
    public static @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @Nullable String _connectionId) throws SQLException {
        return ConstantManager.sqlInstance.getInstance().get(key, defaultValue, _connectionId);
    }
}
