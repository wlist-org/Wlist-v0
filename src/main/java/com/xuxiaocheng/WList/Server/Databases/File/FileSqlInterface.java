package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;

public interface FileSqlInterface extends DatabaseInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;
    void deleteTable(final @Nullable String _connectionId) throws SQLException;
    @Contract(pure = true) @NotNull String getProviderName();
    @Contract(pure = true) long getRootId();


    /* --- Insert --- */

    /**
     * Insert file information / empty directory information. And auto update parents' size recursively.
     * WARNING: ONLY be called when uploading file / creating directory.
     * {@code assert information.isDirectory() ? information.size() == 0 : information.size() >= 0;}
     * @see #insertFilesSameDirectory(Iterator, long, String)
     */
    void insertFileOrDirectory(final @NotNull FileInformation information, final @Nullable String _connectionId) throws SQLException;

    /**
     * Build directory index. And auto update parents' size recursively.
     * WARNING: ONLY be called when first listing the directory.
     *
     * @see #insertFileOrDirectory(FileInformation, String)
     */
    void insertFilesSameDirectory(final @NotNull Iterator<@NotNull FileInformation> iterator, final long directoryId, final @Nullable String _connectionId) throws SQLException;


    /* --- Update --- */

    void updateDirectorySizeRecursively(final long  directoryId, final long delta, final @Nullable String _connectionId) throws SQLException;


    /* --- Select --- */

    @Nullable FileInformation selectFile(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException;

    @NotNull FilesListInformation selectFilesInDirectory(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;

    /* --- Delete --- */

    boolean deleteFile(final long fileId, final @Nullable String _connectionId) throws SQLException;

    long deleteDirectoryRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException;

    /* --- Search --- */


//    void updateDirectoryType(final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException;
//    @Nullable FileInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;
//    @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;
//    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
//    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
//    Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> selectFilesByParentIdInPage(final long parentId, final Options.@NotNull FilterPolicy filter, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException;

//
//    @Nullable Long updateDirectorySize(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException;
//    @Nullable Long calculateDirectorySizeRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException;

    // TODO Search
}
