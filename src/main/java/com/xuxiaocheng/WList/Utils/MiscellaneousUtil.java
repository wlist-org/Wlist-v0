package com.xuxiaocheng.WList.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class MiscellaneousUtil {
    private MiscellaneousUtil() {
        super();
    }

    public static final @NotNull Pattern md5Pattern = Pattern.compile("^[a-z0-9]{32}$");

    public static @NotNull MessageDigest getMd5Digester() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
    }

    public static @NotNull String getMd5(final byte @NotNull [] source) {
        final MessageDigest md5 = MiscellaneousUtil.getMd5Digester();
        md5.update(source);
        final BigInteger i = new BigInteger(1, md5.digest());
        return String.format("%32s", i.toString(16)).replace(' ', '0');
    }

    public static void updateMessageDigest(final @NotNull MessageDigest digester, final @NotNull InputStream source) throws IOException {
        int remaining = source.available();
        final int size = Math.min(1 << 20, remaining);
        int nr;
        for (final byte[] buffer = new byte[size]; remaining > 0L; remaining -= nr) {
            nr = source.read(buffer, 0, Math.min(size, remaining));
            if (nr < 0)
                break;
            digester.update(buffer, 0, nr);
        }
    }

    public static <T> @NotNull Iterator<T> getEmptyIterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public @NotNull T next() {
                throw new NoSuchElementException();
            }
        };
    }

    public static <K, V> @NotNull K randomKeyAndPut(final @NotNull Map<? super @NotNull K, V> map, final @NotNull Supplier<? extends @NotNull K> randomKey, final V value) {
        K k;
        while (true) {
            k = randomKey.get();
            final boolean[] flag = {false};
            final K finalK = k;
            map.computeIfAbsent(finalK, (i) -> {
                flag[0] = true;
                return value;
            });
            if (flag[0])
                break;
        }
        return k;
    }

    public static @NotNull String bin(final byte b) {
        return String.format("0b%8s", Integer.toString(b < 0 ? b + 0xff : b, 2)).replace(' ', '0');
    }

    public static int calculatePartCount(final long total, final int limit) {
        return (int) Math.ceil(((double) total) / limit);
    }

    public static <T> @NotNull Iterator<@NotNull T> wrapCountedBlockingQueueCancellable(final @NotNull BlockingQueue<? extends @NotNull T> queue, final long count, final @NotNull AtomicBoolean cancelFlag, final long query) {
        final AtomicLong spareElement = new AtomicLong(count);
        final AtomicInteger takingElement = new AtomicInteger(0);
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return spareElement.get() > takingElement.get() && !cancelFlag.get();
            }

            @Override
            public @NotNull T next() {
                if (!this.hasNext())
                    throw new NoSuchElementException();
                takingElement.getAndIncrement();
                try {
                    T t = null;
                    while (t == null) {
                        t = queue.poll(query, TimeUnit.MILLISECONDS);
                        if (t == null && cancelFlag.get())
                            throw new InterruptedException();
                    }
                    spareElement.getAndDecrement();
                    return t;
                } catch (final InterruptedException exception) {
                    throw new NoSuchElementException(exception);
                } finally {
                    takingElement.getAndDecrement();
                }
            }
        };
    }
}
