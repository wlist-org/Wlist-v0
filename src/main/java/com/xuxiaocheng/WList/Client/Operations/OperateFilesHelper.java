package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateFilesHandler
 */
public final class OperateFilesHelper {
    private OperateFilesHelper() {
        super();
    }

    public static @Nullable VisibleFilesListInformation listFiles(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation directory, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ListFiles, token);
        directory.dump(send);
        ByteBufIOUtil.writeByte(send, (byte) filter.ordinal());
        Options.dumpOrderPolicies(send, orders);
        ByteBufIOUtil.writeVariableLenLong(send, position);
        ByteBufIOUtil.writeVariableLenInt(send, limit);
        OperateHelper.logOperating(OperationType.ListFiles, token, p -> p.add("directory", directory).add("filter", filter).add("orders", orders).add("position", position).add("limit", limit));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final VisibleFilesListInformation information = VisibleFilesListInformation.parse(receive);
                OperateHelper.logOperated(OperationType.ListFiles, OperateHelper.logReason(null).andThen(p -> p.add("information", information)));
                return information;
            }
            OperateHelper.logOperated(OperationType.ListFiles, OperateHelper.logReason(reason));
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable VisibleFileInformation getFileOrDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location, final boolean isDirectory) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.GetFileOrDirectory, token);
        location.dump(send);
        ByteBufIOUtil.writeBoolean(send, isDirectory);
        OperateHelper.logOperating(OperationType.GetFileOrDirectory, token, p -> p.add("location", location).add("isDirectory", isDirectory));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final VisibleFileInformation information = VisibleFileInformation.parse(receive);
                OperateHelper.logOperated(OperationType.GetFileOrDirectory, OperateHelper.logReason(null).andThen(p -> p.add("information", information)));
                return information;
            }
            OperateHelper.logOperated(OperationType.GetFileOrDirectory, OperateHelper.logReason(reason));
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean refreshDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RefreshDirectory, token);
        location.dump(send);
        OperateHelper.logOperating(OperationType.RefreshDirectory, token, p -> p.add("location", location));
        return OperateHelper.booleanOperation(client, send, OperationType.RefreshDirectory);
    }

    public static boolean trashFileOrDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location, final boolean isDirectory) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.TrashFileOrDirectory, token);
        location.dump(send);
        ByteBufIOUtil.writeBoolean(send, isDirectory);
        OperateHelper.logOperating(OperationType.TrashFileOrDirectory, token, p -> p.add("location", location).add("isDirectory", isDirectory));
        return OperateHelper.booleanOperation(client, send, OperationType.TrashFileOrDirectory);
    }

    public static @Nullable DownloadConfirm requestDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation file, final long from, final long to) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RequestDownloadFile, token);
        file.dump(send);
        ByteBufIOUtil.writeVariableLenLong(send, from);
        ByteBufIOUtil.writeVariable2LenLong(send, to);
        OperateHelper.logOperating(OperationType.RequestDownloadFile, token, p -> p.add("file", file).add("from", from).add("to", to));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final DownloadConfirm confirm = DownloadConfirm.parse(receive);
                OperateHelper.logOperated(OperationType.RequestDownloadFile, OperateHelper.logReason(null).andThen(p -> p.add("confirm", confirm)));
                return confirm;
            }
            OperateHelper.logOperated(OperationType.RequestDownloadFile, OperateHelper.logReason(reason));
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean cancelDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.CancelDownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.CancelDownloadFile, token, p -> p.add("id", id));
        return OperateHelper.booleanOperation(client, send, OperationType.CancelDownloadFile);
    }

    public static DownloadConfirm.@Nullable DownloadInformation confirmDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ConfirmDownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.CancelDownloadFile, token, p -> p.add("id", id));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final DownloadConfirm.DownloadInformation information = DownloadConfirm.DownloadInformation.parse(receive);
                OperateHelper.logOperated(OperationType.ConfirmDownloadFile, OperateHelper.logReason(null).andThen(p -> p.add("information", information)));
                return information;
            }
            OperateHelper.logOperated(OperationType.ConfirmDownloadFile, OperateHelper.logReason(reason));
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable ByteBuf downloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, final int index) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.DownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        ByteBufIOUtil.writeVariableLenInt(send, index);
        OperateHelper.logOperating(OperationType.DownloadFile, token, p -> p.add("id", id).add("index", index));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                OperateHelper.logOperated(OperationType.DownloadFile, OperateHelper.logReason(null).andThen(p -> p.add("size", receive.readableBytes())));
                return receive.retainedDuplicate();
            }
            OperateHelper.logOperated(OperationType.DownloadFile, OperateHelper.logReason(reason));
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean finishDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.FinishDownloadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.FinishDownloadFile, token, p -> p.add("id", id));
        return OperateHelper.booleanOperation(client, send, OperationType.FinishDownloadFile);
    }

//    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> createDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parentLocation, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.CreateDirectory, token);
//        FileLocation.dump(send, parentLocation);
//        ByteBufIOUtil.writeUTF(send, directoryName);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(OperationType.CreateDirectory, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("parentLocation", parentLocation).add("directoryName", directoryName).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                OperateHelper.logOperated(OperationType.CreateDirectory, () -> ParametersMap.create().add("success", true)
//                        .add("information", information));
//                return UnionPair.ok(information);
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.CreateDirectory, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFilesHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> renameFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation fileLocation, final @NotNull String newFilename, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RenameFile, token);
//        FileLocation.dump(send, fileLocation);
//        ByteBufIOUtil.writeUTF(send, newFilename);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(OperationType.RenameFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("fileLocation", fileLocation).add("newFilename", newFilename).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                OperateHelper.logOperated(OperationType.RenameFile, () -> ParametersMap.create().add("success", true)
//                        .add("information", information));
//                return UnionPair.ok(information);
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.RenameFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFilesHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @NotNull UnionPair<@NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull String>, @NotNull FailureReason> requestUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parentLocation, final @NotNull String filename, final long size, final @NotNull String md5, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RequestUploadFile, token);
//        FileLocation.dump(send, parentLocation);
//        ByteBufIOUtil.writeUTF(send, filename);
//        ByteBufIOUtil.writeVariable2LenLong(send, size);
//        ByteBufIOUtil.writeUTF(send, md5);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(OperationType.RequestUploadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("parentLocation", parentLocation).add("filename", filename).add("size", size).add("md5", md5).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive))
//                if (ByteBufIOUtil.readBoolean(receive)) {
//                    final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                    OperateHelper.logOperated(OperationType.RequestUploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("reuse", true).add("information", information));
//                    return UnionPair.ok(UnionPair.ok(information));
//                } else {
//                    final String id = ByteBufIOUtil.readUTF(receive);
//                    OperateHelper.logOperated(OperationType.RequestUploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("reuse", false).add("id", id));
//                    return UnionPair.ok(UnionPair.fail(id));
//                }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(OperationType.RequestUploadFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFilesHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
//
//    // Null: no id, failure.false: invalid content, failure.true: continue, success: complete
//    public static @Nullable UnionPair<@NotNull VisibleFileInformation, @NotNull Boolean> uploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, final int chunk, final @NotNull ByteBuf buffer) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf prefix = ByteBufAllocator.DEFAULT.buffer();
//        ByteBufIOUtil.writeUTF(prefix, OperationType.UploadFile.name());
//        ByteBufIOUtil.writeUTF(prefix, token);
//        ByteBufIOUtil.writeUTF(prefix, id);
//        ByteBufIOUtil.writeVariableLenInt(prefix, chunk);
//        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, buffer);
//        OperateHelper.logOperating(OperationType.UploadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("id", id).add("chunk", chunk).add("chunkSize", buffer.readableBytes()));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive))
//                if (ByteBufIOUtil.readBoolean(receive)) {
//                    OperateHelper.logOperated(OperationType.UploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("continue", true));
//                    return UnionPair.fail(true);
//                } else {
//                    final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                    OperateHelper.logOperated(Operation.OperationType.UploadFile, () -> ParametersMap.create().add("success", true)
//                            .add("continue", false).add("information", information));
//                    return UnionPair.ok(information);
//                }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(Operation.OperationType.UploadFile, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            if ("Id".equals(reason))
//                return null;
//            if ("Content".equals(reason))
//                return UnionPair.fail(false);
//            throw new WrongStateException(ResponseState.DataError, reason + ParametersMap.create().add("id", id).add("chunk", chunk));
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static boolean cancelUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
//        final ByteBuf send = OperateHelper.operateWithToken(Operation.OperationType.CancelUploadFile, token);
//        ByteBufIOUtil.writeUTF(send, id);
//        OperateHelper.logOperating(Operation.OperationType.CancelUploadFile, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("id", id));
//        final ByteBuf receive = client.send(send);
//        try {
//            final boolean success = OperateHelper.handleState(receive);
//            OperateHelper.logOperated(Operation.OperationType.CancelUploadFile, () -> ParametersMap.create().add("success", success));
//            return success;
//        } finally {
//            receive.release();
//        }
//    }
//
//    public static @NotNull UnionPair<@NotNull VisibleFileInformation, @NotNull FailureReason> copyFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation source, final @NotNull FileLocation target, final @NotNull String newFilename, final Options.@NotNull DuplicatePolicy policy, final boolean moveMode) throws IOException, InterruptedException, WrongStateException {
//        final Operation.OperationType type = moveMode ? Operation.OperationType.MoveFile : Operation.OperationType.CopyFile;
//        final ByteBuf send = OperateHelper.operateWithToken(type, token);
//        FileLocation.dump(send, source);
//        FileLocation.dump(send, target);
//        ByteBufIOUtil.writeUTF(send, newFilename);
//        ByteBufIOUtil.writeUTF(send, policy.name());
//        OperateHelper.logOperating(type, () -> ParametersMap.create().add("tokenHash", token.hashCode())
//                .add("source", source).add("target", target).add("newFilename", newFilename).add("policy", policy));
//        final ByteBuf receive = client.send(send);
//        try {
//            if (OperateHelper.handleState(receive)) {
//                final VisibleFileInformation information = VisibleFileInformation.parse(receive);
//                OperateHelper.logOperated(type, () -> ParametersMap.create().add("success", true)
//                        .add("information", information));
//                return UnionPair.ok(information);
//            }
//            final String reason = ByteBufIOUtil.readUTF(receive);
//            OperateHelper.logOperated(type, () -> ParametersMap.create().add("success", false).add("reason", reason));
//            return UnionPair.fail(OperateFilesHelper.handleFailureReason(reason));
//        } finally {
//            receive.release();
//        }
//    }
}
