package com.xuxiaocheng.WList.Server.Storage.Providers;

import org.jetbrains.annotations.NotNull;

public interface ProviderRecyclerInterface<C extends ProviderConfiguration> {
//    @NotNull D getDriver();
//
    void initialize(final @NotNull C configuration) throws Exception;

    void uninitialize(final boolean dropIndex) throws Exception;

//    void buildCache() throws Exception;
//
//    void buildIndex() throws Exception;
//
//    Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedFileInformation>> list(final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception;
//
//    @Nullable TrashedFileInformation info(final @NotNull FileLocation location) throws Exception;
//
//    @NotNull UnionPair<FileInformation, FailureReason> restore(final @NotNull FileLocation location, final long targetParentId, final Options.@NotNull DuplicatePolicy policy) throws Exception;
//
//    void delete(final @NotNull FileLocation location) throws Exception;
//
//    default void deleteAll() throws Exception {
//        while (true) {
//            final Pair.ImmutablePair<Long, List<TrashedFileInformation>> page = this.list(ProviderUtil.DefaultLimitPerRequestPage, 0, ProviderUtil.DefaultOrderPolicy, ProviderUtil.DefaultOrderDirection);
//            for (final TrashedFileInformation information: page.getSecond())
//                this.delete(information.location());
//            if (page.getFirst().longValue() == page.getSecond().size() || page.getSecond().isEmpty())
//                break;
//        }
//    }
//
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    default @NotNull UnionPair<TrashedFileInformation, FailureReason> rename(final @NotNull FileLocation location, final @NotNull String name) throws Exception {
//        throw new UnsupportedOperationException("Renaming file in trash is unsupported.");
//    }
}
