package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
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
import java.util.Set;

public interface FileSqlInterface extends DatabaseInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;
    void deleteTable(final @Nullable String _connectionId) throws SQLException;
    @Contract(pure = true) @NotNull String getProviderName();
    @Contract(pure = true) long getRootId();


    /* --- Insert --- */

    /**
     * Insert file information / empty directory information. And auto update sizes of parents recursively.
     * WARNING: ONLY be called when uploading file / creating directory.
     * {@code assert information.isDirectory() ? information.size() == 0 : information.size() >= 0;}
     * @see #insertIterator(Iterator, long, String)
     * @see #updateOrInsertFile(FileInformation, String)
     * @see #updateOrInsertDirectory(FileInformation, String)
     */
    void insertFileOrDirectory(final @NotNull FileInformation information, final @Nullable String _connectionId) throws SQLException;

    /**
     * Build directory index. And auto update sizes of parents recursively.
     * WARNING: ONLY be called when first listing the directory.
     * {@code assert information.parentId() == directoryId && (information.isDirectory() || information.size() >= 0);}
     * @see #insertFileOrDirectory(FileInformation, String)
     */
    void insertIterator(final @NotNull Iterator<@NotNull FileInformation> iterator, final long directoryId, final @Nullable String _connectionId) throws SQLException;


    /* --- Update --- */

    /**
     * Update file information. {@code parentId, name, size, createTime, updateTime, others}. And auto update sizes of parents recursively.
     * {@code assert !file.isDirectory() && file.size() >= 0;}
     */
    void updateOrInsertFile(final @NotNull FileInformation file, final @Nullable String _connectionId) throws SQLException;

    /**
     * Update directory information. {@code parentId, name, createTime, updateTime, others}. And auto update sizes of parents recursively.
     * {@code assert file.isDirectory();} // Not update directory size. (Be counted by other methods.)
     */
    void updateOrInsertDirectory(final @NotNull FileInformation directory, final @Nullable String _connectionId) throws SQLException;

    default void updateOrInsertFileOrDirectory(final @NotNull FileInformation information, final @Nullable String _connectionId) throws SQLException {
        if (information.isDirectory())
            this.updateOrInsertDirectory(information, _connectionId);
        else
            this.updateOrInsertFile(information, _connectionId);
    }

    void calculateDirectorySize(final long directoryId, final @Nullable String _connectionId) throws SQLException;


    /* --- Select --- */

    /**
     * Select file by id.
     * @return null: not found. !null: found.
     */
    @Nullable FileInformation selectInfo(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select all files in directory. (Do NOT select root directory.)
     */
    @NotNull FilesListInformation selectInfosInDirectory(final long directoryId, final Options.@NotNull FilterPolicy filter, final @NotNull LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select all files' id in directory. (For refreshing.) (Do NOT select root directory.)
     * @return first: files' id in directory. second: directories' id in directory.
     */
    Pair.@NotNull ImmutablePair<@NotNull Set<@NotNull Long>, @NotNull Set<@NotNull Long>> selectIdsInDirectory(final long directoryId, final @Nullable String _connectionId) throws SQLException;

    /**
     * Select file by name in directory. (For duplicate.) (Do NOT select root directory.)
     */
    @Nullable FileInformation selectInfoInDirectoryByName(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;

    /**
     * Is a file/directory in directory. (For recycler detect.)
     * @return false: not in directory / id not exist. true: in directory / directoryId not exist / self is self.
     */
    boolean isInDirectoryRecursively(final long id, final boolean isDirectory, final long directoryId, final @Nullable String _connectionId) throws SQLException;


    /* --- Delete --- */

    /**
     * Delete file by id. And auto update sizes of parents recursively.
     * @return false: not found. true: deleted.
     */
    boolean deleteFile(final long fileId, final @Nullable String _connectionId) throws SQLException;

    /**
     * Delete directory by id. (Do NOT delete root directory but delete any others.) And auto update sizes of parents recursively.
     * @return false: not found. true: deleted.
     */
    boolean deleteDirectoryRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException;

    default boolean deleteFileOrDirectory(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException {
        if (isDirectory)
            return this.deleteDirectoryRecursively(id, _connectionId);
        return this.deleteFile(id, _connectionId);
    }


    /* --- Search --- */

    // TODO Search
}
