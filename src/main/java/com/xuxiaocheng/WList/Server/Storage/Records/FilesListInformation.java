package com.xuxiaocheng.WList.Server.Storage.Records;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.List;

public record FilesListInformation(long total, long filtered, @NotNull @Unmodifiable List<@NotNull FileInformation> informationList) {
    public static final @NotNull FilesListInformation Empty = new FilesListInformation(0, 0, List.of());

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation
     */
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.total);
        ByteBufIOUtil.writeVariableLenLong(buffer, this.filtered);
        ByteBufIOUtil.writeVariableLenInt(buffer, this.informationList.size());
        for (final FileInformation information: this.informationList)
            information.dumpVisible(buffer);
        return buffer;
    }
}
