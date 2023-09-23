package com.xuxiaocheng.WList.Server.Storage.Providers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface SharerInterface<C extends StorageConfiguration> {
    @Contract(pure = true)
    @NotNull StorageTypes<C> getType();

    @NotNull C getConfiguration();

    void initialize(final @NotNull C configuration) throws Exception;

    void uninitialize(final boolean dropIndex) throws Exception;


}
