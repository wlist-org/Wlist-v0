package com.xuxiaocheng.WList.Server.Operations.Helpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressBar {
    protected final @NotNull List<Pair.@NotNull ImmutablePair<@NotNull AtomicLong, @NotNull Long>> stages = new ArrayList<>();

    public int addStage(final long total) {
        assert total >= 0;
        synchronized (this.stages) {
            this.stages.add(Pair.ImmutablePair.makeImmutablePair(new AtomicLong(0), total));
            return this.stages.size() - 1;
        }
    }

    public void progress(final int index, final long delta) {
        this.stages.get(index).getFirst().addAndGet(delta);
    }

    public long getTotal(final int index) {
        return this.stages.get(index).getSecond().longValue();
    }

    public int getStages() {
        return this.stages.size();
    }

    @Contract("_ -> param1")
    public @NotNull ByteBuf dump(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenInt(buffer, this.stages.size());
        for (final Pair.ImmutablePair<AtomicLong, Long> stage: this.stages) {
            ByteBufIOUtil.writeVariableLenLong(buffer, stage.getFirst().longValue());
            ByteBufIOUtil.writeVariableLenLong(buffer, stage.getSecond().longValue());
        }
        return buffer;
    }

    @Override
    public @NotNull String toString() {
        return "ProgressBar{" +
                "stages=" + this.stages +
                '}';
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(long delta);
    }
}
