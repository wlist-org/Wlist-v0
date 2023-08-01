package com.xuxiaocheng.WList.Databases.GenericSql;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class PooledDatabase {
    private PooledDatabase() {
        super();
    }

    public static final @NotNull HInitializer<PooledDatabaseInterface> instance = new HInitializer<>("PooledDatabase");

    public static void quicklyInitialize(final @NotNull PooledDatabaseInterface instance) throws SQLException {
        try {
            PooledDatabase.instance.initializeIfNot(HExceptionWrapper.wrapSupplier(() -> {
                instance.open();
                return instance;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
    }

    public static boolean quicklyUninitialize() throws SQLException {
        final PooledDatabaseInterface instance = PooledDatabase.instance.uninitialize();
        if (instance == null)
            return false;
        instance.close();
        return true;
    }
}
