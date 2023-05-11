package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.Helpers.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.Utils.FileInformation;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.sql.SQLException;
import java.util.List;

public final class Driver_123Pan implements DriverInterface<DriverConfiguration_123Pan> {
    private @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    @Override
    public void initiate(final @NotNull DriverConfiguration_123Pan configuration) throws SQLException {
        DriverSqlHelper.initiate(configuration.getLocalSide().getName());
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(),
                new FileInformation(configuration.getWebSide().getRootDirectoryId(),
                        new DrivePath("/"), true, 0, null, null, "", null), null);
        this.configuration = configuration;
    }

    @Override
    public void uninitiate() throws SQLException {
        DriverSqlHelper.uninitiate(this.configuration.getLocalSide().getName());
    }

    @Override
    public void buildCache() throws IllegalParametersException, IOException {
        DriverManager_123pan.getUserInformation(this.configuration);
    }

    @Override
    public void buildIndex() throws IllegalParametersException, IOException, SQLException {
        DriverManager_123pan.recursiveRefreshDirectory(this.configuration, this.configuration.getWebSide().getRootDirectoryId(), new DrivePath("/"), null, WListServer.IOExecutors);
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> list(final @NotNull DrivePath path, final int limit, final int page,
                                                                                                        final @Nullable OrderDirection direction, final @Nullable OrderPolicy policy) throws SQLException {
        return DriverManager_123pan.listFilesWithCache(this.configuration, path, limit, page, direction, policy, null);
    }

    @Override
    public @Nullable FileInformation info(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getFileInformation(this.configuration, path, true, null, WListServer.IOExecutors);
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull InputStream, @NotNull Long> download(final @NotNull DrivePath path, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IllegalParametersException, IOException, SQLException {
        final Pair.ImmutablePair<String, Long> url = DriverManager_123pan.getDownloadUrl(this.configuration, path, true, null, WListServer.IOExecutors);
        if (url == null)
            return null;
        return DriverManager_123pan.getDownloadStream(url, from, to);
    }

    @Override
    public @Nullable FileInformation mkdirs(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = DriverManager_123pan.getFileInformation(this.configuration, path, true, null, WListServer.IOExecutors);
        if (info != null) {
            if (info.is_dir())
                return info;
            throw new FileAlreadyExistsException(path.getPath());
        }
        final String name = path.getName();
        try {
            if (!DriverHelper_123pan.filenamePredication.test(name))
                return null;
            this.mkdirs(path.parent());
        } finally {
            path.child(name);
        }
        return DriverManager_123pan.createDirectory(this.configuration, path, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable FileInformation upload(final @NotNull DrivePath path, final @NotNull InputStream stream, final @NotNull String tag, final @NotNull List<@NotNull String> ignoredPartTags) throws IllegalParametersException, IOException, SQLException {
        if (this.mkdirs(path.getParent()) == null)
            return null;
        return DriverManager_123pan.uploadFile(this.configuration, path, stream, tag, stream.available(), null, WListServer.IOExecutors);
    }

    @Override
    public void delete(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        DriverManager_123pan.trashFile(this.configuration, path, true, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable FileInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = this.info(source);
        if (info == null)
            return null;
        return DriverManager_123pan.uploadFile(this.configuration, target, InputStream.nullInputStream(), info.tag(), info.size(), null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable FileInformation move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory) throws IllegalParametersException, IOException, SQLException {
        if (targetDirectory.equals(sourceFile.getParent()))
            return this.info(sourceFile);
        return DriverManager_123pan.moveFile(this.configuration, sourceFile, targetDirectory, true, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable FileInformation rename(@NotNull final DrivePath source, @NotNull final String name) throws IllegalParametersException, IOException, SQLException {
        if (source.getName().equals(name))
            return this.info(source);
        return DriverManager_123pan.renameFile(this.configuration, source, name, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123Pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
