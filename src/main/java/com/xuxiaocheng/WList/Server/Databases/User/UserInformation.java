package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @param updateTime last name/password/group change time.
 * @param modifyTime last password/group change time. (for check token.)
 */
public record UserInformation(long id, @NotNull String username, @NotNull String encryptedPassword, @NotNull UserGroupInformation group,
                              @NotNull LocalDateTime createTime, @NotNull LocalDateTime updateTime, @NotNull LocalDateTime modifyTime) {
    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.id);
        ByteBufIOUtil.writeUTF(buffer, this.username);
        ByteBufIOUtil.writeVariableLenLong(buffer, this.group.id());
        ByteBufIOUtil.writeUTF(buffer, this.group.name());
        ByteBufIOUtil.writeUTF(buffer, this.createTime.format(DateTimeFormatter.ISO_DATE_TIME));
        ByteBufIOUtil.writeUTF(buffer, this.updateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        return buffer;
    }
}
