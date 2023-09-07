package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @param driver Driver name. For database.
 * @param id File id. Primary key.
 */
public record FileLocation(@NotNull String driver, long id) {
    public static void dump(final @NotNull ByteBuf buffer, final @NotNull FileLocation location) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, location.driver);
        ByteBufIOUtil.writeVariableLenLong(buffer, location.id);
    }

    public static @NotNull FileLocation parse(final @NotNull ByteBuf buffer) throws IOException {
        final String driver = ByteBufIOUtil.readUTF(buffer);
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        return new FileLocation(driver, id);
    }
}
