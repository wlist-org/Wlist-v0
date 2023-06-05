package com.xuxiaocheng.WList.WebDrivers;

import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum WebDriversType {
    Driver_123Pan(Driver_123Pan::new),
//    LocalDiskDriver(LocalDisk::new),
    ;
    private final @NotNull Supplier<DriverInterface<?>> supplier;
    WebDriversType(final @NotNull Supplier<DriverInterface<?>> supplier) {
        this.supplier = supplier;
    }

    public @NotNull Supplier<DriverInterface<?>> getSupplier() {
        return this.supplier;
    }

    public static @Nullable WebDriversType get(final @Nullable String name) {
        if (name == null)
            return null;
        try {
            return WebDriversType.valueOf(name);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public @NotNull String toString() {
        return "WebDriversType(" + this.name() + ')';
    }
}
