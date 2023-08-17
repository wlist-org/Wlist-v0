package com.xuxiaocheng.WListClient.Client.OperationHelpers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WListClient.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import com.xuxiaocheng.WListClient.Server.FailureReason;
import com.xuxiaocheng.WListClient.Server.FileLocation;
import com.xuxiaocheng.WListClient.Server.MessageCiphers;
import com.xuxiaocheng.WListClient.Server.Operation;
import com.xuxiaocheng.WListClient.Server.Options;
import com.xuxiaocheng.WListClient.Server.VisibleFileInformation;
import com.xuxiaocheng.WListClient.Utils.ByteBufIOUtil;
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

    private static @NotNull FailureReason handleFailureReason(final @NotNull ByteBuf receive) throws IOException {
        final String reason = ByteBufIOUtil.readUTF(receive);
        if ("Parameters".equals(reason))
            throw new IllegalArgumentException();
        return switch (reason) {
            case "Filename" -> FailureReason.InvalidFilename;
            case "Duplicate" -> FailureReason.DuplicatePolicyError;
            case "Size" -> FailureReason.ExceedMaxSize;
            case "File" -> FailureReason.NoSuchFile;
            default -> FailureReason.Others;
        };
    }

    public static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull @UnmodifiableView List<@NotNull VisibleFileInformation>> listFiles(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation directoryLocation, final int limit, final int page, final Options.@NotNull OrderPolicy policy, final Options.@NotNull OrderDirection direction, final boolean refresh) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.ListFiles, token);
        FileLocation.dump(send, directoryLocation);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        ByteBufIOUtil.writeVariableLenInt(send, page);
        ByteBufIOUtil.writeUTF(send, policy.name());
        ByteBufIOUtil.writeUTF(send, direction.name());
        ByteBufIOUtil.writeBoolean(send, refresh);
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

    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> makeDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.MakeDirectories, token);
        FileLocation.dump(send, parentLocation);
        ByteBufIOUtil.writeUTF(send, directoryName);
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive) ? UnionPair.ok(VisibleFileInformation.parse(receive)) : UnionPair.fail(OperateFileHelper.handleFailureReason(receive));
        } finally {
            receive.release();
        }
    }

    public static boolean deleteFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation fileLocation) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DeleteFile, token);
        FileLocation.dump(send, fileLocation);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> renameFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation fileLocation, final @NotNull String newNilename, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RenameFile, token);
        FileLocation.dump(send, fileLocation);
        ByteBufIOUtil.writeUTF(send, newNilename);
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive) ? UnionPair.ok(VisibleFileInformation.parse(receive)) : UnionPair.fail(OperateFileHelper.handleFailureReason(receive));
        } finally {
            receive.release();
        }
    }

    public static Pair.@Nullable ImmutablePair<@NotNull Long, @NotNull String> requestDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation fileLocation, final long from, final long to) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RequestDownloadFile, token);
        FileLocation.dump(send, fileLocation);
        ByteBufIOUtil.writeVariableLenLong(send, from);
        ByteBufIOUtil.writeVariable2LenLong(send, to);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return Pair.ImmutablePair.makeImmutablePair(ByteBufIOUtil.readVariableLenLong(receive), ByteBufIOUtil.readUTF(receive));
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("Parameters".equals(reason))
                throw new IllegalArgumentException();
            assert "File".equals(reason);
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable ByteBuf downloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, final int chunk) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.DownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        ByteBufIOUtil.writeVariableLenInt(send, chunk);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return receive.retain();
            assert "Id".equals(ByteBufIOUtil.readUTF(receive));
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean cancelDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CancelDownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @NotNull UnionPair<@NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull String>, @NotNull FailureReason> requestUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parentLocation, final @NotNull String filename, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.RequestUploadFile, token);
        FileLocation.dump(send, parentLocation);
        ByteBufIOUtil.writeUTF(send, filename);
        ByteBufIOUtil.writeVariable2LenLong(send, size);
        ByteBufIOUtil.writeUTF(send, md5);
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                if (ByteBufIOUtil.readBoolean(receive))
                    return UnionPair.ok(UnionPair.ok(VisibleFileInformation.parse(receive)));
                else
                    return UnionPair.ok(UnionPair.fail(ByteBufIOUtil.readUTF(receive)));
            return UnionPair.fail(OperateFileHelper.handleFailureReason(receive));
        } finally {
            receive.release();
        }
    }

    // Null: no id, failure.false: invalid content, failure.true: continue, success: complete
    public static @Nullable UnionPair<@NotNull VisibleFileInformation, @NotNull Boolean> uploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, final int chunk, final @NotNull ByteBuf buffer) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(prefix, MessageCiphers.defaultDoGZip);
        ByteBufIOUtil.writeUTF(prefix, Operation.Type.UploadFile.name());
        ByteBufIOUtil.writeUTF(prefix, token);
        ByteBufIOUtil.writeUTF(prefix, id);
        ByteBufIOUtil.writeVariableLenInt(prefix, chunk);
        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, buffer);
        final ByteBuf receive = client.send(send);
        try {
            if (OperateHelper.handleState(receive))
                return ByteBufIOUtil.readBoolean(receive) ? UnionPair.fail(true) : UnionPair.ok(VisibleFileInformation.parse(receive));
            final String reason = ByteBufIOUtil.readUTF(receive);
            if ("Content".equals(reason))
                return UnionPair.fail(false);
            assert "Id".equals(reason);
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean cancelUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(Operation.Type.CancelUploadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive);
        } finally {
            receive.release();
        }
    }

    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> copyFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation source, final @NotNull FileLocation target, final @NotNull String newFilename, final Options.@NotNull DuplicatePolicy policy, final boolean moveMode) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(moveMode ? Operation.Type.MoveFile : Operation.Type.CopyFile, token);
        FileLocation.dump(send, source);
        FileLocation.dump(send, target);
        ByteBufIOUtil.writeUTF(send, newFilename);
        ByteBufIOUtil.writeUTF(send, policy.name());
        final ByteBuf receive = client.send(send);
        try {
            return OperateHelper.handleState(receive) ? UnionPair.ok(VisibleFileInformation.parse(receive)) : UnionPair.fail(OperateFileHelper.handleFailureReason(receive));
        } finally {
            receive.release();
        }
    }
}
