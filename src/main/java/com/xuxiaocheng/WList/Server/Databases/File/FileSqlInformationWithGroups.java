package com.xuxiaocheng.WList.Server.Databases.File;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record FileSqlInformationWithGroups(@NotNull FileSqlInformation information, @NotNull Set<@NotNull Long> availableForGroup) {
}
