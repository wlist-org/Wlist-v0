package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DelayQueueInStreamOutByteBuf implements Closeable {
    protected final @NotNull InputStream inputStream;
    protected final @NotNull ExecutorService threadPool;
    protected final int bufferSize;
    protected final int bufferCapacity;
    protected final @NotNull Consumer<@NotNull IOException> exceptionCallback;
    private @NotNull CompletableFuture<Void> worker = CompletableFuture.completedFuture(null);
    protected final @NotNull AtomicInteger workingCount = new AtomicInteger(-1);
    protected final @NotNull BlockingQueue<@NotNull ByteBuf> bufferQueue;
    protected final @NotNull AtomicInteger counter = new AtomicInteger(0);
    protected boolean started = false;
    protected boolean closed = false;

    protected synchronized void addWorker(final int times) {
        if (times < 0)
            throw new IllegalArgumentException("Times is negative. times: " + times);
        if (this.workingCount.getAndAdd(times) < 0)
            this.worker = CompletableFuture.runAsync(() -> {
                this.workingCount.getAndIncrement();
                ByteBuf buffer = null;
                while (this.workingCount.decrementAndGet() >= 0)
                    try {
                        synchronized (this.inputStream) {
                            final int len = Math.min(this.inputStream.available(), this.bufferSize);
                            if (len < 1)
                                return;
                            buffer = ByteBufAllocator.DEFAULT.buffer(len, this.bufferSize);
                            buffer.writeBytes(this.inputStream, len);
                            this.bufferQueue.put(buffer.retain());
                            buffer = null;
                        }
                    } catch (final IOException exception) {
                        this.exceptionCallback.accept(exception);
                    } catch (final InterruptedException ignore) {
                    } finally {
                        if (buffer != null)
                            buffer.release();
                    }
            }, this.threadPool);
    }

    public DelayQueueInStreamOutByteBuf(final @NotNull InputStream inputStream, final @NotNull ExecutorService threadPool, final int bufferSize, final int bufferCapacity, final @NotNull Consumer<@NotNull IOException> exceptionCallback) {
        super();
        this.inputStream = inputStream;
        this.threadPool = threadPool;
        this.bufferSize = bufferSize;
        this.bufferCapacity = bufferCapacity;
        this.exceptionCallback = exceptionCallback.andThen((e) -> this.close());
        this.bufferQueue = new LinkedBlockingQueue<>(bufferCapacity);
    }

    public void start() {
        if (this.started)
            return;
        this.started = true;
        this.addWorker(this.bufferCapacity);
    }

    public @NotNull InputStream getInputStream() {
        return this.inputStream;
    }

    public @NotNull ExecutorService getThreadPool() {
        return this.threadPool;
    }

    public int getBufferSize() {
        return this.bufferSize;
    }

    public int getBufferCapacity() {
        return this.bufferCapacity;
    }

    @Override
    public void close() {
        if (this.closed)
            return;
        this.closed = true;
        synchronized (this) {
            this.worker.cancel(true);
            try {
                this.worker.get();
            } catch (final InterruptedException | ExecutionException ignore) {
            }
            this.workingCount.set(0); // Skip #addWork
        }
        try {
            this.inputStream.close();
        } catch (final IOException exception) {
            this.exceptionCallback.accept(exception);
        }
        while (!this.bufferQueue.isEmpty()) {
            try {
                final ByteBuf buf = this.bufferQueue.poll(0L, TimeUnit.NANOSECONDS);
                if (buf == null)
                    break;
                buf.release();
            } catch (final InterruptedException ignore) {
            }
        }
    }

    public Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> get() throws InterruptedException {
        if (this.closed)
            return null;
        if (!this.started)
            this.start();
        synchronized (this) {
            if (this.worker.isDone() && this.bufferQueue.isEmpty()) {
                this.close();
                return null;
            }
        }
        final ByteBuf cached = this.bufferQueue.take();
        this.addWorker(this.bufferCapacity - this.bufferQueue.size());
        return Pair.ImmutablePair.makeImmutablePair(this.counter.getAndIncrement(), cached);
    }
}
