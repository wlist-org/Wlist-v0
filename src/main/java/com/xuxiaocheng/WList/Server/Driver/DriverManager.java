package com.xuxiaocheng.WList.Server.Driver;

import com.alibaba.fastjson2.TypeReference;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
        final DriverInterface<C> driver;
        try {
            driver = (DriverInterface<C>) type.getSupplier().get();
        } catch (final RuntimeException exception) {
            throw new IllegalParametersException("Failed to get driver.", Map.of("name", name, "type", type), exception);
        }
        final File path = new File("configs", name + ".yml");
        if (!HFileHelper.ensureFileExist(path))
            throw new IOException("Failed to create driver configuration file. path: " + path.getAbsolutePath());
        final C configuration;
        try {
            configuration = (C) new TypeReference<C>() {}.getRawType().getConstructor().newInstance();
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalParametersException("Failed to get driver configuration.", Map.of("name", name, "type", type), exception);
        }
        final Map<String, Object> config = new LinkedHashMap<>();
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            config.putAll(YamlHelper.loadYaml(inputStream));
        }
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        configuration.load(config, errors);
        if (GlobalConfiguration.getInstance().dumpConfiguration())
            try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
                YamlHelper.dumpYaml(config, outputStream);
            }
        try {
            driver.login(configuration);
        } catch (final Exception exception) {
            throw new IllegalParametersException("Failed to login.", Map.of("name", name, "type", type), exception);
        }
        final boolean[] flag = new boolean[] {true};
        DriverManager.drivers.computeIfAbsent(name, (n) -> {
            flag[0] = false;
            return Pair.ImmutablePair.makeImmutablePair(type, driver);
        });
        if (flag[0])
            HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "Conflict driver. Abort newer. name: ", name, " configuration: ", configuration);
    }

    public static void init() {
        DriverManager.drivers.clear();
        for (final Map.Entry<String, WebDriversType> entry: GlobalConfiguration.getInstance().drivers().entrySet())
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
        // TODO dynamically change configuration.
//        GlobalConfiguration.addDriver(name, type);
    }

    public static void del(final @NotNull String name) throws IOException {
        final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver = DriverManager.drivers.remove(name);
        if (driver == null)
            return;
//        GlobalConfiguration.subDriver(name);
        if (GlobalConfiguration.getInstance().dumpConfiguration())
            try {
                driver.getSecond().deleteDriver();
            } catch (final Exception exception) {
                throw new IOException(exception);
            }
    }
}
