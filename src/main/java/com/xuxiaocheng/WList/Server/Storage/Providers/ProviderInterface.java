package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Consumer;

public interface ProviderInterface<C extends StorageConfiguration> {
    /**
     * Get the type of the provider.
     */
    @Contract(pure = true)
    @NotNull StorageTypes<C> getType();

    /**
     * Throw if this provider is not initialized.
     * Otherwise, return the current configuration.
     * @see HInitializer#getInstance()
     */
    @NotNull C getConfiguration();

    /**
     * Initialize the provider (and bind to the configuration). Create sql table in this method, etc.
     * NOTICE: Do NOT request a network in this method.
     * @see FileManager#quicklyInitialize(String, SqlDatabaseInterface, long, String)
     */
    void initialize(final @NotNull C configuration) throws Exception;

    /**
     * Uninitialize this provider (and unbind to the configuration). Release buffers and close handles, etc.
     * Delete sql table in this method if {@code dropIndex} is TRUE.
     * @see FileManager#quicklyUninitialize(String, String)
     */
    void uninitialize(final boolean dropIndex) throws Exception;

    @NotNull UnionPair<Optional<FilesListInformation>, Throwable> ListNotExisted = UnionPair.ok(Optional.empty());
    /**
     * Get the list of files in directory.
     * @param consumer empty: directory is not available / is not existed in web server. present: list of files.
     */
    void list(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @NotNull Consumer<? super @NotNull UnionPair<Optional<FilesListInformation>, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Optional<FileInformation>, Throwable> InfoNotExisted = UnionPair.ok(Optional.empty());
    /**
     * Get the file/directory information of a specific id.
     * @param consumer empty: file/directory is not available / is not existed in web server. present: information.
     */
    void info(final long id, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Optional<FileInformation>, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Boolean, Throwable> RefreshNotExisted = UnionPair.ok(Boolean.FALSE);
    @NotNull UnionPair<Boolean, Throwable> RefreshSuccess = UnionPair.ok(Boolean.TRUE);
    /**
     * Force rebuild (or build) files index to synchronize with web server (not recursively).
     * @param consumer false: directory is not available / is not existed in web server. true: success
     */
    void refreshDirectory(final long directoryId, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Boolean, Throwable> TrashNotSupported = UnionPair.ok(Boolean.FALSE);
    @NotNull UnionPair<Boolean, Throwable> TrashSuccess = UnionPair.ok(Boolean.TRUE);
    /**
     * Delete file/directory.
     * @param consumer empty: not supported. false: file/directory is not available. true: deleted.
     */
    void trash(final long id, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Boolean, Throwable>> consumer) throws Exception;

    /**
     * Get download methods of a specific file.
     * @param location Only by used to create {@code FailureReason}.
     */
    void downloadFile(final long fileId, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation location) throws Exception;

    /**
     * Create an empty directory.
     * @param parentLocation Only by used to create {@code FailureReason}.
     */
    void createDirectory(final long parentId, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) throws Exception;

    /**
     * Upload a file.
     * @param parentLocation Only by used to create {@code FailureReason}.
     */
    void uploadFile(final long parentId, final @NotNull String filename, final @LongRange(minimum = 0) long size, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer, final @NotNull FileLocation parentLocation) throws Exception;

    @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable> CopyNotSupported = UnionPair.ok(Optional.empty());
    @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable> CopySelf = UnionPair.ok(Optional.of(UnionPair.ok(Optional.empty())));
    /**
     * Copy a file directly. (Do NOT download and then upload. That should be done in client side.)
     * @param location Source file location. Only by used to create {@code FailureReason}.
     * @param parentLocation Target parent location. Only by used to create {@code FailureReason}.
     */
    void copyFileDirectly(final long fileId, final long parentId, final @NotNull String filename, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception;

    @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable> MoveNotSupported = UnionPair.ok(Optional.empty());
    @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable> MoveSelf = UnionPair.ok(Optional.of(UnionPair.ok(Optional.empty())));
    /**
     * Move a file/directory. (Do NOT download and then upload. That should be done in client side.)
     * @param location Source file location. Only by used to create {@code FailureReason}.
     * @param parentLocation Target parent location. Only by used to create {@code FailureReason}.
     */
    void moveDirectly(final long id, final boolean isDirectory, final long parentId, final Options.@NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<Optional<FileInformation>, FailureReason>>, Throwable>> consumer, final @NotNull FileLocation location, final @NotNull FileLocation parentLocation) throws Exception;

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
