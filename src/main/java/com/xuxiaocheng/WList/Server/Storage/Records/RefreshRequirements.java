package com.xuxiaocheng.WList.Server.Storage.Records;

import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.RunnableE;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

public record RefreshRequirements(@NotNull ConsumerE<? super @NotNull Consumer<? super @Nullable Throwable>> runner,
                                  @NotNull Runnable canceller) {
    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.RefreshConfirm
     */
    @Contract("_, _ -> param1")
    public @NotNull ByteBuf dumpConfirm(final @NotNull ByteBuf buffer, final @NotNull String id) throws IOException {
        ByteBufIOUtil.writeUTF(buffer, id);
        return buffer;
    }

    public static final @NotNull RefreshRequirements NoRequired = new RefreshRequirements(c -> c.accept(null), RunnableE.EmptyRunnable);
}
