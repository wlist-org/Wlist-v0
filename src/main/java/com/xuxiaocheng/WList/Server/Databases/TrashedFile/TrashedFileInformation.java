package com.xuxiaocheng.WList.Server.Databases.TrashedFile;

import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record TrashedFileInformation(@NotNull FileLocation location, @NotNull String name, boolean isDirectory, long size,
                                     @Nullable LocalDateTime createTime, @Nullable LocalDateTime trashedTime, @Nullable LocalDateTime expireTime,
                                     @NotNull String md5, @Nullable String others) {
    public long id() {
        return this.location.id();
    }

    public static @NotNull TrashedFileInformation fromFileInformation(final @NotNull FileInformation information, final @Nullable LocalDateTime trashedTime, final @Nullable LocalDateTime expireTime) {
        return new TrashedFileInformation(information.location(), information.name(), information.isDirectory(), information.size(), information.createTime(), trashedTime, expireTime, information.md5(), information.others());
    }

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleTrashedFileInformation
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.location.id());
        ByteBufIOUtil.writeUTF(buffer, this.name);
        ByteBufIOUtil.writeBoolean(buffer, this.isDirectory);
        ByteBufIOUtil.writeVariable2LenLong(buffer, this.size);
        ByteBufIOUtil.writeNullableDataTime(buffer, this.createTime, DateTimeFormatter.ISO_DATE_TIME);
        ByteBufIOUtil.writeNullableDataTime(buffer, this.trashedTime, DateTimeFormatter.ISO_DATE_TIME);
        ByteBufIOUtil.writeNullableDataTime(buffer, this.expireTime, DateTimeFormatter.ISO_DATE_TIME);
        ByteBufIOUtil.writeUTF(buffer, this.md5);
        return buffer;
    }
}
