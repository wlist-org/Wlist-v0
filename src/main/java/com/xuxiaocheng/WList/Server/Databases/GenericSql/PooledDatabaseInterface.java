package com.xuxiaocheng.WList.Server.Databases.GenericSql;

import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

public interface PooledDatabaseInterface extends DatabaseInterface, AutoCloseable {
    void open() throws SQLException;
    void close() throws SQLException;

    @NotNull Connection getExplicitConnection(final @NotNull String id) throws SQLException;
    @NotNull Connection getNewConnection(final @Nullable Consumer<? super @NotNull String> idSaver) throws SQLException;
}
