package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.TrashedFileManager;
import com.xuxiaocheng.WList.Databases.File.TrashedSqlInformation;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class Trash_123pan implements DriverTrashInterface<Driver_123Pan> {
    private @NotNull Driver_123Pan driver = new Driver_123Pan();

    @Override
    public @NotNull Driver_123Pan getDriver() {
        return this.driver;
    }

    @Override
    public void initialize(final @NotNull Driver_123Pan driver) throws SQLException {
        TrashedFileManager.initialize(driver.getConfiguration().getLocalSide().getName());
        this.driver = driver;
    }

    @Override
    public void uninitialize() throws SQLException {
        TrashedFileManager.uninitialize(this.driver.getConfiguration().getLocalSide().getName());
    }

    @Override
    public void buildCache() throws SQLException {
        // TODO build time interval.
        this.buildIndex();
    }

    @Override
    public void buildIndex() throws SQLException {
        final Iterator<TrashedSqlInformation> iterator = TrashManager_123pan.listAllFilesNoCache(this.driver.getConfiguration(), null, WListServer.IOExecutors).getB();
        while (iterator.hasNext())
            iterator.next();
    }

    @Override
    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> list(final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
        return TrashManager_123pan.listFiles(this.driver.getConfiguration(), limit, page, policy, direction, true, null, WListServer.IOExecutors);
    }

    @Override
    public @Nullable TrashedSqlInformation info(final long id) throws IllegalParametersException, IOException, SQLException {
        return TrashManager_123pan.getFileInformation(this.driver.getConfiguration(), id, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> restore(final long id, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return TrashManager_123pan.restoreFile(this.driver.getConfiguration(), id, path, policy, true, null, WListServer.IOExecutors);
    }

    @Override
    public void delete(final long id) throws IllegalParametersException, IOException, SQLException {
        TrashManager_123pan.deleteFile(this.driver.getConfiguration(), id, true, null);
    }

    @Override
    public void deleteAll() throws IllegalParametersException, IOException, SQLException {
        TrashManager_123pan.deleteAllFiles(this.driver.getConfiguration(), null);
    }

    @Override
    public @Nullable DownloadMethods download(final long id, final long from, final long to) throws IllegalParametersException, IOException, SQLException {
        return TrashManager_123pan.getDownloadMethods(this.driver.getConfiguration(), id, from, to, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull UnionPair<@NotNull TrashedSqlInformation, @NotNull FailureReason> rename(final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return TrashManager_123pan.renameFile(this.driver.getConfiguration(), id, name, policy, true, null, WListServer.IOExecutors);
    }

    @Override
    public @NotNull String toString() {
        return "Trash_123pan{" +
                "driver=" + this.driver +
                '}';
    }
}
