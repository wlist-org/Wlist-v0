package com.xuxiaocheng.WList.Driver;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.TrashedSqlInformation;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
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

    @Nullable TrashedSqlInformation info(final long id) throws Exception;

    @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> restore(final long id, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws Exception;

    void delete(final long id) throws Exception;

    default @Nullable DownloadMethods download(final long id, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception {
//        final TrashedSqlInformation information = this.info(id);
//        if (information == null)
            return null;
//        final DrivePath tempPath = new DrivePath("/temp_download_" + System.currentTimeMillis() + ".wlist_trash");
//        final UnionPair<FileSqlInformation, FailureReason> temp = this.restore(id, tempPath, Options.DuplicatePolicy.KEEP);
//        if (temp.isFailure())
//            return null;
//        final DownloadMethods raw = this.getDriver().download(tempPath, from, to);
//        return raw == null ? null : new DownloadMethods(raw.total(), raw.methods(), HExceptionWrapper.wrapRunnable(() -> {
//           raw.finisher().run();
//           this.getDriver().delete(tempPath);
//        }));
    }

    default void deleteAll() throws Exception {
        while (true) {
            final Pair.ImmutablePair<Long, List<TrashedSqlInformation>> page = this.list(DriverUtil.DefaultLimitPerRequestPage, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection);
            for (final TrashedSqlInformation information: page.getSecond())
                this.delete(information.id());
            if (page.getFirst().longValue() == page.getSecond().size())
                break;
        }
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    default @NotNull UnionPair<@NotNull TrashedSqlInformation, @NotNull FailureReason> rename(final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final DrivePath tempPath = new DrivePath("/temp_rename.wlist_trash");
        final UnionPair<FileSqlInformation, FailureReason> temp = this.restore(id, tempPath, Options.DuplicatePolicy.KEEP);
//        if (temp.isFailure())
            return UnionPair.fail(temp.getE());
//        final UnionPair<FileSqlInformation, FailureReason> renamedTemp = this.getDriver().rename(tempPath, name, Options.DuplicatePolicy.KEEP);
//        if (renamedTemp.isFailure()) {
//            this.getDriver().delete(tempPath);
//            return UnionPair.fail(renamedTemp.getE());
//        }
//        this.getDriver().delete(renamedTemp.getT().path());
//        final TrashedSqlInformation information = this.info(renamedTemp.getT().id());
//        if (information == null)
//            throw new IllegalStateException("Failed to re-trash file when renaming. [Unknown]. sourceId: " + id + ", targetName: " + name + ", renamedTemp: " + renamedTemp + ", policy: " + policy);
//        return UnionPair.ok(information);
    }
}
