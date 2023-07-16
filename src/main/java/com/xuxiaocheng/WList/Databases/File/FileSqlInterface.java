package com.xuxiaocheng.WList.Databases.File;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Utils.DatabaseInterface;
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

    void insertOrUpdateFiles(final @NotNull Collection<@NotNull FileSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException;
    void updateDirectoryType(final long id, final boolean empty, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull FileSqlInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException;
    @Nullable FileSqlInformation selectFileInDirectory(final long parentId, final @NotNull String name, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull String, @Nullable @UnmodifiableView Set<@NotNull FileSqlInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull @UnmodifiableView Set<@NotNull Long>> selectFilesIdByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull Long> selectFilesCountByParentId(final @NotNull Collection<@NotNull Long> parentIdList, final @Nullable String _connectionId) throws SQLException;
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> selectFilesByParentIdInPage(final long parentId, final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException;
    void deleteFilesRecursively(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException;
    void deleteFilesByMd5Recursively(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;

    // TODO Better Search
    @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathLimited(final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView List<@Nullable FileSqlInformation> searchFilesByNameInParentPathRecursivelyLimited(final @NotNull DrivePath parentPath, final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException;

    enum FileSqlType {
        RegularFile,
        Directory,
        EmptyDirectory, // Different from no cache.
    }
}
