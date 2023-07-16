package com.xuxiaocheng.WList.Driver;

import org.jetbrains.annotations.NotNull;

public enum SpecialDriverName {
    RootDriver("WList#RootDriver"),
    ;
    private final @NotNull String identifier;

    SpecialDriverName(final @NotNull String identifier) {
        this.identifier = identifier;
    }

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    @Override
    public @NotNull String toString() {
        return "SpecialDriverName{" +
                "name='" + this.name() + '\'' +
                ", identify='" + this.identifier + '\'' +
                '}';
    }
}
