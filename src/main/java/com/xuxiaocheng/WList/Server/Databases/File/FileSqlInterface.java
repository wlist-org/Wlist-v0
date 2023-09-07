package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Server.Databases.DatabaseInterface;
import com.xuxiaocheng.WList.Commons.Options;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FileSqlInterface extends DatabaseInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;
    void deleteTable(final @Nullable String _connectionId) throws SQLException;
    @Contract(pure = true) @NotNull String getDriverName();
    @Contract(pure = true) long getRootId();

    // Notice: Exclude root record (id == parent_id) automatically.

    void insertFilesForce(final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable String _connectionId) throws SQLException;
    void updateDirectoryType(final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException;
    @Nullable FileInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
    Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> selectFilesByParentIdInPage(final long parentId, final Options.@NotNull DirectoriesOrFiles filter, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException;
    void mergeFiles(final @NotNull Collection<@NotNull FileInformation> inserters, final @Nullable Collection<@NotNull Long> mergingUniverse, final @Nullable String _connectionId) throws SQLException;
    void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException;
    void deleteFilesByMd5Recursively(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;

    @Nullable Long updateDirectorySize(final long directoryId, final long delta, final @Nullable String _connectionId) throws SQLException;
    @Nullable Long calculateDirectorySizeRecursively(final long directoryId, final @Nullable String _connectionId) throws SQLException;

    // TODO Search

    enum FileSqlType {
        RegularFile,
        Directory,
        EmptyDirectory, // Different from no cache.
    }
}
