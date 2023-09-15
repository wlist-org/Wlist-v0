package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.WList.Server.Storage.Providers.WebProviders.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.WebProviders.Lanzou.LanzouProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.WebProviders.LanzouRecycler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ProviderTypes<C extends ProviderConfiguration> {
    private static final @NotNull Map<@NotNull String, @NotNull ProviderTypes<?>> providers = new HashMap<>();

    public static final @NotNull ProviderTypes<LanzouConfiguration> Lanzou = new ProviderTypes<>("lanzou", LanzouConfiguration::new, LanzouProvider::new, LanzouRecycler::new);


    public static @Nullable ProviderTypes<?> get(final @Nullable String identifier) {
        return ProviderTypes.providers.get(identifier);
    }

    private final @NotNull String identifier;
    private final @NotNull Supplier<@NotNull C> configuration;
    private final @NotNull Supplier<@NotNull ProviderInterface<C>> provider;
    private final @NotNull Supplier<@NotNull ProviderRecyclerInterface<C>> recycler;

    private ProviderTypes(final @NotNull String identifier, final @NotNull Supplier<@NotNull C> configuration, final @NotNull Supplier<@NotNull ProviderInterface<C>> provider, final @NotNull Supplier<@NotNull ProviderRecyclerInterface<C>> recycler) {
        super();
        this.identifier = identifier;
        this.configuration = configuration;
        this.provider = provider;
        this.recycler = recycler;
        ProviderTypes.providers.put(this.identifier, this);
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

    public @NotNull Supplier<@NotNull ProviderRecyclerInterface<C>> getRecycler() {
        return this.recycler;
    }

    @Override
    public @NotNull String toString() {
        return "ProviderTypes{" +
                "identifier='" + this.identifier + '\'' +
                '}';
    }
}
