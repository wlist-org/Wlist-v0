package com.xuxiaocheng.WList.Server.Driver;

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
import org.jetbrains.annotations.UnmodifiableView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
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
        final File path = new File("configs", name + ".yaml");
        if (!HFileHelper.ensureFileExist(path))
            throw new IOException("Failed to create driver configuration file. path: " + path.getAbsolutePath());
        final C configuration;
        try {
            ParameterizedType configType = null;
            for (final Type t: driver.getClass().getGenericInterfaces())
                if (t instanceof ParameterizedType p && DriverInterface.class.equals(p.getRawType())) {
                    configType = p;
                    break;
                }
            configuration = ((Class<C>) Objects.requireNonNull(configType).getActualTypeArguments()[0]).getConstructor().newInstance();
        } catch (final IllegalArgumentException | SecurityException | ClassCastException | NullPointerException
                       | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            throw new IllegalParametersException("Failed to get driver configuration.", Map.of("name", name, "type", type), exception);
        }
        final Map<String, Object> config = new LinkedHashMap<>();
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            config.putAll(YamlHelper.loadYaml(inputStream));
        }
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        configuration.load(config, errors);
        YamlHelper.throwErrors(errors);
        try {
            driver.initiate(configuration);
        } catch (final Exception exception) {
            if (GlobalConfiguration.getInstance().deleteDriver())
                try {
                    driver.uninitiate();
                } catch (final Exception e) {
                    throw new IllegalParametersException("Failed to uninitiate after initiated.", Map.of("name", name, "type", type, "configuration", configuration, "exception", exception), e);
                }
            throw new IllegalParametersException("Failed to login.", Map.of("name", name, "type", type, "configuration", configuration), exception);
        }
        try {
            driver.buildCache();
        } catch (final Exception exception) {
            throw new IllegalParametersException("Failed to build cache.", Map.of("name", name, "type", type, "configuration", configuration), exception);
        } finally {
            if (GlobalConfiguration.getInstance().dumpConfiguration())
                try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path))) {
                    YamlHelper.dumpYaml(configuration.dump(), outputStream);
                }
        }
        DriverManager.drivers.merge(name, Pair.ImmutablePair.makeImmutablePair(type, driver), (o, n) -> {
            HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "Conflict driver. Abort newer. name: ", name, " configuration: ", configuration);
            if (GlobalConfiguration.getInstance().deleteDriver())
                try {
                    n.getSecond().uninitiate();
                } catch (final Exception exception) {
                    HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "Failed to uninitiate when aborting. name: ", name, " configuration: ", configuration, exception);
                }
            return o;
        });
    }

    public static void init() {
        DriverManager.drivers.clear();
        for (final Map.Entry<String, WebDriversType> entry: GlobalConfiguration.getInstance().drivers().entrySet())
            try {
                HLog.getInstance("DefaultLogger").log(HLogLevel.INFO, "Driver: ", entry.getKey(), " type: ", entry.getValue().name());
                DriverManager.add0(entry.getKey(), entry.getValue());
            } catch (final IllegalParametersException | IOException exception) {
                HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "Driver: ", entry.getKey(), " type: ", entry.getValue().name(), exception);
            }
    }

    public static @Nullable DriverInterface<?> get(final @NotNull String name) {
        final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver = DriverManager.drivers.get(name);
        if (driver == null)
            return null;
        return driver.getSecond();
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, Pair.@NotNull ImmutablePair<@NotNull WebDriversType, @NotNull DriverInterface<?>>> getAll() {
        return Collections.unmodifiableMap(DriverManager.drivers);
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
        if (GlobalConfiguration.getInstance().deleteDriver())
            try {
                driver.getSecond().uninitiate();
            } catch (final Exception exception) {
                throw new IOException(exception);
            }
    }
}
