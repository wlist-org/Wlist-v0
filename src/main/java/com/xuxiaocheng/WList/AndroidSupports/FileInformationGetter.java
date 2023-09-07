package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Client.Server.VisibleFileInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @see VisibleFileInformation
 */
public final class FileInformationGetter {
    private FileInformationGetter() {
        super();
    }

    public static long id(final @NotNull VisibleFileInformation information) {
        return information.id();
    }

    public static long parentId(final @NotNull VisibleFileInformation information) {
        return information.parentId();
    }

    public static @NotNull String name(final @NotNull VisibleFileInformation information) {
        return information.name();
    }

    public static boolean isDirectory(final @NotNull VisibleFileInformation information) {
        return information.isDirectory();
    }

    public static long size(final @NotNull VisibleFileInformation information) {
        return information.size();
    }

    public static @Nullable LocalDateTime createTime(final @NotNull VisibleFileInformation information) {
        return information.createTime();
    }

    public static @NotNull String createTimeString(final @NotNull VisibleFileInformation information, final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return information.createTimeString(formatter, unknown);
    }

    public static @Nullable LocalDateTime updateTime(final @NotNull VisibleFileInformation information) {
        return information.updateTime();
    }

    public static @NotNull String updateTimeString(final @NotNull VisibleFileInformation information, final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return information.updateTimeString(formatter, unknown);
    }

    public static @NotNull String md5(final @NotNull VisibleFileInformation information) {
        return information.md5();
    }
}
