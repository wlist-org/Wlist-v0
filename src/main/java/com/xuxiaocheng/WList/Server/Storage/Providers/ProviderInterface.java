package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ProviderInterface<C extends ProviderConfiguration> {
    /**
     * Get the type of the provider.
     */
    @Contract(pure = true)
    @NotNull ProviderTypes<?> getType();

    /**
     * Return the default configuration if this provider is not initialized.
     * Otherwise, return the current configuration.
     */
    @NotNull C getConfiguration();

    /**
     * Initialize the provider (and bind to the configuration). Create sql table in this method, etc.
     * When user modify the configuration, this method will be call again automatically.
     * @see FileManager#quicklyInitialize(String, SqlDatabaseInterface, long, String)
     */
    void initialize(final @NotNull C configuration) throws Exception;

    /**
     * Uninitialize this provider (and unbind to the configuration). Release buffers and close handles, etc.
     * Delete sql table in this method if {@code dropIndex} is TRUE.
     * @see FileManager#quicklyUninitialize(String, String)
     */
    void uninitialize(final boolean dropIndex) throws Exception;

//    /**
//     * Build provider cache. Login and check token, etc.
//     * @see ProviderConfiguration#setLastFileCacheBuildTime(LocalDateTime)
//     */
//    void buildCache() throws Exception;
//
//    /**
//     * Build all files index into sql database. (Recursive from root.)
//     * @see FileManager
//     * @see ProviderConfiguration#setLastFileIndexBuildTime(LocalDateTime)
//     */
//    void buildIndex() throws Exception;
//
//    /**
//     * Force rebuild files index into sql database to synchronize with web server. (Not recursively)
//     * @param location The directory location to refresh.
//     * @see FileManager
//     */
//    void refreshDirectory(final @NotNull FileLocation location) throws Exception;
//
//    /**
//     * Get the list of files in directory.
//     * @param location The directory location.
//     * @return null: directory is not available. !null: list of files.
//     */
//    @Nullable FilesListInformation list(final @NotNull FileLocation location, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final @LongRange(minimum = 0) long position, final @IntRange(minimum = 0) int limit) throws Exception;
//
//    /**
//     * Get the file information of a specific file.
//     * @param location The file location to get information.
//     * @return The file information. Null means not existed.
//     */
//    @Nullable FileInformation info(final @NotNull FileLocation location) throws Exception;
//
//    /**
//     * Get download methods of a specific file.
//     * @see DownloadRequirements#tryGetDownloadFromUrl(OkHttpClient, HttpUrl, Headers, Long, Headers.Builder, long, long, LocalDateTime)
//     */
//    @NotNull UnionPair<DownloadRequirements, FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception;
//
//    /**
//     * Create an empty directory.
//     * @see com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil#getRetryWrapper(String)
//     */
//    @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception;
//
//    /**
//     * Upload file.
//     */
//    @NotNull UnionPair<UploadRequirements, FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception;

//    /**
//     * Delete file.
//     * @param location The file location to delete.
//     * @throws Exception Something went wrong.
//     */
//    void delete(final @NotNull FileLocation location) throws Exception;
//
//    /**
//     * Copy file.
//     * @param sourceLocation The file location to copy.
//     * @param targetParentLocation The target parent directory location.
//     * @param targetFilename The name of target file.
//     * @param policy Duplicate policy.
//     * @return The information of new file.
//     * @throws Exception Something went wrong.
//     */
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    default @NotNull UnionPair<FileInformation, FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Copying by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("targetFilename", targetFilename).add("policy", policy));
//        final FileInformation source = this.info(sourceLocation);
//        if (source == null || source.isDirectory())
//            return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", sourceLocation));
//        Runnable uploadFinisher = null;
//        Runnable downloadFinisher = null;
//        try {
//            final UnionPair<UploadRequirements.UploadMethods, FailureReason> upload = this.upload(targetParentLocation, targetFilename, source.size(), source.md5(), policy);
//            if (upload.isFailure())
//                return UnionPair.fail(upload.getE());
//            uploadFinisher = upload.getT().finisher();
//            if (!upload.getT().methods().isEmpty()) {
//                final UnionPair<DownloadMethods, FailureReason> download = this.download(sourceLocation, 0, Long.MAX_VALUE);
//                if (download.isFailure())
//                    return UnionPair.fail(download.getE());
//                downloadFinisher = download.getT().finisher();
//                assert source.size() == download.getT().total();
//                if (upload.getT().methods().size() != download.getT().methods().size())
//                    throw new AssertionError("Copying. Same size but different methods count." + ParametersMap.create()
//                            .add("size_uploader", source.size()).add("size_downloader", download.getT().total())
//                            .add("downloader", download.getT().methods().size()).add("uploader", upload.getT().methods().size())
//                            .add("source", source).add("target", targetParentLocation).add("filename", targetFilename).add("policy", policy));
//                final Iterator<ConsumerE<ByteBuf>> uploadIterator = upload.getT().methods().iterator();
//                final Iterator<SupplierE<ByteBuf>> downloadIterator = download.getT().methods().iterator();
//                while (uploadIterator.hasNext() && downloadIterator.hasNext())
//                    uploadIterator.next().accept(downloadIterator.next().get());
//                assert !uploadIterator.hasNext() && !downloadIterator.hasNext();
//            }
//            final FileInformation information = upload.getT().supplier().get();
//            if (information == null)
//                throw new IllegalStateException("Failed to copy file. Failed to get target file information." + ParametersMap.create()
//                        .add("sourceLocation", sourceLocation).add("sourceInfo", source).add("targetParentLocation", targetParentLocation).add(targetFilename, "targetFilename").add("policy", policy));
//            return UnionPair.ok(information);
//        } finally {
//            if (uploadFinisher != null)
//                uploadFinisher.run();
//            if (downloadFinisher != null)
//                downloadFinisher.run();
//        }
//    }
//
//    /**
//     * Move file/directory.
//     * @param sourceLocation The file/directory location to move.
//     * @param targetParentLocation The target directory location.
//     * @param policy Duplicate policy.
//     * @return The information of new file/directory.
//     * @throws Exception Something went wrong.
//     */
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    default @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Moving by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("policy", policy));
//        final FileInformation source = this.info(sourceLocation);
//        if (source == null)
//            return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceLocation));
//        if (source.isDirectory()) {
//            final UnionPair<FileInformation, FailureReason> directory = this.createDirectory(targetParentLocation, source.name(), policy);
//            if (directory.isFailure())
//                return UnionPair.fail(directory.getE());
//            Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
//            do {
//                list = this.list(sourceLocation, Options.FilterPolicy.OnlyDirectories, ProviderUtil.DefaultLimitPerRequestPage, 0, ProviderUtil.DefaultOrderPolicy, ProviderUtil.DefaultOrderDirection);
//                if (list == null)
//                    return directory;
//                for (final FileInformation f: list.getC())
//                    this.move(f.location(), directory.getT().location(), policy);
//            } while (list.getB().longValue() > 0);
//            this.moveFilesInDirectory(sourceLocation, policy, directory);
//            return directory;
//        }
//        if (source.location().equals(targetParentLocation))
//            return UnionPair.ok(source);
//        final UnionPair<FileInformation, FailureReason> information = this.copy(sourceLocation, targetParentLocation, source.name(), policy);
//        if (information.isFailure())
//            return UnionPair.fail(information.getE());
//        try {
//            this.delete(sourceLocation);
//        } catch (final Exception exception) {
//            try {
//                this.delete(targetParentLocation);
//            } catch (final Exception e) {
//                throw new IllegalStateException("Failed to delete target file after a failed deletion of source file when moving file by default algorithm." +
//                        ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("policy", policy).add("exception", exception), e);
//            }
//            throw exception;
//        }
//        return information;
//    }
//
//    /**
//     * Rename file/directory.
//     * @param sourceLocation The file/directory location to move.
//     * @param name The name of new file/directory.
//     * @param policy Duplicate policy.
//     * @return The information of new file/directory.
//     * @throws Exception Something went wrong.
//     */
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    default @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Renaming by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("name", name).add("policy", policy));
//        final FileInformation source = this.info(sourceLocation);
//        if (source == null)
//            return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", sourceLocation));
//        if (source.isDirectory()) {
//            final UnionPair<FileInformation, FailureReason> directory = this.createDirectory(new FileLocation(sourceLocation.storage(), source.parentId()), name, policy);
//            if (directory.isFailure())
//                return UnionPair.fail(directory.getE());
//            this.moveFilesInDirectory(sourceLocation, policy, directory);
//            this.delete(sourceLocation);
//            return directory;
//        }
//        if (source.name().equals(name))
//            return UnionPair.ok(source);
//        final UnionPair<FileInformation, FailureReason> information = this.copy(sourceLocation, sourceLocation, name, policy);
//        if (information.isFailure())
//            return UnionPair.fail(information.getE());
//        try {
//            this.delete(sourceLocation);
//        } catch (final Exception exception) {
//            try {
//                this.delete(information.getT().location());
//            } catch (final Exception e) {
//                throw new IllegalStateException("Failed to delete target file after a failed deletion of source file when renaming file by default algorithm." +
//                        ParametersMap.create().add("sourceLocation", sourceLocation).add("name", name).add("targetLocation", information.getT().location()).add("policy", policy).add("exception", exception), e);
//            }
//            throw exception;
//        }
//        return information;
//    }
//
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    private void moveFilesInDirectory(final @NotNull FileLocation sourceLocation, final Options.@NotNull DuplicatePolicy policy, final @NotNull UnionPair<FileInformation, FailureReason> directory) throws Exception {
//        Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
//        do {
//            list = this.list(sourceLocation, Options.FilterPolicy.Both, 10, 0, ProviderUtil.DefaultOrderPolicy, ProviderUtil.DefaultOrderDirection);
//            if (list == null)
//                break;
//            final CountDownLatch latch = new CountDownLatch(list.getC().size());
//            for (final FileInformation f: list.getC())
//                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> this.move(f.location(), directory.getT().location(), policy),
//                        latch::countDown), WListServer.ServerExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
//            latch.await();
//        } while (list.getA().longValue() > 0);
//    }
}
