package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlHelper;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.GenericSql.PooledDatabase;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedFileManager;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedSqlHelper;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
        FileManager.insertOrUpdateFile(this.configuration.getName(), RootDriver.getDatabaseDriverInformation(this.configuration), null);
        TrashedFileManager.quicklyInitialize(new TrashedSqlHelper(PooledDatabase.instance.getInstance(), configuration.getName()), null);
    }

    @Override
    public void uninitialize() throws SQLException {
        FileManager.quicklyUninitialize(this.configuration.getName(), null);
        TrashedFileManager.quicklyUninitialize(this.configuration.getName(), null);
    }

    @Override
    public void buildCache() throws IOException {
        DriverManager_lanzou.ensureLoggedIn(this.configuration);
    }

    @Override
    public void buildIndex() throws IOException, SQLException, InterruptedException {
        final Set<CompletableFuture<?>> futures = ConcurrentHashMap.newKeySet();
        final AtomicLong runningFutures = new AtomicLong(1);
        final AtomicBoolean interruptFlag = new AtomicBoolean(false);
        DriverManager_lanzou.refreshDirectoryRecursively(this.configuration, this.configuration.getWebSide().getRootDirectoryId(), futures, runningFutures, interruptFlag);
        try {
            synchronized (runningFutures) {
                while (runningFutures.get() > 0)
                    runningFutures.wait();
            }
        } catch (final InterruptedException exception) {
            interruptFlag.set(true);
            throw exception;
        }
        for (final CompletableFuture<?> future: futures)
            try {
                future.join();
            } catch (final CancellationException ignore) {
            } catch (final CompletionException exception) {
                Throwable throwable;
                try {
                    throwable = HExceptionWrapper.unwrapException(exception.getCause(), IOException.class, SQLException.class, InterruptedException.class);
                } catch (final IOException | SQLException | InterruptedException e) {
                    throwable = e;
                }
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
            }
    }

    @Override
    public void forceRefreshDirectory(final @NotNull FileLocation location) throws IOException, SQLException, InterruptedException {
        DriverManager_lanzou.waitSyncComplete(DriverManager_lanzou.syncFilesList(this.configuration, location.id(), null));
    }

    @Override
    public Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull FileLocation location, final Options.@NotNull DirectoriesOrFiles filter, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IOException, SQLException, InterruptedException {
        return DriverManager_lanzou.listFiles(this.configuration, location.id(), filter, limit, page, policy, direction, null);
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull FileLocation location) throws IOException, SQLException, InterruptedException {
        return DriverManager_lanzou.getFileInformation(this.configuration, location.id(), null, null);
    }

    @Override
    public @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> download(final @NotNull FileLocation location, final long from, final long to) throws IOException, SQLException, InterruptedException {
        return DriverManager_lanzou.getDownloadMethods(this.configuration, location.id(), from, to, null);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IOException, SQLException, InterruptedException {
        return DriverManager_lanzou.createDirectory(this.configuration, parentLocation.id(), directoryName, policy, null);
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        return DriverManager_lanzou.getUploadMethods(this.configuration, parentLocation.id(), filename, md5, size, policy, null);
    }

    @Override
    public void delete(final @NotNull FileLocation location) throws IOException, SQLException, InterruptedException {
        final FileSqlInformation information = DriverManager_lanzou.getFileInformation(this.configuration, location.id(), null, null);
        if (information != null)
            DriverManager_lanzou.trashFile(this.configuration, information, null, null);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        return DriverInterface.super.copy(sourceLocation, targetParentLocation, targetFilename, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetLocation, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        return DriverInterface.super.move(sourceLocation, targetLocation, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        return DriverInterface.super.rename(sourceLocation, name, policy);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_lanzou{" +
                "configuration=" + this.configuration +
                '}';
    }
}
