package com.xuxiaocheng.WList.Server.Storage.WebProviders;

import com.xuxiaocheng.WList.Server.Storage.WebProviders.Driver_lanzou.Driver_lanzou;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum WebProviderType {
//    driver_123Pan("123pan", Driver_123pan::new, Trash_123pan::new),
    driver_lanzou("lanzou", Driver_lanzou::new, null),
    ;
    private static final @NotNull Map<@NotNull String, @NotNull WebProviderType> drivers = new HashMap<>(); static {
        for (final WebProviderType type: WebProviderType.values())
            WebProviderType.drivers.put(type.identifier, type);
    }

    public static @Nullable WebProviderType get(final @Nullable String identifier) {
        return WebProviderType.drivers.get(identifier);
    }

    private final @NotNull String identifier;
    private final @NotNull Supplier<@NotNull ProviderInterface<?>> driver;
    private final @Nullable Supplier<@NotNull ProviderRecyclerInterface<?>> trash;

    WebProviderType(final @NotNull String identifier, final @NotNull Supplier<@NotNull ProviderInterface<?>> driver, final @Nullable Supplier<@NotNull ProviderRecyclerInterface<?>> trash) {
        this.identifier = identifier;
        this.driver = driver;
        this.trash = trash;
    }

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public @NotNull Supplier<@NotNull ProviderInterface<?>> getDriver() {
        return this.driver;
    }

    public @Nullable Supplier<@NotNull ProviderRecyclerInterface<?>> getTrash() {
        return this.trash;
    }

    @Override
    public @NotNull String toString() {
        return "WebProviderType{" +
                "identifier='" + this.identifier + '\'' +
                ", name='" + this.name() + '\'' +
                '}';
    }
}
