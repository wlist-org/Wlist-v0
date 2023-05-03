package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
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
    public @NotNull Class<DriverConfiguration_123Pan> getDefaultConfigurationClass() {
        return DriverConfiguration_123Pan.class;
    }

    @Override
    public void login(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException, SQLException {
        DriverSqlHelper.initiate(configuration.getLocalSide().getName());
        DriverSqlHelper.insertFile(configuration.getLocalSide().getName(),
                new FileInformation(configuration.getWebSide().getFilePart().getRootDirectoryId(),
                        new DrivePath("/"), true, 0, null, null, "", null), null);
        DriverManager_123pan.getUserInformation(configuration);
        this.configuration = configuration;
    }

    @Override
    public void deleteDriver() throws SQLException {
        DriverSqlHelper.uninitiate(this.configuration.getLocalSide().getName());
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> list(final @NotNull DrivePath path, final int limit, final int page,
                                                                                                        final @Nullable OrderDirection direction, final @Nullable OrderPolicy policy) throws SQLException {
        return DriverManager_123pan.listFilesWithCache(this.configuration, path, limit, page, direction, policy, null);
    }

    @Override
    public @Nullable FileInformation info(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getFileInformation(this.configuration, path, true, null, null);
    }

    @Override
    public @Nullable String download(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getDownloadUrl(this.configuration, path, true, null, null);
    }

    @Override
    public @NotNull FileInformation mkdirs(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = DriverManager_123pan.getFileInformation(this.configuration, path, true, null, null);
        if (info != null) {
            if (info.is_dir())
                return info;
            throw new FileAlreadyExistsException(path.getPath());
        }
        final String name = path.getName();
        try {
            this.mkdirs(path.parent());
        } finally {
            path.child(name);
        }
        return DriverManager_123pan.createDirectory(this.configuration, path, null);
    }

    @Override
    public @NotNull FileInformation upload(final @NotNull DrivePath path, final @NotNull ByteBuf file) throws IllegalParametersException, IOException, SQLException {
        try (final InputStream stream = new ByteBufInputStream(file)) {
            final String md5 = MiscellaneousUtil.getMd5(stream);
            stream.reset();
            return DriverManager_123pan.uploadFile(this.configuration, path, stream, md5, stream.available(), null);
        }
    }

    @Override
    public void delete(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        DriverManager_123pan.trashFile(this.configuration, path, true, null, null);
    }

    @Override
    public @Nullable FileInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = this.info(source);
        if (info == null)
            return null;
        return DriverManager_123pan.uploadFile(this.configuration, target, InputStream.nullInputStream(), info.tag(), info.size(), null);
    }

    @Override
    public @Nullable FileInformation move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory) throws IllegalParametersException, IOException, SQLException {
        if (targetDirectory.equals(sourceFile.getParent()))
            return this.info(sourceFile);
        return DriverManager_123pan.moveFile(this.configuration, sourceFile, targetDirectory, true, null, null);
    }

    @Override
    public @Nullable FileInformation rename(@NotNull final DrivePath source, @NotNull final String name) throws IllegalParametersException, IOException, SQLException {
        if (source.getName().equals(name))
            return this.info(source);
        return DriverManager_123pan.renameFile(this.configuration, source, name, true, null, null);
    }

    @Override
    public void buildCache() throws IllegalParametersException, IOException, SQLException {
        DriverManager_123pan.recursiveRefreshDirectory(this.configuration, this.configuration.getWebSide().getFilePart().getRootDirectoryId(), new DrivePath("/"), null, null);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123Pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
