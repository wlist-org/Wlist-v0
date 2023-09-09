package com.xuxiaocheng.WList.Server.Databases;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.WList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class PooledSqlDatabase {
    private PooledSqlDatabase() {
        super();
    }

    private static final @NotNull HInitializer<Function<@NotNull PooledDatabaseIConfiguration, @NotNull PooledSqlDatabaseInterface>> instance = new HInitializer<>("PooledDatabaseInstance"); static {
        PooledSqlDatabase.reinitialize(c -> PooledSqliteDatabase.getDefault(c.path()));
    }

    private static final @NotNull Map<@NotNull File, @NotNull PooledSqlDatabaseInterface> databases = new ConcurrentHashMap<>();

    // Hooker
    public static void reinitialize(final @NotNull Function<? super @NotNull PooledDatabaseIConfiguration, ? extends @NotNull PooledSqlDatabaseInterface> creator) {
        PooledSqlDatabase.instance.reinitialize(configuration -> {
            final File path = configuration.path().getAbsoluteFile();
            final PooledSqlDatabaseInterface created = PooledSqlDatabase.databases.get(path);
            if (created != null)
                return created;
            final PooledSqlDatabaseInterface instance = creator.apply(configuration);
            return Objects.requireNonNullElse(PooledSqlDatabase.databases.putIfAbsent(path, instance), instance);
        });
    }

    public static @NotNull PooledSqlDatabaseInterface quicklyOpen(final @NotNull File path) throws SQLException {
        final PooledSqlDatabaseInterface instance = PooledSqlDatabase.instance.getInstance().apply(new PooledDatabaseConfiguration(path));
        instance.openIfNot();
        return instance;
    }

    public static boolean quicklyClose(final @NotNull File path) throws SQLException {
        final PooledSqlDatabaseInterface instance = PooledSqlDatabase.databases.remove(path);
        if (instance == null)
            return false;
        instance.close();
        return true;
    }

    public static @NotNull File getDriverDatabasePath(final @NotNull String driverName) {
        assert DriverManager.isDriverNameValid(driverName);
        return new File(WList.RuntimePath.getInstance(), "cache/" + driverName + ".db");
    }

    @FunctionalInterface
    public interface PooledDatabaseIConfiguration {
        @NotNull File path();
        default @Nullable String username() {
            return null;
        }
        default @Nullable String password() {
            return null;
        }
    }

    public record PooledDatabaseConfiguration(@NotNull File path) implements PooledDatabaseIConfiguration {
    }
}
