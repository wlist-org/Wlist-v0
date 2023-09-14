package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.WList.Server.Storage.Providers.WebProviders.Lanzou.LanzouProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum ProviderTypes {
    Lanzou("lanzou", LanzouProvider::new, null),
    ;
    private static final @NotNull Map<@NotNull String, @NotNull ProviderTypes> providers = new HashMap<>(); static {
        for (final ProviderTypes type: ProviderTypes.values())
            ProviderTypes.providers.put(type.identifier, type);
    }

    public static @Nullable ProviderTypes get(final @Nullable String identifier) {
        return ProviderTypes.providers.get(identifier);
    }

    private final @NotNull String identifier;
    private final @NotNull Supplier<@NotNull ProviderInterface<?>> provider;
    private final @NotNull Supplier<@NotNull ProviderRecyclerInterface<?>> recycler;

    ProviderTypes(final @NotNull String identifier, final @NotNull Supplier<@NotNull ProviderInterface<?>> provider, final @NotNull Supplier<@NotNull ProviderRecyclerInterface<?>> recycler) {
        this.identifier = identifier;
        this.provider = provider;
        this.recycler = recycler;
    }

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public @NotNull Supplier<@NotNull ProviderInterface<?>> getProvider() {
        return this.provider;
    }

    public @NotNull Supplier<@NotNull ProviderRecyclerInterface<?>> getRecycler() {
        return this.recycler;
    }

    @Override
    public @NotNull String toString() {
        return "ProviderTypes{" +
                "identifier='" + this.identifier + '\'' +
                ", name='" + this.name() + '\'' +
                '}';
    }
}
