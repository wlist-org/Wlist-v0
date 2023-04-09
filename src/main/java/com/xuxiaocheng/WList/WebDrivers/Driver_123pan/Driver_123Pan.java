package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Driver;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class Driver_123Pan implements Driver<DriverConfiguration_123Pan> {

    private @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    @NotNull DriverConfiguration_123Pan getConfiguration() {
        return this.configuration;
    }

    public @NotNull DriverConfiguration_123Pan login(final @Nullable DriverConfiguration_123Pan info) throws IOException, IllegalParametersException {
        this.configuration = Objects.requireNonNullElseGet(info, DriverConfiguration_123Pan::new);
        DriverHelper_123pan.doRetrieveToken(this.configuration);
        DriverHelper_123pan.doGetUserInformation(this.configuration);
        return this.configuration;
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull List<@NotNull String>> list(final @NotNull DrivePath path, final int page, final int limit) throws IOException, IllegalParametersException {
//        final long id = DriverHelper_123pan.getDirectoryId(this.configuration, path, true, true);
        return Pair.ImmutablePair.makeImmutablePair(0, List.of());
    }

    @Override
    public @Nullable Long size(final @NotNull DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String download(final @NotNull DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String mkdirs(final @NotNull DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String upload(final @NotNull DrivePath path, final @NotNull ByteBuf file) {
        return null;
    }

    @Override
    public void delete(final @NotNull DrivePath path) {

    }

    @Override
    public void rmdir(final @NotNull DrivePath path) {

    }

    @Override
    public @Nullable String copy(final @NotNull DrivePath source, final @NotNull DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String move(final @NotNull DrivePath source, final @NotNull DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String rename(final @NotNull DrivePath source, final @NotNull String name) {
        return null;
    }
}
