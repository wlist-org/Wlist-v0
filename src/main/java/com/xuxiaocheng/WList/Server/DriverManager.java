package com.xuxiaocheng.WList.Server;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStream;
import com.xuxiaocheng.WList.Driver.DriverConfiguration;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.Driver.SpecialDriverName;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Utils.YamlHelper;
import com.xuxiaocheng.WList.WebDrivers.WebDriversType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class DriverManager {
    private DriverManager() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("DriverLogger", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, false, true, HMergedStream.getFileOutputStreamNoException(null));
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
    private static final @NotNull Map<@NotNull String, @NotNull Exception> failedDrivers = new ConcurrentHashMap<>();

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Exception> getFailedDriversAPI() {
        return Collections.unmodifiableMap(DriverManager.failedDrivers);
    }

    public static void initialize(final @NotNull File configurationsPath) {
        final LocalDateTime t1 = LocalDateTime.now();
        DriverManager.configurationsPath.initialize(configurationsPath.getAbsoluteFile());
        DriverManager.drivers.clear();
        final CompletableFuture<?>[] futures = new CompletableFuture[GlobalConfiguration.getInstance().drivers().size()];
        int i = 0;
        for (final Map.Entry<String, WebDriversType> entry: GlobalConfiguration.getInstance().drivers().entrySet())
            futures[i++] = CompletableFuture.runAsync(() -> DriverManager.initializeDriver0(entry.getKey(), entry.getValue()), WListServer.ServerExecutors);
        CompletableFuture.allOf(futures).join();
        final LocalDateTime t2 = LocalDateTime.now();
        DriverManager.logger.log(HLogLevel.ENHANCED, "Loaded ", DriverManager.drivers.size(), " driver", DriverManager.drivers.size() > 1 ? "s" : "", " successfully. ", DriverManager.failedDrivers.size(), " failed.", ParametersMap.create().add("cost", (Supplier<String>) () -> Duration.between(t1, t2).toMillis() + " ms"));
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
    private static <C extends DriverConfiguration<?, ?, ?>> boolean initializeDriver0(final @NotNull String name, final @NotNull WebDriversType type) {
        DriverManager.logger.log(HLogLevel.INFO, "Loading driver.", ParametersMap.create().add("name", name).add("type", type));
        try {
            for (final SpecialDriverName specialDriverName: SpecialDriverName.values())
                if (specialDriverName.getIdentifier().equals(name))
                    throw new IllegalParametersException("Invalid name.", ParametersMap.create().add("name", name).add("type", type).add("specialDriverName", specialDriverName));
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
                } catch (final FileNotFoundException exception) {
                    throw new RuntimeException("Unreachable!", exception);
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
                    DriverManager.uninitializeDriver0(name, true);
                    throw new IllegalParametersException("Failed to initialize.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
                }
                try {
                    final LocalDateTime old = configuration.getCacheSide().getLastFileCacheBuildTime();
                    final LocalDateTime now = LocalDateTime.now();
                    if (old == null || Duration.between(old, now).toMillis() > TimeUnit.HOURS.toMillis(3)) {
                        configuration.getCacheSide().setLastFileCacheBuildTime(now);
                        driver.buildCache();
                        if (trash != null)
                            trash.buildCache();
                    }
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
            DriverManager.logger.log(HLogLevel.VERBOSE, "Load driver successfully.", ParametersMap.create().add("name", name));
            return true;
        } catch (final IllegalParametersException | IOException | RuntimeException exception) {
            DriverManager.logger.log(HLogLevel.ERROR, "Failed to load driver.", ParametersMap.create().add("name", name), exception);
            DriverManager.failedDrivers.put(name, exception);
        } catch (final Throwable throwable) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    private static boolean uninitializeDriver0(final @NotNull String name, final boolean canDelete) {
        DriverManager.logger.log(HLogLevel.INFO, "Unloading driver.", ParametersMap.create().add("name", name));
        try {
            DriverManager.failedDrivers.remove(name);
            final Pair<@NotNull WebDriversType, Pair.@NotNull ImmutablePair<@NotNull DriverInterface<?>, @Nullable DriverTrashInterface<?>>> driver = DriverManager.drivers.remove(name);
            if (driver == null || driver.getSecond() == DriverManager.DriverPlaceholder) return false;
            if (canDelete && GlobalConfiguration.getInstance().deleteDriver())
                try {
                    driver.getSecond().getFirst().uninitialize();
                    if (driver.getSecond().getSecond() != null)
                        driver.getSecond().getSecond().uninitialize();
                } catch (final Exception exception) {
                    throw new IllegalParametersException("Failed to uninitialize driver.", ParametersMap.create().add("name", name).add("type", driver.getFirst()), exception);
                }
            return true;
        } catch (final IllegalParametersException | RuntimeException exception) {
            DriverManager.logger.log(HLogLevel.ERROR, "Failed to unload driver.", ParametersMap.create().add("name", name), exception);
        } catch (final Throwable throwable) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    public static void dumpConfigurationIfModified(final @NotNull DriverConfiguration<?, ?, ?> configuration) throws IOException {
        if (configuration.getCacheSide().resetModified())
            try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(DriverManager.getConfigurationFile(configuration.getName())))) {
                YamlHelper.dumpYaml(configuration.dump(), outputStream);
            }
    }

    public static boolean addDriver(final @NotNull String name, final @NotNull WebDriversType type) throws IOException {
        if (DriverManager.initializeDriver0(name, type)) {
            DriverManager.failedDrivers.remove(name);
            GlobalConfiguration.addUninitializedDriver(name, type);
            return true;
        }
        return false;
    }

    public static void removeDriver(final @NotNull String name) throws IOException {
        if (DriverManager.uninitializeDriver0(name, true))
            GlobalConfiguration.removeUninitializedDriver(name);
    }

    public static boolean reAddDriver(final @NotNull String name, final @NotNull WebDriversType type) throws IOException {
        DriverManager.uninitializeDriver0(name, false);
        GlobalConfiguration.getInstance().drivers().remove(name);
        return DriverManager.addDriver(name, type);
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

    public static @NotNull Map<@NotNull String, @NotNull Exception> operateAllDrivers(final @NotNull ConsumerE<? super @NotNull DriverInterface<?>> consumer) {
        final Map<String, Exception> exceptions = new ConcurrentHashMap<>();
        final Collection<CompletableFuture<?>> futures = new LinkedList<>();
        for (final Map.Entry<String, Pair<WebDriversType, Pair.ImmutablePair<DriverInterface<?>, DriverTrashInterface<?>>>> driver: DriverManager.drivers.entrySet()) {
            if (driver.getValue().getSecond() == DriverManager.DriverPlaceholder) continue;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    consumer.accept(driver.getValue().getSecond().getFirst());
                } catch (final Exception exception) {
                    exceptions.put(driver.getKey(), exception);
                }
            }));
        }
        for (final CompletableFuture<?> future: futures)
            future.join();
        return exceptions;
    }

    public static @NotNull Map<@NotNull String, @NotNull Exception> operateAllTrashes(final @NotNull ConsumerE<? super @NotNull DriverTrashInterface<?>> consumer) {
        final Map<String, Exception> exceptions = new ConcurrentHashMap<>();
        final Collection<CompletableFuture<?>> futures = new LinkedList<>();
        for (final Map.Entry<String, Pair<WebDriversType, Pair.ImmutablePair<DriverInterface<?>, DriverTrashInterface<?>>>> driver: DriverManager.drivers.entrySet()) {
            if (driver.getValue().getSecond() == DriverManager.DriverPlaceholder) continue;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    consumer.accept(driver.getValue().getSecond().getSecond());
                } catch (final Exception exception) {
                    exceptions.put(driver.getKey(), exception);
                }
            }));
        }
        for (final CompletableFuture<?> future: futures)
            future.join();
        return exceptions;
    }
}
