package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
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
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class DriverManager {
    private DriverManager() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull String> driverTypes = new HashMap<>();
    private static final @NotNull Map<@NotNull String, @NotNull DriverInterface<?>> drivers = new HashMap<>();

    public static void init() throws SQLException, IOException {
        DriverManager.driverTypes.clear();
        DriverManager.drivers.clear();
        DriverManager.sqlInit();
        DriverManager.driverTypes.putAll(DriverManager.sqlSelectAllDrivers());
        for (final Map.Entry<String, String> entry: DriverManager.driverTypes.entrySet())
            DriverManager.add(entry.getKey(), entry.getValue());
    }

    @SuppressWarnings({"unchecked"})
    public static boolean add(final @NotNull String name, final @NotNull String type) throws SQLException, IOException {
        if (DriverManager.drivers.containsKey(name))
            return false;
        final String path = "configs/" + name + ".yml";
        final Supplier<DriverInterface<?>> supplier = WebDriversRegisterer.DriversMap.get(type);
        if (supplier == null) {
            HLog.DefaultLogger.log(HLogLevel.WARN, "Unregistered driver. name: ", name, ", type: ", type);
            DriverManager.sqlDeleteDriver(name);
            return false;
        }
        final DriverInterface<?> driver = supplier.get();
        DriverConfiguration<?, ?, ?> configuration = driver.getDefaultConfiguration();
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            final DriverConfiguration<?, ?, ?> temp = (DriverConfiguration<?, ?, ?>) new Yaml().loadAs(inputStream, configuration.getClass());
            if (temp != null)
                configuration = temp;
        } catch (final FileNotFoundException ignore) {
            final File file = new File(path);
            //noinspection unused
            final boolean f = file.getParentFile().mkdirs() && file.createNewFile();
        }
        try {
            ((DriverInterface<DriverConfiguration<?,?,?>>) driver).login(configuration);
        } catch (final Exception exception) {
            HLog.DefaultLogger.log("ERROR", exception);
            return false;
        }
        final Representer representer = new FieldOrderRepresenter();
        configuration.setConfigClassTag(representer);
        try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
            outputStream.write(new Yaml(representer).dump(configuration).getBytes(StandardCharsets.UTF_8));
        }
        if (!DriverManager.sqlInsertDriver(name, type))
            return false;
        DriverManager.drivers.put(name, driver);
        return true;
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
        //noinspection ResultOfMethodCallIgnored
        new File("config/" + name + ".yml").delete();
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

    private static boolean sqlInsertDriver(final @NotNull String name, final @NotNull String type) throws SQLException {
        try (final Connection connection = DataBaseUtil.getDataInstance().getConnection()) {
            try (final PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO drivers (name, type) VALUES (?, ?);
                        """)) {
                statement.setString(1, name);
                statement.setString(2, type);
                statement.executeUpdate();
                return true;
            }
        } catch (final SQLException exception) {
            if (exception.getMessage().contains("UNIQUE"))
                return false;
            throw exception;
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
