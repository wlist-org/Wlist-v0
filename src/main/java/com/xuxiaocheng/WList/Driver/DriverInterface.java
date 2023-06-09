package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Utils.DatabaseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.InputStream;
import java.util.List;

public interface DriverInterface<C extends DriverConfiguration<?, ?, ?>> {
    /**
     * Init the web driver. (bind to the configuration.)
     * When user modify the configuration, this method will be call automatically.
     * @param configuration The modified configuration.
     * @throws Exception Something went wrong.
     */
    void initialize(final @NotNull C configuration) throws Exception;

    /**
     * Completely uninitialize this driver. (cleaner/deleter)
     * @throws Exception Something went wrong.
     */
    void uninitialize() throws Exception;

    /**
     * Login the web server. Check token etc.
     * @throws Exception Something went wrong.
     */
    void buildCache() throws Exception;

    /**
     * Build file index into sql database {@link DatabaseUtil#getInstance()}.
     * @throws Exception Something went wrong.
     * @see FileManager
     */
    void buildIndex() throws Exception;

    default void forceRefreshDirectory(final @NotNull DrivePath path) throws Exception {
        this.buildIndex();
    }

    /**
     * Get the list of files in this directory.
     * @param path The directory path to get files list.
     * @param page The page of the list.
     * @param limit Max length in one page.
     * @return Long means all files in directory count. The second is the files list. The String in the list means file name.
     *          Null means directory is not available.
     * @throws Exception Something went wrong.
     */
    Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull DrivePath path, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception;

    /**
     * Get the file information of a specific file.
     * @param path The file path to get information.
     * @return The file information. Null means not existed.
     * @throws Exception Something went wrong.
     */
    @Nullable FileSqlInformation info(final @NotNull DrivePath path) throws Exception;

    /**
     * Get download link of a specific file.
     * @param path The file path to download.
     * @param from The stream start byte.
     * @param to The stream stop byte.
     * @return The download stream and real available bytes. Null means not existed.
     * @throws Exception Something went wrong.
     */
    Pair.@Nullable ImmutablePair<@NotNull InputStream, @NotNull Long> download(final @NotNull DrivePath path, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception;

    /**
     * Create a new empty directory.
     * @param path The directory path to create.
     * @return The information of new directory.
     * @throws Exception Something went wrong.
     */
    @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> mkdirs(final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    /**
     * Upload file to path. {@link UploadMethods}
     * @param path Target path.
     * @param size File size.
     * @param md5  File md5.
     * @return Upload methods for chunks of file.
     * @throws Exception Something went wrong.
     */
    @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull DrivePath path, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    /**
     * Delete file.
     * @param path The file path.
     * @throws Exception Something went wrong.
     */
    void delete(final @NotNull DrivePath path) throws Exception;

    @SuppressWarnings("OverlyBroadThrowsClause")
    default @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> copy(final @NotNull DrivePath source, final @NotNull DrivePath target, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final FileSqlInformation info = this.info(source);
        if (info == null || info.isDir())
            return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", source));
        final Pair.ImmutablePair<InputStream, Long> url = this.download(source, 0, Long.MAX_VALUE);
        if (url == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", source));
        Runnable finisher = null;
        try {
            if (info.size() != url.getSecond().longValue())
                return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", source));
            final UnionPair<UploadMethods, FailureReason> methods = this.upload(target, info.size(), info.md5(), policy);
            finisher = methods.getT().finisher();
            if (methods.isFailure())
                return UnionPair.fail(methods.getE());
            for (final UploadMethods.UploadPartMethod partMethod: methods.getT().methods()) {
                final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(partMethod.size(), partMethod.size());
                try {
                    buffer.writeBytes(url.getFirst(), partMethod.size());
                    partMethod.consumer().accept(buffer);
                } finally {
                    buffer.release();
                }
            }
            final FileSqlInformation information = methods.getT().supplier().get();
            if (information == null)
                throw new IllegalStateException("Failed to copy file. [Unknown]. source: " + source + ", sourceInfo: " + info + ", target: " + target + ", policy: " + policy);
            return UnionPair.ok(information);
        } finally {
            url.getFirst().close();
            if (finisher != null)
                finisher.run();
        }
    }

    default @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (targetDirectory.equals(sourceFile.getParent())) {
            final FileSqlInformation information = this.info(sourceFile);
            if (information == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceFile));
            return UnionPair.ok(information);
        }
        final UnionPair<FileSqlInformation, FailureReason> t = this.copy(sourceFile, targetDirectory.getChild(sourceFile.getName()), policy);
        if (t.isFailure())
            return UnionPair.fail(t.getE());
        this.delete(sourceFile);
        return t;
    }

    default @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> rename(final @NotNull DrivePath source, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        if (source.getName().equals(name)){
            final FileSqlInformation information = this.info(source);
            if (information == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", source));
            return UnionPair.ok(information);
        }
        final UnionPair<FileSqlInformation, FailureReason> t = this.copy(source, source.getParent().child(name), policy);
        if (t.isFailure())
            return UnionPair.fail(t.getE());
        this.delete(source);
        return t;
    }
}
