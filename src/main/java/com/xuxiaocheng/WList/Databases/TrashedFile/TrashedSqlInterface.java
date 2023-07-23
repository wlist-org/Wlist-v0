package com.xuxiaocheng.WList.Databases.TrashedFile;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Databases.DatabaseInterface;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TrashedSqlInterface extends DatabaseInterface {
    void createTable(final @Nullable String _connectionId) throws SQLException;
    void deleteTable(final @Nullable String _connectionId) throws SQLException;
    @Contract(pure = true) @NotNull String getDriverName();

    void insertOrUpdateFiles(final @NotNull Collection<@NotNull TrashedSqlInformation> inserters, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull Long, @NotNull TrashedSqlInformation> selectFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull String, @NotNull List<@NotNull TrashedSqlInformation>> selectFilesByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Map<@NotNull String, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView Set<@NotNull Long> selectFilesId(final @Nullable String _connectionId) throws SQLException;
    long selectFileCount(final @Nullable String _connectionId) throws SQLException;
    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> selectFilesInPage(final int limit, final long offset, final Options.@NotNull OrderDirection direction, final Options.@NotNull OrderPolicy policy, final @Nullable String _connectionId) throws SQLException;
    void deleteFiles(final @NotNull Collection<@NotNull Long> idList, final @Nullable String _connectionId) throws SQLException;
    void deleteFilesByName(final @NotNull Collection<@NotNull String> nameList, final @Nullable String _connectionId) throws SQLException;
    void deleteFilesByMd5(final @NotNull Collection<@NotNull String> md5List, final @Nullable String _connectionId) throws SQLException;
    void clear(final @Nullable String _connectionId) throws SQLException;
    @NotNull @UnmodifiableView List<@Nullable TrashedSqlInformation> searchFilesByNameLimited(final @NotNull String rule, final boolean caseSensitive, final int limit, final @Nullable String _connectionId) throws SQLException;
}
