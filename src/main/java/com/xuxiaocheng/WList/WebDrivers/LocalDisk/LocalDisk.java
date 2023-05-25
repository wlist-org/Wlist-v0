package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
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
//        FileSqlHelper.insertFile(configuration.getLocalSide().getName(),
//                new FileSqlInformation(0, new DrivePath("/"), true,
//                        0, null, null, "", null), null);
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

    @Nullable
    @Override
    public Pair.ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(@NotNull DrivePath path, int limit, int page, @Nullable OrderDirection direction, @Nullable OrderPolicy policy) throws Exception {
        return null;
    }

    @Override
    public @Nullable FileSqlInformation info(@NotNull DrivePath path) throws Exception {
        return null;
    }

    @Nullable
    @Override
    public Pair.ImmutablePair<@NotNull InputStream, @NotNull Long> download(@NotNull DrivePath path, long from, long to) throws Exception {
        return null;
    }

    @Override
    public @Nullable FileSqlInformation mkdirs(@NotNull DrivePath path) throws Exception {
        return null;
    }

    @Override
    public @Nullable UploadMethods upload(@NotNull final DrivePath path, final long size, @NotNull final String tag) throws Exception {
        return null;
    }

    @Override
    public void delete(@NotNull DrivePath path) throws Exception {

    }

    @Override
    public @Nullable FileSqlInformation copy(@NotNull DrivePath source, @NotNull DrivePath target) throws Exception {
        return DriverInterface.super.copy(source, target);
    }

    @Override
    public @Nullable FileSqlInformation move(@NotNull DrivePath sourceFile, @NotNull DrivePath targetDirectory) throws Exception {
        return DriverInterface.super.move(sourceFile, targetDirectory);
    }

    @Override
    public @Nullable FileSqlInformation rename(@NotNull DrivePath source, @NotNull String name) throws Exception {
        return DriverInterface.super.rename(source, name);
    }
}
