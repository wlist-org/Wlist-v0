package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlHelper;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

public final class Driver_123Pan implements DriverInterface<DriverConfiguration_123Pan> {
    private @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    @Override
    public void initialize(final @NotNull DriverConfiguration_123Pan configuration) throws SQLException {
        FileSqlHelper.initialize(configuration.getLocalSide().getName(), "initialize");
        // TODO specially handle root directory.
        FileSqlHelper.insertFile(configuration.getLocalSide().getName(),
                new FileSqlInformation(configuration.getWebSide().getRootDirectoryId(),
                        new DrivePath("/"), true, 0, null, null, "", List.of()/*TODO*/, null), null);
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() throws SQLException {
        FileSqlHelper.uninitialize(this.configuration.getLocalSide().getName(), "initialize");
    }

    @Override
    public void buildCache() throws IllegalParametersException, IOException {
        DriverManager_123pan.getUserInformation(this.configuration);
    }

    @Override
    public void buildIndex() throws SQLException {
        DriverManager_123pan.refreshDirectoryRecursively(this.configuration, this.configuration.getWebSide().getRootDirectoryId(), new DrivePath("/"), null, WListServer.IOExecutors);
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull DrivePath path, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.listFiles(this.configuration, path, limit, page, policy, direction, true, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getFileInformation(this.configuration, path, true, null, WListServer.IOExecutors);
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull InputStream, @NotNull Long> download(final @NotNull DrivePath path, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IllegalParametersException, IOException, SQLException {
        final Pair.ImmutablePair<String, Long> url = DriverManager_123pan.getDownloadUrl(this.configuration, path, true, null, WListServer.IOExecutors);
        if (url == null) return null;
        return DriverUtil.getDownloadStreamByRangeHeader(DriverHelper_123pan.httpClient, url, from, to, null);
    }

    @Override
    public @Nullable FileSqlInformation mkdirs(final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.createDirectoriesRecursively(this.configuration, path, policy, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable UploadMethods upload(final @NotNull DrivePath path, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getUploadMethods(this.configuration, path, md5, size, policy, null, WListServer.IOExecutors);
    }

    @Override
    public void delete(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        DriverManager_123pan.trashFile(this.configuration, path, true, null, WListServer.IOExecutors);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public @Nullable FileSqlInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final FileSqlInformation info = this.info(source);
        if (info == null)
            return null;
        final UploadMethods methods = DriverManager_123pan.getUploadMethods(this.configuration, target, info.md5(), info.size(), policy, null, WListServer.IOExecutors);
        if (methods == null)
            return null;
        try {
            if (!methods.methods().isEmpty())
                return null;
            return methods.supplier().get();
        } finally {
            methods.finisher().run();
        }
    }

    @Override
    public @Nullable FileSqlInformation move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        if (targetDirectory.equals(sourceFile.getParent()))
            return this.info(sourceFile);
        return DriverManager_123pan.moveFile(this.configuration, sourceFile, targetDirectory, true, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable FileSqlInformation rename(@NotNull final DrivePath source, @NotNull final String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        if (source.getName().equals(name))
            return this.info(source);
        return DriverManager_123pan.renameFile(this.configuration, source, name, policy, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123Pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
