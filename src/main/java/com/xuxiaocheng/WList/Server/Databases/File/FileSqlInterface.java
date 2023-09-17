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
     * {@code assert information.parentId() == directoryId && (information.isDirectory() || information.size() >= 0);}
     * @see #insertFileOrDirectory(FileInformation, String)
     */
    void insertFilesSameDirectory(final @NotNull Iterator<@NotNull FileInformation> iterator, final long directoryId, final @Nullable String _connectionId) throws SQLException;


    /* --- Update --- */

    /**
     * Update directory size recursively.
     */
    void updateDirectorySizeRecursively(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException;


    /* --- Select --- */

    /**
     * Select file by id.
     * @return null: not found. !null: found.
     */
    @Nullable FileInformation selectFile(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select all files in directory.
     */
    @NotNull FilesListInformation selectFilesInDirectory(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;


    /* --- Delete --- */

    boolean deleteFile(final long fileId, final @Nullable String _connectionId) throws SQLException;

    long deleteDirectoryRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException;


    /* --- Search --- */


//    @Nullable FileInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;

    // TODO Search
}
