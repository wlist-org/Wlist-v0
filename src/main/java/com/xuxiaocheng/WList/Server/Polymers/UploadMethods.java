package com.xuxiaocheng.WList.Server.Polymers;

import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The server will first call back the methods in {@code methods} in order based on their size,
 * and then call {@code supplier} to complete the upload task after all are completed.
 * Finally, whether the upload is cancelled or completed, {@code finisher} will be called.
 */
public record UploadMethods(@NotNull List<UploadPartMethod> methods,
                            @NotNull SupplierE<@Nullable FileSqlInformation> supplier,
                            @NotNull Runnable finisher) {
    public record UploadPartMethod(int size, @NotNull ConsumerE<@NotNull ByteBuf> consumer) {
    }
}
