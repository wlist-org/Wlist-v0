package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record VisibleUserInformation(long id, @NotNull String username, long groupId, @NotNull String groupName,
                                     @NotNull LocalDateTime createTime, @NotNull LocalDateTime updateTime) {
     public static @NotNull VisibleUserInformation parse(final @NotNull ByteBuf buffer) throws IOException {
         final long id = ByteBufIOUtil.readVariableLenLong(buffer);
         final String username = ByteBufIOUtil.readUTF(buffer);
         final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
         final String groupName = ByteBufIOUtil.readUTF(buffer);
         final LocalDateTime createTime = LocalDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
         final LocalDateTime updateTime = LocalDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
         return new VisibleUserInformation(id, username, groupId, groupName, createTime, updateTime);
    }

    public enum Order implements Options.OrderPolicy {
        Id, Name, CreateTime, UpdateTime, GroupId, GroupName,
    }

    public static @Nullable Order orderBy(final @NotNull String orderBy) {
        try {
            return Order.valueOf(orderBy);
        } catch (final IllegalArgumentException ignore) {
            return null;
        }
    }
}
