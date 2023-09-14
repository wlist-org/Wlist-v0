package com.xuxiaocheng.WList.Server.Storage;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderRecyclerInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class StorageManager {
    private StorageManager() {
        super();
    }

    private static final @NotNull HLog logger = HLog.create("StorageLogger");
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

    private static @NotNull File getStorageConfigurationFile(final @NotNull String name) throws IOException {
        assert StorageManager.providerNameInvalidReason(name) == null;
        final File file = new File(StorageManager.ConfigurationsDirectory.getInstance(), name + ".yaml");
        HFileHelper.ensureFileAccessible(file, true);
        return file;
    }
    public static @NotNull File getStorageDatabaseFile(final @NotNull String name) {
        assert StorageManager.providerNameInvalidReason(name) == null;
        return new File(StorageManager.CacheDirectory.getInstance(), name + ".db");
    }

    private static final @NotNull Map<@NotNull String, @NotNull Pair<@NotNull ProviderTypes, Pair.@NotNull ImmutablePair<@NotNull ProviderInterface<?>, @NotNull ProviderRecyclerInterface<?>>>> providers = new ConcurrentHashMap<>();
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
        return Collections.unmodifiableMap(StorageManager.failedProviders);
    }

    public static void initialize(final @NotNull File configurationsDirectory, final @NotNull File cacheDirectory) {
        StorageManager.ConfigurationsDirectory.initialize(configurationsDirectory.getAbsoluteFile());
        StorageManager.CacheDirectory.initialize(cacheDirectory.getAbsoluteFile());
        StorageManager.providers.clear();
        if (ServerConfiguration.get().providers().isEmpty()) {
            StorageManager.logger.log(HLogLevel.ENHANCED, "No providers were found!");
            return;
        }
        final Map<String, ProviderTypes> providers = ServerConfiguration.get().providers();
        final LocalDateTime t1 = LocalDateTime.now();
        try {
            HMultiRunHelper.runConsumers(WListServer.ServerExecutors, providers.size(), providers.entrySet().iterator(),
                    e -> StorageManager.initializeProvider0(e.getKey(), e.getValue()));
        } catch (final InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        final LocalDateTime t2 = LocalDateTime.now();
        StorageManager.logger.log(HLogLevel.ENHANCED, "Loaded ", StorageManager.providers.size(), " provider", StorageManager.providers.size() > 1 ? "s" : "", " successfully. ",
                StorageManager.failedProviders.size(), " failed. Totally cost time: ", Duration.between(t1, t2).toMillis() + " ms.");
    }

    @SuppressWarnings("unchecked")
    private static <C extends ProviderConfiguration> boolean initializeProvider0(final @NotNull String name, final @NotNull ProviderTypes type) {
        StorageManager.logger.log(HLogLevel.INFO, "Loading provider:", ParametersMap.create().add("name", name).add("type", type));
        StorageManager.failedProviders.remove(name);
        try {
            final Pair<ProviderTypes, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = Pair.makePair(type, StorageManager.ProviderPlaceholder);
            if (StorageManager.providers.putIfAbsent(name, triad) != null)
                throw new IllegalParametersException("Conflict driver name.", ParametersMap.create().add("name", name).add("type", type));
            try {
                final File configurationFile = StorageManager.getStorageConfigurationFile(name);
//                final Supplier<ProviderRecyclerInterface<?>> trashSupplier = type.getTrash();
//                final ProviderRecyclerInterface<ProviderInterface<C>> trash = trashSupplier == null ? null : (ProviderRecyclerInterface<ProviderInterface<C>>) trashSupplier.get();
//                final ProviderInterface<C> driver = trash == null ? (ProviderInterface<C>) type.getDriver().get() : trash.getDriver();
//                final Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>> driverPair = Pair.ImmutablePair.makeImmutablePair(driver, trash);
//                final C configuration = driver.getConfiguration();
//                final Map<String, Object> config;
//                try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationFile))) {
//                    config = YamlHelper.loadYaml(inputStream);
//                } catch (final FileNotFoundException exception) {
//                    throw new RuntimeException("Unreachable!", exception);
//                }
//                final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
//                configuration.load(config, errors);
//                YamlHelper.throwErrors(errors);
//                configuration.setName(name);
//                try {
//                    driver.initialize(configuration);
//                    if (trash != null)
//                        trash.initialize(driver);
//                } catch (final Exception exception) {
//                    StorageManager.uninitializeProvider0(name, true);
//                    throw new IllegalParametersException("Failed to initialize.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
//                }
//                try {
//                    final LocalDateTime old = configuration.getLastFileCacheBuildTime();
//                    if (old == null || Duration.between(old, LocalDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3)) {
//                        driver.buildCache();
//                        if (trash != null)
//                            trash.buildCache();
//                    }
//                } catch (final Exception exception) {
//                    throw new IllegalParametersException("Failed to build cache.", ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
//                } finally {
//                    StorageManager.dumpConfigurationIfModified(configuration);
//                }
//                triad.setSecond(driverPair);
            } finally {
                if (triad.getSecond() == StorageManager.ProviderPlaceholder)
                    StorageManager.providers.remove(name);
            }
            StorageManager.logger.log(HLogLevel.VERBOSE, "Load provider successfully:", ParametersMap.create().add("name", name));
            return true;
        } catch (final IllegalParametersException | IOException | RuntimeException exception) {
            StorageManager.logger.log(HLogLevel.ERROR, "Failed to load provider.", ParametersMap.create().add("name", name), exception);
            StorageManager.failedProviders.put(name, exception);
        } catch (final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    private static boolean uninitializeProvider0(final @NotNull String name, final boolean canDelete) {
        StorageManager.logger.log(HLogLevel.INFO, "Unloading provider.", ParametersMap.create().add("name", name));
        try {
            StorageManager.failedProviders.remove(name);
            final Pair<@NotNull ProviderTypes, Pair.@NotNull ImmutablePair<@NotNull ProviderInterface<?>, @Nullable ProviderRecyclerInterface<?>>> driver = StorageManager.providers.remove(name);
            if (driver == null || driver.getSecond() == StorageManager.ProviderPlaceholder) return false;
            if (canDelete && ServerConfiguration.get().deleteCacheAfterUninitializeProvider())
                try {
                    driver.getSecond().getFirst().uninitialize(canDelete);
                    if (driver.getSecond().getSecond() != null)
                        driver.getSecond().getSecond().uninitialize();
                } catch (final Exception exception) {
                    throw new IllegalParametersException("Failed to uninitialize driver.", ParametersMap.create().add("name", name).add("type", driver.getFirst()), exception);
                } finally {
                    StorageManager.dumpConfigurationIfModified(driver.getSecond().getFirst().getConfiguration());
                }
            return true;
        } catch (final IllegalParametersException | RuntimeException exception) {
            StorageManager.logger.log(HLogLevel.ERROR, "Failed to unload provider.", ParametersMap.create().add("name", name), exception);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    public static void dumpConfigurationIfModified(final @NotNull ProviderConfiguration configuration) throws IOException {
        synchronized (configuration) {
            if (configuration.resetModified()) {
                final File file = StorageManager.getStorageConfigurationFile(configuration.getName());
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

    public static boolean addProvider(final @NotNull String name, final @NotNull ProviderTypes type) throws IOException {
        if (StorageManager.initializeProvider0(name, type)) {
            ServerConfiguration.get().providers().put(name, type);
            ServerConfiguration.dumpToFile();
            return true;
        }
        return false;
    }

    public static void removeProvider(final @NotNull String name) throws IOException {
        if (StorageManager.uninitializeProvider0(name, true)) {
            ServerConfiguration.get().providers().remove(name);
            ServerConfiguration.dumpToFile();
        }
    }

    public static @Nullable ProviderInterface<?> getProvider(final @NotNull String name) {
        final Pair<ProviderTypes, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = StorageManager.providers.get(name);
        if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return null;
        return triad.getSecond().getFirst();
    }

    public static @Nullable ProviderRecyclerInterface<?> getRecycler(final @NotNull String name) {
        final Pair<ProviderTypes, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = StorageManager.providers.get(name);
        if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return null;
        return triad.getSecond().getSecond();
    }

    public static int getProvidersCount() {
        return StorageManager.providers.size();
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull ProviderInterface<?>> getAllProviders() {
        return StorageManager.providers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSecond().getFirst()));
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull ProviderRecyclerInterface<?>> getAllRecyclers() {
        return StorageManager.providers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSecond().getSecond()));
    }
}
