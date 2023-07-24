package com.xuxiaocheng.WList.Utils;

import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AndroidSupport {
    private AndroidSupport() {
        super();
    }

    public static final boolean jmxEnable = ((Supplier<Boolean>) () -> {
        try {
            Class.forName("java.lang.management.ManagementFactory");
            return true;
        } catch (final ClassNotFoundException ignore){
            return false;
        }
    }).get().booleanValue();

    public static final @NotNull BigInteger BigIntegerTwo = PlatformDependent.isAndroid() ? BigInteger.valueOf(2) : BigInteger.TWO;

    public static <T> @NotNull @UnmodifiableView List<T> streamToList(final @NotNull Stream<T> stream) {
        return PlatformDependent.isAndroid() ? stream.collect(Collectors.toList()) : stream.toList();
    }
}
