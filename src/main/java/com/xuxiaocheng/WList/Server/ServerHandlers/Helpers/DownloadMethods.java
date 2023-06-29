package com.xuxiaocheng.WList.Server.ServerHandlers.Helpers;

import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record DownloadMethods(long total, @NotNull List<@NotNull SupplierE<@NotNull ByteBuf>> methods,
                              @NotNull Runnable finisher) {
}
