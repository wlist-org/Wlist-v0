package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.xuxiaocheng.HeadLibs.Annotations.Range.LongRange;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverNetworkHelper;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Server.DriverManager;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Driver_123pan_NoCache implements DriverInterface<DriverConfiguration_123pan> {
    protected @NotNull DriverConfiguration_123pan configuration = new DriverConfiguration_123pan();

    @Override
    public @NotNull DriverConfiguration_123pan getConfiguration() {
        return this.configuration;
    }

    @Override
    public void initialize(final @NotNull DriverConfiguration_123pan configuration) throws SQLException {
        this.configuration = configuration;
    }

    @Override
    public void uninitialize() throws IllegalParametersException, IOException, SQLException {
        DriverHelper_123pan.logout(this.configuration);
        this.configuration.getCacheSide().setLastFileCacheBuildTime(null);
        this.configuration.getCacheSide().setLastFileIndexBuildTime(null);
        this.configuration.getCacheSide().setModified(true);
    }

    @Override
    public void buildCache() throws IllegalParametersException, IOException {
        DriverHelper_123pan.resetUserInformation(this.configuration);
    }

    @Override
    public void buildIndex() throws SQLException {
    }

    protected long toRootId(final long id) {
        return id == 0 ? this.configuration.getWebSide().getRootDirectoryId() : id;
    }

    @Override
    public void forceRefreshDirectory(final @NotNull FileLocation location) throws IllegalParametersException, IOException, SQLException {
    }

    @Override
    public Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull FileSqlInformation>> list(final @NotNull FileLocation location, final @LongRange(minimum = 0) int limit, final @LongRange(minimum = 0) int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IllegalParametersException, IOException, SQLException {
        return DriverHelper_123pan.listFiles(this.configuration, this.toRootId(location.id()), limit, page, policy, direction);
    }

    @Override
    public @Nullable FileSqlInformation info(final @NotNull FileLocation location) throws IllegalParametersException, IOException, SQLException {
        final long id = this.toRootId(location.id());
        if (id == this.configuration.getWebSide().getRootDirectoryId()) return RootDriver.getDriverInformation(this.configuration);
        if (id == 0) return null; // Out of Root File Tree.
        return DriverHelper_123pan.getFilesInformation(this.configuration, List.of(id)).get(id);
    }

    @Override
    public @NotNull UnionPair<@NotNull DownloadMethods, @NotNull FailureReason> download(final @NotNull FileLocation location, final @LongRange(minimum = 0) long from, final @LongRange(minimum = 0) long to) throws IllegalParametersException, IOException, SQLException {
        final FileSqlInformation info = this.info(location);
        if (info == null || info.isDirectory()) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", location));
        final String url = DriverHelper_123pan.getFileDownloadUrl(this.configuration, info);
        if (url == null) return UnionPair.fail(FailureReason.byNoSuchFile("Downloading.", location));
        return UnionPair.ok(DriverUtil.toCachedDownloadMethods(DriverUtil.getDownloadMethodsByUrlWithRangeHeader(DriverHelper_123pan.fileClient, Pair.ImmutablePair.makeImmutablePair(url, "GET"), info.size(), from, to, null)));
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> createDirectory(final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        return DriverHelper_123pan.createDirectory(this.configuration, this.toRootId(parentLocation.id()), directoryName, policy);
    }

    @Override
    public @NotNull UnionPair<@NotNull UploadMethods, @NotNull FailureReason> upload(final @NotNull FileLocation parentLocation, final @NotNull String filename, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        final UnionPair<UnionPair<FileSqlInformation, DriverHelper_123pan.UploadIdentifier_123pan>, FailureReason> requestUploadData = DriverHelper_123pan.uploadRequest(this.configuration, this.toRootId(parentLocation.id()), filename, size, md5, policy);
        if (requestUploadData.isFailure()) return UnionPair.fail(requestUploadData.getE());
        if (requestUploadData.getT().isSuccess()) {
            final FileSqlInformation information = requestUploadData.getT().getT();
            return UnionPair.ok(new UploadMethods(List.of(), () -> information, RunnableE.EmptyRunnable));
        }
        final int partCount = MiscellaneousUtil.calculatePartCount(size, DriverHelper_123pan.UploadPartSize);
        final List<String> urls = DriverHelper_123pan.uploadPare(this.configuration, requestUploadData.getT().getE(), partCount);
        long readSize = 0;
        final List<ConsumerE<ByteBuf>> consumers = new ArrayList<>(partCount);
        final Collection<Runnable> finishers = new ArrayList<>(partCount);
        final AtomicInteger countDown = new AtomicInteger(urls.size());
        for (final String url: urls) {
            //noinspection NumericCastThatLosesPrecision
            final int len = (int) Math.min(DriverHelper_123pan.UploadPartSize, (size - readSize));readSize += len;
            final Pair.ImmutablePair<List<ConsumerE<ByteBuf>>, Runnable> split = DriverUtil.splitUploadMethod(b -> {
                DriverNetworkHelper.postWithBody(DriverHelper_123pan.fileClient, Pair.ImmutablePair.makeImmutablePair(url, "PUT"), null,
                        DriverNetworkHelper.createOctetStreamRequestBody(b)).execute().close();
                countDown.getAndDecrement();
            }, len);
            consumers.addAll(split.getFirst());
            finishers.add(split.getSecond());
        }
       return UnionPair.ok(new UploadMethods(consumers, () -> {
            if (countDown.get() > 0) return null;
           return DriverHelper_123pan.uploadComplete(this.configuration, requestUploadData.getT().getE(), partCount);
        }, () -> finishers.forEach(Runnable::run)));
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public void delete(final @NotNull FileLocation location) throws Exception {
        if (location.id() == 0 || location.id() == this.configuration.getWebSide().getRootDirectoryId()) {
            DriverManager.removeDriver(this.configuration.getName());
            return;
        }
        DriverHelper_123pan.trashFiles(this.configuration, List.of(this.toRootId(location.id())), true);
    }

    @SuppressWarnings("OverlyBroadThrowsClause")
    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> copy(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetParentLocation, final @NotNull String targetFilename, final Options.@NotNull DuplicatePolicy policy) throws Exception {
        final FileSqlInformation source = this.info(sourceLocation);
        if (source == null)
            return UnionPair.fail(FailureReason.byNoSuchFile("Copying.", sourceLocation));
        final UnionPair<UploadMethods, FailureReason> methods = this.upload(targetParentLocation, source.name(), source.size(), source.md5(), policy);
        if (methods.isFailure())
            return UnionPair.fail(methods.getE());
        try {
            final FileSqlInformation information = methods.getT().supplier().get();
            if (information == null)
                throw new IllegalStateException("Failed to copy file. [Unknown]." + ParametersMap.create().add("configuration", this.configuration).add("sourceLocation", sourceLocation).add("targetParentLocation", targetParentLocation).add("targetFilename", targetFilename).add("policy", policy).add("source", source));
            return UnionPair.ok(information);
        } finally {
            methods.getT().finisher().run();
        }
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> move(final @NotNull FileLocation sourceLocation, final @NotNull FileLocation targetLocation, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        if (policy != Options.DuplicatePolicy.KEEP)
            throw new UnsupportedOperationException("Driver_123pan_NoCache method 'move' only supports duplicate policy KEEP.");
        final Long sourceId = sourceLocation.id();
        final FileSqlInformation information = DriverHelper_123pan.moveFiles(this.configuration, List.of(sourceId), this.toRootId(targetLocation.id()), policy).get(sourceId);
        return information == null ? UnionPair.fail(FailureReason.byNoSuchFile("Moving.", sourceLocation)) : UnionPair.ok(information);
    }

    @Override
    public @NotNull UnionPair<@NotNull FileSqlInformation, @NotNull FailureReason> rename(final @NotNull FileLocation sourceLocation, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IllegalParametersException, IOException, SQLException {
        if (policy != Options.DuplicatePolicy.ERROR)
            throw new UnsupportedOperationException("Driver_123pan_NoCache method 'rename' only supports duplicate policy ERROR.");
        return DriverHelper_123pan.renameFile(this.configuration, sourceLocation.id(), name, policy);
    }

    @Override
    public @NotNull String toString() {
        return "Driver_123pan_NoCache{" +
                "configuration=" + this.configuration +
                '}';
    }
}
