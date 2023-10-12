package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
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

    public static @Nullable ZonedDateTime createTime(final @NotNull VisibleFileInformation information) {
        return information.createTime();
    }

    public static @NotNull String createTimeString(final @NotNull VisibleFileInformation information, final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return information.createTimeString(formatter, unknown);
    }

    public static @Nullable ZonedDateTime updateTime(final @NotNull VisibleFileInformation information) {
        return information.updateTime();
    }

    public static @NotNull String updateTimeString(final @NotNull VisibleFileInformation information, final @NotNull DateTimeFormatter formatter, final @Nullable String unknown) {
        return information.updateTimeString(formatter, unknown);
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    public enum Order {
        Id(VisibleFileInformation.Order.Id),
        Name(VisibleFileInformation.Order.Name),
        Directory(VisibleFileInformation.Order.Directory),
        Size(VisibleFileInformation.Order.Size),
        CreateTime(VisibleFileInformation.Order.CreateTime),
        UpdateTime(VisibleFileInformation.Order.UpdateTime),
        ;
        private final VisibleFileInformation.Order order;
        Order(final VisibleFileInformation.Order order) {
            this.order = order;
        }
        public VisibleFileInformation.Order order() {
            return this.order;
        }
    }
}
