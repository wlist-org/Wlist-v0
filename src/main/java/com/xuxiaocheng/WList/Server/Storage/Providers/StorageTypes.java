package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouRecycler;
import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouSharer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record StorageTypes<C extends StorageConfiguration>(@NotNull String identifier,
                                                           @NotNull Supplier<C> configuration,
                                                           @NotNull Supplier<ProviderInterface<C>> provider,
                                                           @NotNull Supplier<RecyclerInterface<C>> recycler,
                                                           @NotNull Supplier<SharerInterface<C>> sharer) {
    private static final @NotNull Map<@NotNull String, @NotNull StorageTypes<?>> providers = new HashMap<>();

    public static final @NotNull StorageTypes<LanzouConfiguration> Lanzou = new StorageTypes<>("lanzou", LanzouConfiguration::new, LanzouProvider::new, LanzouRecycler::new, LanzouSharer::new);


    public static @Nullable StorageTypes<?> get(final @Nullable String identifier) {
        return StorageTypes.providers.get(identifier);
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull StorageTypes<?>> getAll() {
        return Collections.unmodifiableMap(StorageTypes.providers);
    }

    public StorageTypes(final @NotNull String identifier, final @NotNull Supplier<@NotNull C> configuration, final @NotNull Supplier<@NotNull ProviderInterface<C>> provider, final @NotNull Supplier<@NotNull RecyclerInterface<C>> recycler, final @NotNull Supplier<@NotNull SharerInterface<C>> sharer) {
        this.identifier = identifier;
        this.configuration = configuration;
        this.provider = provider;
        this.recycler = recycler;
        this.sharer = sharer;
        StorageTypes.providers.put(this.identifier, this);
    }

    @Override
    public @NotNull String toString() {
        return "StorageTypes{" +
                "identifier='" + this.identifier + '\'' +
                '}';
    }
}
