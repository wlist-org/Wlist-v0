package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.File.TrashedFileManager;
import com.xuxiaocheng.WList.Server.Databases.File.TrashedSqlInformation;
import com.xuxiaocheng.WList.Server.Driver.BackgroundTaskManager;
import com.xuxiaocheng.WList.Server.Polymers.DownloadMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public final class TrashManager_123pan {
    private TrashManager_123pan() {
        super();
    }

    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull List<@NotNull TrashedSqlInformation>> listFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final Pair.ImmutablePair<Long, List<TrashedSqlInformation>> data = TrashHelper_123pan.listFiles(configuration, limit, page, policy, direction);
        TrashedFileManager.insertOrUpdateFiles(configuration.getLocalSide().getName(), data.getSecond(), _connectionId);
        return data;
    }

    static Triad.@NotNull ImmutableTriad<@NotNull Long, @NotNull Iterator<@NotNull TrashedSqlInformation>, @NotNull Runnable> listAllFilesNoCache(final @NotNull DriverConfiguration_123Pan configuration, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        final Connection connection = TrashedFileManager.getDatabaseUtil().getConnection(_connectionId, connectionId);
        final Set<Long> allIds = ConcurrentHashMap.newKeySet();
        try {
            allIds.addAll(TrashedFileManager.selectFilesId(configuration.getLocalSide().getName(), connectionId.get()));
        } catch (final SQLException | RuntimeException exception) {
            connection.close();
            throw exception;
        }
        return DriverUtil.wrapAllFilesListerInPages(page -> {
                    final Pair.ImmutablePair<@NotNull Long, @NotNull List<TrashedSqlInformation>> list = TrashManager_123pan.listFilesNoCache(configuration,
                            DriverUtil.DefaultLimitPerRequestPage, page.intValue(), DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection, connectionId.get());
                    allIds.removeAll(list.getSecond().stream().map(TrashedSqlInformation::id).collect(Collectors.toSet()));
                    return list;
                }, DriverUtil.DefaultLimitPerRequestPage, HExceptionWrapper.wrapConsumer(e -> {
                    try (connection) {
                        if (e != null) throw e;
                        TrashedFileManager.deleteFiles(configuration.getLocalSide().getName(), allIds, connectionId.get());
                        connection.commit();
                    }
                }), _threadPool);
    }

    static @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation> getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final @NotNull String name, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = TrashedFileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            if (useCache) {
                final List<TrashedSqlInformation> information = TrashedFileManager.selectFileByName(configuration.getLocalSide().getName(), name, _connectionId);
                if (!information.isEmpty())
                    return information;
            }
            final List<TrashedSqlInformation> information = new ArrayList<>();
            final Triad.ImmutableTriad<Long, Iterator<TrashedSqlInformation>, Runnable> lister = TrashManager_123pan.listAllFilesNoCache(configuration, _connectionId, _threadPool);
            try {
                while (lister.getB().hasNext()) {
                    final TrashedSqlInformation info = lister.getB().next();
                    if (name.equals(info.name()))
                        information.add(info);
                }
            } catch (final NoSuchElementException exception) {
                assert exception.getCause() instanceof InterruptedException;
            } catch (final RuntimeException exception) {
                if (!(exception.getCause() instanceof CancellationException))
                    throw HExceptionWrapper.unwrapException(exception, SQLException.class);
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
            connection.commit();
            return Collections.unmodifiableList(information);
        }
    }

    static @Nullable TrashedSqlInformation getFileInformation(final @NotNull DriverConfiguration_123Pan configuration, final long id, final boolean useCache, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = TrashedFileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            if (useCache) {
                final TrashedSqlInformation information = TrashedFileManager.selectFile(configuration.getLocalSide().getName(), id, connectionId.get());
                if (information != null)
                    return information;
            }
            final TrashedSqlInformation information = TrashHelper_123pan.getFilesInformation(configuration, List.of(id)).get(id);
            if (information != null)
                TrashedFileManager.insertOrUpdateFile(configuration.getLocalSide().getName(), information, connectionId.get());
            connection.commit();
            return information;
        }
    }

    static Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> listFiles(final @NotNull DriverConfiguration_123Pan configuration, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = TrashedFileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            if (useCache) {
                final Pair.ImmutablePair<Long, List<TrashedSqlInformation>> list = TrashedFileManager.selectFilesInPage(configuration.getLocalSide().getName(),
                        limit, (long) page * limit, direction, policy, connectionId.get());
                if (list.getFirst().longValue() > 0)
                    return list;
                assert list.getSecond().isEmpty();
            }
            final Pair.ImmutablePair<Long, List<TrashedSqlInformation>> list = TrashManager_123pan.listFilesNoCache(configuration, limit, page, policy, direction, connectionId.get());
            final long cached = useCache ? 0 : TrashedFileManager.selectFileCount(configuration.getLocalSide().getName(), connectionId.get());
            if (cached != list.getFirst().longValue())
                if (list.getFirst().longValue() == list.getSecond().size())
                    TrashedFileManager.insertOrUpdateFiles(configuration.getLocalSide().getName(), list.getSecond(), connectionId.get());
                else
                    BackgroundTaskManager.backgroundOptionally("Driver_123pan: " + configuration.getLocalSide().getName(), "Sync trash",
                            () -> new AtomicLong(0), AtomicLong.class,
                            lock -> lock.get() != list.getFirst().longValue(), HExceptionWrapper.wrapRunnable(() -> {
                                FileManager.getDatabaseUtil().getExplicitConnection(connectionId.get()); // retain
                            }), () -> {
                                final Iterator<TrashedSqlInformation> iterator = TrashManager_123pan.listAllFilesNoCache(configuration, connectionId.get(), _threadPool).getB();
                                while (iterator.hasNext()) iterator.next();
                                connection.commit();
                            }, connection::close);
            connection.commit();
            return list;
        }
    }

    static @Nullable DownloadMethods getDownloadMethods(final @NotNull DriverConfiguration_123Pan configuration, final long id, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final boolean useCache, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        final TrashedSqlInformation info = TrashManager_123pan.getFileInformation(configuration, id, useCache, _connectionId);
        if (info == null || info.isDir())
            return null;
        final String url = DriverHelper_123pan.getFileDownloadUrl(configuration, new FileSqlInformation(id, new DrivePath(info.name()), false, info.size(), null, null, info.md5(), info.others()));
        if (url == null)
            return null;
        return DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(DriverHelper_123pan.fileClient,
                Pair.ImmutablePair.makeImmutablePair(url, "GET"), info.size(), from, to, null));
    }

    static @Nullable DrivePath buildPath(final @NotNull DriverConfiguration_123Pan configuration, final long id, final boolean useCache, final @Nullable String _connectionId) throws IllegalParametersException, IOException, SQLException {
        if (id == configuration.getWebSide().getRootDirectoryId())
            return new DrivePath("");
        if (id == 0)
            return null;
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            if (useCache) {
                final FileSqlInformation information = FileManager.selectFile(configuration.getLocalSide().getName(), id, connectionId.get());
                if (information != null)
                    return information.path();
            }
            final Pair.ImmutablePair<Long, FileSqlInformation> parentId = TrashHelper_123pan.getInformationParentIds(configuration, List.of(id)).get(id);
            if (parentId == null)
                throw new IllegalStateException("Failed to get parent file id. [Unknown]. id: " + id);
            final DrivePath parent = TrashManager_123pan.buildPath(configuration, parentId.getFirst().longValue(), useCache, connectionId.get());
            connection.commit();
            return parent == null ? null : parent.child(parentId.getSecond().path().getName());
        }
    }

    static @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> restoreFile(final @NotNull DriverConfiguration_123Pan configuration, final long id, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy, final boolean useCache, final @Nullable String _connectionId, final @Nullable ExecutorService _threadPool) throws IllegalParametersException, IOException, SQLException {
        final Set<Long> ids = DriverHelper_123pan.trashFiles(configuration, List.of(id), false);
        if (ids.isEmpty())
            return UnionPair.fail(FailureReason.byNoSuchFile("Restoring file.", id));
        assert Set.of(id).equals(ids);
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = TrashedFileManager.getDatabaseUtil().getConnection(_connectionId, connectionId)) {
            connection.setAutoCommit(false);
            TrashedFileManager.deleteFile(configuration.getLocalSide().getName(), id, connectionId.get());
            final Pair.ImmutablePair<Long, FileSqlInformation> parentId = TrashHelper_123pan.getInformationParentIds(configuration, ids).get(id);
            final DrivePath parentPath = TrashManager_123pan.buildPath(configuration, parentId.getFirst().longValue(), useCache, connectionId.get());
            if (parentPath == null)
                return UnionPair.fail(FailureReason.byNoSuchFile("Out of root tree. id: " + id, new DrivePath("")));
            final FileSqlInformation information = parentId.getSecond();
            information.path().addedRoot(parentPath); // sourcePath.child(parentId.getSecond().path().getName()); information.path = sourcePath;
            try (final Connection fileConnection = FileManager.getDatabaseUtil().getConnection(TrashedFileManager.getDatabaseUtil() == FileManager.getDatabaseUtil() ? _connectionId : null, connectionId)) {
                fileConnection.setAutoCommit(false);
                final String name = information.path().getName();
                final long count;
                try {
                    count = FileManager.selectFileCountByParentPath(configuration.getLocalSide().getName(), information.path().parent(), connectionId.get());
                } finally {
                    information.path().child(name);
                }
                if (count > 0)
                    FileManager.insertOrUpdateFile(configuration.getLocalSide().getName(), information, connectionId.get());
                DriverManager_123pan.moveFile(configuration, information.path(), path, policy, true, connectionId.get(), _threadPool);
                fileConnection.commit();
            }
            connection.commit();
            return UnionPair.ok(information);
        }
    }


}
