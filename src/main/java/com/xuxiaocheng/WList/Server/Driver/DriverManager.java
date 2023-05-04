package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Configuration.FieldOrderRepresenter;
import com.xuxiaocheng.WList.Utils.DataBaseUtil;
import com.xuxiaocheng.WList.WebDrivers.WebDriversRegisterer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class DriverManager {
    private DriverManager() {
        super();
    }

    private static final @NotNull Map<@NotNull String, Pair.ImmutablePair<@NotNull String, @NotNull DriverInterface<?>>> drivers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <C extends DriverConfiguration<?, ?, ?>> void add0(final @NotNull String name, final @NotNull String type) throws IOException, IllegalParametersException {
        if (DriverManager.drivers.containsKey(name))
            throw new IllegalParametersException("Conflict driver name.");
        final File path = new File("configs", name + ".yml");
        if (path.exists() && !path.isFile())
            throw new FileNotFoundException(path.getAbsolutePath());
        //noinspection ResultOfMethodCallIgnored
        path.createNewFile();
        final DriverInterface<C> driver; {
            final Supplier<DriverInterface<?>> supplier = WebDriversRegisterer.DriversMap.get(type);
            if (supplier == null) {
                final Map<String, String> p = new LinkedHashMap<>();
                p.put("name", name);
                p.put("type", type);
                throw new IllegalParametersException("Unregistered driver type.", p);
            }
            driver = (DriverInterface<C>) supplier.get();
        }
        C configuration = null;
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            configuration = new Yaml().loadAs(inputStream, driver.getDefaultConfigurationClass());
        } catch (final FileNotFoundException ignore) {
        }
        if (configuration == null)
            try {
                final Constructor<C> constructor = driver.getDefaultConfigurationClass().getConstructor();
                configuration = constructor.newInstance();
            } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalParametersException("Failed to get default configuration.", exception);
            }
        else {
            try {
                driver.login(configuration);
            } catch (final Exception exception) {
                throw new IllegalParametersException("Failed to login.", exception);
            }
            final boolean flag;
            synchronized (DriverManager.drivers) {
                flag = DriverManager.drivers.containsKey(name);
                DriverManager.drivers.put(name, Pair.ImmutablePair.makeImmutablePair(type, driver));
            }
            if (flag)
                HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "Replaced driver. name=", name);
        }
        final Representer representer = new FieldOrderRepresenter();
        configuration.setConfigClassTag(representer);
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
            outputStream.write(new Yaml(representer).dump(configuration).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void init() throws SQLException {
        DriverManager.sqlInit();
        DriverManager.drivers.clear();
        for (final Map.Entry<String, String> entry: DriverManager.sqlSelectAllDrivers().entrySet())
            try {
                DriverManager.add0(entry.getKey(), entry.getValue());
            } catch (final IllegalParametersException | IOException exception) {
                HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "entry: ", entry, exception);
            }
    }

    public static @Nullable DriverInterface<?> get(final @NotNull String name) {
        final Pair.ImmutablePair<String, DriverInterface<?>> driver = DriverManager.drivers.get(name);
        if (driver == null)
            return null;
        return driver.getSecond();
    }

    public static void add(final @NotNull String name, final @NotNull String type) throws IOException, IllegalParametersException, SQLException {
        DriverManager.add0(name, type);
        DriverManager.sqlInsertDriver(name, type);
    }

    public static @Nullable DriverInterface<?> del(final @NotNull String name) throws SQLException {
        final Pair.ImmutablePair<String, DriverInterface<?>> driver = DriverManager.drivers.remove(name);
        if (driver == null)
            return null;
        DriverManager.sqlDeleteDriver(name);
        return driver.getSecond();
    }

    private static void sqlInit() throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        CREATE TABLE IF NOT EXISTS drivers (
                            id          INTEGER    PRIMARY KEY AUTOINCREMENT
                                                   UNIQUE
                                                   NOT NULL,
                            name        TEXT       UNIQUE
                                                   NOT NULL,
                            type        TEXT       NOT NULL
                        );
                        """)) {
                statement.executeUpdate();
            }
        }
    }

    private static void sqlInsertDriver(final @NotNull String name, final @NotNull String type) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO drivers (name, type) VALUES (?, ?)
                        ON CONFLICT (name) DO UPDATE SET type = excluded.type;
                        """)) {
                statement.setString(1, name);
                statement.setString(2, type);
                statement.executeUpdate();
            }
        }
    }

    private static void sqlDeleteDriver(final @NotNull String name) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        DELETE FROM drivers WHERE name == ?;
                        """)) {
                statement.setString(1, name);
                statement.executeUpdate();
            }
        }
    }

    private static @NotNull Map<@NotNull String, @NotNull String> sqlSelectAllDrivers() throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        SELECT name, type FROM drivers;
                        """)) {
                try (final ResultSet user = statement.executeQuery()) {
                    final Map<String, String> drivers = new HashMap<>();
                    while (user.next())
                        drivers.put(user.getString(1), user.getString(2));
                    return drivers;
                }
            }
        }
    }
}
