package com.xuxiaocheng.WList.Commons.Options;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OrderPolicies {
    private OrderPolicies() {
        super();
    }

    @SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
    public interface OrderPolicy {
        @NotNull String name();
    }

    private static final @NotNull Map<@NotNull Class<?>, @NotNull Map<@NotNull String, ?>> typesCache = new LinkedHashMap<>();
    private static <T extends Enum<T> & OrderPolicy> @NotNull Map<@NotNull String, @NotNull T> computeTypeCache(final @NotNull Class<? extends T> type) {
        return Arrays.stream(type.getEnumConstants()).collect(Collectors.toMap(Enum::name, Function.identity()));
    }

    public static <T extends Enum<T> & OrderPolicy> @Nullable UnionPair<LinkedHashMap<@NotNull T, @NotNull OrderDirection>, String> parse(final @NotNull ByteBuf buffer, final @NotNull Class<T> type, final int maxCount_) throws IOException {
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        @SuppressWarnings("unchecked")
        final Map<String, T> enums = (Map<String, T>) OrderPolicies.typesCache.computeIfAbsent(type, t -> OrderPolicies.computeTypeCache((Class<T>) t));
        if (length < 0 || (maxCount_ < 0 ? enums.size() : Math.min(maxCount_, enums.size())) < length)
            return null;
        final LinkedHashMap<T, OrderDirection> orders = new LinkedHashMap<>(length);
        for (int i = 0; i < length; ++i) {
            final String name = ByteBufIOUtil.readUTF(buffer);
            final T policy = enums.get(name);
            if (policy == null)
                return UnionPair.fail(name);
            final boolean direction = ByteBufIOUtil.readBoolean(buffer);
            orders.putIfAbsent(policy, direction ? OrderDirection.ASCEND : OrderDirection.DESCEND);
        }
        return UnionPair.ok(orders);
    }

    public static <T extends Enum<T> & OrderPolicy> void dump(final @NotNull ByteBuf buffer, @SuppressWarnings("TypeMayBeWeakened") final @NotNull LinkedHashMap<@NotNull T, @NotNull OrderDirection> orders) throws IOException {
        ByteBufIOUtil.writeVariableLenInt(buffer, orders.size());
        for (final Map.Entry<T, OrderDirection> policy: orders.entrySet()) {
            ByteBufIOUtil.writeUTF(buffer, policy.getKey().name());
            ByteBufIOUtil.writeBoolean(buffer, switch (policy.getValue()) {
                case ASCEND -> true;
                case DESCEND -> false;
            });
        }
    }
}
