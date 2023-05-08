package com.xuxiaocheng.WList.Utils;

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
import java.util.function.Consumer;

public class DelayQueueInStreamOutByteBuf implements Closeable {
    protected final @NotNull InputStream inputStream;
    protected final @NotNull ExecutorService threadPool;
    protected final int bufferSize;
    protected final int bufferCapacity;
    protected final @NotNull Consumer<Exception> exceptionCallback;
    private @NotNull CompletableFuture<?> worker;
    protected final @NotNull BlockingQueue<@NotNull ByteBuf> bufferQueue;

    private @NotNull CompletableFuture<?> newWorker(final int times) {
        return CompletableFuture.runAsync(() -> {
            ByteBuf buffer = null;
            for (int i = 0; i < times; ++i)
                try {
                    synchronized (this.inputStream) {
                        final int len = Math.min(this.inputStream.available(), this.bufferSize);
                        if (len < 1)
                            return;
                        buffer = ByteBufAllocator.DEFAULT.buffer(len, this.bufferSize);
                        buffer.writeBytes(this.inputStream, len);
                        // Still in synchronized block because of the order in buffer queue.
                        this.bufferQueue.put(buffer);
                    }
                } catch (final IOException | InterruptedException exception) {
                    if (buffer != null)
                        buffer.release();
                    this.exceptionCallback.accept(exception);
                } finally {
                    buffer = null;
                }
        }, this.threadPool);
    }

    public DelayQueueInStreamOutByteBuf(final @NotNull InputStream inputStream, final @NotNull ExecutorService threadPool, final int bufferSize, final int bufferCapacity, final @NotNull Consumer<Exception> exceptionCallback) {
        super();
        this.inputStream = inputStream;
        this.threadPool = threadPool;
        this.bufferSize = bufferSize;
        this.bufferCapacity = bufferCapacity;
        this.exceptionCallback = exceptionCallback;
        this.bufferQueue = new LinkedBlockingQueue<>(bufferCapacity);
        this.worker = this.newWorker(bufferCapacity);
    }

    public @NotNull InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public void close() {
        synchronized (this) {
            this.worker.cancel(true);
            try {
                this.worker.get();
            } catch (final InterruptedException | ExecutionException ignore) {
            }
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

    public @Nullable ByteBuf get() throws InterruptedException {
        synchronized (this) {
            if (this.worker.isDone() && this.bufferQueue.isEmpty()) {
                this.close();
                return null;
            }
        }
        final ByteBuf cached = this.bufferQueue.take();
        try {
            synchronized (this) {
                if (this.worker.isDone()) {
                    if (this.inputStream.available() > 0)
                        this.worker = this.newWorker(this.bufferCapacity - this.bufferQueue.size());
                    else
                        this.inputStream.close();
                }
            }
        } catch (final IOException exception) {
            this.exceptionCallback.accept(exception);
        }
        return cached;
    }

    @Override
    public synchronized @NotNull String toString() {
        return "DelayQueueInStreamOutByteBuf{" +
                "inputStream=" + this.inputStream +
                ", threadPool=" + this.threadPool +
                ", bufferSize=" + this.bufferSize +
                ", bufferCapacity=" + this.bufferCapacity +
                ", worker=" + this.worker +
                ", bufferQueue=" + this.bufferQueue +
                '}';
    }
}
