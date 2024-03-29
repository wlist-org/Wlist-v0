package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Options.OrderPolicies;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

public record VisibleUserInformation(long id, @NotNull String username, long groupId, @NotNull String groupName,
                                     @NotNull ZonedDateTime createTime, @NotNull ZonedDateTime updateTime) implements Serializable {
    /**
     * @see com.xuxiaocheng.WList.Server.Databases.User.UserInformation
     */
     public static @NotNull VisibleUserInformation parse(final @NotNull ByteBuf buffer) throws IOException {
         final long id = ByteBufIOUtil.readVariableLenLong(buffer);
         final String username = ByteBufIOUtil.readUTF(buffer);
         final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
         final String groupName = ByteBufIOUtil.readUTF(buffer);
         final ZonedDateTime createTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
         final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
         return new VisibleUserInformation(id, username, groupId, groupName, createTime, updateTime);
    }

    public enum Order implements OrderPolicies.OrderPolicy {
        Id, Name, CreateTime, UpdateTime, GroupId, GroupName,
        ;
        public static @Nullable Order of(final @NotNull String policy) {
            try {
                return Order.valueOf(policy);
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        }
    }

    private static final @NotNull @Unmodifiable LinkedHashMap<VisibleUserInformation.@NotNull Order, @NotNull OrderDirection> ListEmptyOrder = new LinkedHashMap<>(0);
    public static @NotNull @Unmodifiable LinkedHashMap<VisibleUserInformation.@NotNull Order, @NotNull OrderDirection> emptyOrder() {
        return VisibleUserInformation.ListEmptyOrder;
    }
}
