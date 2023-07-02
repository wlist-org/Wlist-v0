package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.WListServer;
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
import java.io.Serial;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class DriverManager {
    private DriverManager() {
        super();
    }

    private static final @NotNull Map<@NotNull String, Pair.@NotNull ImmutablePair<@NotNull WebDriversType, @NotNull DriverInterface<?>>> drivers = new ConcurrentHashMap<>();
    private static final @NotNull Map<@NotNull String, Pair.@NotNull ImmutablePair<@NotNull WebDriversType, @NotNull DriverTrashInterface<?>>> trashes = new ConcurrentHashMap<>();

    public static @NotNull File getConfigurationFile(final @NotNull String name) {
        if (name.contains("\\") || name.contains("/"))
            throw new InvalidPathException(name, "Driver name cannot contain separator (\\/).");
        return Path.of("configs", name + ".yaml").toFile();
    }

    public static void dumpConfiguration(final DriverConfiguration<?, ?, ?> configuration) throws IOException {
        if (GlobalConfiguration.getInstance().dumpConfiguration() && configuration.getCacheSide().resetModified())
            try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(DriverManager.getConfigurationFile(configuration.getName())))) {
                YamlHelper.dumpYaml(configuration.dump(), outputStream);
            }
    }

    private static final @NotNull Pair.ImmutablePair<@NotNull WebDriversType, @NotNull DriverInterface<?>> NULL = new Pair.ImmutablePair<>() {
        @Serial
        private static final long serialVersionUID = -2237568953583714961L;
    };

    @SuppressWarnings("unchecked")
    private static <C extends DriverConfiguration<?, ?, ?>> void add0(final @NotNull String name, final @NotNull WebDriversType type) throws IOException, IllegalParametersException {
        if (DriverManager.drivers.putIfAbsent(name, DriverManager.NULL) != null)
            throw new IllegalParametersException("Conflict driver name.", name);
        final File path = DriverManager.getConfigurationFile(name);
        if (!HFileHelper.ensureFileExist(path))
            throw new IOException("Failed to create driver configuration file. path: " + path.getAbsolutePath());
        final Supplier<DriverTrashInterface<?>> supplier = type.getTrash();
        final DriverTrashInterface<DriverInterface<C>> trash = supplier == null ? null : (DriverTrashInterface<DriverInterface<C>>) supplier.get();
        final DriverInterface<C> driver = trash == null ? (DriverInterface<C>) type.getDriver().get() : trash.getDriver();
        final C configuration = driver.getConfiguration();
        final Map<String, Object> config = new LinkedHashMap<>();
        try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(path))) {
            config.putAll(YamlHelper.loadYaml(inputStream));
        }
        final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
        configuration.load(config, errors);
        YamlHelper.throwErrors(errors);
        configuration.setName(name);
        try {
            driver.initialize(configuration);
            if (trash != null)
                trash.initialize(driver);
        } catch (final Exception exception) {
            if (GlobalConfiguration.getInstance().deleteDriver())
                try {
                    driver.uninitialize();
                    if (trash != null)
                        trash.uninitialize();
                } catch (final Exception e) {
                    throw new IllegalParametersException("Failed to uninitialize after a failed initialization.", Map.of("name", name, "type", type, "configuration", configuration, "exception", exception), e);
                }
            throw new IllegalParametersException("Failed to initialize.", Map.of("name", name, "type", type, "configuration", configuration), exception);
        }
        try {
            driver.buildCache();
            if (trash != null)
                trash.buildCache();
        } catch (final Exception exception) {
            throw new IllegalParametersException("Failed to build cache.", Map.of("name", name, "type", type, "configuration", configuration), exception);
        }
        DriverManager.dumpConfiguration(configuration);
        DriverManager.drivers.put(name, Pair.ImmutablePair.makeImmutablePair(type, driver));
        if (trash != null)
            DriverManager.trashes.put(name, Pair.ImmutablePair.makeImmutablePair(type, trash));
    }

    public static void initialize() {
        DriverManager.drivers.clear();
        final Collection<CompletableFuture<?>> futures = new ArrayList<>(GlobalConfiguration.getInstance().drivers().size());
        for (final Map.Entry<String, WebDriversType> entry: GlobalConfiguration.getInstance().drivers().entrySet())
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    HLog.getInstance("DefaultLogger").log(HLogLevel.INFO, "Driver: ", entry.getKey(), " type: ", entry.getValue().name());
                    DriverManager.add0(entry.getKey(), entry.getValue());
                } catch (final IllegalParametersException | IOException exception) {
                    HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "Failed to initialize driver: ", entry.getKey(), exception);
                }
            }, WListServer.ServerExecutors));
        for (final CompletableFuture<?> future: futures)
            future.join();
    }

    public static @Nullable DriverInterface<?> get(final @NotNull String name) {
        final Pair.ImmutablePair<WebDriversType, DriverInterface<?>> driver = DriverManager.drivers.get(name);
        if (driver == null || driver == DriverManager.NULL)
            return null;
        return driver.getSecond();
    }

    public static @Nullable DriverTrashInterface<?> getTrash(final @NotNull String name) {
        final Pair.ImmutablePair<WebDriversType, DriverTrashInterface<?>> trash = DriverManager.trashes.get(name);
        if (trash == null)
            return null;
        return trash.getSecond();
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, Pair.@NotNull ImmutablePair<@NotNull WebDriversType, @NotNull DriverInterface<?>>> getAll() {
        return DriverManager.drivers.entrySet().stream().filter(t -> t.getValue() != DriverManager.NULL).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static @NotNull @UnmodifiableView Map<@NotNull String, Pair.@NotNull ImmutablePair<@NotNull WebDriversType, @NotNull DriverTrashInterface<?>>> getAllTrashes() {
        return Collections.unmodifiableMap(DriverManager.trashes);
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
                driver.getSecond().uninitialize();
            } catch (final Exception exception) {
                throw new IOException(exception);
            }
    }
}
