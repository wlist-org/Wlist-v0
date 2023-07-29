package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.TrashedFile.TrashedSqlInformation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public interface DriverTrashInterface<D extends DriverInterface<?>> {
    @NotNull D getDriver();

    void initialize(final @NotNull D driver) throws Exception;

    void uninitialize() throws Exception;

    void buildCache() throws Exception;

    void buildIndex() throws Exception;

    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> list(final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception;

    @Nullable TrashedSqlInformation info(final @NotNull FileLocation location) throws Exception;

    @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> restore(final @NotNull FileLocation location, final long targetParentId, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    void delete(final @NotNull FileLocation location) throws Exception;

    default void deleteAll() throws Exception {
        while (true) {
            final Pair.ImmutablePair<Long, List<TrashedSqlInformation>> page = this.list(DriverUtil.DefaultLimitPerRequestPage, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection);
            for (final TrashedSqlInformation information: page.getSecond())
                this.delete(information.location());
            if (page.getFirst().longValue() == page.getSecond().size() || page.getSecond().isEmpty())
                break;
        }
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    default @NotNull UnionPair<@NotNull TrashedSqlInformation, @NotNull FailureReason> rename(final @NotNull FileLocation location, final @NotNull String name) throws Exception {
        throw new UnsupportedOperationException("Renaming file in trash is unsupported.");
    }
}
