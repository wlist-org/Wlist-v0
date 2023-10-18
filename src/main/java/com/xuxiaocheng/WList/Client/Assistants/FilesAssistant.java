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
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
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
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
import java.io.FileNotFoundException;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
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


    // All callback function should be sync.
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

    private static @NotNull @Unmodifiable List<@NotNull String> calculateChecksums(final @NotNull File file, final @NotNull Collection<@NotNull UploadChecksum> requirements) {
        return FilesAssistant.calculateChecksumsCore(requirements, HExceptionWrapper.wrapConsumer(runner -> {
            try (final RandomAccessFile access = new RandomAccessFile(file, "r");
                 final FileChannel channel = access.getChannel()) {
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

    private static @Nullable VisibleFailureReason uploadCore(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation parent, final @NotNull String filename, final long size, final @NotNull Predicate<? super @NotNull UploadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback, final @NotNull Function<? super @NotNull Collection<@NotNull UploadChecksum>, ? extends @NotNull @Unmodifiable List<@NotNull String>> calculator,
                                                             final @NotNull Consumer<? super @NotNull Consumer<? super @NotNull BiConsumer<? super Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>, ? super @NotNull Consumer<? super @NotNull BiFunction<? super @NotNull Integer,? super @NotNull ByteBuf, @NotNull Integer>>>>> runner) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<UploadConfirm, VisibleFailureReason> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.requestUploadFile(client, TokenAssistant.getToken(address, username), parent, filename, size, ClientConfiguration.get().duplicatePolicy());
        }
        if (confirm == null)
            return new VisibleFailureReason(FailureKind.Others, parent, "Requesting.");
        if (confirm.isFailure())
            return confirm.getE();
        if (!continuer.test(confirm.getT()))
            return null;
        final List<String> checksums = calculator.apply(confirm.getT().checksums());
        HLog.getInstance("ClientLogger").log(HLogLevel.LESS, "Calculated checksums.", ParametersMap.create()
                .add("filename", filename).add("size", size).add("checksums", checksums));
        final UploadConfirm.UploadInformation information;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            information = OperateFilesHelper.confirmUploadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id(), checksums);
        }
        if (information == null)
            return new VisibleFailureReason(FailureKind.Others, parent, "Confirming.");
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(ClientConfiguration.get().threadCount() > 0 ?
                ClientConfiguration.get().threadCount() : Math.min(Runtime.getRuntime().availableProcessors(), information.parallel().size()),
                new DefaultThreadFactory(String.format("UploadingExecutor#%s:%d@%s", parent, size, filename)));
        final AtomicBoolean failure = new AtomicBoolean(false);
        final AtomicBoolean finishSuccess = new AtomicBoolean(false);
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
                                while (length > 0) {
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
            return new VisibleFailureReason(FailureKind.Others, parent, "Uploading.");
        return finishSuccess.get() ? null : new VisibleFailureReason(FailureKind.Others, parent, "Finishing.");
    }

    public static @Nullable VisibleFailureReason upload(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull File file, final @NotNull FileLocation parent, final @Nullable String filename, final @NotNull Predicate<? super @NotNull UploadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        if (!file.isFile() || !file.canRead())
            throw new FileNotFoundException("Not a upload-able file." + ParametersMap.create().add("file", file));
        try {
            return FilesAssistant.uploadCore(address, username, parent, Objects.requireNonNullElseGet(filename, file::getName), file.length(), continuer, callback, requirements -> FilesAssistant.calculateChecksums(file, requirements), HExceptionWrapper.wrapConsumer(runner -> {
                try (final RandomAccessFile accessFile = new RandomAccessFile(file, "r");
                     final FileChannel fileChannel = accessFile.getChannel()) { // Only lock.
                    runner.accept(HExceptionWrapper.wrapBiConsumer((pair, consumer) -> {
                        try(final RandomAccessFile access = new RandomAccessFile(file, "r");
                            final FileChannel channel = access.getChannel().position(pair.getFirst().longValue());
                            final FileLock ignoredLock = fileChannel.lock(pair.getFirst().longValue(), pair.getSecond().longValue(), true)) {
                            consumer.accept(HExceptionWrapper.wrapBiFunction((size, buffer) -> buffer.writeBytes(channel, size.intValue())));
                        }
                    }));
                }
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class);
        }
    }

    /**
     * @param stream Warning: this method will be called many times and may in multi threads. {@link InputStream#skipNBytes(long)} is used to seek position.
     */
    public static @Nullable VisibleFailureReason uploadStream(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull Consumer<? super @NotNull Consumer<? super @NotNull InputStream>> stream, final long size, final @NotNull String filename, final @NotNull FileLocation parent, final @NotNull Predicate<? super @NotNull UploadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        try {
            return FilesAssistant.uploadCore(address, username, parent, filename, size, continuer, callback, requirements -> {
                final AtomicReference<List<String>> res = new AtomicReference<>();
                stream.accept((Consumer<? super InputStream>) inputStream -> res.set(FilesAssistant.calculateChecksumsStream(inputStream, requirements)));
                return res.get();
            }, runner -> runner.accept(HExceptionWrapper.wrapBiConsumer((pair, consumer) -> stream.accept(HExceptionWrapper.wrapConsumer(inputStream -> {
                AndroidSupporter.skipNBytes(inputStream, pair.getFirst().longValue());
                consumer.accept(HExceptionWrapper.wrapBiFunction((length, buffer) -> buffer.writeBytes(inputStream, length.intValue())));
            })))));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class, InterruptedException.class, WrongStateException.class);
        }
    }


    @Contract(pure = true)
    private static @NotNull File getDownloadRecordFile(final @NotNull File file) {
        //noinspection SpellCheckingInspection
        return new File(file.getParentFile(), file.getName() + ".downloadrecord");
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

    public static @Nullable VisibleFailureReason download(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final @NotNull File file, final @NotNull Predicate<? super @NotNull DownloadConfirm> continuer, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        final List<Pair.ImmutablePair<Long, Long>> downloaded = FilesAssistant.readDownloadingProgress(location, file);
        if (downloaded.isEmpty()) {
            FilesAssistant.finishDownloadingProgress(file);
            return null;
        }
        final UnionPair<DownloadConfirm, VisibleFailureReason> firstConfirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            firstConfirm = OperateFilesHelper.requestDownloadFile(client, TokenAssistant.getToken(address, username), location, 0, Long.MAX_VALUE);
        }
        if (firstConfirm == null)
            return new VisibleFailureReason(FailureKind.Others, location, "Requesting.");
        if (firstConfirm.isFailure())
            return firstConfirm.getE();
        if (!continuer.test(firstConfirm.getT()))
            return null;
        if (!firstConfirm.getT().acceptedRange()) {
            downloaded.clear();
            downloaded.add(Pair.ImmutablePair.makeImmutablePair(0L, firstConfirm.getT().downloadingSize()));
        }
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
             OperateFilesHelper.cancelDownloadFile(client, TokenAssistant.getToken(address, username), firstConfirm.getT().id());
        }
        try (final RandomAccessFile accessFile = new RandomAccessFile(file, "rw")) {
            accessFile.setLength(firstConfirm.getT().downloadingSize());
        }
        final EventExecutorGroup executors = new DefaultEventExecutorGroup(ClientConfiguration.get().threadCount() > 0 ?
                ClientConfiguration.get().threadCount() : Math.min(Runtime.getRuntime().availableProcessors(), downloaded.size() + 1),
                new DefaultThreadFactory(String.format("DownloadingExecutor#%s:%d@%s", location, firstConfirm.getT().downloadingSize(), file.getAbsolutePath())));
        boolean flag = true;
        final AtomicBoolean failure = new AtomicBoolean(false);
        final Collection<Pair.ImmutablePair<AtomicLong, Long>> progress = new ArrayList<>();
        try {
            final CountDownLatch latch = new CountDownLatch(downloaded.size());
            final Collection<String> ids = new ArrayList<>();
            for (final Pair.ImmutablePair<Long, Long> state: downloaded) {
                if (failure.get()) break;
                final UnionPair<DownloadConfirm, VisibleFailureReason> confirm;
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                    confirm = OperateFilesHelper.requestDownloadFile(client, TokenAssistant.getToken(address, username), location, state.getFirst().longValue(), state.getSecond().longValue());
                }
                if (confirm == null || confirm.isFailure() || confirm.getT().acceptedRange() != firstConfirm.getT().acceptedRange())
                    return confirm == null || confirm.isSuccess() ? new VisibleFailureReason(FailureKind.Others, location, "Requesting.") : confirm.getE();
                final DownloadConfirm.DownloadInformation information;
                try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                    information = OperateFilesHelper.confirmDownloadFile(client, TokenAssistant.getToken(address, username), confirm.getT().id());
                }
                if (information == null)
                    return new VisibleFailureReason(FailureKind.Others, location, "Confirming.");
                int i = 0;
                long position = 0;
                ids.add(confirm.getT().id());
                for (final Pair.ImmutablePair<Long, Long> pair: information.parallel()) {
                    if (position != pair.getFirst().longValue())
                        throw new IllegalStateException("Invalid download chunk." + ParametersMap.create().add("address", address).add("username", username).add("file", file)
                                .add("location", location).add("confirm", confirm).add("information", information).add("position", position).add("parallel", information.parallel()));
                    position = pair.getSecond().longValue();
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
                        latch.countDown();
                        //noinspection VariableNotUsedInsideIf
                        if (e != null)
                            failure.set(true);
                    }, false), executors).exceptionally(MiscellaneousUtil.exceptionHandler());
                }
                if (position != confirm.getT().downloadingSize())
                    throw new IllegalStateException("Invalid download size." + ParametersMap.create().add("address", address).add("username", username).add("file", file)
                            .add("location", location).add("confirm", confirm).add("information", information).add("position", position).add("parallel", information.parallel()));
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
                        callback.accept(s);
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

    @Contract("_, _, _, _, _, _, _, null, !null -> fail")
    public static @Nullable VisibleFilesListInformation list(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation directory, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.@NotNull Order, Options.@NotNull OrderDirection> orders, final long position, final int limit, final @Nullable ScheduledExecutorService executor, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        final UnionPair<VisibleFilesListInformation, RefreshConfirm> confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.listFiles(client, TokenAssistant.getToken(address, username), directory, filter, orders, position, limit);
        }
        if (confirm == null)
            return null;
        if (confirm.isSuccess())
            return confirm.getT();
        FilesAssistant.refreshCore(address, username, executor, callback, confirm.getE().id());
        return FilesAssistant.list(address, username, directory, filter, orders, position, limit, executor, callback);
    }

    @Contract("_, _, _, null, !null -> fail")
    public static boolean refresh(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation directory, final @Nullable ScheduledExecutorService executor, final @Nullable Consumer<? super @NotNull InstantaneousProgressState> callback) throws IOException, InterruptedException, WrongStateException {
        final RefreshConfirm confirm;
        try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
            confirm = OperateFilesHelper.refreshDirectory(client, TokenAssistant.getToken(address, username), directory);
        }
        if (confirm == null)
            return false;
        FilesAssistant.refreshCore(address, username, executor, callback, confirm.id());
        return true;
    }


    private static boolean trash0(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final AtomicBoolean interruptFlag, final @NotNull Executor executor) throws IOException, InterruptedException, WrongStateException {
        if (interruptFlag.get()) return true;
        final Boolean success;
        if (!isDirectory) {
            try (final WListClientInterface client = WListClientManager.quicklyGetClient(address)) {
                success = OperateFilesHelper.trashFileOrDirectory(client, TokenAssistant.getToken(address, username), location, false);
            }
            return success == null || success.booleanValue();
        }
        while (true) {
            final VisibleFilesListInformation list = FilesAssistant.list(address, username, location, Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, ClientConfiguration.get().limitPerPage(), null, null);
            if (list == null) return true;
            try {
                HMultiRunHelper.runConsumers(executor, list.informationList(), HExceptionWrapper.wrapConsumer(information -> {
                    if (!FilesAssistant.trash0(address, username, new FileLocation(location.storage(), information.id()), information.isDirectory(), interruptFlag, executor))
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
        return s == null || s.booleanValue();
    }

    /**
     * @param trashRecursivelyCallback When trash is too complex, it will be called. Then the operation won't be atomic.
     */
    public static boolean trash(final @NotNull SocketAddress address, final @NotNull String username, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull Predicate<@Nullable Void> trashRecursivelyCallback) throws IOException, InterruptedException, WrongStateException {
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
        try {
            return FilesAssistant.trash0(address, username, location, true, new AtomicBoolean(false), executors);
        } finally {
            executors.shutdownGracefully().sync();
        }
    }

}
