package com.xuxiaocheng.WList.Server.Storage.Selectors;

public final class RootSelector {
    private static final RootSelector instance = new RootSelector();
    public static RootSelector getInstance() {
        return RootSelector.instance;
    }

//    /**
//     * Build provider cache. Login and check token, etc.
//     * @see ProviderConfiguration#setLastFileCacheBuildTime(ZonedDateTime)
//     */
//    void buildCache() throws Exception;
//
//    /**
//     * Build all files index into sql database. (Recursive from root.)
//     * @see FileManager
//     * @see ProviderConfiguration#setLastFileIndexBuildTime(ZonedDateTime)
//     */
//    void buildIndex() throws Exception;
//
//    /**
//     * Force rebuild files index into sql database to synchronize with web server. (Not recursively)
//     * @param location The directory location to refresh.
//     * @see FileManager
//     */
//    void refreshDirectory(final @NotNull FileLocation location) throws Exception;
//
//    /**
//     * Get the list of files in directory.
//     * @param location The directory location.
//     * @return null: directory is not available. !null: list of files.
//     */
//    @Nullable FilesListInformation list(final @NotNull FileLocation location, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final @LongRange(minimum = 0) long position, final @IntRange(minimum = 0) int limit) throws Exception;
//
//    /**
//     * Get the file information of a specific file.
//     * @param location The file location to get information.
//     * @return The file information. Null means not existed.
//     */
//    @Nullable FileInformation info(final @NotNull FileLocation location) throws Exception;
//
//    /**
//     * Get download methods of a specific file.
//     * @see DownloadRequirements#tryGetDownloadFromUrl(OkHttpClient, HttpUrl, Headers, Long, Headers.Builder, long, long, ZonedDateTime)
//     */
//    @NotNull UnionPair<DownloadRequirements, FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception;
//
//    /**
//     * Create an empty directory.
//     * @see com.xuxiaocheng.WList.Server.Storage.Helpers.ProviderUtil#getRetryWrapper(String)
//     */
//    @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception;
//
//    /**
//     * Upload file.
//     */
//    @NotNull UnionPair<UploadRequirements, FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception;

//    /**
//     * Delete file.
//     * @param location The file location to delete.
//     * @throws Exception Something went wrong.
//     */
//    void delete(final @NotNull FileLocation location) throws Exception;
//
//    /**
//     * Copy file.
//     * @param sourceLocation The file location to copy.
//     * @param targetParentLocation The target parent directory location.
//     * @param targetFilename The name of target file.
//     * @param policy Duplicate policy.
//     * @return The information of new file.
//     * @throws Exception Something went wrong.
//     */
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    default @NotNull UnionPair<FileInformation, FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Copying by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("targetFilename", targetFilename).add("policy", policy));
//        final FileInformation source = this.info(sourceLocation);
//        if (source == null || source.isDirectory())
//            return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", sourceLocation));
//        Runnable uploadFinisher = null;
//        Runnable downloadFinisher = null;
//        try {
//            final UnionPair<UploadRequirements.UploadMethods, FailureReason> upload = this.upload(targetParentLocation, targetFilename, source.size(), source.md5(), policy);
//            if (upload.isFailure())
//                return UnionPair.fail(upload.getE());
//            uploadFinisher = upload.getT().finisher();
//            if (!upload.getT().methods().isEmpty()) {
//                final UnionPair<DownloadMethods, FailureReason> download = this.download(sourceLocation, 0, Long.MAX_VALUE);
//                if (download.isFailure())
//                    return UnionPair.fail(download.getE());
//                downloadFinisher = download.getT().finisher();
//                assert source.size() == download.getT().total();
//                if (upload.getT().methods().size() != download.getT().methods().size())
//                    throw new AssertionError("Copying. Same size but different methods count." + ParametersMap.create()
//                            .add("size_uploader", source.size()).add("size_downloader", download.getT().total())
//                            .add("downloader", download.getT().methods().size()).add("uploader", upload.getT().methods().size())
//                            .add("source", source).add("target", targetParentLocation).add("filename", targetFilename).add("policy", policy));
//                final Iterator<ConsumerE<ByteBuf>> uploadIterator = upload.getT().methods().iterator();
//                final Iterator<SupplierE<ByteBuf>> downloadIterator = download.getT().methods().iterator();
//                while (uploadIterator.hasNext() && downloadIterator.hasNext())
//                    uploadIterator.next().accept(downloadIterator.next().get());
//                assert !uploadIterator.hasNext() && !downloadIterator.hasNext();
//            }
//            final FileInformation information = upload.getT().supplier().get();
//            if (information == null)
//                throw new IllegalStateException("Failed to copy file. Failed to get target file information." + ParametersMap.create()
//                        .add("sourceLocation", sourceLocation).add("sourceInfo", source).add("targetParentLocation", targetParentLocation).add(targetFilename, "targetFilename").add("policy", policy));
//            return UnionPair.ok(information);
//        } finally {
//            if (uploadFinisher != null)
//                uploadFinisher.run();
//            if (downloadFinisher != null)
//                downloadFinisher.run();
//        }
//    }
//
//    /**
//     * Move file/directory.
//     * @param sourceLocation The file/directory location to move.
//     * @param targetParentLocation The target directory location.
//     * @param policy Duplicate policy.
//     * @return The information of new file/directory.
//     * @throws Exception Something went wrong.
//     */
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    default @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Moving by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("policy", policy));
//        final FileInformation source = this.info(sourceLocation);
//        if (source == null)
//            return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceLocation));
//        if (source.isDirectory()) {
//            final UnionPair<FileInformation, FailureReason> directory = this.createDirectory(targetParentLocation, source.name(), policy);
//            if (directory.isFailure())
//                return UnionPair.fail(directory.getE());
//            Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
//            do {
//                list = this.list(sourceLocation, Options.FilterPolicy.OnlyDirectories, ProviderUtil.DefaultLimitPerRequestPage, 0, ProviderUtil.DefaultOrderPolicy, ProviderUtil.DefaultOrderDirection);
//                if (list == null)
//                    return directory;
//                for (final FileInformation f: list.getC())
//                    this.move(f.location(), directory.getT().location(), policy);
//            } while (list.getB().longValue() > 0);
//            this.moveFilesInDirectory(sourceLocation, policy, directory);
//            return directory;
//        }
//        if (source.location().equals(targetParentLocation))
//            return UnionPair.ok(source);
//        final UnionPair<FileInformation, FailureReason> information = this.copy(sourceLocation, targetParentLocation, source.name(), policy);
//        if (information.isFailure())
//            return UnionPair.fail(information.getE());
//        try {
//            this.delete(sourceLocation);
//        } catch (final Exception exception) {
//            try {
//                this.delete(targetParentLocation);
//            } catch (final Exception e) {
//                throw new IllegalStateException("Failed to delete target file after a failed deletion of source file when moving file by default algorithm." +
//                        ParametersMap.create().add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("policy", policy).add("exception", exception), e);
//            }
//            throw exception;
//        }
//        return information;
//    }
//
//    /**
//     * Rename file/directory.
//     * @param sourceLocation The file/directory location to move.
//     * @param name The name of new file/directory.
//     * @param policy Duplicate policy.
//     * @return The information of new file/directory.
//     * @throws Exception Something went wrong.
//     */
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    default @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        HLog.getInstance("ServerLogger").log(HLogLevel.WARN, "Renaming by default algorithm.", ParametersMap.create().add("sourceLocation", sourceLocation).add("name", name).add("policy", policy));
//        final FileInformation source = this.info(sourceLocation);
//        if (source == null)
//            return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", sourceLocation));
//        if (source.isDirectory()) {
//            final UnionPair<FileInformation, FailureReason> directory = this.createDirectory(new FileLocation(sourceLocation.storage(), source.parentId()), name, policy);
//            if (directory.isFailure())
//                return UnionPair.fail(directory.getE());
//            this.moveFilesInDirectory(sourceLocation, policy, directory);
//            this.delete(sourceLocation);
//            return directory;
//        }
//        if (source.name().equals(name))
//            return UnionPair.ok(source);
//        final UnionPair<FileInformation, FailureReason> information = this.copy(sourceLocation, sourceLocation, name, policy);
//        if (information.isFailure())
//            return UnionPair.fail(information.getE());
//        try {
//            this.delete(sourceLocation);
//        } catch (final Exception exception) {
//            try {
//                this.delete(information.getT().location());
//            } catch (final Exception e) {
//                throw new IllegalStateException("Failed to delete target file after a failed deletion of source file when renaming file by default algorithm." +
//                        ParametersMap.create().add("sourceLocation", sourceLocation).add("name", name).add("targetLocation", information.getT().location()).add("policy", policy).add("exception", exception), e);
//            }
//            throw exception;
//        }
//        return information;
//    }
//
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    private void moveFilesInDirectory(final @NotNull FileLocation sourceLocation, final Options.@NotNull DuplicatePolicy policy, final @NotNull UnionPair<FileInformation, FailureReason> directory) throws Exception {
//        Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
//        do {
//            list = this.list(sourceLocation, Options.FilterPolicy.Both, 10, 0, ProviderUtil.DefaultOrderPolicy, ProviderUtil.DefaultOrderDirection);
//            if (list == null)
//                break;
//            final CountDownLatch latch = new CountDownLatch(list.getC().size());
//            for (final FileInformation f: list.getC())
//                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> this.move(f.location(), directory.getT().location(), policy),
//                        latch::countDown), WListServer.ServerExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
//            latch.await();
//        } while (list.getA().longValue() > 0);
//    }

//    public static @NotNull FileInformation getDriverInformation(final @NotNull ProviderConfiguration configuration) {
//        return new FileInformation(new FileLocation(IdentifierNames.SelectorProviderName.RootSelector.getIdentifier(), configuration.getRootDirectoryId()),
//                0, configuration.getName(), FileSqlInterface.FileSqlType.Directory, configuration.getSpaceUsed(),
//                configuration.getCreateTime(), configuration.getUpdateTime(),
//                configuration.getDisplayName(), null);
//    }
//
//    public static @NotNull FileInformation getDatabaseDriverInformation(final @NotNull ProviderConfiguration configuration) {
//        return new FileInformation(new FileLocation(IdentifierNames.SelectorProviderName.RootSelector.getIdentifier(), configuration.getRootDirectoryId()),
//                configuration.getRootDirectoryId(), configuration.getName(),
//                FileSqlInterface.FileSqlType.Directory, configuration.getSpaceUsed(),
//                configuration.getCreateTime(), configuration.getUpdateTime(), "", null);
//    }

//    public boolean buildIndex(final @NotNull String name) throws Exception {
//        final ProviderInterface<?> driver = StorageManager.getProvider(name);
//        if (driver != null) {
//            final ProviderRecyclerInterface<?> trash = StorageManager.getRecycler(name);
//            try {
//                final ZonedDateTime old = driver.getConfiguration().getLastFileIndexBuildTime();
//                if (old == null || Duration.between(old, ZonedDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3))
//                    driver.buildIndex();
//            } finally {
//                StorageManager.dumpConfigurationIfModified(driver.getConfiguration());
//            }
//            if (trash != null)
//                try {
//                    final ZonedDateTime old = trash.getDriver().getConfiguration().getLastTrashIndexBuildTime();
//                    if (old == null || Duration.between(old, ZonedDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3))
//                        trash.buildIndex();
//                } finally {
//                    StorageManager.dumpConfigurationIfModified(trash.getDriver().getConfiguration());
//                }
//        }
//        return driver != null;
//    }
//
//    @Override
//    public void refreshDirectory(final @NotNull FileLocation location) throws Exception {
//        final ProviderInterface<?> driver = StorageManager.getProvider(location.storage());
//        if (driver != null)
//            try {
//                driver.refreshDirectory(location);
//            } finally {
//                StorageManager.dumpConfigurationIfModified(driver.getConfiguration());
//            }
//    }
//
//    @Override
//    public @Nullable FilesListInformation list(@NotNull final FileLocation location, @NotNull final Options.FilterPolicy filter, @NotNull @Unmodifiable final LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> orders, final long position, final int limit) throws Exception {
//        return null;
//    }

//    @Override
//    public Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileInformation>> list(final @NotNull FileLocation location, final Options.@NotNull FilterPolicy filter, final @LongRange(minimum = 0) int limit, final @LongRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.storage())) {
//            if (filter == Options.FilterPolicy.OnlyFiles)
//                return Triad.ImmutableTriad.makeImmutableTriad((long) StorageManager.getProvidersCount(), 0L, List.of());
//            final Comparator<ProviderConfiguration<?, ?, ?>> comparator = switch (policy) {
//                case FileName -> Comparator.comparing((ProviderConfiguration a) -> a.getDisplayName());
//                case Size -> Comparator.comparing((ProviderConfiguration a) -> a.getSpaceUsed(), Long::compareUnsigned);
//                case CreateTime -> Comparator.comparing((ProviderConfiguration a) -> a.getCreateTime());
//                case UpdateTime -> Comparator.comparing((ProviderConfiguration a) -> a.getUpdateTime());
//            };
//            final SortedSet<ProviderConfiguration<?, ?, ?>> all = new ConcurrentSkipListSet<>(switch (direction) {
//                case ASCEND -> comparator; case DESCEND -> comparator.reversed();
//            });
//            StorageManager.operateAllDrivers(d -> all.add(d.getConfiguration()));
//            final Iterator<ProviderConfiguration<?, ?, ?>> iterator = all.stream().skip((long) limit * page).iterator();
//            final List<FileInformation> list = new ArrayList<>(limit);
//            while (list.size() < limit && iterator.hasNext())
//                list.add(RootSelector.getDriverInformation(iterator.next()));
//            return Triad.ImmutableTriad.makeImmutableTriad((long) all.size(), (long) all.size(), list);
//        }
//        final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
//        if (real == null) return null;
//        final Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
//        try {
//            list = real.list(location, filter, limit, page, policy, direction);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//        return list;
//    }

//    @Override
//    public @Nullable FileInformation info(final @NotNull FileLocation location) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.storage()))
//            throw new UnsupportedOperationException("Cannot get root info.");
//        final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
//        if (real == null) return null;
//        final FileInformation info;
//        try {
//            info = real.info(location);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//        return info;
//    }

//    @Override
//    public @NotNull UnionPair<DownloadMethods, FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws Exception {
//        final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
//        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", location));
//        final UnionPair<DownloadMethods, FailureReason> methods;
//        try {
//            methods = real.download(location, from, to);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//        return methods;
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(parentLocation.storage()))
//            throw new UnsupportedOperationException("Cannot create root directory.");
//        final ProviderInterface<?> real = StorageManager.getProvider(parentLocation.storage());
//        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Creating directories.", parentLocation));
//        final UnionPair<FileInformation, FailureReason> directory;
//        try {
//            directory = real.createDirectory(parentLocation, directoryName, policy);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//        return directory;
//    }
//
//    @Override
//    public @NotNull UnionPair<UploadMethods, FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final @LongRange(minimum = 0) long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(parentLocation.storage()))
//            throw new UnsupportedOperationException("Cannot create root file.");
//        final ProviderInterface<?> real = StorageManager.getProvider(parentLocation.storage());
//        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Uploading.", parentLocation));
//        if (size > real.getConfiguration().getMaxSizePerFile())
//            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, real.getConfiguration().getMaxSizePerFile(), parentLocation, filename));
//        final UnionPair<UploadMethods, FailureReason> methods;
//        try {
//            methods = real.upload(parentLocation, filename, size, md5, policy);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//        return methods;
//    }
//
//    @SuppressWarnings("OverlyBroadThrowsClause")
//    @Override
//    public void delete(final @NotNull FileLocation location) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(location.storage()))
//            throw new UnsupportedOperationException("Cannot delete root driver.");
//        final ProviderInterface<?> real = StorageManager.getProvider(location.storage());
//        if (real == null) return;
//        try {
//            real.delete(location);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.storage()))
//            throw new UnsupportedOperationException("Cannot copy from root driver.");
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(targetParentLocation.storage()))
//            throw new UnsupportedOperationException("Cannot copy to root driver.");
//        if (sourceLocation.storage().equals(targetParentLocation.storage())) {
//            final ProviderInterface<?> real = StorageManager.getProvider(sourceLocation.storage());
//            if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", sourceLocation));
//            try {
//                return real.copy(sourceLocation, targetParentLocation, targetFilename, policy);
//            } finally {
//                StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//            }
//        }
//        return ProviderInterface.super.copy(sourceLocation, targetParentLocation, targetFilename, policy);
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.storage()))
//            throw new UnsupportedOperationException("Cannot move from root driver.");
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(targetParentLocation.storage()))
//            throw new UnsupportedOperationException("Cannot move to root driver.");
//        if (sourceLocation.storage().equals(targetParentLocation.storage())) {
//            final ProviderInterface<?> real = StorageManager.getProvider(sourceLocation.storage());
//            if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceLocation));
//            try {
//                return real.move(sourceLocation, targetParentLocation, policy);
//            } finally {
//                StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//            }
//        }
//        return ProviderInterface.super.move(sourceLocation, targetParentLocation, policy);
//    }
//
//    @Override
//    public @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws Exception {
//        if (IdentifierNames.SelectorProviderName.RootSelector.getIdentifier().equals(sourceLocation.storage()))
//            throw new UnsupportedOperationException("Cannot rename root driver.");
//        final ProviderInterface<?> real = StorageManager.getProvider(sourceLocation.storage());
//        if (real == null) return UnionPair.fail(FailureReason.byNoSuchFile("Renaming.", sourceLocation));
//        try {
//            return real.rename(sourceLocation, name, policy);
//        } finally {
//            StorageManager.dumpConfigurationIfModified(real.getConfiguration());
//        }
//    }
//
//    protected static class RootDriverConfiguration extends ProviderConfiguration<RootDriverConfiguration.LocalSide, RootDriverConfiguration.WebSide, RootDriverConfiguration.CacheSide> {
//        private RootDriverConfiguration() {
//            super("RootSelector", LocalSide::new, WebSide::new, CacheSide::new);
//        }
//        private static class LocalSide extends LocalSideDriverConfiguration {
//            protected LocalSide() {
//                super();
//            }
//        }
//        private static class WebSide extends WebSideDriverConfiguration {
//            protected WebSide() {
//                super();
//            }
//        }
//        private static class CacheSide extends CacheSideDriverConfiguration {
//            protected CacheSide() {
//                super();
//            }
//        }
//    }
//
//    @Override
//    public @NotNull String toString() {
//        return "RootSelector{" +
//                "configuration=" + this.configuration +
//                '}';
//    }
}
