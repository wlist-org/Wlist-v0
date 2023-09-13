package com.xuxiaocheng.WList.Server.Databases;

import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.File;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class SqlDatabaseManager {
    private SqlDatabaseManager() {
        super();
    }

    private static final @NotNull HInitializer<Function<@NotNull PooledDatabaseIConfiguration, @NotNull SqlDatabaseInterface>> instance = new HInitializer<>("PooledDatabaseInstance"); static {
        SqlDatabaseManager.reinitialize(c -> SqliteDatabaseHelper.getDefault(c.path()));
    }

    private static final @NotNull Map<@NotNull File, @NotNull SqlDatabaseInterface> databases = new ConcurrentHashMap<>();

    // Hooker
    public static void reinitialize(final @NotNull Function<? super @NotNull PooledDatabaseIConfiguration, ? extends @NotNull SqlDatabaseInterface> creator) {
        SqlDatabaseManager.instance.reinitialize(configuration -> {
            final File path = configuration.path().getAbsoluteFile();
            final SqlDatabaseInterface created = SqlDatabaseManager.databases.get(path);
            if (created != null)
                return created;
            final SqlDatabaseInterface instance = creator.apply(configuration);
            return Objects.requireNonNullElse(SqlDatabaseManager.databases.putIfAbsent(path, instance), instance);
        });
    }

    public static @NotNull SqlDatabaseInterface quicklyOpen(final @NotNull File path) throws SQLException {
        final SqlDatabaseInterface instance = SqlDatabaseManager.instance.getInstance().apply(new PooledDatabaseConfiguration(path));
        instance.openIfNot();
        return instance;
    }

    public static boolean quicklyClose(final @NotNull File path) throws SQLException {
        final SqlDatabaseInterface instance = SqlDatabaseManager.databases.remove(path);
        if (instance == null)
            return false;
        instance.close();
        return true;
    }

    public static @NotNull @UnmodifiableView Set<@NotNull File> getOpenedDatabases() {
        return Collections.unmodifiableSet(SqlDatabaseManager.databases.keySet());
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
