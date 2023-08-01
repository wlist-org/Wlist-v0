package com.xuxiaocheng.WList.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public final class MiscellaneousUtil {
    private MiscellaneousUtil() {
        super();
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

    public static int calculatePartCount(final long total, final int limit) {
        //noinspection NumericCastThatLosesPrecision
        return (int) Math.ceil(((double) total) / limit);
    }
}
