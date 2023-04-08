package com.xuxiaocheng.WList.Driver.Configuration;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public abstract class LocalSideDriverConfiguration {
    protected @NotNull String name = "Driver";
    protected @NotNull BigInteger priority = BigInteger.ZERO;
    protected boolean strictMode = true;

    public @NotNull String getName() {
        return this.name;
    }

    public void setName(final @NotNull String name) {
        this.name = name;
    }

    public @NotNull BigInteger getPriority() {
        return this.priority;
    }

    public void setPriority(final @NotNull BigInteger priority) {
        this.priority = priority;
    }

    public boolean getStrictMode() {
        return this.strictMode;
    }

    public void setStrictMode(final boolean strictMode) {
        this.strictMode = strictMode;
    }

    @Override
    public @NotNull String toString() {
        return "LocalSideDriverConfiguration{" +
                "name='" + this.name + '\'' +
                ", priority=" + this.priority +
                ", strictMode=" + this.strictMode +
                '}';
    }
}
