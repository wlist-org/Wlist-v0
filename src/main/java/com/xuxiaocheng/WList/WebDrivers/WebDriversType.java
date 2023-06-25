package com.xuxiaocheng.WList.WebDrivers;

import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Trash_123pan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum WebDriversType {
    Driver_123Pan(Driver_123Pan::new, Trash_123pan::new),
    ;
    private final @NotNull Supplier<@NotNull DriverInterface<?>> driver;
    private final @Nullable Supplier<@NotNull DriverTrashInterface<?>> trash;
    WebDriversType(final @NotNull Supplier<@NotNull DriverInterface<?>> driver, final @Nullable Supplier<@NotNull DriverTrashInterface<?>> trash) {
        this.driver = driver;
        this.trash = trash;
    }

    public @NotNull Supplier<@NotNull DriverInterface<?>> getDriver() {
        return this.driver;
    }

    public @Nullable Supplier<@NotNull DriverTrashInterface<?>> getTrash() {
        return this.trash;
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
