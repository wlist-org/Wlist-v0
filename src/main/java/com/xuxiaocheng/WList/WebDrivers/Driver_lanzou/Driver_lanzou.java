package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlHelper;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class Driver_lanzou implements DriverInterface<DriverConfiguration_lanzou> {
    protected @NotNull DriverConfiguration_lanzou configuration = new DriverConfiguration_lanzou();

    @Override
    public @NotNull DriverConfiguration_lanzou getConfiguration() {
        return this.configuration;
    }

    @Override
    public void initialize(final @NotNull DriverConfiguration_lanzou configuration) throws SQLException {
        FileManager.quicklyInitialize(new FileSqlHelper(PooledDatabase.instance.getInstance(), configuration.getName(), configuration.getWebSide().getRootDirectoryId()), null);
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() throws SQLException {
        FileManager.quicklyUninitialize(this.configuration.getName(), null);
    }

    @Override
    public void buildCache() throws IOException {
        if (this.configuration.getCacheSide().getVei() == null)
            DriverHelper_lanzou.login(this.configuration);
    }

    @Override
    public void buildIndex() throws Exception {

    }

    @Override
    public void forceRefreshDirectory(final @NotNull FileLocation location) throws Exception {

    }

    @Nullable
    @Override
    public Pair.ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(@NotNull FileLocation location, int limit, int page, @NotNull Options.OrderPolicy policy, @NotNull Options.OrderDirection direction, final Options.@NotNull DirectoriesOrFiles filter) throws Exception {
        return null;
    }

    @Override
    public @Nullable FileSqlInformation info(@NotNull FileLocation location) throws Exception {
        return null;
    }

    @Override
    public @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> download(@NotNull FileLocation location, long from, long to) throws Exception {
        return null;
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(@NotNull FileLocation parentLocation, @NotNull String directoryName, @NotNull Options.DuplicatePolicy policy) throws Exception {
        return null;
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(@NotNull FileLocation parentLocation, @NotNull String filename, long size, @NotNull String md5, @NotNull Options.DuplicatePolicy policy) throws Exception {
        return null;
    }

    @Override
    public void delete(@NotNull FileLocation location) throws Exception {

    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> copy(@NotNull FileLocation sourceLocation, @NotNull FileLocation targetParentLocation, @NotNull String targetFilename, @NotNull Options.DuplicatePolicy policy) throws Exception {
        return DriverInterface.super.copy(sourceLocation, targetParentLocation, targetFilename, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> move(@NotNull FileLocation sourceLocation, @NotNull FileLocation targetLocation, @NotNull Options.DuplicatePolicy policy) throws Exception {
        return DriverInterface.super.move(sourceLocation, targetLocation, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> rename(@NotNull FileLocation sourceLocation, @NotNull String name, @NotNull Options.DuplicatePolicy policy) throws Exception {
        return DriverInterface.super.rename(sourceLocation, name, policy);
    }

    @Override
    public@NotNull String toString() {
        return "Driver_lanzou{" +
                "configuration=" + this.configuration +
                '}';
    }
}
