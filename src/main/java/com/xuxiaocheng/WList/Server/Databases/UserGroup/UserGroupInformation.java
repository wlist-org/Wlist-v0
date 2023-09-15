package com.xuxiaocheng.WList.Server.Databases.UserGroup;

import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public record UserGroupInformation(long id, @NotNull String name, @NotNull Set<@NotNull UserPermission> permissions,
                                   @NotNull ZonedDateTime createTime, @NotNull ZonedDateTime updateTime) {
    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.id);
        ByteBufIOUtil.writeUTF(buffer, this.name);
        ByteBufIOUtil.writeUTF(buffer, UserPermission.dump(this.permissions));
        ByteBufIOUtil.writeUTF(buffer, this.createTime.format(DateTimeFormatter.ISO_DATE_TIME));
        ByteBufIOUtil.writeUTF(buffer, this.updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        return buffer;
    }
}
