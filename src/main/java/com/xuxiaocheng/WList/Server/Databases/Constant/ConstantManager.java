package com.xuxiaocheng.WList.Server.Databases.Constant;

import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.function.Supplier;

public final class ConstantManager {
    private ConstantManager() {
        super();
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static final @NotNull String DefaultRandomChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-_=+[]{}\\|;:,.<>/? ";

    public static @NotNull DatabaseUtil getDatabaseUtil() throws SQLException {
        return DatabaseUtil.getInstance();
    }

    public static void initialize() throws SQLException {
        ConstantSqlHelper.initialize(ConstantManager.getDatabaseUtil(), "initialize");
    }

    // Needn't cache. (Each constant should call this method only once.)
    public static @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @Nullable String _connectionId) throws SQLException {
        return ConstantSqlHelper.getInstance().get(key, defaultValue, _connectionId);
    }
}
