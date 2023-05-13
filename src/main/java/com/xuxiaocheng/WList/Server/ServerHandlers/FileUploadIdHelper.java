package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

final class FileUploadIdHelper {
    private FileUploadIdHelper() {
        super();
    }

    private static final @NotNull Map<@NotNull String, @NotNull UploaderData> buffers = new ConcurrentHashMap<>();

    public static @NotNull String generateId(final long size, final @NotNull String tag, final @NotNull List<@NotNull String> tags) {
        final UploaderData data = new UploaderData(size, tag, tags);
        String id;
        while (true) {
            //noinspection SpellCheckingInspection
            id = HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890~!@#$%^&*,.?|");
            final boolean[] flag = {false};
            FileUploadIdHelper.buffers.computeIfAbsent(id, (i) -> {
                flag[0] = true;
                return data;
            });
            if (flag[0])
                break;
        }
        data.id = id;
        return id;
    }

    private static class UploaderData {
        private @NotNull String id = "";
        private final @NotNull BlockingQueue<Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull ByteBuf>> bufferQueue = new LinkedBlockingQueue<>();
        private @NotNull ByteBuf partBuffer;
        private final long size;
        private final @NotNull String tag;
        private final @NotNull List<@NotNull String> tags;

        private UploaderData(final long size, final @NotNull String tag, final @NotNull List<@NotNull String> tags) {
            super();
            this.size = size;
            this.tag = tag;
            this.tags = Collections.unmodifiableList(tags);
        }

        //TODO
    }
}
