package com.xuxiaocheng.WList.WebDrivers;

import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.function.Supplier;

public final class WebDriversRegisterer {
    private WebDriversRegisterer() {
        super();
    }

    public static final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull Supplier<DriverInterface<?>>> DriversMap =
            Map.of("123pan", Driver_123Pan::new);
}
