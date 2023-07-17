package com.xuxiaocheng.WList.Server.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
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
import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class DriverManager {
    private DriverManager() {
        super();
    }

    private static final @NotNull HInitializer<File> configurationsPath = new HInitializer<>("DriverConfigurationsDirectory");
    private static final @NotNull Map<@NotNull String, @NotNull Pair<@NotNull WebDriversType, Pair.@NotNull ImmutablePair<@NotNull DriverInterface<?>, @Nullable DriverTrashInterface<?>>>> drivers = new ConcurrentHashMap<>();
    private static final Pair.@NotNull ImmutablePair<@NotNull DriverInterface<?>, @Nullable DriverTrashInterface<?>> DriverPlaceholder = new Pair.ImmutablePair<>() {
        @Serial
        private static final long serialVersionUID = -2237568953583714961L;

        @Override
        public @NotNull DriverInterface<?> getFirst() {
            throw new IllegalStateException("Unreachable!");
        }

        @Override
        public @Nullable DriverTrashInterface<?> getSecond() {
            throw new IllegalStateException("Unreachable!");
        }
    };

    public static void initialize(final @NotNull File configurationsPath) {
        DriverManager.configurationsPath.initialize(configurationsPath.getAbsoluteFile());
        DriverManager.drivers.clear();
        final CompletableFuture<?>[] futures = new CompletableFuture<?>[GlobalConfiguration.getInstance().drivers().size()];
        int i = 0;
        for (final Map.Entry<String, WebDriversType> entry: GlobalConfiguration.getInstance().drivers().entrySet())
            futures[i++] = CompletableFuture.runAsync(() -> {
                try {
                    HLog.getInstance("DefaultLogger").log(HLogLevel.INFO, "Driver: ", entry.getKey(), " type: ", entry.getValue().name());
                    DriverManager.initializeDriver0(entry.getKey(), entry.getValue());
                } catch (final IllegalParametersException | IOException exception) {
                    HLog.getInstance("DefaultLogger").log(HLogLevel.ERROR, "Failed to initialize driver: ", entry.getKey(), exception);
                } catch (final Throwable throwable) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), throwable);
                    // TODO record failure.
                }
            }, WListServer.ServerExecutors);
        CompletableFuture.allOf(futures).join();
    }

    private static @NotNull File getConfigurationFile(final @NotNull String name) throws IOException {
        final File file = new File(DriverManager.configurationsPath.getInstance(), name + ".yaml");
        if (!DriverManager.configurationsPath.getInstance().equals(file.getParentFile()) || !HFileHelper.ensureFileExist(file))
            throw new IOException("Invalid driver name." + ParametersMap.create().add("name", name).add("configurationsPath", DriverManager.configurationsPath.getInstance()));
        if (!file.canRead() || !file.canWrite())
            throw new AccessDeniedException("No permissions to read or write driver configuration file." + ParametersMap.create().add("name", name).add("file", file));
        return file;
    }

    @SuppressWarnings("unchecked")
    private static <C extends DriverConfiguration<?, ?, ?>> void initializeDriver0(final @NotNull String name, final @NotNull WebDriversType type) throws IOException, IllegalParametersException {
        final Pair<WebDriversType, Pair.ImmutablePair<DriverInterface<?>, DriverTrashInterface<?>>> driverTriad = Pair.makePair(type, DriverManager.DriverPlaceholder);
        if (DriverManager.drivers.putIfAbsent(name, driverTriad) != null)
            throw new IllegalParametersException("Conflict driver name.", ParametersMap.create().add("name", name).add("type", type));
        try {
            final File configurationPath = DriverManager.getConfigurationFile(name);
            final Supplier<DriverTrashInterface<?>> trashSupplier = type.getTrash();
            final DriverTrashInterface<DriverInterface<C>> trash = trashSupplier == null ? null : (DriverTrashInterface<DriverInterface<C>>) trashSupplier.get();
            final DriverInterface<C> driver = trash == null ? (DriverInterface<C>) type.getDriver().get() : trash.getDriver();
            final Pair.ImmutablePair<DriverInterface<?>, DriverTrashInterface<?>> driverPair = Pair.ImmutablePair.makeImmutablePair(driver, trash);
            final C configuration = driver.getConfiguration();
            final Map<String, Object> config;
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationPath))) {
                config = YamlHelper.loadYaml(inputStream);
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
                try {
                    DriverManager.uninitializeDriver0(name);
                } catch (final IllegalParametersException e) {
                    exception.addSuppressed(e.getCause());
                    throw new IllegalParametersException("Failed to uninitialize the driver after a failed initialization.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
                }
                throw new IllegalParametersException("Failed to initialize.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
            }
            try {
                driver.buildCache();
                if (trash != null)
                    trash.buildCache();
            } catch (final Exception exception) {
                throw new IllegalParametersException("Failed to build cache.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
            } finally {
                DriverManager.dumpConfigurationIfModified(configuration);
            }
            driverTriad.setSecond(driverPair);
        } finally {
            if (driverTriad.getSecond() == DriverManager.DriverPlaceholder)
                DriverManager.drivers.remove(name);
        }
    }

    private static boolean uninitializeDriver0(final @NotNull String name) throws IllegalParametersException {
        final Pair<@NotNull WebDriversType, Pair.@NotNull ImmutablePair<@NotNull DriverInterface<?>, @Nullable DriverTrashInterface<?>>> driver = DriverManager.drivers.remove(name);
        if (driver == null || driver.getSecond() == DriverManager.DriverPlaceholder) return false;
        if (GlobalConfiguration.getInstance().deleteDriver()) {
            try {
                driver.getSecond().getFirst().uninitialize();
                if (driver.getSecond().getSecond() != null)
                    driver.getSecond().getSecond().uninitialize();
            } catch (final Exception exception) {
                throw new IllegalParametersException("Failed to uninitialize driver.", ParametersMap.create().add("name", name).add("type", driver.getFirst()), exception);
            }
        }
        return true;
    }

    public static void dumpConfigurationIfModified(final @NotNull DriverConfiguration<?, ?, ?> configuration) throws IOException {
        if (configuration.getCacheSide().resetModified())
            try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(DriverManager.getConfigurationFile(configuration.getName())))) {
                YamlHelper.dumpYaml(configuration.dump(), outputStream);
            }
    }

    public static Triad.@Nullable ImmutableTriad<@NotNull WebDriversType, @NotNull DriverInterface<?>, @Nullable DriverTrashInterface<?>> get(final @NotNull String name) {
        final Pair<WebDriversType, Pair.ImmutablePair<DriverInterface<?>, DriverTrashInterface<?>>> driver = DriverManager.drivers.get(name);
        if (driver == null) return null;
        return Triad.ImmutableTriad.makeImmutableTriad(driver.getFirst(), driver.getSecond().getFirst(), driver.getSecond().getSecond());
    }

    public static @Nullable DriverInterface<?> getDriver(final @NotNull String name) {
        final Pair<WebDriversType, Pair.ImmutablePair<DriverInterface<?>, DriverTrashInterface<?>>> driver = DriverManager.drivers.get(name);
        if (driver == null) return null;
        return driver.getSecond().getFirst();
    }

    public static @Nullable DriverTrashInterface<?> getTrash(final @NotNull String name) {
        final Pair<WebDriversType, Pair.ImmutablePair<DriverInterface<?>, DriverTrashInterface<?>>> driver = DriverManager.drivers.get(name);
        if (driver == null) return null;
        return driver.getSecond().getSecond();
    }

    @Deprecated
    public static @NotNull @UnmodifiableView Set<@NotNull DriverInterface<?>> getAllDrivers() {
        return DriverManager.drivers.values().stream().map(p -> {
            if (p.getSecond() == DriverManager.DriverPlaceholder)
                //noinspection ReturnOfNull
                return null;
            return p.getSecond().getFirst();
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Deprecated
    public static @NotNull @UnmodifiableView Set<@NotNull DriverTrashInterface<?>> getAllTrashes() {
        return DriverManager.drivers.values().stream().map(p -> {
            if (p.getSecond() == DriverManager.DriverPlaceholder)
                //noinspection ReturnOfNull
                return null;
            return p.getSecond().getSecond();
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }


    public static void addDriver(final @NotNull String name, final @NotNull WebDriversType type) throws IOException, IllegalParametersException {
        DriverManager.initializeDriver0(name, type);
        GlobalConfiguration.addUninitializedDriver(name, type);
    }

    public static void removeDriver(final @NotNull String name) throws IOException, IllegalParametersException {
        if (DriverManager.uninitializeDriver0(name))
            GlobalConfiguration.removeUninitializedDriver(name);
    }
}
