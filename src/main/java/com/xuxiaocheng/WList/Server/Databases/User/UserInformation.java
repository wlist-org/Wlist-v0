package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;

public record UserInformation(long id, @NotNull String username, @NotNull String password, @NotNull UserGroupInformation group, @NotNull LocalDateTime modifyTime) {
    public record Inserter(@NotNull String username, @NotNull String password, long groupId) {
    }

    public record Updater(long id, @NotNull String username, @NotNull String encryptedPassword, long groupId, @Nullable LocalDateTime modifyTime) {
    }

    /**
     * @see com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation
     */
    @Contract("_ -> param1")
    public @NotNull ByteBuf dumpVisible(final @NotNull ByteBuf buffer) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, this.id);
        ByteBufIOUtil.writeUTF(buffer, this.username);
        ByteBufIOUtil.writeUTF(buffer, this.group.name());
        return buffer;
    }
}
