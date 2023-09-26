package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record VisibleFilesListInformation(long total, long filtered, @NotNull @Unmodifiable List<@NotNull VisibleFileInformation> informationList) {
    /**
     * @see com.xuxiaocheng.WList.Server.Storage.Records.FilesListInformation
     */
    public static @NotNull VisibleFilesListInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long total = ByteBufIOUtil.readVariableLenLong(buffer);
        final long filtered = ByteBufIOUtil.readVariableLenLong(buffer);
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        final List<VisibleFileInformation> list = new ArrayList<>(length);
        for (int i = 0; i < length; ++i)
            list.add(VisibleFileInformation.parse(buffer));
        return new VisibleFilesListInformation(total, filtered, list);
    }
}
