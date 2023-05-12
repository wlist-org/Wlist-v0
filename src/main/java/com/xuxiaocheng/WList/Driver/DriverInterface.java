package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Driver.Utils.FileInformation;
import com.xuxiaocheng.WList.Utils.DataBaseUtil;
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
    void initiate(final @NotNull C configuration) throws Exception;

    /**
     * Completely uninitiate this driver. (cleaner/deleter)
     * @throws Exception Something went wrong.
     */
    void uninitiate() throws Exception;

    /**
     * Login the web server. Check token etc.
     * @throws Exception Something went wrong.
     */
    void buildCache() throws Exception;

    /**
     * Build file index into sql database {@link DataBaseUtil#getIndexInstance()}.
     * @throws Exception Something went wrong.
     * @see com.xuxiaocheng.WList.Driver.Helpers.DriverSqlHelper
     */
    void buildIndex() throws Exception;

    /**
     * Get the list of files in this directory.
     * @param path The directory path to get files list.
     * @param page The page of the list.
     * @param limit Max length in one page.
     * @return Integer means all files in directory count. The second is the files list. The String in the list means file name.
     *          Null means directory is not available.
     * @throws Exception Something went wrong.
     */
    Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull @UnmodifiableView List<@NotNull FileInformation>> list(final @NotNull DrivePath path, final int limit, final int page,
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
     * @param from The stream start byte.
     * @param to The stream stop byte.
     * @return The download stream and real available bytes. Null means not existed.
     * @throws Exception Something went wrong.
     */
    Pair.@Nullable ImmutablePair<@NotNull InputStream, @NotNull Long> download(final @NotNull DrivePath path, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception;

    /**
     * Create a new empty directory.
     * @param path The directory path to create.
     * @return The information of new directory. Null means failure. (Invalid filename.)
     * @throws Exception Something went wrong.
     */
    @Nullable FileInformation mkdirs(final @NotNull DrivePath path) throws Exception;

    /**
     * Upload file to path.
     * @param path Target path.
     * @param stream Content stream of file.
     * @param tag File md5.
     * @param partTags File md5 of per 4 MB file part.
     * @return The information of new file. Null means failure. (Invalid filename.)
     * @throws Exception Something went wrong.
     */
    @Nullable FileInformation upload(final @NotNull DrivePath path, final @NotNull InputStream stream, final @NotNull String tag, final @NotNull List<@NotNull String> partTags) throws Exception;

    /**
     * Delete file.
     * @param path The file path.
     * @throws Exception Something went wrong.
     */
    void delete(final @NotNull DrivePath path) throws Exception;

    @SuppressWarnings("OverlyBroadThrowsClause")
    default @Nullable FileInformation copy(final @NotNull DrivePath source, final @NotNull DrivePath target) throws Exception {
        final Pair.ImmutablePair<InputStream, Long> url = this.download(source, 0, Long.MAX_VALUE);
        final FileInformation info = this.info(source);
        if (url == null || info == null)
            return null;
        assert info.size() == url.getSecond().longValue();
        if (info.size() > 4 << 20)
            throw new UnsupportedOperationException();
        final InputStream inputStream = url.getFirst();
        final FileInformation t =  this.upload(target, inputStream, info.tag(), List.of(info.tag()));
        inputStream.close();
        return t;
    }

    default @Nullable FileInformation move(final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory) throws Exception {
        if (targetDirectory.equals(sourceFile.getParent()))
            return this.info(sourceFile);
        final FileInformation t = this.copy(sourceFile, targetDirectory.getChild(sourceFile.getName()));
        if (t == null)
            return null;
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
}
