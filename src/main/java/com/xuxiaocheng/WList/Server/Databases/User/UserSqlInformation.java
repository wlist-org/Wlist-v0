package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.WList.Server.Operation;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.SortedSet;

public record UserSqlInformation(long id, @NotNull String username, @NotNull String password,
                                 @NotNull SortedSet<Operation.@NotNull Permission> permissions,
                                 @NotNull LocalDateTime modifyTime) {
}
