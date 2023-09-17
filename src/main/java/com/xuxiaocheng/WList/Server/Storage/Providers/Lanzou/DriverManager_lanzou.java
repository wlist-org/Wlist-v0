package com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou;

@SuppressWarnings("SameParameterValue")
public final class DriverManager_lanzou {
    private DriverManager_lanzou() {
        super();
    }

    // File Reader

//    static Pair.@Nullable ImmutablePair<@NotNull Iterator<@NotNull FileInformation>, @NotNull Runnable> syncFilesList(final @NotNull LanzouConfiguration configuration, final long directoryId, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
//            final FileInformation directoryInformation = FileManager.selectFile(configuration.getName(), directoryId, connectionId.get());
//            if (directoryInformation == null || directoryInformation.type() == FileSqlInterface.FileSqlType.RegularFile)
//                return null;
//            final List<FileInformation> directoriesInformation = DriverHelper_lanzou.listAllDirectory(configuration, directoryId);
//            if (directoriesInformation == null) {
//                FileManager.deleteFileRecursively(configuration.getName(), directoryId, connectionId.get());
//                connection.commit();
//                return null;
//            }
//            final Set<FileInformation> filesInformation = DriverHelper_lanzou.listFilesInPage(configuration, directoryId, 0);
//            if (directoriesInformation.isEmpty() && filesInformation.isEmpty()) {
//                FileManager.deleteFileRecursively(configuration.getName(), directoryId, connectionId.get());
//                FileManager.insertFileForce(configuration.getName(), directoryInformation, connectionId.get()); // TODO: Only delete children.
//                if (directoryInformation.type() == FileSqlInterface.FileSqlType.Directory)
//                    FileManager.updateDirectoryType(configuration.getName(), directoryId, true, connectionId.get());
//                connection.commit();
//                return Pair.ImmutablePair.makeImmutablePair(HMiscellaneousHelper.getEmptyIterator(), RunnableE.EmptyRunnable);
//            }
//            if (directoryInformation.type() == FileSqlInterface.FileSqlType.EmptyDirectory)
//                FileManager.updateDirectoryType(configuration.getName(), directoryId, false, connectionId.get());
//            final Set<Long> deletedIds = ConcurrentHashMap.newKeySet();
//            deletedIds.addAll(FileManager.selectFileIdByParentId(configuration.getName(), directoryId, _connectionId));
//            deletedIds.remove(configuration.getRootDirectoryId());
//            FileManager.getConnection(configuration.getName(), connectionId.get(), null);
//            return DriverUtil.wrapSuppliersInPages(page -> {
//                final Collection<FileInformation> list = switch (page.intValue()) {
//                    case 0 -> directoriesInformation;
//                    case 1 -> filesInformation;
//                    default -> DriverHelper_lanzou.listFilesInPage(configuration, directoryId, page.intValue() - 1);
//                };
//                if (page.intValue() > 1 && list.isEmpty())
//                    return null;
//                final Set<Long> ids = list.stream().map(FileInformation::id).collect(Collectors.toSet());
//                deletedIds.removeAll(ids);
//                FileManager.mergeFiles(configuration.getName(), list, ids, connectionId.get());
//                return list;
//            }, HExceptionWrapper.wrapConsumer(e -> {
//                if (e == null) {
//                    FileManager.deleteFilesRecursively(configuration.getName(), deletedIds, connectionId.get());
//                    FileManager.calculateDirectorySizeRecursively(configuration.getName(), directoryId, connectionId.get());
//                    connection.commit();
//                } else if (!(e instanceof CancellationException))
//                    throw e;
//            }, connection::close));
//        }
//    }
//
//    static void refreshDirectoryRecursively(final @NotNull LanzouConfiguration configuration, final long directoryId, final @NotNull Set<@NotNull CompletableFuture<?>> futures, final @NotNull AtomicLong runningFutures, final @NotNull AtomicBoolean interruptFlag) throws IOException, SQLException, InterruptedException {
//        try {
//            final Pair.ImmutablePair<Iterator<FileInformation>, Runnable> list = DriverManager_lanzou.syncFilesList(configuration, directoryId, null);
//            if (list == null)
//                return;
//            final Collection<Long> directoryIdList = new LinkedList<>();
//            try {
//                while (list.getFirst().hasNext()) {
//                    final FileInformation information = list.getFirst().next();
//                    if (information.isDirectory())
//                        directoryIdList.add(information.id());
//                }
//            } finally {
//                list.getSecond().run();
//            }
//            if (interruptFlag.get()) return;
//            runningFutures.addAndGet(directoryIdList.size());
//            for (final Long id: directoryIdList)
//                futures.add(CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> DriverManager_lanzou.refreshDirectoryRecursively(configuration,
//                        id.longValue(), futures, runningFutures, interruptFlag)), WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler()));
//        } finally {
//            synchronized (runningFutures) {
//                if (runningFutures.decrementAndGet() == 0)
//                    runningFutures.notifyAll();
//            }
//        }
//    }
//
//    static @NotNull UnionPair<DownloadMethods, FailureReason> getDownloadMethods(final @NotNull LanzouConfiguration configuration, final long fileId, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
//        final FileInformation info = DriverManager_lanzou.getFileInformation(configuration, fileId, null, _connectionId);
//        if (info == null || info.isDirectory()) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
//        final Pair.ImmutablePair<String, Headers> url = DriverHelper_lanzou.getFileDownloadUrl(configuration, fileId);
//        if (url == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", new FileLocation(configuration.getName(), fileId)));
//        return UnionPair.ok(DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(configuration.getFileClient(), Pair.ImmutablePair.makeImmutablePair(url.getFirst(), "GET"), url.getSecond(), info.size(), from, to, DriverHelper_lanzou.headers.newBuilder())));
//    }
//
//    // File Writer
//
//    static void trash(final @NotNull LanzouConfiguration configuration, final @NotNull FileInformation information, final @Nullable String _connectionId, final @Nullable String _trashConnectionId) throws IOException, SQLException, InterruptedException {
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        final AtomicReference<String> trashConnectionId = new AtomicReference<>();
//        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId); final Connection trashConnection = TrashedFileManager.getConnection(configuration.getName(), _trashConnectionId, trashConnectionId)) {
//            final ZonedDateTime time;
//            if (information.isDirectory()) {
//                Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
//                do {
//                    list = DriverManager_lanzou.listFiles(configuration, information.id(), Options.DirectoriesOrFiles.OnlyDirectories, DriverUtil.DefaultLimitPerRequestPage, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection, connectionId.get());
//                    if (list == null)
//                        return;
//                    for (final FileInformation f: list.getC())
//                        DriverManager_lanzou.trash(configuration, f, connectionId.get(), trashConnectionId.get());
//                } while (list.getB().longValue() > 0);
//                do {
//                    list = DriverManager_lanzou.listFiles(configuration, information.id(), Options.DirectoriesOrFiles.Both, DriverUtil.DefaultLimitPerRequestPage, 0, DriverUtil.DefaultOrderPolicy, DriverUtil.DefaultOrderDirection, connectionId.get());
//                    if (list == null)
//                        return;
//                    final CountDownLatch latch = new CountDownLatch(list.getC().size());
//                    for (final FileInformation f: list.getC())
//                        CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() ->
//                                                DriverManager_lanzou.trash(configuration, f, connectionId.get(), trashConnectionId.get()),
//                                latch::countDown), WListServer.IOExecutors).exceptionally(MiscellaneousUtil.exceptionHandler());
//                    latch.await();
//                } while (list.getA().longValue() > 0);
//                time = MiscellaneousUtil.now();
//                DriverHelper_lanzou.trashDirectories(configuration, information.id());
//            } else {
//                time = MiscellaneousUtil.now();
//                DriverHelper_lanzou.trashFile(configuration, information.id());
//            }
//            FileManager.deleteFileRecursively(configuration.getName(), information.id(), connectionId.get());
//            FileManager.calculateDirectorySizeRecursively(configuration.getName(), information.parentId(), connectionId.get());
//            final TrashedFileInformation trashed = TrashedFileInformation.fromFileInformation(information, time, null);
//            TrashedFileManager.insertOrUpdateFile(configuration.getName(), trashed, trashConnectionId.get());
//            trashConnection.commit();
//            connection.commit();
//        }
//    }
//
//    static @NotNull UnionPair<UnionPair<String/*new name*/, FileInformation/*for directory*/>, FailureReason> getDuplicatePolicyName(final @NotNull LanzouConfiguration configuration, final long parentId, final @NotNull String name, final boolean requireDirectory, final Options.@NotNull DuplicatePolicy policy, final @NotNull String duplicateErrorMessage, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
//            final FileInformation parentInformation = DriverManager_lanzou.getFileInformation(configuration, parentId, null, connectionId.get());
//            if (parentInformation == null || !parentInformation.isDirectory())
//                return UnionPair.fail(FailureReason.byNoSuchFile(duplicateErrorMessage + " Getting duplicate policy name (parent).", new FileLocation(configuration.getName(), parentId)));
//            if (parentInformation.type() == FileSqlInterface.FileSqlType.Directory && FileManager.selectFileCountByParentId(configuration.getName(), parentId, connectionId.get()) == 0) {
//                DriverManager_lanzou.waitSyncComplete(DriverManager_lanzou.syncFilesList(configuration, parentId, connectionId.get()));
//                connection.commit();
//            }
//            FileInformation information = FileManager.selectFileInDirectory(configuration.getName(), parentId, name, connectionId.get());
//            if (information == null)
//                return UnionPair.ok(UnionPair.ok(name));
//            if (requireDirectory && information.isDirectory())
//                return UnionPair.ok(UnionPair.fail(information));
//            switch (policy) {
//                case DuplicatePolicy.ERROR:
//                    return UnionPair.fail(FailureReason.byDuplicateError(duplicateErrorMessage, new FileLocation(configuration.getName(), parentId), name));
//                case DuplicatePolicy.OVER:
//                    DriverManager_lanzou.trash(configuration, information, connectionId.get(), null);
//                    connection.commit();
//                    return UnionPair.ok(UnionPair.ok(name));
//                case DuplicatePolicy.KEEP:
//                    int retry = 0;
//                    final Pair.ImmutablePair<String, String> wrapper = DriverUtil.getRetryWrapper(name);
//                    while (information != null && !(requireDirectory && information.isDirectory()))
//                        information = FileManager.selectFileInDirectory(configuration.getName(), parentId, wrapper.getFirst() + (++retry) + wrapper.getSecond(), connectionId.get());
//                    return information == null ? UnionPair.ok(UnionPair.ok(wrapper.getFirst() + retry + wrapper.getSecond())) : UnionPair.ok(UnionPair.fail(information));
//            }
//            throw new RuntimeException("Unreachable!");
//        }
//    }
//
//    static @NotNull UnionPair<FileInformation, FailureReason> createDirectory(final @NotNull LanzouConfiguration configuration, final long parentId, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
//            final UnionPair<UnionPair<String, FileInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, parentId, name, true, policy, "Creating directory.", connectionId.get());
//            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
//            if (duplicate.getT().isFailure()) {connection.commit();return UnionPair.ok(duplicate.getT().getE());}
//            final String realName = duplicate.getT().getT();
//            final UnionPair<FileInformation, FailureReason> information = DriverHelper_lanzou.createDirectory(configuration, realName, parentId);
//            if (information.isSuccess()) {
//                FileManager.insertFileForce(configuration.getName(), information.getT(), connectionId.get());
//                FileManager.updateDirectoryType(configuration.getName(), parentId, false, connectionId.get());
//            }
//            connection.commit();
//            return information;
//        }
//    }
//
//    static @NotNull UnionPair<UploadMethods, FailureReason> getUploadMethods(final @NotNull LanzouConfiguration configuration, final long parentId, final @NotNull String name, final @NotNull String md5, final long size, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
//        if (!md5.isEmpty() && !HMessageDigestHelper.MD5.pattern.matcher(md5).matches())
//            throw new IllegalStateException("Invalid md5." + ParametersMap.create().add("md5", md5));
//        if (!DriverHelper_lanzou.filenamePredication.test(name))
//            return UnionPair.fail(FailureReason.byInvalidName("Uploading.", new FileLocation(configuration.getName(), parentId), name));
//        if (size > configuration.getMaxSizePerFile())
//            return UnionPair.fail(FailureReason.byExceedMaxSize("Uploading.", size, configuration.getMaxSizePerFile(), new FileLocation(configuration.getName(), parentId), name));
//        final int intSize = Math.toIntExact(size);
//        final UnionPair<UnionPair<String, FileInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, parentId, name, false, policy, "Uploading.", _connectionId);
//        if (duplicate.isFailure()) return UnionPair.fail(duplicate.getE());
//        final String realName = duplicate.getT().getT();
//        final AtomicReference<UnionPair<FileInformation, FailureReason>> reference = new AtomicReference<>(null);
//        final Pair.ImmutablePair<List<ConsumerE<ByteBuf>>, Runnable> methods;
//        if (size == 0) {
//            reference.set(DriverHelper_lanzou.uploadFile(configuration, realName, parentId, Unpooled.EMPTY_BUFFER, md5));
//            methods = Pair.ImmutablePair.makeImmutablePair(List.of(), RunnableE.EmptyRunnable);
//        } else
//            methods = DriverUtil.splitUploadMethodEveryFileTransferBufferSize(b ->
//                reference.set(DriverHelper_lanzou.uploadFile(configuration, realName, parentId, b, md5)), intSize);
//        return UnionPair.ok(new UploadMethods(methods.getFirst(), () -> {
//            final UnionPair<FileInformation, FailureReason> result = reference.get();
//            if (result == null) return null;
//            final FileInformation information = result.getT();
//            final AtomicReference<String> connectionId = new AtomicReference<>();
//            try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
//                FileManager.insertFileForce(configuration.getName(), information, connectionId.get());
//                FileManager.updateDirectoryType(configuration.getName(), parentId, false, connectionId.get());
//                FileManager.updateDirectorySize(configuration.getName(), parentId, size, connectionId.get());
//                connection.commit();
//            }
//            return information;
//        }, HExceptionWrapper.wrapRunnable(() -> methods.getSecond().run())));
//    }
//
//    static @NotNull UnionPair<FileInformation, FailureReason> move(final @NotNull LanzouConfiguration configuration, final @NotNull FileInformation source, final long targetId, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
//        if (source.parentId() == targetId) return UnionPair.ok(source);
//        if (policy == Options.DuplicatePolicy.KEEP) // TODO: vip
//            throw new UnsupportedOperationException("Driver lanzou not support rename file while moving.");
//        if (source.isDirectory()) // TODO: directory
//            throw new UnsupportedOperationException("Driver lanzou not support move directory.");
//        final AtomicReference<String> connectionId = new AtomicReference<>();
//        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
//            final UnionPair<UnionPair<String, FileInformation>, FailureReason> duplicate = DriverManager_lanzou.getDuplicatePolicyName(configuration, targetId, source.name(), false, policy, "Moving.", connectionId.get());
//            if (duplicate.isFailure()) {connection.commit();return UnionPair.fail(duplicate.getE());}
//            assert duplicate.getT().getT().equals(source.name());
//            final UnionPair<ZonedDateTime, FailureReason> information = DriverHelper_lanzou.moveFile(configuration, source.id(), targetId);
//            if (information == null) {connection.commit();return UnionPair.ok(source);}
//            if (information.isFailure()) {connection.commit();return UnionPair.fail(information.getE());}
//            FileManager.mergeFile(configuration.getName(), new FileInformation(source.location(), targetId,
//                    source.name(), source.type(), source.size(), source.createTime(), information.getT(), source.md5(), source.others()), connectionId.get());
//            FileManager.updateDirectorySize(configuration.getName(), source.parentId(), -source.size(), connectionId.get());
//            FileManager.updateDirectoryType(configuration.getName(), targetId, false, connectionId.get());
//            FileManager.updateDirectorySize(configuration.getName(), targetId, source.size(), connectionId.get());
//            connection.commit();
//            return UnionPair.ok(source);
//        }
//    }
//
//    static @NotNull UnionPair<FileInformation, FailureReason> rename(final @NotNull LanzouConfiguration configuration, final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy, final @Nullable String _connectionId) throws IOException, SQLException {
//        if (!DriverHelper_lanzou.filenamePredication.test(name))
//            return UnionPair.fail(FailureReason.byInvalidName(name, new FileLocation(configuration.getName(), id), name));
//        throw new UnsupportedOperationException("Driver lanzou not support rename."); // TODO: vip
//    }
}
