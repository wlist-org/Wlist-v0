package com.xuxiaocheng.WList.Server.Storage;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Commons.IdentifierNames;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Commons.Utils.YamlHelper;
import com.xuxiaocheng.WList.Server.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.RecyclerInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.SharerInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.Nls;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class StorageManager {
    private StorageManager() {
        super();
    }

    private static final @NotNull HLog logger = HLog.create("StorageLogger");
    private static final @NotNull HInitializer<File> ConfigurationsDirectory = new HInitializer<>("ProviderConfigurationsDirectory");
    private static final @NotNull HInitializer<File> CacheDirectory = new HInitializer<>("ProviderCacheDirectory");

    private static final @NotNull Path DirectoryRootPathCache = new File("").getAbsoluteFile().toPath();
    public static @Nullable @Nls String providerNameInvalidReason(final @NotNull String name) {
        if (AndroidSupporter.isBlank(name))
            return I18NUtil.get("server.provider.invalid_name.blank");
        if (!StorageManager.DirectoryRootPathCache.equals(new File(name).getAbsoluteFile().getParentFile().toPath()))
            return I18NUtil.get("server.provider.invalid_name.characters");
        if (IdentifierNames.RootSelector.equals(name))
            return I18NUtil.get("server.provider.invalid_name.selector");
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

    private static final @NotNull Map<@NotNull String, @NotNull Pair<@NotNull StorageTypes<?>, Triad.@NotNull ImmutableTriad<@NotNull ProviderInterface<?>, @NotNull RecyclerInterface<?>, @NotNull SharerInterface<?>>>> storages = new ConcurrentHashMap<>();
    private static final Triad.@NotNull ImmutableTriad<@NotNull ProviderInterface<?>, @NotNull RecyclerInterface<?>, @NotNull SharerInterface<?>> ProviderPlaceholder = new Triad.ImmutableTriad<>() {
        @Override
        public @NotNull ProviderInterface<?> getA() {
            throw new IllegalStateException("Unreachable!");
        }

        @Override
        public @Nullable RecyclerInterface<?> getB() {
            throw new IllegalStateException("Unreachable!");
        }

        @Override
        public @Nullable SharerInterface<?> getC() {
            throw new IllegalStateException("Unreachable!");
        }
    };
    private static final @NotNull Map<@NotNull String, @NotNull Exception> failedStorages = new ConcurrentHashMap<>();

    public static @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Exception> getFailedStoragesAPI() {
        return Collections.unmodifiableMap(StorageManager.failedStorages);
    }


    public static void initialize(final @NotNull File configurationsDirectory, final @NotNull File cacheDirectory) {
        StorageManager.ConfigurationsDirectory.initialize(configurationsDirectory.getAbsoluteFile());
        StorageManager.CacheDirectory.initialize(cacheDirectory.getAbsoluteFile());
        StorageManager.storages.clear();
        if (ServerConfiguration.get().providers().isEmpty()) {
            StorageManager.logger.log(HLogLevel.ENHANCED, "No storages were found!");
            return;
        }
        final Map<String, StorageTypes<?>> providers = ServerConfiguration.get().providers();
        final ZonedDateTime t1 = MiscellaneousUtil.now();
        try {
            HMultiRunHelper.runConsumers(WListServer.ServerExecutors, providers.entrySet(), e -> {
                try {
                    final File configurationFile = StorageManager.getStorageConfigurationFile(e.getKey());
                    final Map<String, Object> config;
                    try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(configurationFile))) {
                        config = YamlHelper.loadYaml(inputStream);
                    }
                    final List<Pair.ImmutablePair<String, String>> errors = StorageManager.initializeStorage0(e.getKey(), e.getValue(), config);
                    if (errors != null)
                        StorageManager.logger.log(HLogLevel.ENHANCED, "Errors while initializing storage.", ParametersMap.create()
                                .add("storage", e.getKey()).add("errors", errors));
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Exception exception) {
                    StorageManager.failedStorages.put(e.getKey(), exception);
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                }
            });
        } catch (final InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        final ZonedDateTime t2 = MiscellaneousUtil.now();
        StorageManager.logger.log(HLogLevel.ENHANCED, "Loaded ", StorageManager.storages.size(), " storages successfully. ",
                StorageManager.failedStorages.size(), " failed. Totally cost time: ", Duration.between(t1, t2).toMillis() + " ms.");
    }

    private static <C extends StorageConfiguration> @Nullable List<Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> initializeStorage0(final @NotNull String name, final @NotNull StorageTypes<C> type, final @NotNull @Unmodifiable Map<? super String, Object> config) throws IllegalParametersException, IOException {
        StorageManager.logger.log(HLogLevel.LESS, "Loading storage:", ParametersMap.create().add("name", name).add("type", type));
        StorageManager.failedStorages.remove(name);
        final Pair<StorageTypes<?>, Triad.ImmutableTriad<ProviderInterface<?>, RecyclerInterface<?>, SharerInterface<?>>> triad = Pair.makePair(type, StorageManager.ProviderPlaceholder);
        if (StorageManager.storages.putIfAbsent(name, triad) != null)
            throw new IllegalParametersException("Conflict storage name.", ParametersMap.create().add("name", name).add("type", type));
        try {
            final C configuration = type.getConfiguration().get();
            final ProviderInterface<C> provider = type.getProvider().get();
            final RecyclerInterface<C> recycler = type.getRecycler().get();
            final SharerInterface<C> sharer = type.getSharer().get();
            final List<Pair.ImmutablePair<String, String>> errors = new LinkedList<>();
            configuration.load(config, errors);
            if (!errors.isEmpty())
                return errors;
            configuration.setName(name);
            try {
                provider.initialize(configuration);
                recycler.initialize(configuration);
                sharer.initialize(configuration);
            } catch (final Exception exception) {
                throw new RuntimeException("Failed to initialize storage." + ParametersMap.create().add("name", name).add("type", type).add("configuration", configuration), exception);
            }
            StorageManager.dumpConfigurationIfModified(configuration);
            triad.setSecond(Triad.ImmutableTriad.makeImmutableTriad(provider, recycler, sharer));
        } finally {
            if (triad.getSecond() == StorageManager.ProviderPlaceholder)
                StorageManager.storages.remove(name);
        }
        StorageManager.logger.log(HLogLevel.INFO, "Load storage successfully:", ParametersMap.create().add("name", name));
        return null;
    }

    private static boolean uninitializeStorage0(final @NotNull String name, final boolean dropIndex) {
        StorageManager.logger.log(HLogLevel.LESS, "Unloading storage.", ParametersMap.create().add("name", name));
        try {
            StorageManager.failedStorages.remove(name);
            final var triad = StorageManager.storages.remove(name);
            if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return false;
            StorageManager.dumpConfigurationIfModified(triad.getSecond().getA().getConfiguration());
            final boolean drop = dropIndex && ServerConfiguration.get().allowDropIndexAfterUninitializeProvider();
            try {
                triad.getSecond().getA().uninitialize(drop);
                triad.getSecond().getB().uninitialize(drop);
                triad.getSecond().getC().uninitialize(drop);
            } catch (final Exception exception) {
                throw new RuntimeException("Failed to uninitialize storage." + ParametersMap.create().add("name", name).add("type", triad.getFirst()), exception);
            }
            StorageManager.logger.log(HLogLevel.INFO, "Unload storage successfully:", ParametersMap.create().add("name", name));
            return true;
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable throwable) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
        }
        return false;
    }

    public static @Nullable List<Pair.@NotNull ImmutablePair<@NotNull String, @NotNull String>> addStorage(final @NotNull String name, final @NotNull StorageTypes<?> type, final @Nullable Map<String, Object> config) throws IOException, IllegalParametersException {
        final Map<String, Object> configuration;
        if (config == null)
            try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(StorageManager.getStorageConfigurationFile(name)))) {
                configuration = YamlHelper.loadYaml(inputStream);
            }
        else configuration = config;
        final List<Pair.ImmutablePair<String, String>> error = StorageManager.initializeStorage0(name, type, configuration);
        if (error != null)
            return error;
        ServerConfiguration.get().providers().put(name, type);
        ServerConfiguration.dumpToFile();
        return null;
    }

    public static boolean removeStorage(final @NotNull String name, final boolean dropIndex) throws IOException {
        final boolean success = StorageManager.uninitializeStorage0(name, dropIndex);
        if (success) {
            ServerConfiguration.get().providers().remove(name);
            ServerConfiguration.dumpToFile();
        }
        return success;
    }

    public static void dumpConfigurationIfModified(final @NotNull StorageConfiguration configuration) throws IOException {
        synchronized (configuration) {
            if (configuration.resetModified()) {
                final Map<String, Object> config = configuration.dump();
                HFileHelper.writeFileAtomically(StorageManager.getStorageConfigurationFile(configuration.getName()), stream -> YamlHelper.dumpYaml(config, stream));
            }
        }
    }


    public static @Nullable ProviderInterface<?> getProvider(final @NotNull String name) {
        final var triad = StorageManager.storages.get(name);
        if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return null;
        return triad.getSecond().getA();
    }

    public static @Nullable RecyclerInterface<?> getRecycler(final @NotNull String name) {
        final var triad = StorageManager.storages.get(name);
        if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return null;
        return triad.getSecond().getB();
    }

    public static @Nullable SharerInterface<?> getSharer(final @NotNull String name) {
        final var triad = StorageManager.storages.get(name);
        if (triad == null || triad.getSecond() == StorageManager.ProviderPlaceholder) return null;
        return triad.getSecond().getC();
    }

    public static int getProvidersCount() {
        return StorageManager.storages.size();
    }

    public static @NotNull @Unmodifiable Set<@NotNull StorageConfiguration> getAllConfigurations() {
        return StorageManager.storages.values().stream().filter(e -> e.getSecond() != StorageManager.ProviderPlaceholder)
                .map(p -> p.getSecond().getA().getConfiguration()).collect(Collectors.toSet());
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull ProviderInterface<?>> getAllProviders() {
        return StorageManager.storages.entrySet().stream().filter(e -> e.getValue().getSecond() != StorageManager.ProviderPlaceholder)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSecond().getA()));
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull RecyclerInterface<?>> getAllRecyclers() {
        return StorageManager.storages.entrySet().stream().filter(e -> e.getValue().getSecond() != StorageManager.ProviderPlaceholder)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSecond().getB()));
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull SharerInterface<?>> getAllSharers() {
        return StorageManager.storages.entrySet().stream().filter(e -> e.getValue().getSecond() != StorageManager.ProviderPlaceholder)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSecond().getC()));
    }
}
