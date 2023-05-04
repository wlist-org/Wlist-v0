package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface DriverInterface<C extends DriverConfiguration<?, ?, ?>> {
    @SuppressWarnings("MethodReturnAlwaysConstant")
    @NotNull Class<C> getDefaultConfigurationClass();

    /**
     * Login in the web server.
     * When user modify the configuration, this method will be call automatically.
     * @param configuration The modified configuration.
     * @throws Exception Something went wrong.
     */
    void login(final @NotNull C configuration) throws Exception;

    /**
     * Completely delete this driver. (cleaner)
     * @throws Exception Something went wrong.
     */
    void deleteDriver() throws Exception;

    /**
     * Get the list of files in this directory.
     * @param path The directory path to get files list.
     * @param page The page of the list.
     * @param limit Max length in one page.
     * @return Integer means all files in directory count. The second is the files list. The String in the list means file name.
     *          Null means directory is not available.
     * @throws Exception Something went wrong.
     */
    Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> list(final @NotNull DrivePath path,final int limit, final int page,
                                                                                                 final @Nullable OrderDirection direction, final @Nullable OrderPolicy policy) throws Exception;

    /**
     * Get the file information of a specific file.
     * @param path The file path to get information.
     * @return The file information. Null means not existed.
     * @throws Exception Something went wrong.
     */
    @Nullable FileInformation info(final @NotNull DrivePath path) throws Exception;

    /**
     * Get download link of a specific file.
     * @param path The file path to download.
     * @return The download link. Null means not existed.
     * @throws Exception Something went wrong.
     */
    @Nullable String download(final @NotNull DrivePath path) throws Exception;

    /**
     * Create a new empty directory.
     * @param path The directory path to create.
     * @return The information of new directory.
     * @throws Exception Something went wrong.
     */
    @NotNull FileInformation mkdirs(final @NotNull DrivePath path) throws Exception;

    /**
     * Upload small file to path. (! Only small file)
     * @param path Target path.
     * @param file Content of file.
     * @return The information of new file.
     * @throws Exception Something went wrong.
     */
    @NotNull FileInformation upload(final @NotNull DrivePath path, final @NotNull ByteBuf file) throws Exception;

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
    default @Nullable FileInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target) throws Exception {
        final String url = this.download(source);
        if (url == null)
            return null;
        final InputStream inputStream = DriverInterface.downloadFromString(url);
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(inputStream.available());
        buffer.writeBytes(inputStream.readAllBytes());
        final FileInformation t =  this.upload(target, buffer);
        inputStream.close();
        return t;
    }

    default @Nullable FileInformation move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory) throws Exception {
        if (targetDirectory.equals(sourceFile.getParent()))
            return this.info(sourceFile);
        final FileInformation t = this.copy(sourceFile, targetDirectory.getChild(sourceFile.getName()));
        this.delete(sourceFile);
        return t;
    }

    default @Nullable FileInformation rename(final @NotNull DrivePath source, final @NotNull String name) throws Exception {
        if (source.getName().equals(name))
            return this.info(source);
        final FileInformation t = this.copy(source, source.getParent().child(name));
        this.delete(source);
        return t;
    }

    default void buildCache() throws Exception {
    }

    static @NotNull InputStream downloadFromString(final @NotNull String url) throws IOException {
        return new URL(url).openConnection().getInputStream();
    }
}
