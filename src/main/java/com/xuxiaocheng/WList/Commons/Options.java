package com.xuxiaocheng.WList.Commons;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class Options {
    private Options() {
        super();
    }

    public enum OrderDirection {
        ASCEND, DESCEND,
    }

    @FunctionalInterface
    public interface OrderPolicy {
        @NotNull String name();
    }

    public static <T extends OrderPolicy> @Nullable UnionPair<List<Pair.@NotNull ImmutablePair<@NotNull T, @NotNull OrderDirection>>, String> parseOrderPolicies(final @NotNull ByteBuf buffer, final @NotNull Function<? super @NotNull String, ? extends @Nullable T> parser, final int maxCount) throws IOException {
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        if (length <= 0 || maxCount < length)
            return null;
        final List<Pair.ImmutablePair<T, OrderDirection>> policies = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            final String name = ByteBufIOUtil.readUTF(buffer);
            final T policy = parser.apply(name);
            if (policy == null)
                return UnionPair.fail(name);
            final boolean direction = ByteBufIOUtil.readBoolean(buffer);
            policies.add(Pair.ImmutablePair.makeImmutablePair(policy, direction ? OrderDirection.ASCEND : OrderDirection.DESCEND));
        }
        return UnionPair.ok(policies);
    }

    public static <T extends OrderPolicy> void dumpOrderPolicies(final @NotNull ByteBuf buffer, final @NotNull Collection<? extends Pair.@NotNull ImmutablePair<@NotNull T, @NotNull OrderDirection>> policies, final @NotNull Function<? super @NotNull T, @NotNull String> dumper) throws IOException {
        ByteBufIOUtil.writeVariableLenInt(buffer, policies.size());
        for (final Pair.ImmutablePair<T, OrderDirection> policy: policies) {
            ByteBufIOUtil.writeUTF(buffer, dumper.apply(policy.getFirst()));
            ByteBufIOUtil.writeBoolean(buffer, policy.getSecond() == OrderDirection.ASCEND);
        }
    }

//    public enum OrderPolicy {
//        FileName,
//        Size,
//        CreateTime,
//        UpdateTime,
//    }

    public enum DirectoriesOrFiles {
        OnlyDirectories,
        OnlyFiles,
        Both,
    }

    public static @Nullable OrderDirection valueOfOrderDirection(final @NotNull String direction) {
        try {
            return OrderDirection.valueOf(direction);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

//    public static @Nullable OrderPolicy valueOfOrderPolicy(final @NotNull String policy) {
//        try {
//            return OrderPolicy.valueOf(policy);
//        } catch (final IllegalArgumentException exception) {
//            return null;
//        }
//    }

    public static @Nullable DirectoriesOrFiles valueOfDirectoriesOrFiles(final byte policy) {
        return switch (policy) {
            case 1 -> DirectoriesOrFiles.OnlyDirectories;
            case 2 -> DirectoriesOrFiles.OnlyFiles;
            case 3 -> DirectoriesOrFiles.Both;
            default -> null;
        };
    }

    public enum DuplicatePolicy {
        ERROR,
        OVER,
        KEEP,
    }

    public static @Nullable DuplicatePolicy valueOfDuplicatePolicy(final @NotNull String policy) {
        try {
            return DuplicatePolicy.valueOf(policy);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }
}
