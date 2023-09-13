package com.xuxiaocheng.WList.Server.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMiscellaneousHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.WebProviders.ProviderConfiguration;
import com.xuxiaocheng.WList.Server.Storage.WebProviders.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.WebProviders.ProviderRecyclerInterface;
import com.xuxiaocheng.WList.Server.Storage.WebProviders.WebProviderType;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ProviderManager {
    private ProviderManager() {
        super();
    }

    private static final @NotNull HLog logger = HLog.create("DriverLogger");
    private static final @NotNull HInitializer<File> ConfigurationsDirectory = new HInitializer<>("ProviderConfigurationsDirectory");
    private static final @NotNull HInitializer<File> CacheDirectory = new HInitializer<>("ProviderCacheDirectory");

    public static @Nullable String providerNameInvalidReason(final @NotNull String name) {
        if (name.isBlank())
            return "Blank name";
        if (!Path.of(name).toAbsolutePath().getParent().equals(Path.of(".").toAbsolutePath()))
            return "Contain special characters.";
        if (IdentifierNames.SelectorProviderName.contains(name))
            return "Conflict with internal selector provider name.";
        return null;
    }

    private static @NotNull File getProviderConfigurationFile(final @NotNull String name) throws IOException {
        assert ProviderManager.providerNameInvalidReason(name) == null;
        final File file = new File(ProviderManager.ConfigurationsDirectory.getInstance(), name + ".yaml");
        HFileHelper.ensureFileAccessible(file, true);
        return file;
    }
    public static @NotNull File getProviderDatabaseFile(final @NotNull String name) {
        assert ProviderManager.providerNameInvalidReason(name) == null;
        return new File(ProviderManager.CacheDirectory.getInstance(), name + ".db");
    }

    private static final @NotNull Map<@NotNull String, @NotNull Pair<@NotNull WebProviderType, Pair.@NotNull ImmutablePair<@NotNull ProviderInterface<?>, @NotNull ProviderRecyclerInterface<?>>>> providers = new ConcurrentHashMap<>();
    private static final Pair.@NotNull ImmutablePair<@NotNull ProviderInterface<?>, @NotNull ProviderRecyclerInterface<?>> ProviderPlaceholder = new Pair.ImmutablePair<>() {
        @Override
        public @NotNull ProviderInterface<?> getFirst() {
            throw new IllegalStateException("Unreachable!");
        }

        @Override
        public @Nullable ProviderRecyclerInterface<?> getSecond() {
            throw new IllegalStateException("Unreachable!");
        }
    };
    private static final @NotNull Map<@NotNull String, @NotNull Exception> failedProviders = new ConcurrentHashMap<>();

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Exception> getFailedProvidersAPI() {
        return Collections.unmodifiableMap(ProviderManager.failedProviders);
    }

    public static void initialize(final @NotNull File configurationsDirectory, final @NotNull File cacheDirectory) {
        ProviderManager.ConfigurationsDirectory.initialize(configurationsDirectory.getAbsoluteFile());
        ProviderManager.CacheDirectory.initialize(cacheDirectory.getAbsoluteFile());
        ProviderManager.providers.clear();
        if (ServerConfiguration.get().providers().isEmpty()) {
            ProviderManager.logger.log(HLogLevel.ENHANCED, "No providers were found!");
            return;
        }
        final Runnable[] tasks = new Runnable[ServerConfiguration.get().providers().size()];
        int i = 0;
        for (final Map.Entry<String, WebProviderType> entry: ServerConfiguration.get().providers().entrySet())
            tasks[i++] = () -> ProviderManager.initializeProvider0(entry.getKey(), entry.getValue());
        final LocalDateTime t1 = LocalDateTime.now();
        try {
            HMiscellaneousHelper.runMultiTasks(WListServer.ServerExecutors, tasks);
        } catch (final InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        final LocalDateTime t2 = LocalDateTime.now();
        ProviderManager.logger.log(HLogLevel.ENHANCED, "Loaded ", ProviderManager.providers.size(), " provider", ProviderManager.providers.size() > 1 ? "s" : "", " successfully. ",
                ProviderManager.failedProviders.size(), " failed. Totally cost time: ", Duration.between(t1, t2).toMillis() + " ms.");
    }

    @SuppressWarnings("unchecked")
    private static <C extends ProviderConfiguration<?, ?, ?>> boolean initializeProvider0(final @NotNull String name, final @NotNull WebProviderType type) {
        ProviderManager.logger.log(HLogLevel.INFO, "Loading provider:", ParametersMap.create().add("name", name).add("type", type));
        ProviderManager.failedProviders.remove(name);
        try {
            final Pair<WebProviderType, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = Pair.makePair(type, ProviderManager.ProviderPlaceholder);
            if (ProviderManager.providers.putIfAbsent(name, triad) != null)
                throw new IllegalParametersException("Conflict driver name.", ParametersMap.create().add("name", name).add("type", type));
            try {
                final File configurationFile = ProviderManager.getProviderConfigurationFile(name);
                final Supplier<ProviderRecyclerInterface<?>> trashSupplier = type.getTrash();
                final ProviderRecyclerInterface<ProviderInterface<C>> trash = trashSupplier == null ? null : (ProviderRecyclerInterface<ProviderInterface<C>>) trashSupplier.get();
                final ProviderInterface<C> driver = trash == null ? (ProviderInterface<C>) type.getDriver().get() : trash.getDriver();
                final Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>> driverPair = Pair.ImmutablePair.makeImmutablePair(driver, trash);
                final C configuration = driver.getConfiguration();
                final Map<String, Object> config;
                try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationFile))) {
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
                    ProviderManager.uninitializeProvider0(name, true);
                    throw new IllegalParametersException("Failed to initialize.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
                }
                try {
                    final LocalDateTime old = configuration.getCacheSide().getLastFileCacheBuildTime();
                    if (old == null || Duration.between(old, LocalDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3)) {
                        driver.buildCache();
                        if (trash != null)
                            trash.buildCache();
                    }
                } catch (final Exception exception) {
                    throw new IllegalParametersException("Failed to build cache.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
                } finally {
                    ProviderManager.dumpConfigurationIfModified(configuration);
                }
                triad.setSecond(driverPair);
            } finally {
                if (triad.getSecond() == ProviderManager.ProviderPlaceholder)
                    ProviderManager.providers.remove(name);
            }
            ProviderManager.logger.log(HLogLevel.VERBOSE, "Load provider successfully:", ParametersMap.create().add("name", name));
            return true;
        } catch (final IllegalParametersException | IOException | RuntimeException exception) {
            ProviderManager.logger.log(HLogLevel.ERROR, "Failed to load provider.", ParametersMap.create().add("name", name), exception);
            ProviderManager.failedProviders.put(name, exception);
        } catch (final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    private static boolean uninitializeProvider0(final @NotNull String name, final boolean canDelete) {
        ProviderManager.logger.log(HLogLevel.INFO, "Unloading provider.", ParametersMap.create().add("name", name));
        try {
            ProviderManager.failedProviders.remove(name);
            final Pair<@NotNull WebProviderType, Pair.@NotNull ImmutablePair<@NotNull ProviderInterface<?>, @Nullable ProviderRecyclerInterface<?>>> driver = ProviderManager.providers.remove(name);
            if (driver == null || driver.getSecond() == ProviderManager.ProviderPlaceholder) return false;
            if (canDelete && ServerConfiguration.get().deleteCacheAfterUninitializeProvider())
                try {
                    driver.getSecond().getFirst().uninitialize();
                    if (driver.getSecond().getSecond() != null)
                        driver.getSecond().getSecond().uninitialize();
                } catch (final Exception exception) {
                    throw new IllegalParametersException("Failed to uninitialize driver.", ParametersMap.create().add("name", name).add("type", driver.getFirst()), exception);
                } finally {
                    ProviderManager.dumpConfigurationIfModified(driver.getSecond().getFirst().getConfiguration());
                }
            return true;
        } catch (final IllegalParametersException | RuntimeException exception) {
            ProviderManager.logger.log(HLogLevel.ERROR, "Failed to unload provider.", ParametersMap.create().add("name", name), exception);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    public static void dumpConfigurationIfModified(final @NotNull ProviderConfiguration<?, ?, ?> configuration) throws IOException {
        synchronized (configuration) {
            if (configuration.getCacheSide().resetModified()) {
                final File file = ProviderManager.getProviderConfigurationFile(configuration.getName());
                final File tmp = new File(file.getAbsolutePath() + ".tmp");
                HFileHelper.ensureFileAccessible(tmp, true);
                try {
                    try (final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                        YamlHelper.dumpYaml(configuration.dump(), outputStream);
                    }
                    try {
                        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (final IOException ignore) {
                        Files.deleteIfExists(file.toPath());
                        Files.move(tmp.toPath(), file.toPath());
                    }
                } finally {
                    Files.deleteIfExists(tmp.toPath());
                }
            }
        }
    }

    public static boolean addProvider(final @NotNull String name, final @NotNull WebProviderType type) throws IOException {
        if (ProviderManager.initializeProvider0(name, type)) {
            ServerConfiguration.get().providers().put(name, type);
            ServerConfiguration.dumpToFile();
            return true;
        }
        return false;
    }

    public static void removeProvider(final @NotNull String name) throws IOException {
        if (ProviderManager.uninitializeProvider0(name, true)) {
            ServerConfiguration.get().providers().remove(name);
            ServerConfiguration.dumpToFile();
        }
    }

    public static @Nullable ProviderInterface<?> getProvider(final @NotNull String name) {
        final Pair<WebProviderType, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = ProviderManager.providers.get(name);
        if (triad == null || triad.getSecond() == ProviderManager.ProviderPlaceholder) return null;
        return triad.getSecond().getFirst();
    }

    public static @Nullable ProviderRecyclerInterface<?> getRecycler(final @NotNull String name) {
        final Pair<WebProviderType, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = ProviderManager.providers.get(name);
        if (triad == null || triad.getSecond() == ProviderManager.ProviderPlaceholder) return null;
        return triad.getSecond().getSecond();
    }

    public static int getProvidersCount() {
        return ProviderManager.providers.size();
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull ProviderInterface<?>> getAllProviders() {
        return ProviderManager.providers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSecond().getFirst()));
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull ProviderRecyclerInterface<?>> getAllRecyclers() {
        return ProviderManager.providers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSecond().getSecond()));
    }
}
