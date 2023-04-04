package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface Driver<C extends DriverConfiguration> {
    /**
     * Login in the web server.
     * When user modify the configuration, this method will be call automatically.
     * @param info The old deserialized configuration.
     * @return The new configuration to serialize.
     * @throws Exception Something went wrong.
     */
    @NotNull C login(final @Nullable C info) throws Exception;

    /**
     * Get the list of files in this directory.
     * @param path The directory path to get files list.
     * @param page The page of the list.
     * @param limit Max length in one page.
     * @return Long means files count. The second is the files list. Null means directory is not available.
     *          The String in the list means file name.
     * @throws Exception Something went wrong.
     */
    @Nullable Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull List<@NotNull String>> list(final @NotNull DrivePath path, final int page, final int limit) throws Exception;

    /**
     * Get the size of a specific file.
     * @param path The file path to get size.
     * @return The file size. Null means not available. 0 means an empty file. -1 means a directory.
     * @throws Exception Something went wrong.
     */
    @Nullable Long size(final @NotNull DrivePath path) throws Exception;

    /**
     * Get download link of a specific file.
     * @param path The file path to download.
     * @return The download link. Null means failure.
     * @throws Exception Something went wrong.
     */
    @Nullable String download(final @NotNull DrivePath path) throws Exception;

    /**
     * Create an empty directory.
     * @param path The directory path to create.
     * @return The name of new directory. Null means failure.
     * @throws Exception Something went wrong.
     */
    @Nullable String mkdirs(final @NotNull DrivePath path) throws Exception;

    /**
     * Upload file to path.
     * @param path Target path.
     * @param file Content of file.
     * @return The name of new file. Null means failure.
     * @throws Exception Something went wrong.
     */
    @Nullable String upload(final @NotNull DrivePath path, final @NotNull InputStream file) throws Exception;//TODO Multi-thread upload

    /**
     * Delete file.
     * @param path The file path.
     * @throws Exception Something went wrong.
     */
    void delete(final @NotNull DrivePath path) throws Exception;

    default void rmdir(final @NotNull DrivePath path) throws Exception {
        this.delete(path);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    default @Nullable String copy(final @NotNull DrivePath source, final @NotNull DrivePath target) throws Exception {
        final String url = this.download(source);
        if (url == null)
            return null;
        final InputStream inputStream = Driver.downloadFromString(url);
        final String t =  this.upload(target, inputStream);
        inputStream.close();
        return t;
    }

    default @Nullable String move(final @NotNull DrivePath source, final @NotNull DrivePath target) throws Exception {
        final String t = this.copy(source, target);
        this.delete(source);
        return t;
    }

    default @Nullable String rename(final @NotNull DrivePath source, final @NotNull String name) throws Exception {
        return this.move(source, new DrivePath(source.getParent(), name));
    }

    static @NotNull InputStream downloadFromString(final @NotNull String url) throws IOException {
        return new URL(url).openConnection().getInputStream();
    }
}
