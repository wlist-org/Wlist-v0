package com.xuxiaocheng.WList.Server.Storage;

import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderRecyclerInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum ProviderTypes {
//    driver_123Pan("123pan", Driver_123pan::new, Trash_123pan::new),
//    driver_lanzou("lanzou", Driver_lanzou::new, null),
    ;
    private static final @NotNull Map<@NotNull String, @NotNull ProviderTypes> drivers = new HashMap<>(); static {
        for (final ProviderTypes type: ProviderTypes.values())
            ProviderTypes.drivers.put(type.identifier, type);
    }

    public static @Nullable ProviderTypes get(final @Nullable String identifier) {
        return ProviderTypes.drivers.get(identifier);
    }

    private final @NotNull String identifier;
    private final @NotNull Supplier<@NotNull ProviderInterface<?>> driver;
    private final @Nullable Supplier<@NotNull ProviderRecyclerInterface<?>> trash;

    ProviderTypes(final @NotNull String identifier, final @NotNull Supplier<@NotNull ProviderInterface<?>> driver, final @Nullable Supplier<@NotNull ProviderRecyclerInterface<?>> trash) {
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
        return "ProviderTypes{" +
                "identifier='" + this.identifier + '\'' +
                ", name='" + this.name() + '\'' +
                '}';
    }
}
