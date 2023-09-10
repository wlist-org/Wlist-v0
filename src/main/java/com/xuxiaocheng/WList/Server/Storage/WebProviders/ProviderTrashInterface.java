package com.xuxiaocheng.WList.Server.Storage.WebProviders;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.TrashedFile.TrashedFileInformation;
import com.xuxiaocheng.WList.Server.Storage.FailureReason;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Server.Storage.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Commons.Options.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public interface ProviderTrashInterface<D extends ProviderInterface<?>> {
    @NotNull D getDriver();

    void initialize(final @NotNull D driver) throws Exception;

    void uninitialize() throws Exception;

    void buildCache() throws Exception;

    void buildIndex() throws Exception;

    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedFileInformation>> list(final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception;

    @Nullable TrashedFileInformation info(final @NotNull FileLocation location) throws Exception;

    @NotNull UnionPair<FileInformation, FailureReason> restore(final @NotNull FileLocation location, final long targetParentId, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    void delete(final @NotNull FileLocation location) throws Exception;

    default void deleteAll() throws Exception {
        while (true) {
            final Pair.ImmutablePair<Long, List<TrashedFileInformation>> page = this.list(DriverUtil.DefaultLimitPerRequestPage, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection);
            for (final TrashedFileInformation information: page.getSecond())
                this.delete(information.location());
            if (page.getFirst().longValue() == page.getSecond().size() || page.getSecond().isEmpty())
                break;
        }
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    default @NotNull UnionPair<TrashedFileInformation, FailureReason> rename(final @NotNull FileLocation location, final @NotNull String name) throws Exception {
        throw new UnsupportedOperationException("Renaming file in trash is unsupported.");
    }
}
