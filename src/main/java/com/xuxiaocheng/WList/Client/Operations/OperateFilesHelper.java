package com.xuxiaocheng.WList.Client.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.WList.Client.Exceptions.WrongStateException;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.DownloadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.RefreshConfirm;
import com.xuxiaocheng.WList.Commons.Beans.UploadConfirm;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFailureReason;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFilesListInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.OperateFilesHandler
 */
public final class OperateFilesHelper {
    private OperateFilesHelper() {
        super();
    }

    public static @Nullable UnionPair<VisibleFilesListInformation, RefreshConfirm> listFiles(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation directory, final Options.@NotNull FilterPolicy filter, final @NotNull @Unmodifiable LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection> orders, final long position, final int limit) throws IOException, InterruptedException, WrongStateException {
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
                if (ByteBufIOUtil.readBoolean(receive)) {
                    final VisibleFilesListInformation information = VisibleFilesListInformation.parse(receive);
                    OperateHelper.logOperated(OperationType.ListFiles, null, p -> p.add("information", information));
                    return UnionPair.ok(information);
                }
                final RefreshConfirm confirm = RefreshConfirm.parse(receive);
                OperateHelper.logOperated(OperationType.ListFiles, null, p -> p.add("confirm", confirm));
                return UnionPair.fail(confirm);
            }
            OperateHelper.logOperated(OperationType.ListFiles, reason, null);
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
                OperateHelper.logOperated(OperationType.GetFileOrDirectory, null, p -> p.add("information", information));
                return information;
            }
            OperateHelper.logOperated(OperationType.GetFileOrDirectory, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable RefreshConfirm refreshDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RefreshDirectory, token);
        location.dump(send);
        OperateHelper.logOperating(OperationType.RefreshDirectory, token, p -> p.add("location", location));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final RefreshConfirm confirm = RefreshConfirm.parse(receive);
                OperateHelper.logOperated(OperationType.RefreshDirectory, null, p -> p.add("confirm", confirm));
                return confirm;
            }
            OperateHelper.logOperated(OperationType.RefreshDirectory, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean cancelRefresh(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.CancelRefresh, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.CancelRefresh, token, p -> p.add("id", id));
        return OperateHelper.booleanOperation(client, send, OperationType.CancelRefresh);
    }

    public static void confirmRefresh(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ConfirmRefresh, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.ConfirmRefresh, token, p -> p.add("id", id));
        OperateHelper.booleanOperation(client, send, OperationType.ConfirmRefresh);
    }

    public static boolean trashFileOrDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location, final boolean isDirectory) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.TrashFileOrDirectory, token);
        location.dump(send);
        ByteBufIOUtil.writeBoolean(send, isDirectory);
        OperateHelper.logOperating(OperationType.TrashFileOrDirectory, token, p -> p.add("location", location).add("isDirectory", isDirectory));
        return OperateHelper.booleanOperation(client, send, OperationType.TrashFileOrDirectory);
    }

    public static @Nullable UnionPair<DownloadConfirm, VisibleFailureReason> requestDownloadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation file, final long from, final long to) throws IOException, InterruptedException, WrongStateException {
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
                OperateHelper.logOperated(OperationType.RequestDownloadFile, null, p -> p.add("confirm", confirm));
                return UnionPair.ok(confirm);
            }
            if ("Failure".equals(reason)) {
                final VisibleFailureReason failureReason = VisibleFailureReason.parse(receive);
                OperateHelper.logOperated(OperationType.RequestDownloadFile, failureReason.toString(), null);
                return UnionPair.fail(failureReason);
            }
            OperateHelper.logOperated(OperationType.RequestDownloadFile, reason, null);
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
        OperateHelper.logOperating(OperationType.ConfirmDownloadFile, token, p -> p.add("id", id));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final DownloadConfirm.DownloadInformation information = DownloadConfirm.DownloadInformation.parse(receive);
                OperateHelper.logOperated(OperationType.ConfirmDownloadFile, null, p -> p.add("information", information));
                return information;
            }
            OperateHelper.logOperated(OperationType.ConfirmDownloadFile, reason, null);
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
                OperateHelper.logOperated(OperationType.DownloadFile, null, p -> p.add("size", receive.readableBytes()));
                return receive.retainedSlice();
            }
            OperateHelper.logOperated(OperationType.DownloadFile, reason, null);
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

    public static @Nullable UnionPair<Object, VisibleFailureReason> createDirectory(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parent, final @NotNull String directoryName, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.CreateDirectory, token);
        parent.dump(send);
        ByteBufIOUtil.writeUTF(send, directoryName);
        ByteBufIOUtil.writeUTF(send, policy.name());
        OperateHelper.logOperating(OperationType.CreateDirectory, token, p -> p.add("parent", parent).add("directoryName", directoryName).add("policy", policy));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                OperateHelper.logOperated(OperationType.CreateDirectory, null, null);
                return UnionPair.ok(token); // null.
            }
            if ("Failure".equals(reason)) {
                final VisibleFailureReason failureReason = VisibleFailureReason.parse(receive);
                OperateHelper.logOperated(OperationType.CreateDirectory, failureReason.toString(), null);
                return UnionPair.fail(failureReason);
            }
            OperateHelper.logOperated(OperationType.CreateDirectory, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable UnionPair<UploadConfirm, VisibleFailureReason> requestUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation parent, final @NotNull String filename, final long size, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RequestUploadFile, token);
        parent.dump(send);
        ByteBufIOUtil.writeUTF(send, filename);
        ByteBufIOUtil.writeVariable2LenLong(send, size);
        ByteBufIOUtil.writeUTF(send, policy.name());
        OperateHelper.logOperating(OperationType.RequestUploadFile, token, p -> p.add("parent", parent).add("filename", filename).add("size", size).add("policy", policy));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final UploadConfirm confirm = UploadConfirm.parse(receive);
                OperateHelper.logOperated(OperationType.RequestUploadFile, null, p -> p.add("confirm", confirm));
                return UnionPair.ok(confirm);
            }
            if ("Failure".equals(reason)) {
                final VisibleFailureReason failureReason = VisibleFailureReason.parse(receive);
                OperateHelper.logOperated(OperationType.RequestUploadFile, failureReason.toString(), null);
                return UnionPair.fail(failureReason);
            }
            OperateHelper.logOperated(OperationType.RequestUploadFile, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean cancelUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.CancelUploadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.CancelUploadFile, token, p -> p.add("id", id));
        return OperateHelper.booleanOperation(client, send, OperationType.CancelUploadFile);
    }

    public static UploadConfirm.@Nullable UploadInformation confirmUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, @SuppressWarnings("TypeMayBeWeakened") final @NotNull List<@NotNull String> checksums) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.ConfirmUploadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        ByteBufIOUtil.writeVariableLenInt(send, checksums.size());
        for (final String checksum: checksums)
            ByteBufIOUtil.writeUTF(send, checksum);
        OperateHelper.logOperating(OperationType.ConfirmUploadFile, token, p -> p.add("id", id).add("checksums", checksums));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                final UploadConfirm.UploadInformation information = UploadConfirm.UploadInformation.parse(receive);
                OperateHelper.logOperated(OperationType.ConfirmUploadFile, null, p -> p.add("information", information));
                return information;
            }
            OperateHelper.logOperated(OperationType.ConfirmUploadFile, reason, null);
            return null;
        } finally {
            receive.release();
        }
    }

    public static boolean uploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id, final int index, final @NotNull ByteBuf buffer) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf prefix = OperateHelper.operateWithToken(OperationType.UploadFile, token);
        ByteBufIOUtil.writeUTF(prefix, id);
        ByteBufIOUtil.writeVariableLenInt(prefix, index);
        final ByteBuf send = ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, prefix, buffer);
        OperateHelper.logOperating(OperationType.UploadFile, token, p -> p.add("id", id).add("index", index).add("size", buffer.readableBytes()));
        return OperateHelper.booleanOperation(client, send, OperationType.UploadFile);
    }

    public static boolean finishUploadFile(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull String id) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.FinishUploadFile, token);
        ByteBufIOUtil.writeUTF(send, id);
        OperateHelper.logOperating(OperationType.FinishUploadFile, token, p -> p.add("id", id));
        return OperateHelper.booleanOperation(client, send, OperationType.FinishUploadFile);
    }

    public static @Nullable UnionPair<Boolean, VisibleFailureReason> copyDirectly(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.CopyFile, token);
        location.dump(send);
        ByteBufIOUtil.writeBoolean(send, isDirectory);
        parent.dump(send);
        ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeUTF(send, policy.name());
        OperateHelper.logOperating(OperationType.CopyFile, token, p -> p.add("location", location).add("isDirectory", isDirectory).add("parent", parent).add("name", name).add("policy", policy));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                OperateHelper.logOperated(OperationType.CopyFile, null, null);
                return UnionPair.ok(Boolean.TRUE);
            }
            if ("Failure".equals(reason)) {
                final VisibleFailureReason failureReason = VisibleFailureReason.parse(receive);
                OperateHelper.logOperated(OperationType.CopyFile, failureReason.toString(), null);
                return UnionPair.fail(failureReason);
            }
            OperateHelper.logOperated(OperationType.CopyFile, reason, null);
            return "Complex".equals(reason) ? UnionPair.ok(Boolean.FALSE) : null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable UnionPair<Boolean, VisibleFailureReason> moveDirectly(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull FileLocation parent, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.MoveFile, token);
        location.dump(send);
        ByteBufIOUtil.writeBoolean(send, isDirectory);
        parent.dump(send);
        ByteBufIOUtil.writeUTF(send, policy.name());
        OperateHelper.logOperating(OperationType.MoveFile, token, p -> p.add("location", location).add("isDirectory", isDirectory).add("parent", parent).add("policy", policy));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                OperateHelper.logOperated(OperationType.MoveFile, null, null);
                return UnionPair.ok(Boolean.TRUE);
            }
            if ("Failure".equals(reason)) {
                final VisibleFailureReason failureReason = VisibleFailureReason.parse(receive);
                OperateHelper.logOperated(OperationType.MoveFile, failureReason.toString(), null);
                return UnionPair.fail(failureReason);
            }
            OperateHelper.logOperated(OperationType.MoveFile, reason, null);
            return "Complex".equals(reason) ? UnionPair.ok(Boolean.FALSE) : null;
        } finally {
            receive.release();
        }
    }

    public static @Nullable UnionPair<Boolean, VisibleFailureReason> renameDirectly(final @NotNull WListClientInterface client, final @NotNull String token, final @NotNull FileLocation location, final boolean isDirectory, final @NotNull String name, final Options.@NotNull DuplicatePolicy policy) throws IOException, InterruptedException, WrongStateException {
        final ByteBuf send = OperateHelper.operateWithToken(OperationType.RenameFile, token);
        location.dump(send);
        ByteBufIOUtil.writeBoolean(send, isDirectory);
        ByteBufIOUtil.writeUTF(send, name);
        ByteBufIOUtil.writeUTF(send, policy.name());
        OperateHelper.logOperating(OperationType.RenameFile, token, p -> p.add("location", location).add("isDirectory", isDirectory).add("name", name).add("policy", policy));
        final ByteBuf receive = client.send(send);
        try {
            final String reason = OperateHelper.handleState(receive);
            if (reason == null) {
                OperateHelper.logOperated(OperationType.RenameFile, null, null);
                return UnionPair.ok(Boolean.TRUE);
            }
            if ("Failure".equals(reason)) {
                final VisibleFailureReason failureReason = VisibleFailureReason.parse(receive);
                OperateHelper.logOperated(OperationType.RenameFile, failureReason.toString(), null);
                return UnionPair.fail(failureReason);
            }
            OperateHelper.logOperated(OperationType.RenameFile, reason, null);
            return "Complex".equals(reason) ? UnionPair.ok(Boolean.FALSE) : null;
        } finally {
            receive.release();
        }
    }
}
