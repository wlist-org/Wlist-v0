package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;

/**
 * @see StorageTypes
 */
public final class StorageTypeGetter {
    private StorageTypeGetter() {
        super();
    }

    public static @NotNull String identifier(final @NotNull StorageTypes<?> type) {
        return type.identifier();
    }

    public static final @NotNull StorageTypes<LanzouConfiguration> Lanzou = StorageTypes.Lanzou;


    public static @Nullable StorageTypes<?> get(final @Nullable String identifier) {
        return StorageTypes.get(identifier);
    }

    public static @NotNull @Unmodifiable Map<@NotNull String, @NotNull StorageTypes<?>> getAll() {
        return StorageTypes.getAll();
    }
}
