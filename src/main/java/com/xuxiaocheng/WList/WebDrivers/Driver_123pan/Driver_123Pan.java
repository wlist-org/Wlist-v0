package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Driver_123Pan implements DriverInterface<DriverConfiguration_123Pan> {
    private @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    @NotNull DriverConfiguration_123Pan getConfiguration() {
        return this.configuration;
    }

    @Override
    public @NotNull DriverConfiguration_123Pan getDefaultConfiguration() {
        return new DriverConfiguration_123Pan();
    }

    public void login(final @NotNull DriverConfiguration_123Pan configuration) throws IllegalParametersException, IOException, SQLException {
        DriverSqlHelper.initiate(configuration.getLocalSide().getName());
        DriverManager_123pan.getUserInformation(configuration);
        this.configuration = configuration;
    }

    @Override
    public void deleteDriver() throws SQLException {
        DriverSqlHelper.uninitiate(this.configuration.getLocalSide().getName());
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull List<@NotNull String>> list(final @NotNull DrivePath path, final int page, final int limit) throws IllegalParametersException, IOException, SQLException {
        final long id = DriverManager_123pan.getFileId(this.configuration, path, FileInformation::is_dir, true, null, null);
        if (id < 0)
            return null;
        final Pair<Integer, List<FileInformation>> info = DriverManager_123pan.listFiles(this.configuration, id, limit, page, path, null);
        final List<String> list = new ArrayList<>(info.getSecond().size());
        for (final FileInformation obj: info.getSecond())
            list.add(obj.path().getName());
        return Pair.ImmutablePair.makeImmutablePair(info.getFirst(), list);
    }

    @Override
    public @Nullable FileInformation info(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.getFileInformation(this.configuration, path, true, null, null);
    }

    @Override
    public @Nullable String download(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverManager_123pan.doGetDownloadUrl(this.configuration, path, true, null, null);
    }

    @Override
    public boolean mkdirs(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = DriverManager_123pan.getFileInformation(this.configuration, path, true, null, null);
        if (info != null) {
            if (info.is_dir())
                return false;
            throw new FileAlreadyExistsException(path.getPath());
        }
        this.mkdirs(path.getParent());
        DriverManager_123pan.createDirectory(this.configuration, path, null);
        return true;
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
    public void delete(final @NotNull DrivePath path) {

    }

    @Override
    public @Nullable FileInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = this.info(source);
        if (info == null)
            return null;
        return DriverManager_123pan.uploadFile(this.configuration, target, InputStream.nullInputStream(), info.tag(), info.size(), null);
    }

    @Override
    public @Nullable FileInformation move(final @NotNull DrivePath source, final @NotNull DrivePath target) {
        return null;
    }

    @Override
    public void buildCache() throws SQLException, IOException, IllegalParametersException {
        DriverManager_123pan.recursiveRefreshDirectory(this.configuration, this.configuration.getWebSide().getFilePart().getRootDirectoryId(), new DrivePath("/"), null, null);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123Pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
