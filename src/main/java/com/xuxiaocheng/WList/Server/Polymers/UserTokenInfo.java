package com.xuxiaocheng.WList.Server.Polymers;

import com.xuxiaocheng.WList.Server.Operation;
import org.jetbrains.annotations.NotNull;

import java.util.SortedSet;

public record UserTokenInfo(@NotNull String username, @NotNull String password, @NotNull SortedSet<Operation.@NotNull Permission> permissions) {
}
