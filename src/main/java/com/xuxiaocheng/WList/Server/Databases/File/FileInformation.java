package com.xuxiaocheng.WList.Server.Databases.File;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record FileInformation(long id, long parentId, @NotNull String name, boolean isDirectory, long size,
                              @Nullable ZonedDateTime createTime, @Nullable ZonedDateTime updateTime, @Nullable String others) {
    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.id);
        ByteBufIOUtil.writeVariableLenLong(buffer, this.parentId);
        ByteBufIOUtil.writeUTF(buffer, this.name);
        ByteBufIOUtil.writeBoolean(buffer, this.isDirectory);
        ByteBufIOUtil.writeVariable2LenLong(buffer, this.size);
        ByteBufIOUtil.writeNullableDataTime(buffer, this.createTime, DateTimeFormatter.ISO_DATE_TIME);
        ByteBufIOUtil.writeNullableDataTime(buffer, this.updateTime, DateTimeFormatter.ISO_DATE_TIME);
        return buffer;
    }
}
