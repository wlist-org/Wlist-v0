package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.File.TrashedSqlInformation;
import com.xuxiaocheng.WList.Server.Polymers.DownloadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public interface DriverTrashInterface<D extends DriverInterface<?>> {
    void initialize(final @NotNull D driver) throws Exception;

    void uninitialize() throws Exception;

    void buildIndex() throws Exception;

    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> list(final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception;

    @NotNull List<@NotNull TrashedSqlInformation> info(final @NotNull String name) throws Exception;

    @Nullable DownloadMethods download(final long id, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception;

    @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> restore(final long id, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    void delete(final long id) throws Exception;

    void deleteAll() throws Exception;

    @NotNull UnionPair<@NotNull TrashedSqlInformation, @NotNull FailureReason> rename(final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception;
}