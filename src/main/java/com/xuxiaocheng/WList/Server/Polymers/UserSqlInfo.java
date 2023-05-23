package com.xuxiaocheng.WList.Server.Polymers;

import com.xuxiaocheng.WList.Server.Operation;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.SortedSet;

public record UserSqlInfo(@NotNull String password, @NotNull SortedSet<Operation.@NotNull Permission> permissions, @NotNull LocalDateTime modifyTime) {
}
