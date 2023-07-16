package com.xuxiaocheng.WList.Utils;

import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public final class AndroidSupport {
    private AndroidSupport() {
        super();
    }

    public static final @NotNull BigInteger BigIntegerTwo = PlatformDependent.isAndroid() ? BigInteger.valueOf(2) : BigInteger.TWO;
}
