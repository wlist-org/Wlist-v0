package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

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

    public static @Nullable ZonedDateTime updateTime(final @NotNull VisibleFileInformation information) {
        return information.updateTime();
    }

    public static boolean equals(final @NotNull VisibleFileInformation a, final @NotNull VisibleFileInformation b) {
        return a.equals(b);
    }

    public static int hashCode(final @NotNull VisibleFileInformation information) {
        return information.hashCode();
    }

    public static @NotNull String toString(final @NotNull VisibleFileInformation information) {
        return information.toString();
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

        public static @Nullable Order of(final @NotNull String policy) {
            try {
                return Order.valueOf(policy);
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        }
    }

    public static @NotNull Comparator<VisibleFileInformation> buildComparator() {
        return VisibleFileInformation.buildComparator();
    }

    public static final @NotNull VisibleFileInformation[] EmptyInformationArray = new VisibleFileInformation[0];
    public static @NotNull VisibleFileInformation @NotNull [] asArray(final @NotNull Collection<@NotNull VisibleFileInformation> list) {
        return list.toArray(FileInformationGetter.EmptyInformationArray);
    }

    public static int binarySearch(final @NotNull VisibleFileInformation @NotNull [] list, final @NotNull VisibleFileInformation target, final @NotNull Comparator<? super VisibleFileInformation> comparator) {
        return Arrays.binarySearch(list, target, comparator);
    }
}
