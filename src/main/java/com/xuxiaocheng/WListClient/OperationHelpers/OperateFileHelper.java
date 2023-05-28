package com.xuxiaocheng.WListClient.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.OptionalNullable;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WListClient.Server.DrivePath;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WListClient.WListClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OperateFileHelper {
    private OperateFileHelper() {
        super();
    }

    private static @Nullable VisibleFileInformation receiveFileInformation(final @NotNull ByteBuf receive) throws IOException, WrongStateException {
        if (OperateHelper.handleState(receive))
            return VisibleFileInformation.parse(receive);
        final String reason = ByteBufIOUtil.readUTF(receive);
        if ("Parameters".equals(reason))
            throw new IllegalArgumentException();
        assert "File".equals(reason);
        return null;
    }

    public static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleFileInformation>> listFiles(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath path, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ListFiles, token);
        ByteBufIOUtil.writeUTF(send, path.getPath());
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        ByteBufIOUtil.writeVariableLenInt(send, page);
        ByteBufIOUtil.writeUTF(send, policy.name());
        ByteBufIOUtil.writeUTF(send, direction.name());
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive)) {
                final long total = ByteBufIOUtil.readVariableLenLong(receive);
                final int count = ByteBufIOUtil.readVariableLenInt(receive);
                final List<VisibleFileInformation> list = new ArrayList<>(count);
                for (int i = 0; i < count; ++i)
                    list.add(VisibleFileInformation.parse(receive));
                return Pair.ImmutablePair.makeImmutablePair(total, Collections.unmodifiableList(list));
            }
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("Parameters".equals(reason))
                throw new IllegalArgumentException();
            assert "File".equals(reason);
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable VisibleFileInformation makeDirectories(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath path, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.MakeDirectories, token);
        ByteBufIOUtil.writeUTF(send, path.getPath());
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            return OperateFileHelper.receiveFileInformation(receive);
        } finally {
            receive.release();
        }
    }

    public static boolean deleteFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath path) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DeleteFile, token);
        ByteBufIOUtil.writeUTF(send, path.getPath());
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @Nullable VisibleFileInformation renameFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath path, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RenameFile, token);
        ByteBufIOUtil.writeUTF(send, path.getPath());
        ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            return OperateFileHelper.receiveFileInformation(receive);
        } finally {
            receive.release();
        }
    }

    public static Triad.@Nullable ImmutableTriad<@NotNull Long, @NotNull Integer, @NotNull String> requestDownloadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath path, final long from, final long to) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RequestDownloadFile, token);
        ByteBufIOUtil.writeUTF(send, path.getPath());
        ByteBufIOUtil.writeVariableLenLong(send, from);
        ByteBufIOUtil.writeVariableLenLong(send, to);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return Triad.ImmutableTriad.makeImmutableTriad(ByteBufIOUtil.readVariableLenLong(receive),
                        ByteBufIOUtil.readVariableLenInt(receive), ByteBufIOUtil.readUTF(receive));
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("Parameters".equals(reason))
                throw new IllegalArgumentException();
            assert "File".equals(reason);
            return null;
        } finally {
            receive.release();
        }
    }

    public static Pair.@Nullable ImmutablePair<@NotNull Integer, @NotNull ByteBuf> downloadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return Pair.ImmutablePair.makeImmutablePair(ByteBufIOUtil.readVariableLenInt(receive), receive.retain());
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean cancelDownloadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CancelDownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    // Null: failure, Optional.null: reused, Optional.of: upload
    public static @Nullable OptionalNullable<@Nullable String> requestUploadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath path, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RequestUploadFile, token);
        ByteBufIOUtil.writeUTF(send, path.getPath());
        ByteBufIOUtil.writeVariableLenLong(send, size);
        ByteBufIOUtil.writeUTF(send, md5);
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return OptionalNullable.ofNullable(ByteBufIOUtil.readBoolean(receive) ? null : ByteBufIOUtil.readUTF(receive));
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("Parameters".equals(reason))
                throw new IllegalArgumentException();
            assert "File".equals(reason);
            return null;
        } finally {
            receive.release();
        }
    }

    // Null: no id, Optional.null: continue, Optional.of: complete
    public static @Nullable OptionalNullable<@Nullable VisibleFileInformation> uploadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull String id, final int chunk, final @NotNull ByteBuf buffer) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf prefix = OperateHelper.operateWithToken(Operation.Type.UploadFile, token);
        ByteBufIOUtil.writeUTF(prefix, id);
        ByteBufIOUtil.writeVariableLenInt(prefix, chunk);
        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, buffer);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return OptionalNullable.ofNullable(ByteBufIOUtil.readBoolean(receive) ? null : VisibleFileInformation.parse(receive));
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean cancelUploadFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CancelUploadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    private static @Nullable VisibleFileInformation sendSourceTargetFileInformation(final @NotNull WListClient client, final @NotNull DrivePath source, final @NotNull DrivePath target, final Options.@NotNull DuplicatePolicy policy, final ByteBuf send) throws IOException, InterruptedException, WrongStateException {
        ByteBufIOUtil.writeUTF(send, source.getPath());
        ByteBufIOUtil.writeUTF(send, target.getPath());
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            return OperateFileHelper.receiveFileInformation(receive);
        } finally {
            receive.release();
        }
    }

    public static @Nullable VisibleFileInformation copyFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath source, final @NotNull DrivePath target, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CopyFile, token);
        return OperateFileHelper.sendSourceTargetFileInformation(client, source, target, policy, send);
    }

    public static @Nullable VisibleFileInformation moveFile(final @NotNull WListClient client, final @NotNull String token, final @NotNull DrivePath sourceFile, final @NotNull DrivePath targetDirectory, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.MoveFile, token);
        return OperateFileHelper.sendSourceTargetFileInformation(client, sourceFile, targetDirectory, policy, send);
    }
}
