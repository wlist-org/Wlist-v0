package com.xuxiaocheng.WList.Databases.Constant;

import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.function.Supplier;

public final class ConstantManager {
    private ConstantManager() {
        super();
    }

    public static final @NotNull HInitializer<ConstantSqlInterface> sqlInstance = new HInitializer<>("ConstantSqlInstance");

    @SuppressWarnings("SpellCheckingInspection")
    public static final @NotNull String DefaultRandomChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-_=+[]{}\\|;:,.<>/? ";

    // Each constant should call this method only once and then save the value in static fields.
    public static @NotNull String get(final @NotNull String key, final @NotNull Supplier<@NotNull String> defaultValue, final @Nullable String _connectionId) throws SQLException {
        return ConstantManager.sqlInstance.getInstance().get(key, defaultValue, _connectionId);
    }
}
