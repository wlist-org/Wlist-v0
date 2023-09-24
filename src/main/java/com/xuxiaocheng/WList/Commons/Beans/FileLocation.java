package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record FileLocation(@NotNull String storage, long id) {
    public static @NotNull FileLocation parse(final @NotNull ByteBuf buffer) throws IOException {
        final String storage = ByteBufIOUtil.readUTF(buffer);
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        return new FileLocation(storage, id);
    }

    @Contract("_ -> param1")
    public @NotNull ByteBuf dump(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, this.storage);
        ByteBufIOUtil.writeVariableLenLong(buffer, this.id);
        return buffer;
    }
}
