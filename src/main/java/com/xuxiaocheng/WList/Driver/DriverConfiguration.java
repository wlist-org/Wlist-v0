package com.xuxiaocheng.WList.Driver;

import com.alibaba.fastjson2.JSONArray;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class DriverConfiguration {
    protected final @NotNull JSONArray configurations = new JSONArray();

    protected static <T> @NotNull T getNonNullOrSetDefault(final @NotNull JSONArray configurations, final int index, final Class<? extends T> type, final @NotNull T obj) {
        for (int i = index - configurations.size(); i >= 0; --i)
            configurations.add(null);
        return Objects.requireNonNullElseGet(configurations.getObject(index, type), () -> {
            configurations.set(1, obj);
            return obj;
        });
    }

    protected static <T> @NotNull T getNonNullOrSetDefault(final @NotNull JSONArray configurations, final int index, final Class<? extends T> type, final @NotNull Supplier<? extends @NotNull T> supplier) {
        for (int i = index - configurations.size(); i >= 0; --i)
            configurations.add(null);
        return Objects.requireNonNullElseGet(configurations.getObject(index, type), () -> {
            final T obj = supplier.get();
            configurations.set(1, obj);
            return obj;
        });
    }

    protected @NotNull JSONArray arrayLocal() {
        return DriverConfiguration.getNonNullOrSetDefault(this.configurations, 0, JSONArray.class, JSONArray::new);
    }

    protected @NotNull JSONArray arrayWeb() {
        return DriverConfiguration.getNonNullOrSetDefault(this.configurations, 1, JSONArray.class, JSONArray::new);
    }

    public @NotNull String getName() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayLocal(), 0, String.class, () -> this.getClass().getName());
    }

    public void setName(final @NotNull String name) {
        this.arrayLocal().set(0, name);
    }

    public int getPriority() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayLocal(),1, Integer.class, 0).intValue();
    }

    public void setPriority(final int priority) {
        this.arrayLocal().set(1, priority);
    }

    public boolean getStrictMode() {
        return DriverConfiguration.getNonNullOrSetDefault(this.arrayLocal(),2, Boolean.class, true).booleanValue();
    }

    public void setStrictMode(final boolean strictMode) {
        this.arrayLocal().set(2, strictMode);
    }

    @Override
    public @NotNull String toString() {
        return "DriverConfiguration:" + this.configurations;
    }
}
