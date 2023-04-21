package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.WList.Driver.Configuration.CacheSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.LocalSideDriverConfiguration;
import com.xuxiaocheng.WList.Driver.Configuration.WebSideDriverConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.function.Supplier;

public abstract class DriverConfiguration<L extends LocalSideDriverConfiguration, W extends WebSideDriverConfiguration, C extends CacheSideDriverConfiguration> {
    protected @NotNull L localSide;
    protected @NotNull W webSide;
    protected @NotNull C cacheSide;

    public void setConfigClassTag(final @NotNull Representer representer) {
        representer.addClassTag(this.getClass(), Tag.MAP);
        representer.addClassTag(this.localSide.getClass(), Tag.MAP);
        representer.addClassTag(this.webSide.getClass(), Tag.MAP);
        representer.addClassTag(this.cacheSide.getClass(), Tag.MAP);
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
