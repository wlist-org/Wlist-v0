package com.xuxiaocheng.WList.Client.Assistants;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Beans.UploadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class FilesAssistant {
    private FilesAssistant() {
        super();
    }

    private static @NotNull @Unmodifiable List<@NotNull String> calculateChecksums(final @NotNull File file, final @NotNull Collection<@NotNull UploadChecksum> requirements) throws IOException {
        if (requirements.isEmpty())
            return List.of();
        final NavigableMap<Long, List<Integer>> map = new TreeMap<>();
        final List<HMessageDigestHelper.MessageDigestAlgorithm> algorithms = new ArrayList<>(requirements.size());
        final List<MessageDigest> digests = new ArrayList<>(requirements.size());
        int i = 0;
        for (final UploadChecksum checksum: requirements) {
            assert checksum.start() < checksum.end();
            final int k = i++;
            map.compute(checksum.start(), (a, b) -> Objects.requireNonNullElseGet(b, ArrayList::new)).add(k);
            map.compute(checksum.end(), (a, b) -> Objects.requireNonNullElseGet(b, ArrayList::new)).add(-k-1);
            final HMessageDigestHelper.MessageDigestAlgorithm algorithm = UploadChecksum.getAlgorithm(checksum.algorithm());
            algorithms.add(algorithm);
            digests.add(algorithm.getDigester());
        }
        final String[] checksums = new String[requirements.size()];
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer(8192);
        try (final RandomAccessFile access = new RandomAccessFile(file, "r");
             final FileChannel channel = access.getChannel()) {
            final Collection<Integer> reading = new HashSet<>();
            while (!map.isEmpty()) {
                final Map.Entry<Long, List<Integer>> entry = map.pollFirstEntry();
                for (final Integer integer: entry.getValue())
                    if (integer.intValue() >= 0)
                        reading.add(integer);
                    else {
                        reading.remove(integer);
                        final int k = -integer.intValue()-1;
                        checksums[k] = algorithms.get(k).digest(digests.get(k));
                        digests.get(k).reset();
                    }
                if (reading.isEmpty())
                    continue;
                final Map.Entry<Long, List<Integer>> next = map.firstEntry();
                if (next == null)
                    break;
                channel.position(entry.getKey().longValue());
                long length = next.getKey().longValue() - entry.getKey().longValue();
                try (final FileLock ignoredLock = channel.lock(entry.getKey().longValue(), length, true)) {
                    while (length > 0) {
                        final int read = buffer.clear().writeBytes(channel, Math.toIntExact(Math.min(8192, length)));
                        if (read < 0)
                            break;
                        if (read == 0)
                            AndroidSupporter.onSpinWait();
                        for (final Integer integer: reading)
                            digests.get(integer.intValue()).update(buffer.array(), 0, read);
                        length -= read;
                    }
                }
            }
        } finally {
            buffer.release();
        }
        for (final String checksum: checksums)
            if (checksum == null)
                throw new IllegalStateException("Failed to calculate checksum." + ParametersMap.create().add("file", file).add("requirements", requirements).add("checksums", checksums));
        return List.of(checksums);
    }

    public static @Nullable VisibleFailureReason upload(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull File file, final Options.@NotNull DuplicatePolicy policy, final @NotNull FileLocation parent) throws IOException, InterruptedException, WrongStateException {
        if (!file.isFile() || !file.canRead())
            throw new FileNotFoundException("Not a upload-able file." + ParametersMap.create().add("file", file));
        final UnionPair<UploadConfirm, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.requestUploadFile(client, TokenAssistant.getToken(address, username), parent, file.getName(), file.length(), policy);
        }
        if (confirm == null)
            return new VisibleFailureReason(FailureKind.Others, parent, "Requesting.");
        if (confirm.isFailure())
            return confirm.getE();
        final List<String> checksums = FilesAssistant.calculateChecksums(file, confirm.getT().checksums());
        HLog.getInstance("ClientLogger").log(HLogLevel.LESS, "Calculated checksums.", ParametersMap.create()
                .add("file", file).add("checksums", checksums));
        final UploadConfirm.UploadInformation information;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            information = OperateFilesHelper.confirmUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id(), checksums);
        }
        if (information == null)
            return new VisibleFailureReason(FailureKind.Others, parent, "Confirming.");
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(ClientConfiguration.get().threadCount() > 0 ?
                ClientConfiguration.get().threadCount() : Math.min(Runtime.getRuntime().availableProcessors(), information.parallel().size()),
                new DefaultThreadFactory("UploadingExecutor@" + file), information.parallel().size(), (t, e) ->
                HLog.getInstance("ClientLogger").log(HLogLevel.MISTAKE, "Something went wrong when uploading.", ParametersMap.create()
                        .add("address", address).add("username", username).add("file", file)));
        final AtomicBoolean flag = new AtomicBoolean(false);
        final boolean success;
        try (final RandomAccessFile accessFile = new RandomAccessFile(file, "r");
             final FileChannel fileChannel = accessFile.getChannel()) { // Only lock.
            final CountDownLatch failure = new CountDownLatch(information.parallel().size());
            final CountDownLatch latch = new CountDownLatch(information.parallel().size());
            int i = 0;
            for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                final int index = i++;
                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                    final ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(NetworkTransmission.FileTransferBufferSize, NetworkTransmission.FileTransferBufferSize);
                    long length = pair.getSecond().longValue() - pair.getFirst().longValue();
                    try (final WListClientInterface c = WListClientManager.quicklyGetClient(address);
                         final RandomAccessFile access = new RandomAccessFile(file, "r");
                         final FileChannel channel = access.getChannel().position(pair.getFirst().longValue());
                         final FileLock ignoredLock = fileChannel.lock(pair.getFirst().longValue(), length, true)) {
                        while (length > 0) {
                            final int read = buf.writeBytes(channel, Math.toIntExact(Math.min(length, NetworkTransmission.FileTransferBufferSize)));
                            if (read < 0 || !OperateFilesHelper.uploadFile(c, TokenAssistant.getToken(address, username), confirm.getT().id(), index, buf.retain())) {
                                flag.set(true);
                                while (failure.getCount() > 0)
                                    failure.countDown();
                            }
                            if (failure.getCount() <= 0)
                                break;
                            length -= read;
                            buf.clear();
                        }
                    } finally {
                        buf.release();
                    }
                }, latch::countDown), executors).exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            failure.await();
            latch.await();
        } finally {
            final Future<?> future = executors.shutdownGracefully().await();
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                success = OperateFilesHelper.finishUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
            }
            future.sync(); // rethrow.
        }
        if (flag.get())
            return new VisibleFailureReason(FailureKind.Others, parent, "Uploading.");
        return success ? null : new VisibleFailureReason(FailureKind.Others, parent, "Finishing.");
    }

    public static @Nullable VisibleFailureReason download(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final @NotNull Predicate<? super @NotNull DownloadConfirm> continuer, final @NotNull File file) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<DownloadConfirm, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) { // TODO: record downloading progress.
            confirm = OperateFilesHelper.requestDownloadFile(client, TokenAssistant.getToken(address, username), location, 0, Long.MAX_VALUE);
        }
        if (confirm == null)
            return new VisibleFailureReason(FailureKind.Others, location, "Requesting.");
        if (confirm.isFailure())
            return confirm.getE();
        if (!continuer.test(confirm.getT()))
            return null;
        final DownloadConfirm.DownloadInformation information;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            information = OperateFilesHelper.confirmDownloadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
        }
        if (information == null)
            return new VisibleFailureReason(FailureKind.Others, location, "Confirming.");
        try (final RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
             final FileChannel channel = accessFile.getChannel()) {
            channel.truncate(confirm.getT().downloadingSize());
        }
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(ClientConfiguration.get().threadCount() > 0 ?
                ClientConfiguration.get().threadCount() : Math.min(Runtime.getRuntime().availableProcessors(), information.parallel().size()),
                new DefaultThreadFactory("DownloadingExecutor@" + file), information.parallel().size(), (t, e) ->
                HLog.getInstance("ClientLogger").log(HLogLevel.MISTAKE, "Something went wrong when downloading.", ParametersMap.create()
                        .add("address", address).add("username", username).add("file", file).add("location", location)));
        final AtomicBoolean flag = new AtomicBoolean(false);
        try {
            final CountDownLatch latch = new CountDownLatch(information.parallel().size());
            int i = 0;
            long position = 0;
            for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                if (position != pair.getFirst().longValue())
                    throw new IllegalStateException("Invalid download chunk." + ParametersMap.create().add("address", address).add("username", username).add("file", file)
                            .add("location", location).add("confirm", confirm).add("information", information));
                position = pair.getSecond().longValue();
                final int k = i++;
                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                    long length = pair.getSecond().longValue() - pair.getFirst().longValue();
                    try (final WListClientInterface c = WListClientManager.quicklyGetClient(address);
                         final RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
                         final FileChannel channel = accessFile.getChannel().position(pair.getFirst().longValue());
                         final FileLock ignoredLock = channel.lock(pair.getFirst().longValue(), length, false)) {
                        while (length > 0) {
                            final ByteBuf buf = OperateFilesHelper.downloadFile(c, TokenAssistant.getToken(address, username), confirm.getT().id(), k);
                            if (buf == null) {
                                flag.set(true);
                                while (latch.getCount() > 0)
                                    latch.countDown();
                                break;
                            }
                            try {
                                if (latch.getCount() <= 0)
                                    break;
                                length -= buf.readableBytes();
                                buf.readBytes(channel, buf.readableBytes());
                            } finally {
                                buf.release();
                            }
                        }
                    }
                }, latch::countDown), executors).exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            if (position != confirm.getT().downloadingSize())
                throw new IllegalStateException("Invalid download size." + ParametersMap.create().add("address", address).add("username", username).add("file", file)
                        .add("location", location).add("confirm", confirm).add("information", information).add("position", position));
            latch.await();
        } finally {
            final Future<?> future = executors.shutdownGracefully().await();
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                OperateFilesHelper.finishDownloadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
            }
            future.sync(); // rethrow.
        }
        if (flag.get())
            return new VisibleFailureReason(FailureKind.Others, location, "Downloading.");
        return null;
    }
}
