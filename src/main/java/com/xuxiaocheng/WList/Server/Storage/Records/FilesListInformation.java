package com.xuxiaocheng.WList.Server.Storage.Records;

import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public record FilesListInformation(long total, long filtered, @NotNull @Unmodifiable List<@NotNull FileInformation> informationList) {
    static final @NotNull FilesListInformation Empty = new FilesListInformation(0, 0, List.of());
}
