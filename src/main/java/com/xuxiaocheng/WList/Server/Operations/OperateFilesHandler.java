package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NetworkTransmission;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserInformation;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operations.Helpers.DownloadIdHelper;
import com.xuxiaocheng.WList.Server.Operations.Helpers.UploadIdHelper;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Records.DownloadRequirements;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Records.UploadRequirements;
import com.xuxiaocheng.WList.Server.Storage.RootSelector;
import com.xuxiaocheng.WList.Server.WListServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OperateFilesHandler {
    private OperateFilesHandler() {
        super();
    }

    private static final @NotNull MessageProto FilterDataError = MessageProto.composeMessage(ResponseState.DataError, "Filter");
    private static final @NotNull MessageProto OrdersDataError = MessageProto.composeMessage(ResponseState.DataError, "Orders");
    private static final @NotNull MessageProto PolicyDataError = MessageProto.composeMessage(ResponseState.DataError, "Policy");
    private static final @NotNull MessageProto LocationNotAvailable = MessageProto.composeMessage(ResponseState.DataError, "Location");
    private static final @NotNull MessageProto ComplexOperation = MessageProto.composeMessage(ResponseState.DataError, "Complex");
    private static final @NotNull MessageProto InInsideError = MessageProto.composeMessage(ResponseState.DataError, "Inside");
    private static final @NotNull MessageProto IdDataError = MessageProto.composeMessage(ResponseState.DataError, "Id");
    private static final @NotNull MessageProto UploadDataError = MessageProto.composeMessage(ResponseState.DataError, "Content");
    private static @NotNull MessageProto Failure(final @NotNull FailureReason reason) {
        return new MessageProto(ResponseState.DataError, buf -> {
            ByteBufIOUtil.writeUTF(buf, "Failure");
            return reason.dumpVisible(buf);
        });
    }
    private static @NotNull MessageProto ChecksumError(final int index) {
        return new MessageProto(ResponseState.DataError, buf -> {
            ByteBufIOUtil.writeUTF(buf, "Checksum");
            ByteBufIOUtil.writeVariableLenInt(buf, index);
            return buf;
        });
    }

    public static void initialize() {
        ServerHandlerManager.register(OperationType.ListFiles, OperateFilesHandler.doListFiles);
        ServerHandlerManager.register(OperationType.GetFileOrDirectory, OperateFilesHandler.doGetFileOrDirectory);
        ServerHandlerManager.register(OperationType.RefreshDirectory, OperateFilesHandler.doRefreshDirectory);
        ServerHandlerManager.register(OperationType.TrashFileOrDirectory, OperateFilesHandler.doTrashFileOrDirectory);
        ServerHandlerManager.register(OperationType.RequestDownloadFile, OperateFilesHandler.doRequestDownloadFile);
        ServerHandlerManager.register(OperationType.CancelDownloadFile, OperateFilesHandler.doCancelDownloadFile);
        ServerHandlerManager.register(OperationType.ConfirmDownloadFile, OperateFilesHandler.doConfirmDownloadFile);
        ServerHandlerManager.register(OperationType.DownloadFile, OperateFilesHandler.doDownloadFile);
        ServerHandlerManager.register(OperationType.FinishDownloadFile, OperateFilesHandler.doFinishDownloadFile);
        ServerHandlerManager.register(OperationType.CreateDirectory, OperateFilesHandler.doCreateDirectory);
        ServerHandlerManager.register(OperationType.RequestUploadFile, OperateFilesHandler.doRequestUploadFile);
        ServerHandlerManager.register(OperationType.CancelUploadFile, OperateFilesHandler.doCancelUploadFile);
        ServerHandlerManager.register(OperationType.ConfirmUploadFile, OperateFilesHandler.doConfirmUploadFile);
        ServerHandlerManager.register(OperationType.UploadFile, OperateFilesHandler.doUploadFile);
        ServerHandlerManager.register(OperationType.FinishUploadFile, OperateFilesHandler.doFinishUploadFile);
        ServerHandlerManager.register(OperationType.CopyFile, OperateFilesHandler.doCopyFile);
        ServerHandlerManager.register(OperationType.MoveFile, OperateFilesHandler.doMoveFile);
        ServerHandlerManager.register(OperationType.RenameFile, OperateFilesHandler.doRenameFile);
    }

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#listFiles(WListClientInterface, String, FileLocation, Options.FilterPolicy, LinkedHashMap, long, int)
     */
    private static final @NotNull ServerHandler doListFiles = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FilesList);
        final FileLocation directory = FileLocation.parse(buffer);
        final Options.FilterPolicy filter = Options.FilterPolicy.of(ByteBufIOUtil.readByte(buffer));
        final UnionPair<LinkedHashMap<VisibleFileInformation.Order, Options.OrderDirection>, String> orders =
                Options.parseOrderPolicies(buffer, VisibleFileInformation.Order.class, -1);
        final long position = ByteBufIOUtil.readVariableLenLong(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.ListFiles, user, () -> ParametersMap.create()
                .add("directory", directory).add("filter", filter).add("orders", orders).add("position", position).add("limit", limit));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else //noinspection VariableNotUsedInsideIf
            if (filter == null)
            message = OperateFilesHandler.FilterDataError;
        else if (orders == null || orders.isFailure())
            message = OperateFilesHandler.OrdersDataError;
        else if (position < 0 || limit < 1 || ServerConfiguration.get().maxLimitPerPage() < limit)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.list(directory, filter, orders.getT(), position, limit, p -> {
//            if (!barrier.compareAndSet(true, false)) {
//                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doListFiles'.", ParametersMap.create()
//                        .add("p", p).add("directory", directory).add("filter", filter).add("orders", orders).add("position", position).add("limit", limit));
//                return;
//            }
//            if (p.isFailure()) {
//                channel.pipeline().fireExceptionCaught(p.getE());
//                return;
//            }
//            WListServer.ServerChannelHandler.write(channel, p.getT().isPresent() ?
//                    MessageProto.successMessage(p.getT().get()::dumpVisible) : OperateFilesHandler.LocationNotAvailable);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#getFileOrDirectory(WListClientInterface, String, FileLocation, boolean)
     */
    private static final @NotNull ServerHandler doGetFileOrDirectory = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileDownload);
        final FileLocation location = FileLocation.parse(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        ServerHandler.logOperation(channel, OperationType.GetFileOrDirectory, user, () -> ParametersMap.create()
                .add("location", location).add("isDirectory", isDirectory));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.info(location, isDirectory, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doGetFileOrDirectory'.", ParametersMap.create()
                        .add("p", p).add("location", location).add("isDirectory", isDirectory));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            WListServer.ServerChannelHandler.write(channel, p.getT().isPresent() ?
                    MessageProto.successMessage(p.getT().get()::dumpVisible) : OperateFilesHandler.LocationNotAvailable);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#refreshDirectory(WListClientInterface, String, FileLocation)
     */
    private static final @NotNull ServerHandler doRefreshDirectory = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FilesRefresh);
        final FileLocation directory = FileLocation.parse(buffer);
        ServerHandler.logOperation(channel, OperationType.RefreshDirectory, user, () -> ParametersMap.create()
                .add("directory", directory));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.refreshDirectory(directory, p -> {
//            if (!barrier.compareAndSet(true, false)) {
//                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doRefreshDirectory'.", ParametersMap.create()
//                        .add("p", p).add("directory", directory));
//                return;
//            }
//            if (p.isFailure()) {
//                channel.pipeline().fireExceptionCaught(p.getE());
//                return;
//            }
//            WListServer.ServerChannelHandler.write(channel, p.getT().booleanValue() ? MessageProto.Success : OperateFilesHandler.LocationNotAvailable);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#trashFileOrDirectory(WListClientInterface, String, FileLocation, boolean)
     */
    private static final @NotNull ServerHandler doTrashFileOrDirectory = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileTrash);
        final FileLocation location = FileLocation.parse(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        ServerHandler.logOperation(channel, OperationType.TrashFileOrDirectory, user, () -> ParametersMap.create()
                .add("location", location).add("isDirectory", isDirectory));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.trash(location, isDirectory, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doTrashFileOrDirectory'.", ParametersMap.create()
                        .add("p", p).add("location", location).add("isDirectory", isDirectory));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            WListServer.ServerChannelHandler.write(channel, p.getT().isPresent() ? p.getT().get().booleanValue() ?
                    MessageProto.Success : OperateFilesHandler.LocationNotAvailable : OperateFilesHandler.ComplexOperation);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#requestDownloadFile(WListClientInterface, String, FileLocation, long, long)
     */
    private static final @NotNull ServerHandler doRequestDownloadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FilesList, UserPermission.FileDownload);
        final FileLocation file = FileLocation.parse(buffer);
        final long from = ByteBufIOUtil.readVariableLenLong(buffer);
        final long to = ByteBufIOUtil.readVariable2LenLong(buffer);
        ServerHandler.logOperation(channel, OperationType.RequestDownloadFile, user, () -> ParametersMap.create()
                .add("file", file).add("from", from).add("to", to));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (from < 0 || to < 0 || from >= to)
            message = MessageProto.WrongParameters;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.downloadFile(file, from, to, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doRequestDownloadFile'.", ParametersMap.create()
                        .add("p", p).add("file", file).add("from", from).add("to", to));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            if (p.getT().isFailure()) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.Failure(p.getT().getE()));
                return;
            }
            final DownloadRequirements requirements = p.getT().getT();
            final String id = DownloadIdHelper.generateId(requirements);
            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed download id.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("file", file).add("from", from).add("to", to).add("id", id)
                            .add("acceptedRange", requirements.acceptedRange()).add("downloadingSize", requirements.downloadingSize()));
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> requirements.dumpConfirm(buf, id)));
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#cancelDownloadFile(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doCancelDownloadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileDownload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.CancelDownloadFile, user, () -> ParametersMap.create().add("id", id));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            if (!DownloadIdHelper.cancel(id)) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Cancelled download.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("id", id));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#confirmDownloadFile(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doConfirmDownloadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileDownload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.ConfirmDownloadFile, user, () -> ParametersMap.create().add("id", id));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            final DownloadRequirements.DownloadMethods parallel = DownloadIdHelper.confirm(id);
            if (parallel == null) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(parallel::dumpInformation));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#downloadFile(WListClientInterface, String, String, int)
     */
    private static final @NotNull ServerHandler doDownloadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileDownload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int index = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.DownloadFile, user, () -> ParametersMap.create().add("id", id).add("index", index));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (index < 0)
            message = OperateFilesHandler.IdDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> DownloadIdHelper.download(id, index, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doDownloadFile'.", ParametersMap.create()
                        .add("p", p).add("id", id).add("index", index));
                return;
            }
            if (p == null) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            assert p.getT().readableBytes() <= NetworkTransmission.FileTransferBufferSize;
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf ->
                    ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, buf, p.getT())
            ));
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#finishDownloadFile(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doFinishDownloadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileDownload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.FinishDownloadFile, user, () -> ParametersMap.create().add("id", id));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            if (!DownloadIdHelper.finish(id)) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Finished download.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("id", id));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#createDirectory(WListClientInterface, String, FileLocation, String, Options.DuplicatePolicy)
     */
    private static final @NotNull ServerHandler doCreateDirectory = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FilesList, UserPermission.FileUpload);
        final FileLocation parent = FileLocation.parse(buffer);
        final String directoryName = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy policy = Options.DuplicatePolicy.of(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.CreateDirectory, user, () -> ParametersMap.create()
                .add("parent", parent).add("directoryName", directoryName).add("policy", policy));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (policy == null)
            message = OperateFilesHandler.PolicyDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.createDirectory(parent, directoryName, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doCreateDirectory'.", ParametersMap.create()
                        .add("p", p).add("parent", parent).add("directoryName", directoryName).add("policy", policy));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            if (p.getT().isFailure()) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.Failure(p.getT().getE()));
                return;
            }
            final FileInformation directory = p.getT().getT();
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Created directory.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("directory", directory));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#requestUploadFile(WListClientInterface, String, FileLocation, String, long, Options.DuplicatePolicy)
     */
    private static final @NotNull ServerHandler doRequestUploadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FilesList, UserPermission.FileUpload);
        final FileLocation parent = FileLocation.parse(buffer);
        final String filename = ByteBufIOUtil.readUTF(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final Options.DuplicatePolicy policy = Options.DuplicatePolicy.of(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.RequestUploadFile, user, () -> ParametersMap.create()
                .add("parent", parent).add("filename", filename).add("size", size).add("policy", policy)
                .optionallyAddSupplier(policy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () ->
                        user.getT().group().permissions().contains(UserPermission.FileTrash)));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (size < 0)
            message = MessageProto.WrongParameters;
        else if (policy == null)
            message = OperateFilesHandler.PolicyDataError;
        else if (policy == Options.DuplicatePolicy.OVER && user.getT().group().permissions().contains(UserPermission.FileTrash))
            message = OperateSelfHandler.NoPermission(List.of(UserPermission.FileTrash));
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.uploadFile(parent, filename, size, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doRequestUploadFile'.", ParametersMap.create()
                        .add("p", p).add("parent", parent).add("filename", filename).add("size", size).add("policy", policy));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            if (p.getT().isFailure()) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.Failure(p.getT().getE()));
                return;
            }
            final UploadRequirements requirements = p.getT().getT();
            final String id = UploadIdHelper.generateId(requirements);
            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed upload id.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("parent", parent).add("filename", filename).add("size", size).add("checksums", requirements.checksums()));
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> requirements.dumpConfirm(buf, id)));
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#cancelUploadFile(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doCancelUploadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileUpload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.CancelUploadFile, user, () -> ParametersMap.create().add("id", id));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            if (!UploadIdHelper.cancel(id)) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Cancelled upload.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("id", id));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#confirmUploadFile(WListClientInterface, String, String, List)
     */
    private static final @NotNull ServerHandler doConfirmUploadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileUpload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        final List<String> checksums = new ArrayList<>(length);
        for (int i = 0; i < length; ++i)
            checksums.add(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.ConfirmUploadFile, user, () -> ParametersMap.create().add("id", id).add("checksums", checksums));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            final UnionPair<UploadRequirements.UploadMethods, Integer> parallel = UploadIdHelper.confirm(id, checksums);
            if (parallel == null) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
                return;
            }
            if (parallel.isFailure()) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.ChecksumError(parallel.getE().intValue()));
                return;
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(parallel.getT()::dumpInformation));
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#uploadFile(WListClientInterface, String, String, int, ByteBuf)
     */
    private static final @NotNull ServerHandler doUploadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileUpload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int index = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, OperationType.UploadFile, user, () -> ParametersMap.create().add("id", id).add("index", index).add("size", buffer.readableBytes()));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (index < 0)
            message = OperateFilesHandler.IdDataError;
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        assert buffer.readableBytes() <= NetworkTransmission.FileTransferBufferSize;
        final ByteBuf content = buffer.retainedDuplicate();
        buffer.readerIndex(buffer.writerIndex());
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> {
            if (UploadIdHelper.upload(id, content, index, p -> {
                if (!barrier.compareAndSet(true, false)) {
                    HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doUploadFile'.", ParametersMap.create()
                            .add("p", p).add("id", id).add("index", index).add("size", content.capacity()));
                    return;
                }
                if (p != null) {
                    channel.pipeline().fireExceptionCaught(p);
                    return;
                }
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
            })) return;
            WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
        };
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#finishUploadFile(WListClientInterface, String, String)
     */
    private static final @NotNull ServerHandler doFinishUploadFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileUpload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, OperationType.FinishUploadFile, user, () -> ParametersMap.create().add("id", id));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> UploadIdHelper.finish(id, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doFinishUploadFile'.", ParametersMap.create()
                        .add("p", p).add("id", id));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            if (p.getT().isFailure()) {
                if (p.getT().getE().booleanValue()) {
                    HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Failed to upload.", ServerHandler.user(null, user.getT()),
                            ParametersMap.create().add("id", id));
                    WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.UploadDataError);
                } else WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.IdDataError);
                return;
            }
            final FileInformation file = p.getT().getT();
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Uploaded file.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("file", file));
            WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#copyDirectly(WListClientInterface, String, FileLocation, boolean, FileLocation, String, Options.DuplicatePolicy)
     */
    private static final @NotNull ServerHandler doCopyFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileCopy);
        final FileLocation location = FileLocation.parse(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final FileLocation parent = FileLocation.parse(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy policy = Options.DuplicatePolicy.of(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.CopyFile, user, () -> ParametersMap.create()
                .add("location", location).add("isDirectory", isDirectory).add("parent", parent).add("name", name).add("policy", policy)
                .optionallyAddSupplier(policy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () ->
                        user.getT().group().permissions().contains(UserPermission.FileTrash)));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (policy == null)
            message = OperateFilesHandler.PolicyDataError;
        else if (policy == Options.DuplicatePolicy.OVER && user.getT().group().permissions().contains(UserPermission.FileTrash))
            message = OperateSelfHandler.NoPermission(List.of(UserPermission.FileTrash));
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.copyDirectly(location, isDirectory, parent, name, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doCopyFile'.", ParametersMap.create()
                        .add("p", p).add("location", location).add("isDirectory", isDirectory).add("parent", parent).add("name", name).add("policy", policy));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            if (p.getT().isPresent()) {
                if (p.getT().get().isFailure()) {
                    WListServer.ServerChannelHandler.write(channel, p.getT().get().getE().isPresent() ?
                            OperateFilesHandler.Failure(p.getT().get().getE().get()) : OperateFilesHandler.InInsideError);
                    return;
                }
                final FileInformation information = p.getT().get().getT();
                HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Copied.", ServerHandler.user(null, user.getT()),
                        ParametersMap.create().add("from", location).add(isDirectory ? "directory" : "file", information));
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.ComplexOperation);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#moveDirectly(WListClientInterface, String, FileLocation, boolean, FileLocation, Options.DuplicatePolicy)
     */
    private static final @NotNull ServerHandler doMoveFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileMove);
        final FileLocation location = FileLocation.parse(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final FileLocation parent = FileLocation.parse(buffer);
        final Options.DuplicatePolicy policy = Options.DuplicatePolicy.of(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.MoveFile, user, () -> ParametersMap.create()
                .add("location", location).add("isDirectory", isDirectory).add("parent", parent).add("policy", policy)
                .optionallyAddSupplier(policy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () ->
                        user.getT().group().permissions().contains(UserPermission.FileTrash)));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (policy == null)
            message = OperateFilesHandler.PolicyDataError;
        else if (policy == Options.DuplicatePolicy.OVER && user.getT().group().permissions().contains(UserPermission.FileTrash))
            message = OperateSelfHandler.NoPermission(List.of(UserPermission.FileTrash));
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.moveDirectly(location, isDirectory, parent, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doMoveFile'.", ParametersMap.create()
                        .add("p", p).add("location", location).add("isDirectory", isDirectory).add("parent", parent).add("policy", policy));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            if (p.getT().isPresent()) {
                if (p.getT().get().isFailure()) {
                    WListServer.ServerChannelHandler.write(channel, p.getT().get().getE().isPresent() ?
                            OperateFilesHandler.Failure(p.getT().get().getE().get()) : OperateFilesHandler.InInsideError);
                    return;
                }
                final FileInformation information = p.getT().get().getT();
                HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Moved.", ServerHandler.user(null, user.getT()),
                        ParametersMap.create().add("from", location).add(isDirectory ? "directory" : "file", information));
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.ComplexOperation);
        });
    };

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#renameDirectly(WListClientInterface, String, FileLocation, boolean, String, Options.DuplicatePolicy)
     */
    private static final @NotNull ServerHandler doRenameFile = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileMove);
        final FileLocation location = FileLocation.parse(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy policy = Options.DuplicatePolicy.of(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.RenameFile, user, () -> ParametersMap.create()
                .add("location", location).add("isDirectory", isDirectory).add("name", name).add("policy", policy)
                .optionallyAddSupplier(policy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () ->
                        user.getT().group().permissions().contains(UserPermission.FileTrash)));
        MessageProto message = null;
        if (user.isFailure())
            message = user.getE();
        else if (policy == null)
            message = OperateFilesHandler.PolicyDataError;
        else if (policy == Options.DuplicatePolicy.OVER && user.getT().group().permissions().contains(UserPermission.FileTrash))
            message = OperateSelfHandler.NoPermission(List.of(UserPermission.FileTrash));
        if (message != null) {
            WListServer.ServerChannelHandler.write(channel, message);
            return null;
        }
        final AtomicBoolean barrier = new AtomicBoolean(true);
        return () -> RootSelector.renameDirectly(location, isDirectory, name, policy, p -> {
            if (!barrier.compareAndSet(true, false)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.MISTAKE, "Duplicate message on 'doRenameFile'.", ParametersMap.create()
                        .add("p", p).add("location", location).add("isDirectory", isDirectory).add("name", name).add("policy", policy));
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            if (p.getT().isPresent()) {
                if (p.getT().get().isFailure()) {
                    WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.Failure(p.getT().get().getE()));
                    return;
                }
                final FileInformation information = p.getT().get().getT();
                HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Renamed.", ServerHandler.user(null, user.getT()),
                        ParametersMap.create().add("from", location).add(isDirectory ? "directory" : "file", information));
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.ComplexOperation);
        });
    };
}
