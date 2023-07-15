package com.xuxiaocheng.WListClientAndroid.Helpers;

import androidx.annotation.NonNull;

public enum SpecialDriverName {
    RootDriver("WList#RootDriver")
    ;

    @NonNull
    private final String identifier;

    SpecialDriverName(@NonNull final String identifier) {
        this.identifier = identifier;
    }

    @NonNull public String getIdentifier() {
        return this.identifier;
    }

    @Override
    @NonNull public String toString() {
        return "SpecialDriverName{" +
                "identifier='" + this.identifier + '\'' +
                '}';
    }
}
