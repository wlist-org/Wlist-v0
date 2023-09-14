package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformationWithGroups;
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
        FileManager.initialize(configuration.getName());
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() throws SQLException {
        FileManager.uninitialize(this.configuration.getName());
    }

    @Override
    public void buildCache() throws IllegalParametersException {
        if (!HFileHelper.ensureDirectoryExist(this.configuration.getRootDirectoryPath()))
            throw new IllegalParametersException("Failed to create root directory.", this.configuration.getRootDirectoryPath());
        this.configuration.setNickname("Server Disk (" + this.configuration.getName() + ")");
        this.configuration.setVip(true);
        this.configuration.setSpaceAll(Math.min(this.configuration.getMaxSpaceUse(), this.configuration.getRootDirectoryPath().getTotalSpace()));
    }

    @Override
    public void buildIndex() {
        //TODO
        this.configuration.setSpaceUsed(this.configuration.getRootDirectoryPath().getUsableSpace() /*TODO: count each file*/);
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformationWithGroups>> list(final @NotNull DrivePath path, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) {
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
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull Boolean> mkdirs(final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable UploadMethods upload(@NotNull final DrivePath path, final long size, @NotNull final String tag, final Options.@NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final @NotNull DrivePath path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target, final Options.@NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformationWithGroups move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory, final Options.@NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable FileSqlInformationWithGroups rename(final @NotNull DrivePath source, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull String toString() {
        return "LocalDisk{" +
                "configuration=" + this.configuration +
                '}';
    }
}
