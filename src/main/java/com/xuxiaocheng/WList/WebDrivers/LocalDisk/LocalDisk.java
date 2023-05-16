package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.Helpers.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Driver.Utils.FileInformation;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

public final class LocalDisk implements DriverInterface<LocalDiskConfiguration> {
    private @NotNull LocalDiskConfiguration configuration = new LocalDiskConfiguration();

    @Override
    public void initiate(final @NotNull LocalDiskConfiguration configuration) throws SQLException {
        DriverSqlHelper.initiate(configuration.getLocalSide().getName());
//        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(),
//                new FileInformation(0, new DrivePath("/"), true,
//                        0, null, null, "", null), null);
        this.configuration = configuration;
    }

    @Override
    public void uninitiate() throws SQLException {
        DriverSqlHelper.uninitiate(this.configuration.getLocalSide().getName());
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
    public Pair.ImmutablePair<@NotNull Integer, @NotNull @UnmodifiableView List<@NotNull FileInformation>> list(@NotNull DrivePath path, int limit, int page, @Nullable OrderDirection direction, @Nullable OrderPolicy policy) throws Exception {
        return null;
    }

    @Override
    public @Nullable FileInformation info(@NotNull DrivePath path) throws Exception {
        return null;
    }

    @Nullable
    @Override
    public Pair.ImmutablePair<@NotNull InputStream, @NotNull Long> download(@NotNull DrivePath path, long from, long to) throws Exception {
        return null;
    }

    @Override
    public @Nullable FileInformation mkdirs(@NotNull DrivePath path) throws Exception {
        return null;
    }

    @Override
    public Triad.@Nullable ImmutableTriad<@NotNull List<Pair.ImmutablePair<@NotNull Long, @NotNull ConsumerE<@NotNull ByteBuf>>>, @NotNull Supplier<@Nullable FileInformation>, @NotNull Runnable> upload(@NotNull final DrivePath path, final long size, @NotNull final String tag) throws Exception {
        return null;
    }

    @Override
    public void delete(@NotNull DrivePath path) throws Exception {

    }

    @Override
    public @Nullable FileInformation copy(@NotNull DrivePath source, @NotNull DrivePath target) throws Exception {
        return DriverInterface.super.copy(source, target);
    }

    @Override
    public @Nullable FileInformation move(@NotNull DrivePath sourceFile, @NotNull DrivePath targetDirectory) throws Exception {
        return DriverInterface.super.move(sourceFile, targetDirectory);
    }

    @Override
    public @Nullable FileInformation rename(@NotNull DrivePath source, @NotNull String name) throws Exception {
        return DriverInterface.super.rename(source, name);
    }
}
