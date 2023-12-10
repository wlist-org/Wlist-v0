package com.xuxiaocheng.WList.Client.Assistants;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Functions.PredicateE;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateProgressHelper;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.InstantaneousProgressState;
import com.xuxiaocheng.WList.Commons.Beans.RefreshConfirm;
import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import com.xuxiaocheng.WList.Commons.Beans.UploadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Options.FilterPolicy;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class FilesAssistant {
    private FilesAssistant() {
        super();
    }

    private static @NotNull InstantaneousProgressState callbackCore(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull Collection<@NotNull String> ids) throws InterruptedException {
        final Collection<InstantaneousProgressState> states = ConcurrentHashMap.newKeySet();
        try {
            HMultiRunHelper.runConsumers(BroadcastAssistant.CallbackExecutors, ids, HExceptionWrapper.wrapConsumer(id -> {
                final InstantaneousProgressState state;
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                    state = OperateProgressHelper.getProgress(client, TokenAssistant.getToken(address, username), id);
                }
                if (state != null)
                    states.add(state);
            }));
        } catch (final RuntimeException exception) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            states.clear();
        }
        return new InstantaneousProgressState(AndroidSupporter.streamToList(states.stream().flatMap(s -> s.stages().stream())));
    }

    private static void callbackSync(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull Consumer<? super @NotNull InstantaneousProgressState> callback, final @NotNull AtomicBoolean failure, final @NotNull CountDownLatch latch, final @NotNull Collection<@NotNull String> ids) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(ClientConfiguration.get().progressStartDelay());
        int failed = 0;
        while (!latch.await(ClientConfiguration.get().progressInterval(), TimeUnit.MILLISECONDS)) {
            final InstantaneousProgressState state = FilesAssistant.callbackCore(address, username, ids);
            if (state.stages().isEmpty()) {
                if (++failed > 3) {
                    failure.set(true);
                    break;
                }
            } else {
                failed = 0;
                BroadcastAssistant.CallbackExecutors.submit(() -> callback.accept(state)).addListener(MiscellaneousUtil.exceptionListener());
            }
        }
    }

    private static @NotNull ScheduledFuture<?> callbackAsync(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull ScheduledExecutorService executor, final @NotNull Consumer<? super @NotNull InstantaneousProgressState> callback, final @NotNull Collection<@NotNull String> ids) {
        final AtomicInteger failed = new AtomicInteger(0);
        return executor.scheduleWithFixedDelay(HExceptionWrapper.wrapRunnable(() -> {
            final InstantaneousProgressState state = FilesAssistant.callbackCore(address, username, ids);
            if (state.stages().isEmpty()) {
                if (failed.incrementAndGet() >= 3)
                    throw new CancellationException();
            } else {
                failed.set(0);
                callback.accept(state);
            }
        }), ClientConfiguration.get().progressStartDelay(), ClientConfiguration.get().progressInterval(), TimeUnit.MILLISECONDS);
    }


    private static @NotNull @Unmodifiable List<@NotNull String> calculateChecksumsCore(final @NotNull Collection<@NotNull UploadChecksum> requirements, final @NotNull Consumer<? super @NotNull BiConsumer<? super @NotNull Consumer<? super @NotNull Long>, ? super @NotNull BiConsumer<? super @NotNull Long, ? super @NotNull Consumer<? super @NotNull BiFunction<? super @NotNull Integer, ? super @NotNull ByteBuf, @NotNull Integer>>>>> runner) {
        if (requirements.isEmpty())
            return List.of();
        final NavigableMap<Long, List<Integer>> map = new TreeMap<>();
        final List<HMessageDigestHelper.MessageDigestAlgorithm> algorithms = new ArrayList<>(requirements.size());
        final List<MessageDigest> digests = new ArrayList<>(requirements.size());
        int i = 0;
        for (final UploadChecksum checksum: requirements) {
            assert checksum.start() < checksum.end();
            final int k1 = i++;
            map.compute(checksum.start(), (a, b) -> Objects.requireNonNullElseGet(b, ArrayList::new)).add(k1);
            map.compute(checksum.end(), (a, b) -> Objects.requireNonNullElseGet(b, ArrayList::new)).add(-k1 -1);
            final HMessageDigestHelper.MessageDigestAlgorithm algorithm = UploadChecksum.getAlgorithm(checksum.algorithm());
            algorithms.add(algorithm);
            digests.add(algorithm.getDigester());
        }
        final String[] checksums = new String[requirements.size()];
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer(8192, 8192);
        runner.accept(HExceptionWrapper.wrapBiConsumer((seeker, reader) -> {
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
                seeker.accept(entry.getKey().longValue());
                final long l = next.getKey().longValue() - entry.getKey().longValue();
                reader.accept(l, (Consumer<? super BiFunction<? super Integer,? super ByteBuf, Integer>>) consumer -> {
                    long length = l;
                    while (length > 0) {
                        final int read = consumer.apply(Math.toIntExact(Math.min(8192, length)), buffer.clear()).intValue();
                        if (read < 0)
                            break;
                        if (read == 0)
                            AndroidSupporter.onSpinWait();
                        for (final Integer integer: reading)
                            digests.get(integer.intValue()).update(buffer.array(), 0, read);
                        length -= read;
                    }
                });
            }
        }, buffer::release));
        for (final String checksum: checksums)
            if (checksum == null)
                throw new IllegalStateException("Failed to calculate checksum." + ParametersMap.create().add("requirements", requirements).add("checksums", checksums));
        return List.of(checksums);
    }

    private static @NotNull @Unmodifiable List<@NotNull String> calculateChecksums(final @NotNull FileChannel channel, final @NotNull Collection<@NotNull UploadChecksum> requirements) {
        return FilesAssistant.calculateChecksumsCore(requirements, HExceptionWrapper.wrapConsumer(runner -> {
            try (channel) {
                final AtomicLong position = new AtomicLong();
                runner.accept(HExceptionWrapper.wrapConsumer(pos -> {
                    channel.position(pos.longValue());
                    position.set(pos.longValue());
                }), HExceptionWrapper.wrapBiConsumer((length, consumer) -> {
                    try (final FileLock ignoredLock = channel.lock(position.get(), length.longValue(), true)) {
                        consumer.accept(HExceptionWrapper.wrapBiFunction((size, buffer) -> buffer.writeBytes(channel, size.intValue())));
                    }
                }));
            }
        }));
    }

    private static @NotNull @Unmodifiable List<@NotNull String> calculateChecksumsStream(final @NotNull InputStream stream, final @NotNull Collection<@NotNull UploadChecksum> requirements) {
        return FilesAssistant.calculateChecksumsCore(requirements, runner -> {
            final AtomicLong position = new AtomicLong(0);
            runner.accept(HExceptionWrapper.wrapConsumer(pos -> {
                AndroidSupporter.skipNBytes(stream, pos.longValue() - position.get());
                position.set(pos.longValue());
            }), HExceptionWrapper.wrapBiConsumer((length, consumer) -> {
                consumer.accept(HExceptionWrapper.wrapBiFunction((size, buffer) -> buffer.writeBytes(stream, size.intValue())));
                position.getAndAdd(length.longValue());
            }));
        });
    }

    private static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> uploadCore(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation parent, final @NotNull String filename, final long size, final @NotNull Predicate<? super @NotNull UploadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback, final @NotNull Function<? super @NotNull Collection<@NotNull UploadChecksum>, ? extends @NotNull @Unmodifiable List<@NotNull String>> calculator,
                                                             final @NotNull Consumer<? super @NotNull Consumer<? super @NotNull BiConsumer<? super Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>, ? super @NotNull Consumer<? super @NotNull BiFunction<? super @NotNull Integer,? super @NotNull ByteBuf, @NotNull Integer>>>>> runner) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<UploadConfirm, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.requestUploadFile(client, TokenAssistant.getToken(address, username), parent, filename, size, ClientConfiguration.get().duplicatePolicy());
        }
        if (confirm.isFailure()) return UnionPair.fail(confirm.getE());
        if (!continuer.test(confirm.getT())) {
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                OperateFilesHelper.cancelUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
            }
            return null;
        }
        final List<String> checksums;
        try {
            checksums = confirm.getT().checksums().isEmpty() ? List.of() : calculator.apply(confirm.getT().checksums());
            if (!checksums.isEmpty())
                HLog.getInstance("ClientLogger").log(HLogLevel.LESS, "Calculated checksums.", ParametersMap.create()
                        .add("filename", filename).add("size", size).add("checksums", checksums));
        } catch (final RuntimeException exception) {
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                OperateFilesHelper.cancelUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
            }
            throw exception;
        }
        final UploadConfirm.UploadInformation information;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            information = OperateFilesHelper.confirmUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id(), checksums);
        }
        if (information == null)
            return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, parent, "Confirming upload file."));
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(ClientConfiguration.get().threadCount() > 0 ?
                ClientConfiguration.get().threadCount() : Math.min(Runtime.getRuntime().availableProcessors(), information.parallel().size()),
                new DefaultThreadFactory(String.format("UploadingExecutor#%s:%d@%s", parent, size, filename)));
        final AtomicBoolean failure = new AtomicBoolean(false);
        final AtomicReference<VisibleFileInformation> finishSuccess = new AtomicReference<>();
        runner.accept(HExceptionWrapper.wrapConsumer(reader -> {
                final CountDownLatch latch = new CountDownLatch(information.parallel().size());
                int i = 0;
                for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                    final int index = i++;
                    CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                        final ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(NetworkTransmission.FileTransferBufferSize, NetworkTransmission.FileTransferBufferSize);
                        try (final WListClientInterface c = WListClientManager.quicklyGetClient(address)) {
                            final long l = pair.getSecond().longValue() - pair.getFirst().longValue();
                            reader.accept(Pair.ImmutablePair.makeImmutablePair(pair.getFirst().longValue(), l), HExceptionWrapper.wrapConsumer(consumer -> {
                                long length = l;
                                while (length > 0) { // TODO: MappedByteBuffer
                                    final int read = consumer.apply(Math.toIntExact(Math.min(length, NetworkTransmission.FileTransferBufferSize)), buf.clear()).intValue();
                                    if (read < 0 || !OperateFilesHelper.uploadFile(c, TokenAssistant.getToken(address, username), confirm.getT().id(), index, buf.retain()))
                                        failure.set(true);
                                    if (failure.get())
                                        break;
                                    length -= read;
                                }
                            }));
                        } finally {
                            buf.release();
                        }
                    }, e -> {
                        latch.countDown();
                        //noinspection VariableNotUsedInsideIf
                        if (e != null)
                            failure.set(true);
                    }, false), executors).exceptionally(MiscellaneousUtil.exceptionHandler());
                }
                FilesAssistant.callbackSync(address, username, s -> {
                    if (callback != null)
                        callback.accept(s);
                }, failure, latch, List.of(confirm.getT().id()));
                latch.await();
        }, () -> {
            final Future<?> future = executors.shutdownGracefully().await();
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                finishSuccess.set(OperateFilesHelper.finishUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id()));
            }
            future.sync(); // rethrow.
        }));
        if (failure.get())
            return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, parent, "Uploading."));
        return finishSuccess.get() != null ? UnionPair.ok(finishSuccess.get()) : UnionPair.fail(new VisibleFailureReason(FailureKind.Others, parent, "Finishing upload file."));
    }

    public static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> upload(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull File file, final @NotNull FileLocation parent, final @NotNull Predicate<? super @NotNull UploadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        try {
            return FilesAssistant.upload(address, username, HExceptionWrapper.wrapConsumer(access -> {
                try (final RandomAccessFile accessFile = new RandomAccessFile(file, "r");
                     final FileChannel channel = accessFile.getChannel()) {
                    access.accept(channel);
                }
            }), parent, file.getName(), file.length(), continuer, callback);
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class);
        }
    }

    public static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> upload(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull Consumer<@NotNull Consumer<@NotNull FileChannel>> access, final @NotNull FileLocation parent, final @NotNull String filename, final long filesize, final @NotNull Predicate<? super @NotNull UploadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        try {
            return FilesAssistant.uploadCore(address, username, parent, filename, filesize, continuer, callback, requirements -> {
                final AtomicReference<List<String>> checksums = new AtomicReference<>();
                access.accept(channel -> checksums.set(FilesAssistant.calculateChecksums(channel, requirements)));
                return checksums.get();
            }, HExceptionWrapper.wrapConsumer(runner -> access.accept(fileChannel/*Only lock.*/ ->
                    runner.accept(HExceptionWrapper.wrapBiConsumer((pair, consumer) -> access.accept(HExceptionWrapper.wrapConsumer(channel -> {
                            channel.position(pair.getFirst().longValue());
                            try(final FileLock ignoredLock = fileChannel.lock(pair.getFirst().longValue(), pair.getSecond().longValue(), true)) {
                                consumer.accept(HExceptionWrapper.wrapBiFunction((size, buffer) -> buffer.writeBytes(channel, size.intValue())));
                            }
            })))))));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class);
        }
    }

    /**
     * @param stream The first parameter is the start position and length of the stream will be read. (For optimising.)
     *               Warning: this method will be called many times and may in multi threads.
     * @see AndroidSupporter#skipNBytes(InputStream, long)
     */
    public static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> uploadStream(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull BiConsumer<? super Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>, ? super @NotNull Consumer<? super @NotNull InputStream>> stream, final long size, final @NotNull String filename, final @NotNull FileLocation parent, final @NotNull Predicate<? super @NotNull UploadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        try {
            return FilesAssistant.uploadCore(address, username, parent, filename, size, continuer, callback, requirements -> {
                final AtomicReference<List<String>> res = new AtomicReference<>();
                stream.accept(Pair.ImmutablePair.makeImmutablePair(0L, size), (Consumer<? super InputStream>) inputStream ->
                        res.set(FilesAssistant.calculateChecksumsStream(inputStream, requirements)));
                return res.get();
            }, runner -> runner.accept(HExceptionWrapper.wrapBiConsumer((pair, consumer) -> stream.accept(pair,
                    HExceptionWrapper.wrapConsumer(inputStream -> consumer.accept(HExceptionWrapper.wrapBiFunction((length, buffer) ->
                            buffer.writeBytes(inputStream, length.intValue()))))))));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class, InterruptedException.class, WrongStateException.class);
        }
    }

    public static final @NotNull HInitializer<UnaryOperator<@NotNull File>> DownloadRecordFileTransfer = new HInitializer<>("DownloadRecordFileTransfer", file -> {
        //noinspection SpellCheckingInspection
        return new File(file.getParentFile(), file.getName() + ".wdrd");
    }); // TODO optimized record content.

    @Contract(pure = true)
    public static @NotNull File getDownloadRecordFile(final @NotNull File file) {
        return FilesAssistant.DownloadRecordFileTransfer.getInstance().apply(file);
    }

    private static final @NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> FullDownloadingProgress = List.of(Pair.ImmutablePair.makeImmutablePair(0L, Long.MAX_VALUE));
    private static @NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> readDownloadingProgress(final @NotNull FileLocation location, final @NotNull File file) {
        final File recordFile = FilesAssistant.getDownloadRecordFile(file);
        if (!recordFile.isFile() || !recordFile.canRead())
            return FilesAssistant.FullDownloadingProgress;
        final JSONObject json;
        try (final InputStream stream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(recordFile)))) {
            json = JSON.parseObject(stream);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final IOException | JSONException exception) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            return FilesAssistant.FullDownloadingProgress;
        }
        try {
            if (json == null || !location.storage().equals(json.get("storage")) || !json.containsKey("id") || location.id() != json.getLongValue("id"))
                return FilesAssistant.FullDownloadingProgress;
            final JSONArray record = json.getJSONArray("progress");
            if (record == null || record.isEmpty())
                return FilesAssistant.FullDownloadingProgress;
            final List<Pair.ImmutablePair<Long, Long>> progress = new ArrayList<>(record.size());
            for (final JSONObject state: record.toArray(JSONObject.class)) {
                final Long start = state.getLong("start");
                final Long end = state.getLong("end");
                if (start == null || end == null || start.longValue() > end.longValue() || start.longValue() < 0)
                    return FilesAssistant.FullDownloadingProgress;
                progress.add(Pair.ImmutablePair.makeImmutablePair(start, end));
            }
            return progress;
        } catch (final JSONException exception) {
            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            return FilesAssistant.FullDownloadingProgress;
        }
    }

    private static void saveDownloadingProgress(final @NotNull FileLocation location, final @NotNull File file, final @NotNull Collection<? extends Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> progress) throws IOException {
        final File recordFile = FilesAssistant.getDownloadRecordFile(file);
        final Map<String, Object> json = new JSONObject(3);
        json.put("storage", location.storage());
        json.put("id", location.id());
        final Collection<Object> record = new JSONArray(progress.size());
        for (final Pair.ImmutablePair<AtomicLong, Long> state: progress) {
            final Map<String, Object> s = new JSONObject(2);
            s.put("start", state.getFirst().get());
            s.put("end", state.getSecond());
            record.add(s);
        }
        json.put("progress", record);
        final byte[] bytes = JSON.toJSONBytes(json);
        HFileHelper.writeFileAtomically(recordFile, stream -> {
            try (final OutputStream outputStream = new GZIPOutputStream(stream)) {
                outputStream.write(bytes);
            }
        });
    }

    private static void finishDownloadingProgress(final @NotNull File file) throws IOException {
        final File recordFile = FilesAssistant.getDownloadRecordFile(file);
        if (recordFile.isFile())
            Files.delete(recordFile.toPath());
    }

    public static @Nullable VisibleFailureReason download(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final @NotNull File file, final @NotNull Predicate<? super @NotNull DownloadConfirm> continuer, final @Nullable BiConsumer<? super @NotNull InstantaneousProgressState, ? super @NotNull List<Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>>> callback) throws IOException, InterruptedException, WrongStateException {
        List<Pair.ImmutablePair<Long, Long>> downloaded = FilesAssistant.readDownloadingProgress(location, file);
        if (downloaded.isEmpty()) {
            FilesAssistant.finishDownloadingProgress(file);
            return null;
        }
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            if (OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), location, false) == null)
                return new VisibleFailureReason(FailureKind.Others, location, "Updating.");
        }
        final UnionPair<DownloadConfirm, VisibleFailureReason> firstConfirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            firstConfirm = OperateFilesHelper.requestDownloadFile(client, TokenAssistant.getToken(address, username), location, 0, Long.MAX_VALUE);
        }
        if (firstConfirm.isFailure()) return firstConfirm.getE();
        if (!continuer.test(firstConfirm.getT())) {
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                OperateFilesHelper.cancelDownloadFile(client, TokenAssistant.getToken(address, username), firstConfirm.getT().id());
            }
            return null;
        }
        if (!firstConfirm.getT().acceptedRange())
            downloaded = FilesAssistant.FullDownloadingProgress;
        try (final RandomAccessFile accessFile = new RandomAccessFile(file, "rw")) {
            accessFile.setLength(firstConfirm.getT().downloadingSize());
        }
        if (downloaded != FilesAssistant.FullDownloadingProgress)
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                 OperateFilesHelper.cancelDownloadFile(client, TokenAssistant.getToken(address, username), firstConfirm.getT().id());
            }
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(ClientConfiguration.get().threadCount() > 0 ?
                ClientConfiguration.get().threadCount() : Math.min(Runtime.getRuntime().availableProcessors(), downloaded.size() + 1),
                new DefaultThreadFactory(String.format("DownloadingExecutor#%s:%d@%s", location, firstConfirm.getT().downloadingSize(), file.getAbsolutePath())));
        boolean flag = true;
        final AtomicBoolean failure = new AtomicBoolean(false);
        final List<Pair.ImmutablePair<AtomicLong, Long>> progress = new ArrayList<>();
        try {
            final CountDownLatch latch = new CountDownLatch(downloaded.size());
            final Collection<String> ids = new ArrayList<>();
            for (final Pair.ImmutablePair<Long, Long> state: downloaded) {
                if (failure.get()) break;
                if (state.getFirst().longValue() >= state.getSecond().longValue()) {
                    latch.countDown();
                    continue;
                }
                final UnionPair<DownloadConfirm, VisibleFailureReason> confirm;
                if (downloaded == FilesAssistant.FullDownloadingProgress)
                    confirm = firstConfirm;
                else {
                    try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                        confirm = OperateFilesHelper.requestDownloadFile(client, TokenAssistant.getToken(address, username), location, state.getFirst().longValue(), state.getSecond().longValue());
                    }
                    if (confirm.isFailure()) return confirm.getE();
                    if (confirm.getT().acceptedRange() != firstConfirm.getT().acceptedRange())
                        return new VisibleFailureReason(FailureKind.Others, location, "Requesting download file: inconsistent acceptedRange.");
                }
                final DownloadConfirm.DownloadInformation information;
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                    information = OperateFilesHelper.confirmDownloadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
                }
                if (information == null)
                    return new VisibleFailureReason(FailureKind.Others, location, "Confirming download file.");
                int i = 0;
                ids.add(confirm.getT().id());
                final AtomicLong insideLatch = new AtomicLong(information.parallel().size());
                if (information.parallel().isEmpty())
                    latch.countDown();
                for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                    final int index = i++;
                    final AtomicLong saved = new AtomicLong(pair.getFirst().longValue());
                    progress.add(Pair.ImmutablePair.makeImmutablePair(saved, pair.getSecond()));
                    CompletableFuture.runAsync(HExceptionWrapper.wrapRunnable(() -> {
                        long length = pair.getSecond().longValue() - pair.getFirst().longValue();
                        try (final WListClientInterface c = WListClientManager.quicklyGetClient(address);
                             final RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
                             final FileChannel channel = accessFile.getChannel().position(pair.getFirst().longValue());
                             final FileLock ignoredLock = channel.lock(pair.getFirst().longValue(), length, false)) {
                            while (length > 0) {
                                final ByteBuf buf = OperateFilesHelper.downloadFile(c, TokenAssistant.getToken(address, username), confirm.getT().id(), index);
                                if (buf == null) {
                                    failure.set(true);
                                    break;
                                }
                                try {
                                    if (failure.get())
                                        break;
                                    length -= buf.readableBytes();
                                    buf.getBytes(0, channel, buf.readableBytes());
                                    saved.getAndAdd(buf.readableBytes());
                                } finally {
                                    buf.release();
                                }
                            }
                        }
                    }, e -> {
                        //noinspection VariableNotUsedInsideIf
                        if (e != null)
                            failure.set(true);
                        if (insideLatch.getAndDecrement() == 1) {
                            latch.countDown();
                            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                                ids.forEach(HExceptionWrapper.wrapConsumer(id -> OperateFilesHelper.finishDownloadFile(client, TokenAssistant.getToken(address, username), id),
                                        MiscellaneousUtil.exceptionCallback, true));
                            }

                        }
                    }, false), executors).exceptionally(MiscellaneousUtil.exceptionHandler());
                }
            }
            if (!failure.get()) {
                final AtomicBoolean savable = new AtomicBoolean(true);
                FilesAssistant.callbackSync(address, username, HExceptionWrapper.wrapConsumer(s -> {
                    if (savable.get())
                        try {
                            FilesAssistant.saveDownloadingProgress(location, file, progress);
                        } catch (final AccessDeniedException exception) {
                            HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                            savable.set(false);
                        }
                    if (callback != null)
                        callback.accept(s, progress);
                }), failure, latch, ids);
                latch.await();
            }
            flag = false;
        } finally {
            if (flag)
                failure.set(true);
            executors.shutdownGracefully().sync();
        }
        if (failure.get())
            return new VisibleFailureReason(FailureKind.Others, location, "Downloading.");
        FilesAssistant.finishDownloadingProgress(file);
        return null;
    }

    public static @Nullable UnionPair<InputStream, VisibleFailureReason> downloadStream(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final @NotNull Predicate<? super @NotNull DownloadConfirm> continuer, final long from, final long to, final @NotNull Executor executor) throws IOException, InterruptedException, WrongStateException {
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            if (OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), location, false) == null)
                return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, location, "Updating."));
        }
        final UnionPair<DownloadConfirm, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.requestDownloadFile(client, TokenAssistant.getToken(address, username), location, from, to);
        }
        if (confirm.isFailure()) return UnionPair.fail(confirm.getE());
        final String id = confirm.getT().id();
        if (from > 0 && !continuer.test(confirm.getT())) {
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                OperateFilesHelper.cancelDownloadFile(client, TokenAssistant.getToken(address, username), id);
            }
            return null;
        }
        final DownloadConfirm.DownloadInformation information;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            information = OperateFilesHelper.confirmDownloadFile(client, TokenAssistant.getToken(address, username), id);
        }
        if (information == null)
            return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, location, "Confirming."));
        final int highWaterMark = 5;
        final int lowWaterMark = 2;
        final BlockingQueue<ByteBuf> buffersQueue = new LinkedBlockingQueue<>();
        final AtomicBoolean threadRunning = new AtomicBoolean(false);
        final AtomicInteger nextIndex = new AtomicInteger(-1);
        final AtomicBoolean noNextIndex = new AtomicBoolean(false);
        final AtomicLong length = new AtomicLong(0);
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        final Runnable supplier = () -> {
            if (threadRunning.compareAndSet(false, true))
                CompletableFuture.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        boolean flag = true;
                        try {
                            if (length.get() <= 0) {
                                if (noNextIndex.get())
                                    return;
                                final int index = nextIndex.incrementAndGet();
                                if (index >= information.parallel().size()) {
                                    noNextIndex.set(true);
                                    return;
                                }
                                final Pair.ImmutablePair<Long, Long> pair = information.parallel().get(index);
                                length.set(pair.getSecond().longValue() - pair.getFirst().longValue());
                            }
                            final int index = nextIndex.get();
                            final ByteBuf buf;
                            try (final WListClientInterface c = WListClientManager.quicklyGetClient(address)) {
                                buf = OperateFilesHelper.downloadFile(c, TokenAssistant.getToken(address, username), id, index);
                            }
                            if (buf == null)
                                throw new IllegalStateException("Invalid download id." + ParametersMap.create().add("address", address).add("username", username).add("id", id)
                                        .add("location", location).add("confirm", confirm).add("information", information).add("parallel", information.parallel()));
                            length.getAndAdd(-buf.readableBytes());
                            synchronized (threadRunning) {
                                buffersQueue.add(buf);
                                threadRunning.notifyAll();
                            }
                            if (buffersQueue.size() < highWaterMark) {
                                this.run();
                                flag = false;
                            }
                        } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                            noNextIndex.set(true);
                            noNextIndex.set(true);
                            if (!throwable.compareAndSet(null, exception) && !exception.equals(throwable.get()))
                                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                            synchronized (threadRunning) {
                                threadRunning.set(false);
                                threadRunning.notifyAll();
                            }
                        } finally {
                            if (flag) {
                                synchronized (threadRunning) {
                                    threadRunning.set(false);
                                    threadRunning.notifyAll();
                                }
                            }
                        }
                    }
                }, executor).exceptionally(MiscellaneousUtil.exceptionHandler());
        };
        supplier.run();
        final AtomicReference<ByteBufInputStream> current = new AtomicReference<>();
        final InputStream stream = new InputStream() {
            private final @NotNull AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public int available() throws IOException {
                final ByteBufInputStream stream = current.get();
                return stream == null ? 0 : stream.available();
            }

            @Override
            public int read() throws IOException {
                if (this.closed.get())
                    throw new IOException("Closed stream.");
                final ByteBufInputStream stream = current.get();
                if (stream != null) {
                    final int read = stream.read();
                    if (read != -1)
                        return read;
                    current.compareAndSet(stream, null);
                    stream.close();
                }
                final ByteBuf buffer = buffersQueue.poll();
                if (buffer != null) {
                    if (buffersQueue.size() < lowWaterMark)
                        supplier.run();
                    if (!current.compareAndSet(null, new ByteBufInputStream(buffer, true))) {
                        buffer.release();
                        throw new ConcurrentModificationException();
                    }
                    return this.read();
                }
                supplier.run();
                synchronized (threadRunning) {
                    while (threadRunning.get() && buffersQueue.isEmpty())
                        try {
                            threadRunning.wait();
                        } catch (final InterruptedException ignore) {
                            return -1;
                        }
                }
                if (buffersQueue.isEmpty())
                    return -1;
                return this.read();
            }

            @Override
            public void close() throws IOException {
                if (!this.closed.compareAndSet(false, true))
                    return;
                super.close();
                noNextIndex.set(true);
                length.set(0);
                final ByteBufInputStream stream = current.getAndSet(null);
                if (stream != null)
                    stream.close();
                synchronized (threadRunning) {
                    while (threadRunning.get())
                        try {
                            threadRunning.wait();
                        } catch (final InterruptedException ignore) {
                            break;
                        }
                }
                while (true) {
                    final ByteBuf buf = buffersQueue.poll();
                    if (buf == null)
                        break;
                    buf.release();
                }
                final Throwable exception = throwable.get();
                HExceptionWrapper.wrapRunnable(() -> {
                    try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                        OperateFilesHelper.finishDownloadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
                    }
                }, () -> MiscellaneousUtil.throwException(exception)).run(); // Build exception.
            }
        };
        if (!confirm.getT().acceptedRange())
            AndroidSupporter.skipNBytes(stream, from);
        return UnionPair.ok(stream);
    }


    @Contract("_, _, null, !null, _ -> fail")
    private static void refreshCore(final @NotNull SocketAddress address, final @NotNull String username, final @Nullable ScheduledExecutorService executor, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        if (callback == null) {
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                OperateFilesHelper.confirmRefresh(client, TokenAssistant.getToken(address, username), id);
            }
        } else {
            final ScheduledFuture<?> future = FilesAssistant.callbackAsync(address, username, Objects.requireNonNull(executor), callback, List.of(id));
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                OperateFilesHelper.confirmRefresh(client, TokenAssistant.getToken(address, username), id);
            } finally {
                future.cancel(true);
            }
        }
    }

    @Contract("_, _, _, _, _, _, _, null, _, !null -> fail")
    public static @Nullable VisibleFilesListInformation list(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation directory, final @Nullable FilterPolicy filter, final @Nullable @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, @NotNull OrderDirection> orders, final long position, final int limit, final @Nullable ScheduledExecutorService executor, final @NotNull Predicate<? super @NotNull RefreshConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            if (OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), directory, true) == null)
                return null;
        }
        final UnionPair<VisibleFilesListInformation, RefreshConfirm> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.listFiles(client, TokenAssistant.getToken(address, username), directory,
                    Objects.requireNonNullElseGet(filter, () -> ClientConfiguration.get().filterPolicy()),
                    Objects.requireNonNullElseGet(orders, () -> ClientConfiguration.get().fileOrders()), position, limit);
        }
        if (confirm == null)
            return null;
        if (confirm.isSuccess())
            return confirm.getT();
        if (!continuer.test(confirm.getE()))
            return null;
        FilesAssistant.refreshCore(address, username, executor, callback, confirm.getE().id());
        return FilesAssistant.list(address, username, directory, filter, orders, position, limit, executor, PredicateE.truePredicate(), callback);
    }

    @Contract("_, _, _, null, !null -> fail")
    public static boolean refresh(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation directory, final @Nullable ScheduledExecutorService executor, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            if (OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), directory, true) == null)
                return false;
        }
        final RefreshConfirm confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.refreshDirectory(client, TokenAssistant.getToken(address, username), directory);
        }
        if (confirm == null)
            return false;
        FilesAssistant.refreshCore(address, username, executor, callback, confirm.id());
        return true;
    }


    private static boolean trash0(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final AtomicBoolean interruptFlag, final @NotNull Executor executor, final @NotNull AtomicLong done, final @NotNull AtomicLong total) throws IOException, InterruptedException, WrongStateException {
        if (interruptFlag.get()) return true;
        total.getAndIncrement();
        final Boolean success;
        if (!isDirectory) {
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                success = OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(address, username), location, false);
            }
            done.getAndIncrement();
            return success == null || success.booleanValue();
        }
        while (true) {
            final VisibleFilesListInformation list = FilesAssistant.list(address, username, location, FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, ClientConfiguration.get().limitPerPage(), null, PredicateE.truePredicate(), null);
            if (list == null) return true;
            try {
                total.getAndAdd(list.informationList().size());
                HMultiRunHelper.runConsumers(executor, list.informationList(), HExceptionWrapper.wrapConsumer(information -> {
                    if (!FilesAssistant.trash0(address, username, new FileLocation(location.storage(), information.id()), information.isDirectory(), interruptFlag, executor, done, total))
                        interruptFlag.set(true);
                }));
            } catch (final RuntimeException exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                interruptFlag.set(true);
            }
            if (interruptFlag.get()) return false;
            if (list.total() == list.informationList().size())
                break;
        }
        final Boolean s;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            s = OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(address, username), location, true);
        }
        done.getAndIncrement();
        return s == null || s.booleanValue();
    }

    /**
     * @param trashRecursivelyCallback When trash is too complex, it will be called. Then the operation won't be atomic.
     */
    public static boolean trash(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull Predicate<@Nullable Void> trashRecursivelyCallback, final @Nullable BiConsumer<@NotNull AtomicLong, @NotNull AtomicLong> callback) throws IOException, InterruptedException, WrongStateException {
        final Boolean success;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            success = OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(address, username), location, isDirectory);
        }
        if (success == null || success.booleanValue()) return true;
        if (!isDirectory) return false; // Trash file but complex. (Unreachable.)
        if (!trashRecursivelyCallback.test(null)) return true;
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(ClientConfiguration.get().threadCount() > 0 ?
                ClientConfiguration.get().threadCount() : Runtime.getRuntime().availableProcessors(),
                new DefaultThreadFactory(String.format("TrashingExecutor#%s", location)));
        final AtomicLong done = new AtomicLong(0), total = new AtomicLong(0);
        final ScheduledFuture<?> callbackFuture = callback == null ? null : executors.scheduleWithFixedDelay(() -> callback.accept(done, total),
                ClientConfiguration.get().progressStartDelay(), ClientConfiguration.get().progressInterval(), TimeUnit.MILLISECONDS);
        try {
            return FilesAssistant.trash0(address, username, location, true, new AtomicBoolean(false), executors, done, total);
        } finally {
            if (callbackFuture != null)
                callbackFuture.cancel(true);
            executors.shutdownGracefully().sync();
        }
    }


    public enum NotDirectlyPolicy {
        DownloadAndUpload, // Handle file by download and then upload.
        BuildTree, // Handle directory by creating directory and handle each file (Then may call 'DownloadAndUpload').
    }

    private static @NotNull UnionPair<VisibleFileInformation, VisibleFailureReason> copyFileDownloadAndUpload(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final @NotNull FileLocation parent, final @NotNull String name, final @NotNull Executor executor) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<VisibleFileInformation, VisibleFailureReason> res;
        if (ClientConfiguration.get().copyNoTempFile()) {
            final VisibleFileInformation file;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                file = OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), location, false);
            }
            if (file == null)
                return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, location, "Updating."));
            final AtomicReference<VisibleFailureReason> downloading = new AtomicReference<>();
            try {
                res = FilesAssistant.uploadStream(address, username, HExceptionWrapper.wrapBiConsumer((pair, consumer) -> {
                    final UnionPair<InputStream, VisibleFailureReason> stream = FilesAssistant.downloadStream(address, username, location, PredicateE.truePredicate(),
                            pair.getFirst().longValue(), pair.getFirst().longValue() + pair.getSecond().longValue(), executor);
                    assert stream != null;
                    if (stream.isFailure()) {
                        downloading.set(stream.getE());
                        throw new RuntimeException();
                    }
                    consumer.accept(stream.getT());
                }), file.size(), name, parent, PredicateE.truePredicate(), null);
            } catch (final RuntimeException exception) {
                if (downloading.get() != null)
                    return UnionPair.fail(downloading.get());
                throw exception;
            }
        } else {
            final File temp = Files.createTempFile(name, ".wlist.tmp").toFile();
            try {
                final VisibleFailureReason download = FilesAssistant.download(address, username, location, temp, PredicateE.truePredicate(), null);
                if (download != null) return UnionPair.fail(download);
                res = FilesAssistant.upload(address, username, HExceptionWrapper.wrapConsumer(access -> {
                    try (final RandomAccessFile accessFile = new RandomAccessFile(temp, "r");
                         final FileChannel channel = accessFile.getChannel()) {
                        access.accept(channel);
                    }
                }), parent, name, temp.length(), PredicateE.truePredicate(), null);
            } catch (final RuntimeException exception) {
                throw HExceptionWrapper.unwrapException(exception, IOException.class);
            } finally {
                Files.deleteIfExists(temp.toPath());
            }
        }
        assert res != null;
        return res;
    }


    private static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> copy0(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final @NotNull String name, final @Nullable Executor downloaderExecutor, final @NotNull AtomicReference<Boolean> directly, final @NotNull Predicate<? super @NotNull NotDirectlyPolicy> continuer) throws IOException, InterruptedException, WrongStateException {
        if (isDirectory) {
            final UnionPair<VisibleFileInformation, VisibleFailureReason> directory;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                directory = OperateFilesHelper.createDirectory(client, TokenAssistant.getToken(address, username), parent, name, ClientConfiguration.get().duplicatePolicy());
                if (directory.isFailure()) return UnionPair.fail(directory.getE());
            }
            final FileLocation p = new FileLocation(parent.storage(), directory.getT().id());
            final AtomicLong pos = new AtomicLong(0);
            while (true) {
                final int limit = ClientConfiguration.get().limitPerPage();
                final VisibleFilesListInformation list = FilesAssistant.list(address, username, location, FilterPolicy.Both, VisibleFileInformation.emptyOrder(), pos.getAndAdd(limit), limit, null, PredicateE.truePredicate(), null);
                if (list == null) break;
                for (final VisibleFileInformation info: list.informationList()) {
                    final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.copy0(address, username, new FileLocation(location.storage(), info.id()), info.isDirectory(), p, info.name(), downloaderExecutor, directly, continuer);
                    if (res == null || res.isFailure()) return res;
                }
                if (list.total() == list.informationList().size()) break;
            }
            return directory;
        }
        if (directly.get() == null || directly.get().booleanValue()) {
            final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> file;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                file = OperateFilesHelper.copyDirectly(client, TokenAssistant.getToken(address, username), location, false, parent, name, ClientConfiguration.get().duplicatePolicy());
            }
            if (file.isFailure()) return UnionPair.fail(file.getE());
            final Boolean old = directly.getAndSet(file.getT().isPresent());
            if (directly.get().booleanValue()) return UnionPair.ok(file.getT().get());
            if (old == null && !continuer.test(NotDirectlyPolicy.DownloadAndUpload)) return null;
        }
        return FilesAssistant.copyFileDownloadAndUpload(address, username, location, parent, name, Objects.requireNonNullElseGet(downloaderExecutor, ForkJoinPool::commonPool));
    }

    // TODO: progress callback.
    public static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> copy(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final @NotNull String name, final @Nullable Executor downloaderExecutor, final @NotNull Predicate<? super @NotNull NotDirectlyPolicy> continuer) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.copyDirectly(client, TokenAssistant.getToken(address, username), location, isDirectory, parent, name, ClientConfiguration.get().duplicatePolicy());
        }
        if (confirm.isFailure()) return UnionPair.fail(confirm.getE());
        if (confirm.getT().isPresent()) return UnionPair.ok(confirm.getT().get());
        if (!continuer.test(isDirectory ? NotDirectlyPolicy.BuildTree : NotDirectlyPolicy.DownloadAndUpload)) return null;
        return isDirectory ? FilesAssistant.copy0(address, username, location, true, parent, name, downloaderExecutor, new AtomicReference<>(null), continuer)
                : FilesAssistant.copyFileDownloadAndUpload(address, username, location, parent, name, Objects.requireNonNullElseGet(downloaderExecutor, ForkJoinPool::commonPool));
    }


    private static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> move0(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final @Nullable Executor downloaderExecutor, final @NotNull AtomicReference<Boolean> directly, final @NotNull Predicate<? super @NotNull NotDirectlyPolicy> continuer) throws IOException, InterruptedException, WrongStateException {
        if (isDirectory) {
            final VisibleFileInformation source;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                source = OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), location, true);
                if (source == null) return UnionPair.fail(new VisibleFailureReason(FailureKind.NoSuchFile, location, "No such directory."));
            }
            final UnionPair<VisibleFileInformation, VisibleFailureReason> directory;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                directory = OperateFilesHelper.createDirectory(client, TokenAssistant.getToken(address, username), parent, source.name(), ClientConfiguration.get().duplicatePolicy());
            }
            if (directory.isFailure()) return UnionPair.fail(directory.getE());
            final FileLocation p = new FileLocation(parent.storage(), directory.getT().id());
            while (true) {
                final int limit = ClientConfiguration.get().limitPerPage();
                final VisibleFilesListInformation list = FilesAssistant.list(address, username, location, FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, limit, null, PredicateE.truePredicate(), null);
                if (list == null) break;
                for (final VisibleFileInformation info: list.informationList()) {
                    final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.move0(address, username, new FileLocation(location.storage(), info.id()), info.isDirectory(), p, downloaderExecutor, directly, continuer);
                    if (res == null || res.isFailure()) return res;
                }
                if (list.total() == list.informationList().size()) break;
            }
            if (FilesAssistant.trash(address, username, location, true, PredicateE.truePredicate(), null)) return directory; // TODO: move file after refresh.
            return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, location, "Trashing."));
        }
        if (directly.get() == null || directly.get().booleanValue()) {
            final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> file;
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                file = OperateFilesHelper.moveDirectly(client, TokenAssistant.getToken(address, username), location, false, parent, ClientConfiguration.get().duplicatePolicy());
            }
            if (file.isFailure()) return UnionPair.fail(file.getE());
            final Boolean old = directly.getAndSet(file.getT().isPresent());
            if (directly.get().booleanValue()) return UnionPair.ok(file.getT().get());
            if (old == null && !continuer.test(NotDirectlyPolicy.DownloadAndUpload)) return null;
        }
        final VisibleFileInformation source;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            source = OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), location, false);
            if (source == null) return UnionPair.fail(new VisibleFailureReason(FailureKind.NoSuchFile, location, "No such file."));
        }
        final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.copyFileDownloadAndUpload(address, username, location, parent, source.name(), Objects.requireNonNullElseGet(downloaderExecutor, ForkJoinPool::commonPool));
        if (res.isFailure() || FilesAssistant.trash(address, username, location, false, PredicateE.truePredicate(), null)) return res;
        return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, location, "Trashing."));
    }

    public static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> move(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final @Nullable Executor downloaderExecutor, final @NotNull Predicate<? super @NotNull NotDirectlyPolicy> continuer) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.moveDirectly(client, TokenAssistant.getToken(address, username), location, isDirectory, parent, ClientConfiguration.get().duplicatePolicy());
        }
        if (confirm.isFailure()) return UnionPair.fail(confirm.getE());
        if (confirm.getT().isPresent()) return UnionPair.ok(confirm.getT().get());
        if (!continuer.test(isDirectory ? NotDirectlyPolicy.BuildTree : NotDirectlyPolicy.DownloadAndUpload)) return null;
        return isDirectory ? FilesAssistant.move0(address, username, location, true, parent, downloaderExecutor, new AtomicReference<>(null), continuer)
                : FilesAssistant.move0(address, username, location, false, parent, downloaderExecutor, new AtomicReference<>(Boolean.FALSE), PredicateE.truePredicate());
    }


    public static @Nullable UnionPair<VisibleFileInformation, VisibleFailureReason> rename(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull String name, final @Nullable Executor downloaderExecutor, final @NotNull Predicate<? super @NotNull NotDirectlyPolicy> continuer) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<Optional<VisibleFileInformation>, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.renameDirectly(client, TokenAssistant.getToken(address, username), location, isDirectory, name, ClientConfiguration.get().duplicatePolicy());
        }
        if (confirm.isFailure()) return UnionPair.fail(confirm.getE());
        if (confirm.getT().isPresent()) return UnionPair.ok(confirm.getT().get());
        if (!continuer.test(isDirectory ? NotDirectlyPolicy.BuildTree : NotDirectlyPolicy.DownloadAndUpload)) return null;
        final VisibleFileInformation source;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            source = OperateFilesHelper.getFileOrDirectory(client, TokenAssistant.getToken(address, username), location, isDirectory);
            if (source == null) return UnionPair.fail(new VisibleFailureReason(FailureKind.NoSuchFile, location, isDirectory ? "No such directory." : "No such file."));
        }
        final FileLocation parent = new FileLocation(location.storage(), source.parentId());
        if (!isDirectory) {
            final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.copyFileDownloadAndUpload(address, username, location, parent, name, Objects.requireNonNullElseGet(downloaderExecutor, ForkJoinPool::commonPool));
            if (res.isFailure() || FilesAssistant.trash(address, username, location, false, PredicateE.truePredicate(), null)) return res;
            return UnionPair.fail(new VisibleFailureReason(FailureKind.Others, location, "Trashing."));
        }
        final UnionPair<VisibleFileInformation, VisibleFailureReason> directory;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            directory = OperateFilesHelper.createDirectory(client, TokenAssistant.getToken(address, username), parent, source.name(), ClientConfiguration.get().duplicatePolicy());
        }
        if (directory.isFailure()) return UnionPair.fail(directory.getE());
        final AtomicReference<Boolean> tested = new AtomicReference<>(null);
        while (true) {
            final VisibleFilesListInformation list = FilesAssistant.list(address, username, location, FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, ClientConfiguration.get().limitPerPage(), null, PredicateE.truePredicate(), null);
            if (list == null) return directory;
            try {
                for (final VisibleFileInformation information: list.informationList()) {
                    final UnionPair<VisibleFileInformation, VisibleFailureReason> res = FilesAssistant.move(address, username, new FileLocation(location.storage(), information.id()), true, parent, downloaderExecutor, p -> {
                        if (p == NotDirectlyPolicy.DownloadAndUpload) {
                            if (tested.get() == null)
                                tested.set(continuer.test(NotDirectlyPolicy.DownloadAndUpload));
                            return tested.get().booleanValue();
                        }
                        return true;
                    });
                    if (res == null || res.isFailure()) return res;
                }
            } catch (final RuntimeException exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            }
            if (list.total() == list.informationList().size()) break;
        }
        return directory;
    }
}
