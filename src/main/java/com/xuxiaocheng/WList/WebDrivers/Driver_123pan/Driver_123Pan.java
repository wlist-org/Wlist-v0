package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Driver;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Driver_123Pan implements Driver<DriverConfiguration_123Pan> {
    private @NotNull DriverConfiguration_123Pan configuration = new DriverConfiguration_123Pan();

    @NotNull DriverConfiguration_123Pan getConfiguration() {
        return this.configuration;
    }

    @Override
    public @NotNull DriverConfiguration_123Pan getDefaultConfiguration() {
        return new DriverConfiguration_123Pan();
    }

    public void login(final @NotNull DriverConfiguration_123Pan configuration) throws IOException, IllegalParametersException, SQLException {
        DriverSQL_123pan.initiate(configuration.getLocalSide().getName());
        DriverUtil_123pan.doRetrieveToken(configuration);
        DriverUtil_123pan.doGetUserInformation(configuration);
        this.configuration = configuration;
    }

    @Override
    public void deleteDriver() throws SQLException {
        DriverSQL_123pan.uninitiate(this.configuration.getLocalSide().getName());
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull List<@NotNull String>> list(final @NotNull DrivePath path, final int page, final int limit) throws IOException, IllegalParametersException, SQLException {
        final long id = DriverUtil_123pan.getFileId(this.configuration, path, DriverHelper_123pan::isDirectory, true);
        if (id < 0)
            return null;
        final Pair<Integer, List<FileInformation_123pan>> info = DriverUtil_123pan.doListFiles(this.configuration, id, limit, page, path, null);
        final List<String> list = new ArrayList<>(info.getSecond().size());
        for (final FileInformation_123pan obj: info.getSecond())
            list.add(obj.path().getName());
        return Pair.ImmutablePair.makeImmutablePair(info.getFirst(), list);
    }

    @Override
    public @Nullable Long size(final @NotNull DrivePath path) throws SQLException, IOException, IllegalParametersException {
        final FileInformation_123pan info = DriverUtil_123pan.getFileInformation(this.configuration, path, true);
        if (info == null)
            return null;
        return info.size();
    }

    @Override
    public @Nullable Long createTime(final @NotNull DrivePath path) throws SQLException, IOException, IllegalParametersException {
        final FileInformation_123pan info = DriverUtil_123pan.getFileInformation(this.configuration, path, true);
        if (info == null)
            return null;
        return info.createTime();
    }

    @Override
    public @Nullable Long updateTime(final @NotNull DrivePath path) throws SQLException, IOException, IllegalParametersException {
        final FileInformation_123pan info = DriverUtil_123pan.getFileInformation(this.configuration, path, true);
        if (info == null)
            return null;
        return info.updateTime();
    }

    @Override
    public @Nullable String download(final @NotNull DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String mkdirs(final @NotNull DrivePath path) {
        return null;
    }

    @Override
    public @Nullable String upload(final @NotNull DrivePath path, final @NotNull ByteBuf file) {
        return null;
    }

    @Override
    public void delete(final @NotNull DrivePath path) {

    }

    @Override
    public void rmdir(final @NotNull DrivePath path) {

    }

    @Override
    public @Nullable String copy(final @NotNull DrivePath source, final @NotNull DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String move(final @NotNull DrivePath source, final @NotNull DrivePath target) {
        return null;
    }

    @Override
    public @Nullable String rename(final @NotNull DrivePath source, final @NotNull String name) {
        return null;
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123Pan{" +
                "configuration=" + this.configuration +
                '}';
    }
}
