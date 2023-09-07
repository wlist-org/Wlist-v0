package com.xuxiaocheng.WList.Server.Databases.Constant;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ConstantManager {
    private ConstantManager() {
        super();
    }

    private static final @NotNull HInitializer<ConstantSqlInterface> sqlInstance = new HInitializer<>("ConstantSqlInstance");

    public static void quicklyInitialize(final @NotNull ConstantSqlInterface sqlInstance, final @Nullable String _connectionId) throws SQLException {
        try {
            ConstantManager.sqlInstance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                sqlInstance.createTable(_connectionId);
                return sqlInstance;
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
