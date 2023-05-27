package com.xuxiaocheng.WList.Driver.Options;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum DuplicatePolicy {
    ERROR,
    OVER,
    KEEP,
//    KEEP_DIFFERENT,
    ;
    public static final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull DuplicatePolicy> Map = Stream.of(DuplicatePolicy.values())
            .collect(Collectors.toMap(Enum::name, p -> p));
}
