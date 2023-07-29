package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

public class Trash_123pan {//extends Trash_123pan_NoCache {
//    @Override
//    public void initialize(final @NotNull Driver_123pan driver) throws SQLException {
//        TrashedFileManager.quicklyInitialize(new TrashedSqlHelper(PooledDatabase.instance.getInstance(), driver.configuration.getName()), null);
//        this.driver = driver;
//    }
//
//    @Override
//    public void uninitialize() throws SQLException {
//        TrashedFileManager.quicklyUninitialize(this.driver.configuration.getName(), null);
//    }
//
//    @Override
//    public void buildCache() throws SQLException {
//        final LocalDateTime old = this.driver.configuration.getCacheSide().getLastTrashIndexBuildTime();
//        final LocalDateTime now = LocalDateTime.now();
//        if (old == null || Duration.between(old, now).toMillis() > TimeUnit.HOURS.toMillis(3))
//            this.buildIndex();
//    }
//
//    @Override
//    public void buildIndex() throws SQLException {
//        this.driver.configuration.getCacheSide().setLastTrashIndexBuildTime(LocalDateTime.now());
//        this.driver.configuration.getCacheSide().setModified(true);
//        final Iterator<TrashedSqlInformation> iterator = TrashManager_123pan.listAllFilesNoCache(this.driver.configuration, null, WListServer.IOExecutors).getB();
//        while (iterator.hasNext())
//            iterator.next();
//    }
//
//    @Override
//    public Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull TrashedSqlInformation>> list(final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws Exception {
//        return TrashManager_123pan.listFiles(this.driver.configuration, limit, page, policy, direction, true, null, WListServer.IOExecutors);
//    }
//
//    @Override
//    public @Nullable TrashedSqlInformation info(final long id) throws IllegalParametersException, IOException, SQLException {
//        return TrashManager_123pan.getFileInformation(this.driver.configuration, id, true, null, WListServer.IOExecutors);
//    }
//
//    @Override
//    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> restore(final long id, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
//        return TrashManager_123pan.restoreFile(this.driver.configuration, id, path, policy, true, null, WListServer.IOExecutors);
//    }
//
//    @Override
//    public void delete(final long id) throws IllegalParametersException, IOException, SQLException {
//        TrashManager_123pan.deleteFile(this.driver.configuration, id, true, null);
//    }
//
//    @Override
//    public void deleteAll() throws IllegalParametersException, IOException, SQLException {
//        TrashManager_123pan.deleteAllFiles(this.driver.configuration, null);
//    }
//
//    @Override
//    public @Nullable DownloadMethods download(final long id, final long from, final long to) throws IllegalParametersException, IOException, SQLException {
//        return TrashManager_123pan.getDownloadMethods(this.driver.configuration, id, from, to, true, null, WListServer.IOExecutors);
//    }
//
//    @Override
//    public @NotNull UnionPair<@NotNull TrashedSqlInformation, @NotNull FailureReason> rename(final long id, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
//        return TrashManager_123pan.renameFile(this.driver.configuration, id, name, policy, true, null, WListServer.IOExecutors);
//    }
//
//    @Override
//    public @NotNull String toString() {
//        return "Trash_123pan{" +
//                "driver=" + this.driver +
//                '}';
//    }
}
