package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Driver;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public final class Driver_123Pan implements Driver<DriverConfiguration_123Pan> {

    private @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    @NotNull DriverConfiguration_123Pan getConfiguration() {
        return this.configuration;
    }

    public @NotNull DriverConfiguration_123Pan login(final @Nullable DriverConfiguration_123Pan info) throws IOException, IllegalParametersException {
        final DriverConfiguration_123Pan config = Objects.requireNonNullElseGet(info, DriverConfiguration_123Pan::new);
        final long time = System.currentTimeMillis();
        if (config.getTokenExpire() >= time && config.getToken() != null) {
            this.configuration = config;
            return config;
        }
        if (config.getRefreshExpire() >= time && config.getToken() != null)
            DriverHelper_123pan.doRefreshToken(config);
        else
            DriverHelper_123pan.doGetToken(config);
        this.configuration = config;
        return config;
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull String>> list(@NotNull final DrivePath path, final int page, final int limit) {
        final long id = DriverHelper_123pan.getDirectoryId(path, true, this);
// TODO list
        return Pair.ImmutablePair.makeImmutablePair(0L, List.of());
    }

    @Override
    public @Nullable Long size(@NotNull final DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String download(@NotNull final DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String mkdirs(@NotNull final DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String upload(@NotNull final DrivePath path, @NotNull final InputStream file) {
        return null;
    }

    @Override
    public void delete(@NotNull final DrivePath path) {

    }

    @Override
    public void rmdir(@NotNull final DrivePath path) {

    }

    @Override
    public @Nullable String copy(@NotNull final DrivePath source, @NotNull final DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String move(@NotNull final DrivePath source, @NotNull final DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String rename(@NotNull final DrivePath source, @NotNull final String name) {
        return null;
    }
}
