package com.xuxiaocheng.WList.Driver.Options;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum OrderDirection {
    ASCEND, DESCEND,
    ;
    public static final @NotNull @UnmodifiableView Map<@NotNull String, @NotNull OrderDirection> Map = Stream.of(OrderDirection.values())
            .collect(Collectors.toMap(Enum::name, d -> d));
}
