package com.xuxiaocheng.WList.WebDrivers;

import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverTrashInterface;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Driver_123Pan_NoCache;
import com.xuxiaocheng.WList.WebDrivers.Driver_123pan.Trash_123pan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum WebDriversType {
//    Driver_123Pan("123pan", Driver_123Pan::new, Trash_123pan::new),
    Driver_123PanNoCache("123pan_nocache", Driver_123Pan_NoCache::new, null),
    ;
    private static final @NotNull Map<@NotNull String, @NotNull WebDriversType> drivers = new HashMap<>(); static {
        for (final WebDriversType type: WebDriversType.values())
            WebDriversType.drivers.put(type.identifier, type);
    }

    public static @Nullable WebDriversType get(final @Nullable String identifier) {
        return WebDriversType.drivers.get(identifier);
    }

    private final @NotNull String identifier;
    private final @NotNull Supplier<@NotNull DriverInterface<?>> driver;
    private final @Nullable Supplier<@NotNull DriverTrashInterface<?>> trash;

    WebDriversType(final @NotNull String identifier, final @NotNull Supplier<@NotNull DriverInterface<?>> driver, final @Nullable Supplier<@NotNull DriverTrashInterface<?>> trash) {
        this.identifier = identifier;
        this.driver = driver;
        this.trash = trash;
    }

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public @NotNull Supplier<@NotNull DriverInterface<?>> getDriver() {
        return this.driver;
    }

    public @Nullable Supplier<@NotNull DriverTrashInterface<?>> getTrash() {
        return this.trash;
    }

    @Override
    public @NotNull String toString() {
        return "WebDriversType(" + this.name() + ')';
    }
}
