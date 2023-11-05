package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Operations.FailureKind;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;

public record VisibleFailureReason(@NotNull FailureKind kind, @NotNull FileLocation location, @NotNull String message) implements Serializable {
    /**
     * @see com.xuxiaocheng.WList.Server.Storage.Records.FailureReason
     */
    public static @NotNull VisibleFailureReason parse(final @NotNull ByteBuf buffer) throws IOException {
        final FailureKind kind = FailureKind.of(ByteBufIOUtil.readUTF(buffer));
        final FileLocation location = FileLocation.parse(buffer);
        final String message = ByteBufIOUtil.readUTF(buffer);
        return new VisibleFailureReason(kind, location, message);
    }
}
