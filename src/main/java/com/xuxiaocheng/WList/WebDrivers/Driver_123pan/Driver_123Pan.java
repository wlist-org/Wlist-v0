package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public final class Driver_123Pan implements DriverInterface<DriverConfiguration_123Pan> {
    private @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    @Override
    public @NotNull DriverConfiguration_123Pan getConfiguration() {
        return this.configuration;
    }

    @Override
    public void initialize(final @NotNull DriverConfiguration_123Pan configuration) throws SQLException {
        FileManager.initialize(configuration.getLocalSide().getName());
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() throws SQLException {
        FileManager.uninitialize(this.configuration.getLocalSide().getName());
    }

    @Override
    public void buildCache() throws IllegalParametersException, IOException {
        DriverManager_123pan.resetUserInformation(this.configuration);
    }

    @Override
    public void buildIndex() throws SQLException {
        DriverManager_123pan.refreshDirectoryRecursively(this.configuration, this.configuration.getWebSide().getRootDirectoryId(), new DrivePath("/"), null, WListServer.IOExecutors);
    }

    @Override
    public void forceRefreshDirectory(final @NotNull DrivePath path) throws SQLException {
        final FileSqlInformation information = DriverManager_123pan.getFileInformation(this.configuration, path, true, true, false, null, WListServer.IOExecutors);
        if (information == null)
            return;
        final Iterator<FileSqlInformation> iterator = DriverManager_123pan.listAllFilesNoCache(this.configuration, information.id(), path, null, WListServer.IOExecutors).getB();
        while (iterator.hasNext())
            iterator.next();
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull DrivePath path, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.listFiles(this.configuration, path, limit, page, policy, direction, true, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getFileInformation(this.configuration, path, false, true, false, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable DownloadMethods download(final @NotNull DrivePath path, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getDownloadMethods(this.configuration, path, from, to, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> mkdirs(final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.createDirectoriesRecursively(this.configuration, path, policy, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull DrivePath path, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getUploadMethods(this.configuration, path, md5, size, policy, true, null, WListServer.IOExecutors);
    }

    @Override
    public void delete(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        if (path.getDepth() == 0)
            this.uninitialize();
        else
            DriverManager_123pan.trashFile(this.configuration, path, true, null, WListServer.IOExecutors);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> copy(final @NotNull DrivePath source, final @NotNull DrivePath target, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final FileSqlInformation info = this.info(source);
        if (info == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", source));
        final UnionPair<UploadMethods, FailureReason> methods = this.upload(target, info.size(), info.md5(), policy);
        if (methods.isFailure())
            return UnionPair.fail(methods.getE());
        try {
            if (!methods.getT().methods().isEmpty())
                throw new IllegalStateException("Failed to copy file. [Unreachable]. source: " + source + ", sourceInfo: " + info + ", target: " + target + ", policy: " + policy);
            final FileSqlInformation information = methods.getT().supplier().get();
            if (information == null)
                throw new IllegalStateException("Failed to copy file. [Unknown]. source: " + source + ", sourceInfo: " + info + ", target: " + target + ", policy: " + policy);
            return UnionPair.ok(information);
        } finally {
            methods.getT().finisher().run();
        }
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        if (targetDirectory.equals(sourceFile.getParent())) {
            final FileSqlInformation information = this.info(sourceFile);
            if (information == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceFile));
            return UnionPair.ok(information);
        }
        return DriverManager_123pan.moveFile(this.configuration, sourceFile, targetDirectory, policy, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> rename(@NotNull final DrivePath source, @NotNull final String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        if (source.getName().equals(name)) {
            final FileSqlInformation information = this.info(source);
            if (information == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", source));
            return UnionPair.ok(information);
        }
        return DriverManager_123pan.renameFile(this.configuration, source, name, policy, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123Pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
