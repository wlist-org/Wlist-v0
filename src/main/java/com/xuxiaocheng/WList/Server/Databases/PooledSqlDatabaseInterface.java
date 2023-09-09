package com.xuxiaocheng.WList.Server.Databases;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

public interface PooledSqlDatabaseInterface extends DatabaseInterface, AutoCloseable {
    @Contract(pure = true)
    @NotNull String sqlLanguage();

    void openIfNot() throws SQLException;

    void close() throws SQLException;

    @NotNull Connection getExplicitConnection(final @NotNull String id) throws SQLException;

    @NotNull Connection getNewConnection(final @Nullable Consumer<? super @NotNull String> idSaver) throws SQLException;
}
