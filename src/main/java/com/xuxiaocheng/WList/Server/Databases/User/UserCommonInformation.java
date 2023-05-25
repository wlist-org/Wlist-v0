package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.WList.Server.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;

public record UserCommonInformation(@NotNull String username, @NotNull String password, @Nullable SortedSet<Operation.@NotNull Permission> permissions) {
}
