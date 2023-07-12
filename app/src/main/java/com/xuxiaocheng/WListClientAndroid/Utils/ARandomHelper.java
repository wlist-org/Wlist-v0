package com.xuxiaocheng.WListClientAndroid.Utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;

import java.util.Random;
import java.util.UUID;

/**
 * Special version for Android.
 * @see HRandomHelper
 */
public final class ARandomHelper {
    private ARandomHelper() {
        super();
    }

    public static void setArray(@NonNull final Random random, @NonNull final byte[] target, final byte[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    public static void setArray(@NonNull final Random random, final short[] target, final short[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    public static void setArray(@NonNull final Random random, final int[] target, final int[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    public static void setArray(@NonNull final Random random, final long[] target, final long[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    public static void setArray(@NonNull final Random random, final float[] target, final float[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    public static void setArray(@NonNull final Random random, final double[] target, final double[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    public static void setArray(@NonNull final Random random, final char[] target, final char[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    public static <T> void setArray(@NonNull final Random random, final T[] target, final T[] available) {
        for (int i = 0; i < target.length; ++i)
            target[i] = available[random.nextInt(available.length)];
    }

    @NonNull
    public static String nextString(@NonNull final Random random, final int length, @Nullable final String words) {
        final char[] word = new char[length];
        ARandomHelper.setArray(random, word, (words == null ? HRandomHelper.DefaultWords : words).toCharArray());
        return new String(word);
    }

    /**
     * @see UUID#randomUUID()
     */
    @NonNull
    public static UUID getRandomUUID(@NonNull final Random random) {
        final byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        randomBytes[6] = (byte) (randomBytes[6] & 0x0f);
        randomBytes[6] = (byte) (randomBytes[6] | 0x40);
        randomBytes[8] = (byte) (randomBytes[8] & 0x3f);
        //noinspection NumericCastThatLosesPrecision
        randomBytes[8] = (byte) (randomBytes[8] | 0x80);
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; ++i)
            msb = (msb << 8) | (randomBytes[i] & 0xff);
        for (int i = 8; i < 16; ++i)
            lsb = (lsb << 8) | (randomBytes[i] & 0xff);
        return new UUID(msb, lsb);
    }
}
