package com.xuxiaocheng.WList.Server.Storage.Providers.WebProviders.Lanzou;

import com.xuxiaocheng.WList.Server.Storage.Providers.AbstractIdBaseProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import org.jetbrains.annotations.NotNull;

public class LanzouProvider extends AbstractIdBaseProvider<LanzouConfiguration> {
    @Override
    public @NotNull ProviderTypes<LanzouConfiguration> getType() {
        return ProviderTypes.Lanzou;
    }

//        FileManager.mergeFile(this.configuration.getName(), RootSelector.getDatabaseDriverInformation(this.configuration), null);


//    @Override
//    public void buildCache() throws IOException {
//        DriverManager_lanzou.ensureLoggedIn(this.configuration);
//        this.configuration.setLastFileCacheBuildTime(ZonedDateTime.now());
//        this.configuration.setModified(true);
//    }
//
//    @Override
//    public void buildIndex() throws IOException, SQLException, InterruptedException {
//        final Set<CompletableFuture<?>> futures = ConcurrentHashMap.newKeySet();
//        final AtomicLong runningFutures = new AtomicLong(1);
//        final AtomicBoolean interruptFlag = new AtomicBoolean(false);
//        DriverManager_lanzou.refreshDirectoryRecursively(this.configuration, this.configuration.getRootDirectoryId(), futures, runningFutures, interruptFlag);
//        try {
//            synchronized (runningFutures) {
//                while (runningFutures.get() > 0)
//                    runningFutures.wait();
//            }
//        } catch (final InterruptedException exception) {
//            interruptFlag.set(true);
//            throw exception;
//        }
//        for (final CompletableFuture<?> future: futures)
//            try {
//                future.join();
//            } catch (final CancellationException ignore) {
//            } catch (final CompletionException exception) {
//                Throwable throwable;
//                try {
//                    throwable = HExceptionWrapper.unwrapException(exception.getCause(), IOException.class, SQLException.class, InterruptedException.class);
//                } catch (final IOException | SQLException | InterruptedException e) {
//                    throwable = e;
//                }
//                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), throwable);
//            }
//        final FileInformation root = FileManager.selectFile(this.configuration.getName(), this.configuration.getRootDirectoryId(), null);
//        if (root != null)
//            this.configuration.setSpaceUsed(root.size());
//        this.configuration.setLastFileIndexBuildTime(ZonedDateTime.now());
//        this.configuration.setModified(true);
//    }
//
//    @Override
//    public void refreshDirectory(final @NotNull FileLocation location) throws IOException, SQLException, InterruptedException {
//        DriverManager_lanzou.waitSyncComplete(DriverManager_lanzou.syncFilesList(this.configuration, location.id(), null));
//    }
//
//    @Override
//    public Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> list(final @NotNull FileLocation location, final Options.@NotNull DirectoriesOrFiles filter, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IOException, SQLException, InterruptedException {
//        return DriverManager_lanzou.listFiles(this.configuration, location.id(), filter, limit, page, policy, direction, null);
//    }
//
//    @Override
//    public @Nullable FileInformation info(final @NotNull FileLocation location) throws IOException, SQLException, InterruptedException {
//        return DriverManager_lanzou.getFileInformation(this.configuration, location.id(), null, null);
//    }
//
//    @Override
//    public @NotNull UnionPair<DownloadMethods, FailureReason> download(final @NotNull FileLocation location, final long from, final long to) throws IOException, SQLException, InterruptedException {
//        return DriverManager_lanzou.getDownloadMethods(this.configuration, location.id(), from, to, null);
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IOException, SQLException, InterruptedException {
//        return DriverManager_lanzou.createDirectory(this.configuration, parentLocation.id(), directoryName, policy, null);
//    }
//
//    @Override
//    public @NotNull UnionPair<UploadMethods, FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IOException, SQLException, InterruptedException {
//        return DriverManager_lanzou.getUploadMethods(this.configuration, parentLocation.id(), filename, md5, size, policy, null);
//    }
//
//    @Override
//    public void delete(final @NotNull FileLocation location) throws IOException, SQLException, InterruptedException {
//        final FileInformation information = DriverManager_lanzou.getFileInformation(this.configuration, location.id(), null, null);
//        if (information != null)
//            DriverManager_lanzou.trash(this.configuration, information, null, null);
//    }
//
//    // Default copy method.
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final Options.@NotNull DuplicatePolicy policy) throws IOException, SQLException, InterruptedException {
//        final FileInformation information = DriverManager_lanzou.getFileInformation(this.configuration, sourceLocation.id(), null, null);
//        if (information == null) return UnionPair.fail(FailureReason.byNoSuchFile("Moving (source).", sourceLocation));
//        return DriverManager_lanzou.move(this.configuration, information, targetParentLocation.id(), policy, null);
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IOException, SQLException {
//        return DriverManager_lanzou.rename(this.configuration, sourceLocation.id(), name, policy, null);
//    }
}
