package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

public interface FileSqlInterface extends DatabaseInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;
    void deleteTable(final @Nullable String _connectionId) throws SQLException;
    @Contract(pure = true) @NotNull String getProviderName();
    @Contract(pure = true) long getRootId();

    /* --- Insert --- */

//    void insertFilesForce(final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable String _connectionId) throws SQLException;

//    void mergeFiles(final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable Collection<@NotNull Long> mergingUniverse, final @Nullable String _connectionId) throws SQLException;

    /* --- Update --- */

    Pair.@NotNull ImmutablePair<@Nullable FileInformation, @Nullable FileInformation> selectFile(final long id, final @Nullable String _connectionId) throws SQLException;

    default @Nullable FileInformation selectFile(final long id, final boolean isDirectory, final @Nullable String _connectionId) throws SQLException {
        final Pair.ImmutablePair<FileInformation, FileInformation> pair = this.selectFile(id, _connectionId);
        return isDirectory ? pair.getFirst() : pair.getSecond();
    }

    /* --- Select --- */
    /* --- Delete --- */
    /* --- Search --- */


//    void updateDirectoryType(final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException;
//    @Nullable FileInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;
//    @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;
//    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
//    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
//    Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> selectFilesByParentIdInPage(final long parentId, final Options.@NotNull FilterPolicy filter, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException;
//    void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException;
//
//    @Nullable Long updateDirectorySize(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException;
//    @Nullable Long calculateDirectorySizeRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException;

    // TODO Search
}
