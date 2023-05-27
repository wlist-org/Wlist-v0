package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.Driver.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public final class LocalDisk implements DriverInterface<LocalDiskConfiguration> {
    private @NotNull LocalDiskConfiguration configuration = new LocalDiskConfiguration();

    @Override
    public void initialize(final @NotNull LocalDiskConfiguration configuration) throws SQLException {
        FileSqlHelper.initialize(configuration.getLocalSide().getName(), "initialize");
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() throws SQLException {
        FileSqlHelper.uninitialize(this.configuration.getLocalSide().getName(), "initialize");
    }

    @Override
    public void buildCache() throws IllegalParametersException {
        if (!HFileHelper.ensureDirectoryExist(this.configuration.getWebSide().getRootDirectoryPath()))
            throw new IllegalParametersException("Failed to create root directory.", this.configuration.getWebSide().getRootDirectoryPath());
        this.configuration.getCacheSide().setNickname("Server Disk (" + this.configuration.getLocalSide().getName() + ")");
        this.configuration.getCacheSide().setVip(true);
        this.configuration.getCacheSide().setSpaceAll(Math.min(this.configuration.getWebSide().getMaxSpaceUse(), this.configuration.getWebSide().getRootDirectoryPath().getTotalSpace()));
    }

    @Override
    public void buildIndex() {
        //TODO
        this.configuration.getCacheSide().setSpaceUsed(this.configuration.getWebSide().getRootDirectoryPath().getUsableSpace() /*TODO: count each file*/);
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull DrivePath path, final int limit, final int page, final @NotNull OrderPolicy policy, final @NotNull OrderDirection direction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull DrivePath path) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public Pair.ImmutablePair<@NotNull InputStream, @NotNull Long> download(final @NotNull DrivePath path, final long from, final long to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformation mkdirs(final @NotNull DrivePath path, final @NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable UploadMethods upload(@NotNull final DrivePath path, final long size, @NotNull final String tag, final @NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final @NotNull DrivePath path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target, final @NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformation move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory, final @NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformation rename(final @NotNull DrivePath source, final @NotNull String name, final @NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull String toString() {
        return "LocalDisk{" +
                "configuration=" + this.configuration +
                '}';
    }
}
