package com.xuxiaocheng.WList.Server.Storage.Selectors;

public final class RootSelector {
    private static final RootSelector instance = new RootSelector();
    public static RootSelector getInstance() {
        return RootSelector.instance;
    }
//
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
//                final LocalDateTime old = driver.getConfiguration().getLastFileIndexBuildTime();
//                if (old == null || Duration.between(old, LocalDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3))
//                    driver.buildIndex();
//            } finally {
//                StorageManager.dumpConfigurationIfModified(driver.getConfiguration());
//            }
//            if (trash != null)
//                try {
//                    final LocalDateTime old = trash.getDriver().getConfiguration().getLastTrashIndexBuildTime();
//                    if (old == null || Duration.between(old, LocalDateTime.now()).toMillis() > TimeUnit.HOURS.toMillis(3))
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
