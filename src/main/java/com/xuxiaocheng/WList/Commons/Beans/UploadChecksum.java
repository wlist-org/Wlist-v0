package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record UploadChecksum(long start, long end, @NotNull String algorithm) {
    public static @NotNull UploadChecksum parse(final @NotNull ByteBuf buffer) throws IOException {
        final long start = ByteBufIOUtil.readVariableLenLong(buffer);
        final long end = ByteBufIOUtil.readVariableLenLong(buffer);
        final String algorithm = ByteBufIOUtil.readUTF(buffer);
        return new UploadChecksum(start, end, algorithm);
    }

    @Contract("_ -> param1")
    public @NotNull ByteBuf dump(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.start);
        ByteBufIOUtil.writeVariableLenLong(buffer, this.end);
        ByteBufIOUtil.writeUTF(buffer, this.algorithm);
        return buffer;
    }
}
