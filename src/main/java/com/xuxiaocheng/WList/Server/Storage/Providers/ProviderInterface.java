package com.xuxiaocheng.WList.Server.Storage.Providers;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Ranges.LongRange;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.DuplicatePolicy;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import com.xuxiaocheng.WList.Server.Storage.Records.RefreshRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @see AbstractIdBaseProvider
 * @see com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager#onFileUpdate(FileLocation, boolean)
 * @see com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager#onFileTrash(FileLocation, boolean)
 * @see com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager#onFileUpload(String, FileInformation)
 */
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

    @NotNull UnionPair<Optional<UnionPair<FilesListInformation, RefreshRequirements>>, Throwable> ListNotExisted = UnionPair.ok(Optional.empty());
    @NotNull UnionPair<Optional<UnionPair<FilesListInformation, RefreshRequirements>>, Throwable> ListEmpty = UnionPair.ok(Optional.of(UnionPair.ok(FilesListInformation.Empty)));
    /**
     * Get the list of files in directory.
     * @param consumer empty: directory is not available / does not exist in web server. success: list of files. failure: need build index.
     * @see #ListNotExisted
     */
    void list(final long directoryId, final @NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FilesListInformation, RefreshRequirements>>, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Optional<FileInformation>, Throwable> InfoNotExisted = UnionPair.ok(Optional.empty());
    /**
     * Get the file/directory information of a specific id.
     * @param consumer empty: file/directory is not available / does not exist in web server. present: information.
     * @see #InfoNotExisted
     */
    void info(final long id, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Optional<FileInformation>, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Optional<RefreshRequirements>, Throwable> RefreshNoRequire = UnionPair.ok(Optional.of(RefreshRequirements.NoRequired));
    @NotNull UnionPair<Optional<RefreshRequirements>, Throwable> RefreshNotExisted = UnionPair.ok(Optional.empty());
    /**
     * Force rebuild (or build) files index to synchronize with web server (not recursively).
     * @param consumer empty: directory is not available / does not exist in web server. present: success.
     * @see #RefreshNoRequire
     * @see #RefreshNotExisted
     */
    void refreshDirectory(final long directoryId, final @NotNull Consumer<? super @NotNull UnionPair<Optional<RefreshRequirements>, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Optional<Boolean>, Throwable> TrashTooComplex = UnionPair.ok(Optional.empty());
    @NotNull UnionPair<Optional<Boolean>, Throwable> TrashNotExisted = UnionPair.ok(Optional.of(Boolean.FALSE));
    @NotNull UnionPair<Optional<Boolean>, Throwable> TrashSuccess = UnionPair.ok(Optional.of(Boolean.TRUE));
    /**
     * Delete file/directory.
     * @param consumer empty: not supported. false: file/directory is not available. true: deleted.
     * @see #TrashTooComplex
     * @see #TrashNotExisted
     * @see #TrashSuccess
     */
    void trash(final long id, final boolean isDirectory, final @NotNull Consumer<? super @NotNull UnionPair<Optional<Boolean>, Throwable>> consumer) throws Exception;

    /**
     * Get download methods of a specific file.
     */
    void downloadFile(final long fileId, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<DownloadRequirements, FailureReason>, Throwable>> consumer) throws Exception;

    /**
     * Create an empty directory.
     */
    void createDirectory(final long parentId, final @NotNull String directoryName, final @NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<FileInformation, FailureReason>, Throwable>> consumer) throws Exception;

    /**
     * Upload a file.
     */
    void uploadFile(final long parentId, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<UnionPair<UploadRequirements, FailureReason>, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable> CMTooComplex = UnionPair.ok(Optional.empty());
    @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable> CMToInside = UnionPair.ok(Optional.of(UnionPair.fail(Optional.empty())));

    /**
     * Copy a file/directory directly.
     * @see #CMTooComplex
     * @see #CMToInside
     */
    void copyDirectly(final long id, final boolean isDirectory, final long parentId, final @NotNull String name, final @NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) throws Exception;

    /**
     * Move a file/directory.
     * @see #CMTooComplex
     * @see #CMToInside
     */
    void moveDirectly(final long id, final boolean isDirectory, final long parentId, final @NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, Optional<FailureReason>>>, Throwable>> consumer) throws Exception;

    @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable> RenameTooComplex = UnionPair.ok(Optional.empty());
    /**
     * Rename a file/directory.
     * @see #RenameTooComplex
     */
    void renameDirectly(final long id, final boolean isDirectory, final @NotNull String name, final @NotNull DuplicatePolicy policy, final @NotNull Consumer<? super @NotNull UnionPair<Optional<UnionPair<FileInformation, FailureReason>>, Throwable>> consumer) throws Exception;
}
