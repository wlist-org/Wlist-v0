package com.xuxiaocheng.WList.Server.Driver.WebDrivers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Server.Driver.FailureReason;
import com.xuxiaocheng.WList.Server.Driver.FileLocation;
import com.xuxiaocheng.WList.Server.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Server.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Server.Driver.Options;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.Handlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public interface DriverInterface<C extends DriverConfiguration<?, ?, ?>> {
    /**
     * Get default configuration instance to initialize.
     * Also be used to serialize.
     */
    @NotNull C getConfiguration();

    /**
     * Initialize the web driver. (and bind to the configuration.) Create sql table in this method.
     * When user modify the configuration, this method will be call again automatically.
     * @param configuration The modified configuration.
     * @see FileManager#quicklyInitialize(FileSqlInterface, String)
     * @throws Exception Something went wrong.
     */
    void initialize(final @NotNull C configuration) throws Exception;

    /**
     * Completely uninitialize this driver. (cleaner/deleter) Delete sql table in this method.
     * Only be called when {@link com.xuxiaocheng.WList.Server.GlobalConfiguration#deleteDriver()} is true.
     * @see FileManager#quicklyUninitialize(String, String)
     * @throws Exception Something went wrong.
     */
    void uninitialize() throws Exception;

    /**
     * Login the web server. Check token etc.
     * @throws Exception Something went wrong.
     * @see DriverConfiguration.CacheSideDriverConfiguration#setLastFileCacheBuildTime(LocalDateTime)
     */
    void buildCache() throws Exception;

    /**
     * Build file index into sql database.
     * @throws Exception Something went wrong.
     * @see FileManager
     * @see DriverConfiguration.CacheSideDriverConfiguration#setLastFileIndexBuildTime(LocalDateTime)
     */
    void buildIndex() throws Exception;

    /**
     * Force refresh cache to synchronize with web server. (Not recursively)
     * @param location The directory location to refresh.
     * @throws Exception Something went wrong.
     * @see FileManager
     */
    default void forceRefreshDirectory(final @NotNull FileLocation location) throws Exception {
    }

    /**
     * Get the list of files in directory.
     * @param location The directory location to get files list.
     * @param filter Directories or files filter.
     * @param page The page of the list.
     * @param limit Max length in one page.
     * @param policy Sort order policy.
     * @param direction Sort order direction.
     * @return The A {@code long} is files count in directory. The B {@code long} is files count after applied filter. The C {@code list} is the files list. Null means directory is not available.
     * @throws Exception Something went wrong.
     */
    Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull FileLocation location, final Options.@NotNull DirectoriesOrFiles filter, final @LongRange(minimum = 0) int limit, final @LongRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception;

    /**
     * Get the file information of a specific file.
     * @param location The file location to get information.
     * @return The file information. Null means not existed.
     * @throws Exception Something went wrong.
     */
    @Nullable FileSqlInformation info(final @NotNull FileLocation location) throws Exception;

    /**
     * Get download methods of a specific file.
     * @param location The file location to download.
     * @param from The stream start byte.
     * @param to The stream stop byte.
     * @return Download methods for every 4MB ({@link com.xuxiaocheng.Rust.NetworkTransmission#FileTransferBufferSize}) chunks of file.
     * @see DriverUtil#getDownloadMethodsByUrlWithRangeHeader(OkHttpClient, Pair.ImmutablePair, Headers, long, long, long, Headers.Builder)
     * @see DriverUtil#toCachedDownloadMethods(DownloadMethods) (Suggest)
     * @throws Exception Something went wrong.
     */
    @NotNull UnionPair<DownloadMethods, FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception;

    /**
     * Create an empty directory.
     * @param parentLocation The parent directory location.
     * @param directoryName The name of directory to create.
     * @param policy Duplicate policy.
     * @return The information of new directory.
     * @see DriverUtil#getRetryWrapper(String)
     * @throws Exception Something went wrong.
     */
    @NotNull UnionPair<FileSqlInformation, FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    /**
     * Upload file.
     * @param parentLocation The parent directory location.
     * @param filename The name of file to create.
     * @param size File size.
     * @param md5  File md5.
     * @param policy Duplicate policy.
     * @return Upload methods for every 4MB ({@link com.xuxiaocheng.Rust.NetworkTransmission#FileTransferBufferSize}) chunks of file.
     * @see DriverUtil#getRetryWrapper(String)
     * @see DriverNetworkHelper#createOctetStreamRequestBody
     * @see DriverUtil#splitUploadMethodEveryFileTransferBufferSize(ConsumerE, int)
     * @throws Exception Something went wrong.
     */
    @NotNull UnionPair<UploadMethods, FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    /**
     * Delete file.
     * @param location The file location to delete.
     * @throws Exception Something went wrong.
     */
    void delete(final @NotNull FileLocation location) throws Exception;

    /**
     * Copy file.
     * @param sourceLocation The file location to copy.
     * @param targetParentLocation The target parent directory location.
     * @param targetFilename The name of target file.
     * @param policy Duplicate policy.
     * @return The information of new file.
     * @throws Exception Something went wrong.
     */
    @SuppressWarnings("OverlyBroadThrowsClause")
    default @NotNull UnionPair<FileSqlInformation, FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Copying by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("targetFilename", targetFilename).add("policy", policy));
        final FileSqlInformation source = this.info(sourceLocation);
        if (source == null || source.isDirectory())
            return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", sourceLocation));
        Runnable uploadFinisher = null;
        Runnable downloadFinisher = null;
        try {
            final UnionPair<UploadMethods, FailureReason> upload = this.upload(targetParentLocation, targetFilename, source.size(), source.md5(), policy);
            if (upload.isFailure())
                return UnionPair.fail(upload.getE());
            uploadFinisher = upload.getT().finisher();
            if (!upload.getT().methods().isEmpty()) {
                final UnionPair<DownloadMethods, FailureReason> download = this.download(sourceLocation, 0, Long.MAX_VALUE);
                if (download.isFailure())
                    return UnionPair.fail(download.getE());
                downloadFinisher = download.getT().finisher();
                assert source.size() == download.getT().total();
                if (upload.getT().methods().size() != download.getT().methods().size())
                    throw new AssertionError("Copying. Same size but different methods count." + ParametersMap.create()
                            .add("size_uploader", source.size()).add("size_downloader", download.getT().total())
                            .add("downloader", download.getT().methods().size()).add("uploader", upload.getT().methods().size())
                            .add("source", source).add("target", targetParentLocation).add("filename", targetFilename).add("policy", policy));
                final Iterator<ConsumerE<ByteBuf>> uploadIterator = upload.getT().methods().iterator();
                final Iterator<SupplierE<ByteBuf>> downloadIterator = download.getT().methods().iterator();
                while (uploadIterator.hasNext() && downloadIterator.hasNext())
                    uploadIterator.next().accept(downloadIterator.next().get());
                assert !uploadIterator.hasNext() && !downloadIterator.hasNext();
            }
            final FileSqlInformation information = upload.getT().supplier().get();
            if (information == null)
                throw new IllegalStateException("Failed to copy file. Failed to get target file information." + ParametersMap.create()
                        .add("sourceLocation", sourceLocation).add("sourceInfo", source).add("targetParentLocation", targetParentLocation).add(targetFilename, "targetFilename").add("policy", policy));
            return UnionPair.ok(information);
        } finally {
            if (uploadFinisher != null)
                uploadFinisher.run();
            if (downloadFinisher != null)
                downloadFinisher.run();
        }
    }

    /**
     * Move file/directory.
     * @param sourceLocation The file/directory location to move.
     * @param targetParentLocation The target directory location.
     * @param policy Duplicate policy.
     * @return The information of new file/directory.
     * @throws Exception Something went wrong.
     */
    @SuppressWarnings("OverlyBroadThrowsClause")
    default @NotNull UnionPair<FileSqlInformation, FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Moving by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("policy", policy));
        final FileSqlInformation source = this.info(sourceLocation);
        if (source == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceLocation));
        if (source.isDirectory()) {
            final UnionPair<FileSqlInformation, FailureReason> directory = this.createDirectory(targetParentLocation, source.name(), policy);
            if (directory.isFailure())
                return UnionPair.fail(directory.getE());
            Triad.ImmutableTriad<Long, Long, List<FileSqlInformation>> list;
            do {
                list = this.list(sourceLocation, Options.DirectoriesOrFiles.OnlyDirectories, DriverUtil.DefaultLimitPerRequestPage, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection);
                if (list == null)
                    return directory;
                for (final FileSqlInformation f: list.getC())
                    this.move(f.location(), directory.getT().location(), policy);
            } while (list.getB().longValue() > 0);
            this.moveFilesInDirectory(sourceLocation, policy, directory);
            return directory;
        }
        if (source.location().equals(targetParentLocation))
            return UnionPair.ok(source);
        final UnionPair<FileSqlInformation, FailureReason> information = this.copy(sourceLocation, targetParentLocation, source.name(), policy);
        if (information.isFailure())
            return UnionPair.fail(information.getE());
        try {
            this.delete(sourceLocation);
        } catch (final Exception exception) {
            try {
                this.delete(targetParentLocation);
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to delete target file after a failed deletion of source file when moving file by default algorithm." +
                        ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("policy", policy).add("exception", exception), e);
            }
            throw exception;
        }
        return information;
    }

    /**
     * Rename file/directory.
     * @param sourceLocation The file/directory location to move.
     * @param name The name of new file/directory.
     * @param policy Duplicate policy.
     * @return The information of new file/directory.
     * @throws Exception Something went wrong.
     */
    @SuppressWarnings("OverlyBroadThrowsClause")
    default @NotNull UnionPair<FileSqlInformation, FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Renaming by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("name", name).add("policy", policy));
        final FileSqlInformation source = this.info(sourceLocation);
        if (source == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", sourceLocation));
        if (source.isDirectory()) {
            final UnionPair<FileSqlInformation, FailureReason> directory = this.createDirectory(new FileLocation(sourceLocation.driver(), source.parentId()), name, policy);
            if (directory.isFailure())
                return UnionPair.fail(directory.getE());
            this.moveFilesInDirectory(sourceLocation, policy, directory);
            this.delete(sourceLocation);
            return directory;
        }
        if (source.name().equals(name))
            return UnionPair.ok(source);
        final UnionPair<FileSqlInformation, FailureReason> information = this.copy(sourceLocation, sourceLocation, name, policy);
        if (information.isFailure())
            return UnionPair.fail(information.getE());
        try {
            this.delete(sourceLocation);
        } catch (final Exception exception) {
            try {
                this.delete(information.getT().location());
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to delete target file after a failed deletion of source file when renaming file by default algorithm." +
                        ParametersMap.create().add("sourceLocation", sourceLocation).add("name", name).add("targetLocation", information.getT().location()).add("policy", policy).add("exception", exception), e);
            }
            throw exception;
        }
        return information;
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    private void moveFilesInDirectory(final @NotNull FileLocation sourceLocation, final Options.@NotNull DuplicatePolicy policy, final @NotNull UnionPair<FileSqlInformation, FailureReason> directory) throws Exception {
        Triad.ImmutableTriad<Long, Long, List<FileSqlInformation>> list;
        do {
            list = this.list(sourceLocation, Options.DirectoriesOrFiles.Both, 10, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection);
            if (list == null)
                break;
            final CountDownLatch latch = new CountDownLatch(list.getC().size());
            for (final FileSqlInformation f: list.getC())
                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> this.move(f.location(), directory.getT().location(), policy),
                        latch::countDown), WListServer.ServerExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
            latch.await();
        } while (list.getA().longValue() > 0);
    }
}
