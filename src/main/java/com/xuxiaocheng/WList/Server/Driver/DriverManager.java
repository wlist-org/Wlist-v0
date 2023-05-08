package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Configuration.FieldOrderRepresenter;
import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DriverManager {
    private DriverManager() {
        super();
    }

    private static final @NotNull Map<@NotNull String, Pair.@NotNull ImmutablePair<@NotNull WebDriversType, @NotNull DriverInterface<?>>> drivers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private static <C extends DriverConfiguration<?, ?, ?>> void add0(final @NotNull String name, final @NotNull WebDriversType type) throws IOException, IllegalParametersException {
        if (DriverManager.drivers.containsKey(name))
            throw new IllegalParametersException("Conflict driver name.");
        final File path = new File("configs", name + ".yml");
        if (!HFileHelper.ensureFileExist(path))
            throw new IOException("Failed to create driver configuration file. path: " + path.getAbsolutePath());
        final DriverInterface<C> driver = (DriverInterface<C>) type.getSupplier().get();
        C configuration = null;
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            configuration = new Yaml().loadAs(inputStream, driver.getDefaultConfigurationClass());
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

    public static void init() {
        DriverManager.drivers.clear();
        for (final Map.Entry<String, WebDriversType> entry: GlobalConfiguration.getInstance().getDrivers().entrySet())
            try {
                HLog.getInstance("DefaultLogger").log(HLogLevel.INFO, "Driver: ", entry.getKey(), " type: ", entry.getValue().name());
                DriverManager.add0(entry.getKey(), entry.getValue());
            } catch (final IllegalParametersException | IOException exception) {
                HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "entry: ", entry, exception);
            }
    }

    public static @Nullable DriverInterface<?> get(final @NotNull String name) {
        final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver = DriverManager.drivers.get(name);
        if (driver == null)
            return null;
        return driver.getSecond();
    }

    public static void add(final @NotNull String name, final @NotNull WebDriversType type) throws IOException, IllegalParametersException {
        DriverManager.add0(name, type);
        GlobalConfiguration.addDriver(name, type);
    }

    public static @Nullable DriverInterface<?> del(final @NotNull String name) throws IOException {
        final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver = DriverManager.drivers.remove(name);
        if (driver == null)
            return null;
        GlobalConfiguration.subDriver(name);
        return driver.getSecond();
    }
}
