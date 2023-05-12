package com.xuxiaocheng.WListClient.Server.Options;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum OrderPolicy {
    FileName,
    Size,
    CreateTime,
    UpdateTime,
    ;
    public static final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull OrderPolicy> Map = Stream.of(OrderPolicy.values())
            .collect(Collectors.toMap(Enum::name, p -> p));
}
