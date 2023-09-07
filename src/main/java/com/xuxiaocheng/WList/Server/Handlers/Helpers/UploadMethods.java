package com.xuxiaocheng.WList.Server.Handlers.Helpers;

import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The server will first call back the methods in {@code methods} (may not in order)
 *   (Ensure that all lengths of {@code ByteBuf} are {@link com.xuxiaocheng.Rust.NetworkTransmission#FileTransferBufferSize}, except for the last one less than or equal to),
 * and then call {@code supplier} to complete the upload task after all are completed.
 * Finally, whether the upload is cancelled or completed, {@code finisher} will be called.
 */
public record UploadMethods(@NotNull List<@NotNull ConsumerE<@NotNull ByteBuf>> methods,
                            @NotNull SupplierE<@Nullable FileSqlInformation> supplier,
                            @NotNull Runnable finisher) {
}
