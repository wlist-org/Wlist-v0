package com.xuxiaocheng.WList.Server.Databases.User;

import com.xuxiaocheng.WList.Server.Databases.UserGroup.UserGroupInformation;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;

public record UserInformation(long id, @NotNull String username, @NotNull String password, @NotNull UserGroupInformation group, @NotNull LocalDateTime modifyTime) {
    public record Inserter(@NotNull String username, @NotNull String password, long groupId) {
    }

    public record Updater(long id, @NotNull String username, @NotNull String encryptedPassword, long groupId, @Nullable LocalDateTime modifyTime) {
    }

    @Deprecated // only for client
    public record VisibleUserInformation(long id, @NotNull String username, @NotNull String group) {
    }

    public static void dumpVisible(final @NotNull ByteBuf buffer, final @NotNull UserInformation information) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, information.id());
        ByteBufIOUtil.writeUTF(buffer, information.username());
        ByteBufIOUtil.writeUTF(buffer, information.group.name());
    }

    @Deprecated // only for client
    public static @NotNull VisibleUserInformation parseVisible(final @NotNull ByteBuf buffer) throws IOException {
        final long id = ByteBufIOUtil.readVariableLenLong(buffer);
        final String username = ByteBufIOUtil.readUTF(buffer);
        final String group = ByteBufIOUtil.readUTF(buffer);
        return new VisibleUserInformation(id, username, group);
    }
}
