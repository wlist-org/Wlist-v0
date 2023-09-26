package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

public record VisibleUserInformation(long id, @NotNull String username, long groupId, @NotNull String groupName,
                                     @NotNull ZonedDateTime createTime, @NotNull ZonedDateTime updateTime) {
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

    public enum Order implements Options.OrderPolicy {
        Id, Name, CreateTime, UpdateTime, GroupId, GroupName,
    }

    private static final @NotNull @Unmodifiable LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> ListEmptyOrder = new LinkedHashMap<>(0);
    public static @NotNull @Unmodifiable LinkedHashMap<VisibleUserInformation.@NotNull Order, Options.@NotNull OrderDirection> emptyOrder() {
        return VisibleUserInformation.ListEmptyOrder;
    }
}
