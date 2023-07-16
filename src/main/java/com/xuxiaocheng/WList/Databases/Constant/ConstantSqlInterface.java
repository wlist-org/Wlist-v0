package com.xuxiaocheng.WList.Databases.Constant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.function.Supplier;

public interface ConstantSqlInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;

    @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @Nullable String _connectionId) throws SQLException;
}
