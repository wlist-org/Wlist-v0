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
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
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

    private static final @NotNull Path DirectoryRootPathCache = Path.of("").toAbsolutePath();
    public static @Nullable String providerNameInvalidReason(final @NotNull String name) {
        if (name.isBlank())
            return "Blank name";
        if (!StorageManager.DirectoryRootPathCache.equals(Path.of(name).toAbsolutePath().getParent()))
            return "Contain special characters.";
        if (IdentifierNames.SelectorProviderName.contains(name))
            return "Conflict with internal selector provider name.";
        return null;
    }

    public static @NotNull File getStorageConfigurationFile(final @NotNull String name) throws IOException {
        assert StorageManager.providerNameInvalidReason(name) == null;
        final File file = new File(StorageManager.ConfigurationsDirectory.getInstance(), name + ".yaml");
        HFileHelper.ensureFileAccessible(file, true);
        return file;
    }
    public static @NotNull File getStorageDatabaseFile(final @NotNull String name) {
        assert StorageManager.providerNameInvalidReason(name) == null;
        return new File(StorageManager.CacheDirectory.getInstance(), name + ".db");
    }

    private static final @NotNull Map<@NotNull String, @NotNull Pair<@NotNull ProviderTypes<?>, Pair.@NotNull ImmutablePair<@NotNull ProviderInterface<?>, @NotNull ProviderRecyclerInterface<?>>>> providers = new ConcurrentHashMap<>();
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
        final Map<String, ProviderTypes<?>> providers = ServerConfiguration.get().providers();
        final ZonedDateTime t1 = MiscellaneousUtil.now();
        try {
            HMultiRunHelper.runConsumers(WListServer.ServerExecutors, providers.size(), providers.entrySet().iterator(), e -> {
                try {
                    final File configurationFile = StorageManager.getStorageConfigurationFile(e.getKey());
                    final Map<String, Object> config;
                    try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationFile))) {
                        config = YamlHelper.loadYaml(inputStream);
                    }
                    StorageManager.initializeProvider0(e.getKey(), e.getValue(), config);
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Exception exception) {
                    StorageManager.failedProviders.put(e.getKey(), exception);
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                }
            });
        } catch (final InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        final ZonedDateTime t2 = MiscellaneousUtil.now();
        StorageManager.logger.log(HLogLevel.ENHANCED, "Loaded ", StorageManager.providers.size(), " providers successfully. ",
                StorageManager.failedProviders.size(), " failed. Totally cost time: ", Duration.between(t1, t2).toMillis() + " ms.");
    }

    private static <C extends ProviderConfiguration> void initializeProvider0(final @NotNull String name, final @NotNull ProviderTypes<C> type, final @NotNull @Unmodifiable Map<? super String, Object> config) throws IllegalParametersException, IOException {
        StorageManager.logger.log(HLogLevel.INFO, "Loading provider:", ParametersMap.create().add("name", name).add("type", type));
        StorageManager.failedProviders.remove(name);
        final Pair<ProviderTypes<?>, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = Pair.makePair(type, StorageManager.ProviderPlaceholder);
        if (StorageManager.providers.putIfAbsent(name, triad) != null)
            throw new IllegalParametersException("Conflict provider name.", ParametersMap.create().add("name", name).add("type", type));
        try {
            final C configuration = type.getConfiguration().get();
            final ProviderInterface<C> provider = type.getProvider().get();
            final ProviderRecyclerInterface<C> recycler = type.getRecycler().get();
            final Collection<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
            configuration.load(config, errors);
            YamlHelper.throwErrors(errors);
            configuration.setName(name);
            try {
                provider.initialize(configuration);
                recycler.initialize(configuration);
            } catch (final Exception exception) {
                throw new RuntimeException("Failed to initialize provider." + ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
            }
            StorageManager.dumpConfigurationIfModified(configuration);
            triad.setSecond(Pair.ImmutablePair.makeImmutablePair(provider, recycler));
        } finally {
            if (triad.getSecond() == StorageManager.ProviderPlaceholder)
                StorageManager.providers.remove(name);
        }
        StorageManager.logger.log(HLogLevel.VERBOSE, "Load provider successfully:", ParametersMap.create().add("name", name));
    }

    private static boolean uninitializeProvider0(final @NotNull String name, final boolean dropIndex) {
        StorageManager.logger.log(HLogLevel.INFO, "Unloading provider.", ParametersMap.create().add("name", name));
        try {
            StorageManager.failedProviders.remove(name);
            final Pair<ProviderTypes<?>, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = StorageManager.providers.remove(name);
            if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return false;
            StorageManager.dumpConfigurationIfModified(triad.getSecond().getFirst().getConfiguration());
            final boolean drop = dropIndex && ServerConfiguration.get().allowDropIndexAfterUninitializeProvider();
            try {
                triad.getSecond().getFirst().uninitialize(drop);
                triad.getSecond().getSecond().uninitialize(drop);
            } catch (final Exception exception) {
                throw new RuntimeException("Failed to uninitialize provider." + ParametersMap.create().add("name", name).add("type", triad.getFirst()), exception);
            }
            StorageManager.logger.log(HLogLevel.VERBOSE, "Unload provider successfully:", ParametersMap.create().add("name", name));
            return true;
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    public static void addProvider(final @NotNull String name, final @NotNull ProviderTypes<?> type, final @Nullable Map<String, Object> config) throws IOException, IllegalParametersException {
        final Map<String, Object> configuration;
        if (config == null)
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(StorageManager.getStorageConfigurationFile(name)))) {
                configuration = YamlHelper.loadYaml(inputStream);
            }
        else configuration = config;
        StorageManager.initializeProvider0(name, type, configuration);
        ServerConfiguration.get().providers().put(name, type);
        ServerConfiguration.dumpToFile();
    }

    public static boolean removeProvider(final @NotNull String name, final boolean dropIndex) throws IOException {
        final boolean success = StorageManager.uninitializeProvider0(name, dropIndex);
        if (success) {
            ServerConfiguration.get().providers().remove(name);
            ServerConfiguration.dumpToFile();
        }
        return success;
    }

    public static void dumpConfigurationIfModified(final @NotNull ProviderConfiguration configuration) throws IOException {
        synchronized (configuration) {
            if (configuration.resetModified()) {
                final Map<String, Object> config = configuration.dump();
                HFileHelper.writeFileAtomically(StorageManager.getStorageConfigurationFile(configuration.getName()), stream -> YamlHelper.dumpYaml(config, stream));
            }
        }
    }


    public static @Nullable ProviderInterface<?> getProvider(final @NotNull String name) {
        final Pair<ProviderTypes<?>, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = StorageManager.providers.get(name);
        if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return null;
        return triad.getSecond().getFirst();
    }

    public static @Nullable ProviderRecyclerInterface<?> getRecycler(final @NotNull String name) {
        final Pair<ProviderTypes<?>, Pair.ImmutablePair<ProviderInterface<?>, ProviderRecyclerInterface<?>>> triad = StorageManager.providers.get(name);
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
