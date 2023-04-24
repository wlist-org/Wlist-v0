package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Driver.FileInformation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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
        DriverUtil_123pan.doRetrieveToken(configuration);
        DriverUtil_123pan.doGetUserInformation(configuration);
        this.configuration = configuration;
    }

    @Override
    public void deleteDriver() throws SQLException {
        DriverSqlHelper.uninitiate(this.configuration.getLocalSide().getName());
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull List<@NotNull String>> list(final @NotNull DrivePath path, final int page, final int limit) throws IllegalParametersException, IOException, SQLException {
        final long id = DriverUtil_123pan.getFileId(this.configuration, path, FileInformation::is_dir, true, null);
        if (id < 0)
            return null;
        final Pair<Integer, List<FileInformation>> info = DriverUtil_123pan.doListFiles(this.configuration, id, limit, page, path, null);
        final List<String> list = new ArrayList<>(info.getSecond().size());
        for (final FileInformation obj: info.getSecond())
            list.add(obj.path().getName());
        return Pair.ImmutablePair.makeImmutablePair(info.getFirst(), list);
    }

    @Override
    public @Nullable FileInformation info(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverUtil_123pan.getFileInformation(this.configuration, path, true, null);
    }

    @Override
    public @Nullable String download(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        return DriverUtil_123pan.doGetDownloadUrl(this.configuration, path, null);
    }

    @Override
    public boolean mkdirs(final @NotNull DrivePath path) throws IllegalParametersException, IOException, SQLException {
        final FileInformation info = DriverUtil_123pan.getFileInformation(this.configuration, path, true, null);
        if (info != null) {
            if (info.is_dir())
                return false;
            throw new FileAlreadyExistsException(path.getPath());
        }
        this.mkdirs(path.getParent());
        DriverUtil_123pan.doCreateDirectory(this.configuration, path, null);
        return true;
    }

    @Override
    public @NotNull FileInformation upload(final @NotNull DrivePath path, final @NotNull ByteBuf file) {
//        return DriverUtil_123pan.doUpload(this.configuration, path, file, null);
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final @NotNull DrivePath path) {

    }

    @Override
    public @Nullable FileInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target) {
        return null;
    }

    @Override
    public @Nullable FileInformation move(final @NotNull DrivePath source, final @NotNull DrivePath target) {
        return null;
    }

    @Override
    public void buildCache() throws SQLException, IOException, IllegalParametersException {
        DriverUtil_123pan.recursiveRefreshDirectory(this.configuration, new DrivePath("/"), this.configuration.getWebSide().getFilePart().getRootDirectoryId(), null, null);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123Pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
