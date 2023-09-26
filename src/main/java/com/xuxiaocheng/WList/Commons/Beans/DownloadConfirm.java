package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record DownloadConfirm(boolean acceptedRange, long downloadingSize, @NotNull String id) {
    /**
     * @see com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements
     */
    public static @NotNull DownloadConfirm parse(final @NotNull ByteBuf buffer) throws IOException {
        final boolean acceptedRange = ByteBufIOUtil.readBoolean(buffer);
        final long downloadingSize = ByteBufIOUtil.readVariable2LenLong(buffer);
        final String id = ByteBufIOUtil.readUTF(buffer);
        return new DownloadConfirm(acceptedRange, downloadingSize, id);
    }

    public record DownloadInformation(@NotNull List<Pair.@NotNull ImmutablePair<@NotNull Long, @NotNull Long>> parallel, @Nullable ZonedDateTime expire) {
        /**
         * @see com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements.DownloadMethods
         */
        public static @NotNull DownloadInformation parse(final @NotNull ByteBuf buffer) throws IOException {
            final int size = ByteBufIOUtil.readVariableLenInt(buffer);
            final List<Pair.ImmutablePair<Long, Long>> parallel = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                final long start = ByteBufIOUtil.readVariable2LenLong(buffer);
                final long end = ByteBufIOUtil.readVariable2LenLong(buffer);
                parallel.add(Pair.ImmutablePair.makeImmutablePair(start, end));
            }
            final ZonedDateTime expire = ByteBufIOUtil.readNullableDataTime(buffer, DateTimeFormatter.ISO_DATE_TIME);
            return new DownloadInformation(parallel, expire);
        }
    }
}
