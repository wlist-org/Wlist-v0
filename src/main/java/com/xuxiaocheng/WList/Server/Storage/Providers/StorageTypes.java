package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouRecycler;
import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouSharer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class StorageTypes<C extends StorageConfiguration> {
    private static final @NotNull Map<@NotNull String, @NotNull StorageTypes<?>> providers = new HashMap<>();

    public static final @NotNull StorageTypes<LanzouConfiguration> Lanzou = new StorageTypes<>("lanzou", LanzouConfiguration::new, LanzouProvider::new, LanzouRecycler::new, LanzouSharer::new);


    public static @Nullable StorageTypes<?> get(final @Nullable String identifier) {
        return StorageTypes.providers.get(identifier);
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull StorageTypes<?>> getAll() {
        return Collections.unmodifiableMap(StorageTypes.providers);
    }

    private final @NotNull String identifier;
    private final @NotNull Supplier<@NotNull C> configuration;
    private final @NotNull Supplier<@NotNull ProviderInterface<C>> provider;
    private final @NotNull Supplier<@NotNull RecyclerInterface<C>> recycler;
    private final @NotNull Supplier<@NotNull SharerInterface<C>> sharer;

    private StorageTypes(final @NotNull String identifier, final @NotNull Supplier<@NotNull C> configuration, final @NotNull Supplier<@NotNull ProviderInterface<C>> provider, final @NotNull Supplier<@NotNull RecyclerInterface<C>> recycler, final @NotNull Supplier<@NotNull SharerInterface<C>> sharer) {
        super();
        this.identifier = identifier;
        this.configuration = configuration;
        this.provider = provider;
        this.recycler = recycler;
        this.sharer = sharer;
        StorageTypes.providers.put(this.identifier, this);
    }

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public @NotNull Supplier<@NotNull C> getConfiguration() {
        return this.configuration;
    }

    public @NotNull Supplier<@NotNull ProviderInterface<C>> getProvider() {
        return this.provider;
    }

    public @NotNull Supplier<@NotNull RecyclerInterface<C>> getRecycler() {
        return this.recycler;
    }

    public @NotNull Supplier<@NotNull SharerInterface<C>> getSharer() {
        return this.sharer;
    }

    @Override
    public @NotNull String toString() {
        return "StorageTypes{" +
                "identifier='" + this.identifier + '\'' +
                '}';
    }
}
