package com.xuxiaocheng.WList.Databases;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface DatabaseInterface {
    @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException;
}
