package com.xuxiaocheng.WList.Server.Driver;

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
import org.jetbrains.annotations.UnmodifiableView;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class DriverManager {
    private DriverManager() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull DriverInterface<?>> drivers = new ConcurrentHashMap<>();
    private static final @NotNull Map<@NotNull String, @NotNull String> driverTypes = new ConcurrentHashMap<>();

    private static @NotNull File getConfigPath(final @NotNull String name) {
        return new File("configs", name + ".yml");
    }

    public static void init() throws SQLException {
        DriverManager.driverTypes.clear();
        DriverManager.drivers.clear();
        DriverManager.sqlInit();
        for (final Map.Entry<String, String> entry: DriverManager.sqlSelectAllDrivers().entrySet())
            try {
                DriverManager.add(entry.getKey(), entry.getValue());
            } catch (final IllegalParametersException exception) {
                HLog.DefaultLogger.log(HLogLevel.WARN, exception);
                DriverManager.sqlDeleteDriver(entry.getKey());
            } catch (final SQLException | IOException exception) {
                HLog.DefaultLogger.log(HLogLevel.ERROR, "entry: ", entry, exception);
            }
    }

    @SuppressWarnings("unchecked")
    public static void add(final @NotNull String name, final @NotNull String type) throws SQLException, IOException, IllegalParametersException {
        if (DriverManager.driverTypes.containsKey(name) || DriverManager.drivers.containsKey(name))
            throw new IllegalParametersException("Conflict driver name.");
        final File path = DriverManager.getConfigPath(name);
        if (!path.isFile())
            throw new FileNotFoundException();
        final DriverInterface<?> driver;
        final DriverConfiguration<?, ?, ?> configuration;
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            final Supplier<DriverInterface<?>> supplier = WebDriversRegisterer.DriversMap.get(type);
            if (supplier == null) {
                final Map<String, String> p = new LinkedHashMap<>();
                p.put("name", name);
                p.put("type", type);
                throw new IllegalParametersException("Unregistered driver type.", p);
            }
            driver = supplier.get();
            try {
                configuration = (DriverConfiguration<?, ?, ?>) Objects.requireNonNullElseGet(new Yaml().loadAs(inputStream, driver.getDefaultConfigurationClass()), () -> {
                    try {
                        final Constructor<?> constructor = driver.getDefaultConfigurationClass().getConstructor();
                        return constructor.newInstance();
                    } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException |
                                   InvocationTargetException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            } catch (final RuntimeException exception) {
                if (exception.getCause() instanceof NoSuchMethodException)
                    throw new IllegalParametersException("Need default constructor.", type);
                if (exception.getCause() instanceof InstantiationException ||
                        exception.getCause() instanceof IllegalAccessException ||
                        exception.getCause() instanceof InvocationTargetException)
                    throw new IllegalParametersException("Failed to get default configuration.", exception);
                throw exception;
            }
        }
        try {
            ((DriverInterface<DriverConfiguration<?,?,?>>) driver).login(configuration);
        } catch (final Exception exception) {
            throw new IllegalParametersException("Failed to login.", exception);
        }
        final Representer representer = new FieldOrderRepresenter();
        configuration.setConfigClassTag(representer);
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
            outputStream.write(new Yaml(representer).dump(configuration).getBytes(StandardCharsets.UTF_8));
        }
        DriverManager.driverTypes.put(name, type);
        DriverManager.drivers.put(name, driver);
        DriverManager.sqlInsertDriver(name, type);
    }

    public static @Nullable DriverInterface<?> get(final @NotNull String name) {
        return DriverManager.drivers.get(name);
    }

    public static void del(final @NotNull String name) throws Exception {
        DriverManager.driverTypes.remove(name);
        final DriverInterface<?> driver = DriverManager.drivers.remove(name);
        if (driver == null)
            return;
        driver.deleteDriver();
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull String> getDriverTypes() {
        return Collections.unmodifiableMap(DriverManager.driverTypes);
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
