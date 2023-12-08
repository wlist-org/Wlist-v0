package com.xuxiaocheng.WList.Commons.Beans;

import com.xuxiaocheng.WList.Client.ClientConfiguration;
import com.xuxiaocheng.WList.Commons.Options.OrderDirection;
import com.xuxiaocheng.WList.Commons.Options.OrderPolicies;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record VisibleFileInformation(long id, long parentId, @NotNull String name, boolean isDirectory, long size,
                                     @Nullable ZonedDateTime createTime, @Nullable ZonedDateTime updateTime) implements Serializable {
    /**
     * @see com.xuxiaocheng.WList.Server.Databases.File.FileInformation
     */
    public static @NotNull VisibleFileInformation parse(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final long parentId = ByteBufIOUtil.readVariableLenLong(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final ZonedDateTime createTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        final ZonedDateTime updateTime = ByteBufIOUtil.readNullableDataTime(buffer,DateTimeFormatter.ISO_DATE_TIME);
        return new VisibleFileInformation(id, parentId, name, isDirectory, size, createTime, updateTime);
    }

    public enum Order implements OrderPolicies.OrderPolicy {
        Id, Name, Directory, Size, CreateTime, UpdateTime,
        ;
        public static @Nullable Order of(final @NotNull String policy) {
            try {
                return Order.valueOf(policy);
            } catch (final IllegalArgumentException exception) {
                return null;
            }
        }
    }

    private static final @NotNull @Unmodifiable LinkedHashMap<@NotNull Order, @NotNull OrderDirection> ListEmptyOrder = new LinkedHashMap<>(0);
    public static @NotNull @Unmodifiable LinkedHashMap<@NotNull Order, @NotNull OrderDirection> emptyOrder() {
        return VisibleFileInformation.ListEmptyOrder;
    }

    public static @NotNull Comparator<VisibleFileInformation> buildComparator() {
        final LinkedHashMap<VisibleFileInformation.Order, OrderDirection> orders = ClientConfiguration.get().fileOrders();
        Comparator<VisibleFileInformation> comparators = null;
        for (final Map.Entry<VisibleFileInformation.Order, OrderDirection> order: orders.entrySet()) {
            final Comparator<VisibleFileInformation> select = switch (order.getKey()) {
                case Id -> Comparator.comparing(VisibleFileInformation::id);
                case Name -> Comparator.comparing(VisibleFileInformation::name);
                case Directory -> Comparator.comparing(VisibleFileInformation::isDirectory);
                case Size -> Comparator.comparing(VisibleFileInformation::size, Long::compareUnsigned);
                case CreateTime -> Comparator.comparing(VisibleFileInformation::createTime, Comparator.nullsFirst(ZonedDateTime::compareTo));
                case UpdateTime -> Comparator.comparing(VisibleFileInformation::updateTime, Comparator.nullsFirst(ZonedDateTime::compareTo));
            };
            final Comparator<VisibleFileInformation> real = (switch (order.getValue()) {
                case ASCEND -> select;
                case DESCEND -> select.reversed();
            });
            if (comparators == null)
                comparators = real;
            else
                comparators = comparators.thenComparing(real);
        }
        return Objects.requireNonNullElse(comparators, Comparator.comparing(VisibleFileInformation::name));
    }
}
