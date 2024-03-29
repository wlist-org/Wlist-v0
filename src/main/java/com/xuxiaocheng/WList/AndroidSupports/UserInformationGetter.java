package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;

/**
 * @see VisibleUserInformation
 */
public final class UserInformationGetter {
    private UserInformationGetter() {
        super();
    }

    public static long id(final @NotNull VisibleUserInformation information) {
        return information.id();
    }

    public static @NotNull String username(final @NotNull VisibleUserInformation information) {
        return information.username();
    }

    public static long groupId(final @NotNull VisibleUserInformation information) {
        return information.groupId();
    }

    public static @NotNull String groupName(final @NotNull VisibleUserInformation information) {
        return information.groupName();
    }

    public static @NotNull ZonedDateTime createTime(final @NotNull VisibleUserInformation information) {
        return information.createTime();
    }

    public static @NotNull ZonedDateTime updateTime(final @NotNull VisibleUserInformation information) {
        return information.updateTime();
    }

    public static boolean equals(final @NotNull VisibleUserInformation a, final @NotNull VisibleUserInformation b) {
        return a.equals(b);
    }

    public static int hashCode(final @NotNull VisibleUserInformation information) {
        return information.hashCode();
    }

    public static @NotNull String toString(final @NotNull VisibleUserInformation information) {
        return information.toString();
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    public enum Order {
        Id(VisibleUserInformation.Order.Id),
        Name(VisibleUserInformation.Order.Name),
        CreateTime(VisibleUserInformation.Order.CreateTime),
        UpdateTime(VisibleUserInformation.Order.UpdateTime),
        GroupId(VisibleUserInformation.Order.GroupId),
        GroupName(VisibleUserInformation.Order.GroupName),
        ;
        private final VisibleUserInformation.Order order;
        Order(final VisibleUserInformation.Order order) {
            this.order = order;
        }
        public VisibleUserInformation.Order order() {
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
}
