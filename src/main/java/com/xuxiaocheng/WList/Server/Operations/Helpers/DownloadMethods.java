package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public record DownloadMethods(long total, @NotNull List<@NotNull SupplierE<@NotNull ByteBuf>> methods,
                              @NotNull Runnable finisher, @Nullable LocalDateTime expireTime) {
}
