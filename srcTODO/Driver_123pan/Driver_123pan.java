package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlHelper;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.Operations.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.Operations.Helpers.UploadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

public class Driver_123pan extends Driver_123pan_NoCache {
    @Override
    public void initialize(final @NotNull DriverConfiguration_123pan configuration) throws SQLException {
        FileManager.quicklyInitialize(new FileSqlHelper(PooledDatabase.instance.getInstance(), configuration.getName(), configuration.getWebSide().getRootDirectoryId()), null);
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() throws IllegalParametersException, IOException, SQLException {
        final String name = this.configuration.getName();
        super.uninitialize();
        FileManager.quicklyUninitialize(name, null);
    }

    @Override
    public void buildIndex() throws SQLException {
        this.configuration.getCacheSide().setLastFileIndexBuildTime(LocalDateTime.now());
        DriverManager_123pan.refreshDirectoryRecursively(this.configuration, this.configuration.getWebSide().getRootDirectoryId(), null);
        this.configuration.getCacheSide().setModified(true);
    }


    @Override
    public void forceRefreshDirectory(final @NotNull FileLocation location) throws IllegalParametersException, IOException, SQLException {
        final FileSqlInformation information = DriverManager_123pan.getFileInformation(this.configuration, this.toRootId(location.id()), null);
        if (information == null) return;
        final Iterator<FileSqlInformation> lister = DriverManager_123pan.listAllFilesNoCache(this.configuration, information.id(), null).getB();
        while (lister.hasNext()) lister.next();
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull FileLocation location, final @LongRange(minimum = 0) int limit, final @LongRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.listFiles(this.configuration, this.toRootId(location.id()), limit, page, policy, direction, null);
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull FileLocation location) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getFileInformation(this.configuration, this.toRootId(location.id()), null);
    }

    @Override
    public @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getDownloadMethods(this.configuration, location.id(), from, to, null);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.createDirectory(this.configuration, this.toRootId(parentLocation.id()), directoryName, policy, null);
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getUploadMethods(this.configuration, this.toRootId(parentLocation.id()), filename, md5, size, policy, null);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public void delete(final @NotNull FileLocation location) throws Exception {
        if (location.id() == 0 || location.id() == this.configuration.getWebSide().getRootDirectoryId()) {
            DriverManager.removeDriver(this.configuration.getName());
            return;
        }
        DriverManager_123pan.trashFile(this.configuration, location.id(), null);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetLocation, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.moveFile(this.configuration, sourceLocation.id(), this.toRootId(targetLocation.id()), policy, null);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.renameFile(this.configuration, sourceLocation.id(), name, policy, null);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
