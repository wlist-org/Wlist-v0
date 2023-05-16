import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.WList.Utils.DelayQueueInByteBufOutByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DelayQueueTest {
    @Test
    public void recompose() throws InterruptedException, IOException {
        try (final DelayQueueInByteBufOutByteBuf queue = new DelayQueueInByteBufOutByteBuf()) {
            queue.put(ByteBufAllocator.DEFAULT.buffer(1).writeByte(1), 0);
            queue.put(ByteBufAllocator.DEFAULT.buffer(2).writeByte(2).writeByte(3), 1);
            this.get(queue, 2, 1);
            this.get(queue, 1, 3);
        }
    }

    @Test
    public void chunk() throws InterruptedException, IOException {
        try (final DelayQueueInByteBufOutByteBuf queue = new DelayQueueInByteBufOutByteBuf()) {
            queue.put(ByteBufAllocator.DEFAULT.buffer(1).writeByte(5), 2);
            queue.put(ByteBufAllocator.DEFAULT.buffer(2).writeByte(3).writeByte(4), 1);
            queue.put(ByteBufAllocator.DEFAULT.buffer(2).writeByte(1).writeByte(2), 0);
            this.get(queue, 1, 1);
            this.get(queue, 2, 2);
            this.get(queue, 2, 4);
        }
    }

    @Test
    public void thread() throws InterruptedException, IOException {
        try (final DelayQueueInByteBufOutByteBuf queue = new DelayQueueInByteBufOutByteBuf()) {
            final ExecutorService pool = Executors.newFixedThreadPool(8);
            pool.submit(() -> queue.put(ByteBufAllocator.DEFAULT.buffer(1).writeByte(5), 2));
            pool.submit(() -> queue.put(ByteBufAllocator.DEFAULT.buffer(2).writeByte(3).writeByte(4), 1));
            pool.submit(() -> queue.put(ByteBufAllocator.DEFAULT.buffer(2).writeByte(1).writeByte(2), 0));
            pool.submit(queue::discardSomeReadBytes);
            pool.submit(() -> queue.put(ByteBufAllocator.DEFAULT.buffer(2).writeByte(7).writeByte(8), 4));
            pool.submit(() -> queue.put(ByteBufAllocator.DEFAULT.buffer(1).writeByte(6), 3));
            this.get(queue, 4, 1);
            this.get(queue, 4, 5);
            HLog.DefaultLogger.log("", "ok.");
            pool.shutdown();
            if (!pool.awaitTermination(15, TimeUnit.SECONDS))
                HLog.DefaultLogger.log("", "rerun.");
        }
    }

    @Test
    public void release() throws InterruptedException, IOException {
        try (final DelayQueueInByteBufOutByteBuf queue = new DelayQueueInByteBufOutByteBuf()) {
            final ByteBuf buf1 = ByteBufAllocator.DEFAULT.buffer(1).writeByte(0);
            final ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer(1).writeByte(1);
            queue.put(buf2, 1);
            queue.put(buf1, 0);
            queue.discardSomeReadBytes();
            this.get(queue, 2, 0);
            queue.discardSomeReadBytes();
            assert buf1.refCnt() == 0;
            assert buf2.refCnt() == 0;
        }
        try (final DelayQueueInByteBufOutByteBuf queue = new DelayQueueInByteBufOutByteBuf()) {
            final ByteBuf buf1 = ByteBufAllocator.DEFAULT.buffer(1).writeByte(0);
            final ByteBuf buf2 = ByteBufAllocator.DEFAULT.buffer(1).writeByte(1);
            queue.put(buf2, 1);
            queue.put(buf1, 0);
            queue.discardSomeReadBytes();
            this.get(queue, 2, 0);
            queue.discardSomeReadBytes();
            assert buf1.refCnt() == 0;
            assert buf2.refCnt() == 0;
        }
    }

    @Test
    public void big() throws InterruptedException, IOException {
        final ByteBuf f = ByteBufAllocator.DEFAULT.buffer(1000000 * 10);
        HLog.DefaultLogger.log("", "pre");
        TimeUnit.SECONDS.sleep(5);
        f.release();
        HLog.DefaultLogger.log("", "start");
        try (final DelayQueueInByteBufOutByteBuf queue = new DelayQueueInByteBufOutByteBuf()) {
            final Collection<ByteBuf> list = new ArrayList<>();
            for (int i = 0; i < 1000000; ++i) {
                final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
                for (int k = 0; k < 10; ++k)
                    buf.writeByte(k);
                queue.put(buf, i);
                this.get(queue, 10, 0);
                list.add(buf);
                if (i % 10 == 0) {
                    queue.discardSomeReadBytes();
                    list.forEach(b -> {assert b.refCnt() == 0;});
                    list.clear();
                    if (i % 10000 == 0)
                        Runtime.getRuntime().gc();
                }
            }
        }
        HLog.DefaultLogger.log("", "ok. Please check memory usage manually.");
        TimeUnit.SECONDS.sleep(5);
    }

    @Test
    public void manyThreads() throws IOException, InterruptedException {
        HLog.DefaultLogger.log("", "start");
        try (final DelayQueueInByteBufOutByteBuf queue = new DelayQueueInByteBufOutByteBuf()) {
            final ExecutorService pool1 = Executors.newFixedThreadPool(8);
            final ExecutorService pool2 = Executors.newFixedThreadPool(8);
            final Queue<ByteBuf> list = new ConcurrentLinkedQueue<>();
            final AtomicInteger removable = new AtomicInteger(0);
            for (int i = 0; i < 1000000; ++i) {
                final int chunk = i;
                pool1.submit(() -> {
                    final ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
                    for (int k = 0; k < 10; ++k)
                        buf.writeByte(k);
                    queue.put(buf, chunk);
                    list.offer(buf);
                    return null;
                });
                pool2.submit(() -> {
                    this.get(queue, 10, 0);
                    removable.getAndIncrement();
                    if (chunk % 10 == 0) {
                        queue.discardSomeReadBytes();
                        while (removable.get() > 0) {
                            if (removable.decrementAndGet() < 0) {
                                removable.getAndDecrement();
                                break;
                            }
                            final ByteBuf buf = list.remove();
                            if (buf.refCnt() != 0)
                                list.add(buf);
                        }
                        if (chunk % 10000 == 0)
                            Runtime.getRuntime().gc();
                    }
                    return null;
                });
            }
            HLog.DefaultLogger.log("", "submitted");
            pool1.shutdown();
            pool2.shutdown();
            if (!pool1.awaitTermination(60, TimeUnit.SECONDS))
                HLog.DefaultLogger.log("", "rerun1.");
            if (!pool2.awaitTermination(60, TimeUnit.SECONDS))
                HLog.DefaultLogger.log("", "rerun2.");
            HLog.DefaultLogger.log("", removable);
            queue.discardSomeReadBytes();
            HLog.DefaultLogger.log("", list.size());
            while (!list.isEmpty()) {
                final ByteBuf buf = list.remove();
                if (buf.refCnt() != 0) {
                    HLog.DefaultLogger.log("ERROR", buf);
                    throw new AssertionError();
                }
            }
        }
        Runtime.getRuntime().gc();
        HLog.DefaultLogger.log("", "ok");
        // Warning: memory should be released.
        TimeUnit.SECONDS.sleep(5);
    }

    void get(final @NotNull DelayQueueInByteBufOutByteBuf queue, final int size, final int start) throws InterruptedException {
        final ByteBuf buf = Objects.requireNonNull(queue.get(size)).getSecond();
        assert buf.readableBytes() == size;
        for (int i = 0; i < size; ++i)
            assert buf.readByte() == start + i;
        buf.release();
        assert buf.refCnt() == 1;
    }
}
