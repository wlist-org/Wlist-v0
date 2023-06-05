package com.xuxiaocheng.WListClient.Server;

import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record VisibleUserInformation(long id, @NotNull String username, @NotNull String group) {
     public static @NotNull VisibleUserInformation parse(final @NotNull ByteBuf buffer) throws IOException {
         final long id = ByteBufIOUtil.readVariableLenLong(buffer);
         final String username = ByteBufIOUtil.readUTF(buffer);
         final String group = ByteBufIOUtil.readUTF(buffer);
         return new VisibleUserInformation(id, username, group);
    }
}
