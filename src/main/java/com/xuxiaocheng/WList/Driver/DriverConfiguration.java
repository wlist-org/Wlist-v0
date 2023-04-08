package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.WList.Driver.Configuration.CacheSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.LocalSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.WebSideDriverConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public abstract class DriverConfiguration<L extends LocalSideDriverConfiguration, W extends WebSideDriverConfiguration, C extends CacheSideDriverConfiguration> {
    protected @NotNull L localSide;
    protected @NotNull W webSide;
    protected @NotNull C cacheSide;

    @Contract(pure = true)
    public @NotNull Set<Class<?>> getDumpMapClasses() {
        final Set<Class<?>> classes = new HashSet<>(4);
        classes.add(this.getClass());
        classes.add(this.localSide.getClass());
        classes.add(this.webSide.getClass());
        classes.add(this.cacheSide.getClass());
        return classes;
    }

    protected DriverConfiguration(@NotNull final Supplier<? extends L> local, final @NotNull Supplier<? extends W> web, final @NotNull Supplier<? extends C> cache) {
        super();
        this.localSide = local.get();
        this.webSide = web.get();
        this.cacheSide = cache.get();
    }

    public @NotNull L getLocalSide() {
        return this.localSide;
    }

    public void setLocalSide(@NotNull final L localSide) {
        this.localSide = localSide;
    }

    public @NotNull W getWebSide() {
        return this.webSide;
    }

    public void setWebSide(@NotNull final W webSide) {
        this.webSide = webSide;
    }

    public @NotNull C getCacheSide() {
        return this.cacheSide;
    }

    public void setCacheSide(@NotNull final C cacheSide) {
        this.cacheSide = cacheSide;
    }

    @Override
    public @NotNull String toString() {
        return "DriverConfiguration{" +
                "localSide=" + this.localSide +
                ", webSide=" + this.webSide +
                ", cacheSide=" + this.cacheSide +
                '}';
    }
}
