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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FilesAssistant {
    private FilesAssistant() {
        super();
    }

    private static @NotNull @Unmodifiable List<@NotNull String> calculateChecksums(final @NotNull File file, final @NotNull Collection<@NotNull UploadChecksum> requires) throws IOException {
        if (requires.isEmpty())
            return List.of();
//        final NavigableMap<Long, List<Integer>> map = new TreeMap<>();
//        final List<HMessageDigestHelper.MessageDigestAlgorithm> algorithms = new ArrayList<>(requires.size());
//        final List<MessageDigest> digests = new ArrayList<>(requires.size());
//        int i = 0;
//        for (final UploadChecksum checksum: requires) {
//            assert checksum.start() < checksum.end();
//            final int k = i++;
//            map.compute(checksum.start(), (a, b) -> Objects.requireNonNullElseGet(b, ArrayList::new)).add(k);
//            map.compute(checksum.end(), (a, b) -> Objects.requireNonNullElseGet(b, ArrayList::new)).add(-k-1);
//            final HMessageDigestHelper.MessageDigestAlgorithm algorithm = UploadChecksum.getAlgorithm(checksum.algorithm());
//            algorithms.add(algorithm);
//            digests.add(algorithm.getDigester());
//        }
        final String[] checksums = new String[requires.size()];
        try (final RandomAccessFile access = new RandomAccessFile(file, "r")) {
//            while (!map.isEmpty()) {
//                final Map.Entry<Long, List<Integer>> entry = map.pollFirstEntry();
//            }
            // TODO: optimise.
            int i = 0;
            final byte[] buffer = new byte[8192];
            for (final UploadChecksum checksum: requires) {
                assert checksum.start() < checksum.end();
                access.seek(checksum.start());
                final HMessageDigestHelper.MessageDigestAlgorithm algorithm = UploadChecksum.getAlgorithm(checksum.algorithm());
                final MessageDigest digester = algorithm.getDigester();
                long length = checksum.end() - checksum.start();
                while (true) {
                    if (length <= 0)
                        break;
                    final int read = access.read(buffer, 0, Math.toIntExact(Math.min(8192, length)));
                    if (read < 0)
                        break;
                    if (read == 0)
                        AndroidSupporter.onSpinWait();
                    digester.update(buffer, 0, read);
                    length -= read;
                }
                checksums[i++] = algorithm.digest(digester);
            }
        }
        return List.of(checksums);
    }

    public static @Nullable VisibleFailureReason upload(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull File file, final Options.@NotNull DuplicatePolicy policy, final @NotNull FileLocation location) throws IOException, InterruptedException, WrongStateException {
        if (!file.isFile() || !file.canRead())
            throw new FileNotFoundException("Not a upload-able file." + ParametersMap.create().add("file", file));
        final int thread = ClientConfiguration.get().threadCount();
        final UnionPair<UploadConfirm, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.requestUploadFile(client, TokenAssistant.getToken(address, username), location, file.getName(), file.length(), policy);
        }
        if (confirm == null)
            return new VisibleFailureReason(FailureKind.Others, location, "Requesting.");
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
            return new VisibleFailureReason(FailureKind.Others, location, "Confirming.");
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(thread > 0 ? thread : Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory("UploadingExecutor@" + file), information.parallel().size(), (t, e) ->
                HLog.getInstance("ClientLogger").log(HLogLevel.MISTAKE, "Something went wrong when uploading.", ParametersMap.create()
                        .add("address", address).add("username", username).add("file", file)));
        final AtomicBoolean flag = new AtomicBoolean(false);
        try {
            final CountDownLatch latch = new CountDownLatch(information.parallel().size());
            int i = 0;
            for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                final int index = i++;
                CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                    final ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(NetworkTransmission.FileTransferBufferSize, NetworkTransmission.FileTransferBufferSize);
                    try (final WListClientInterface c = WListClientManager.quicklyGetClient(address);
                         final RandomAccessFile accessFile = new RandomAccessFile(file, "r");
                         final FileChannel channel = accessFile.getChannel()) {
                        channel.position(pair.getFirst().longValue());
                        long length = pair.getSecond().longValue() - pair.getFirst().longValue();
                        while (length > 0) {
                            final int l = buf.writeBytes(channel, Math.toIntExact(Math.min(length, NetworkTransmission.FileTransferBufferSize)));
                            if (l < 0 || !OperateFilesHelper.uploadFile(c, TokenAssistant.getToken(address, username), confirm.getT().id(), index, buf.retain())) {
                                flag.set(true);
                                while (latch.getCount() > 0)
                                    latch.countDown();
                            }
                            length -= l;
                            buf.clear();
                        }
                    } finally {
                        buf.release();
                    }
                }, latch::countDown), executors).exceptionally(MiscellaneousUtil.exceptionHandler());
            }
            latch.await();
        } finally {
            executors.shutdownGracefully();
        }
        if (flag.get())
            return new VisibleFailureReason(FailureKind.Others, location, "Uploading.");
        final boolean success;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            success = OperateFilesHelper.finishUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
        }
        return success ? null : new VisibleFailureReason(FailureKind.Others, location, "Finishing.");
    }
}
