package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record RefreshConfirm(@NotNull String id) {
    /**
     * @see com.xuxiaocheng.WList.Server.Storage.Records.RefreshRequirements
     */
    public static @NotNull RefreshConfirm parse(final @NotNull ByteBuf buffer) throws IOException {
        final String id = ByteBufIOUtil.readUTF(buffer);
        return new RefreshConfirm(id);
    }
}
