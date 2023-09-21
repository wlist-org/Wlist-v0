package com.xuxiaocheng.WList.Server.Operations;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
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
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Records.FailureReason;
import com.xuxiaocheng.WList.Server.Storage.Selectors.RootSelector;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

public final class OperateFilesHandler {
    private OperateFilesHandler() {
        super();
    }

    private static final @NotNull MessageProto FilterDataError = MessageProto.composeMessage(ResponseState.DataError, "Filter");
    private static final @NotNull MessageProto OrdersDataError = MessageProto.composeMessage(ResponseState.DataError, "Orders");
    private static final @NotNull MessageProto PolicyDataError = MessageProto.composeMessage(ResponseState.DataError, "Policy");
    private static final @NotNull MessageProto LocationNotAvailable = MessageProto.composeMessage(ResponseState.DataError, "Available");
    private static final @NotNull MessageProto LocationNotFound = MessageProto.composeMessage(ResponseState.DataError, "Location");
    private static @NotNull MessageProto Failure(final @NotNull FailureReason reason) {
        return new MessageProto(ResponseState.DataError, buf -> {
            ByteBufIOUtil.writeUTF(buf, "Failure");
            ByteBufIOUtil.writeUTF(buf, reason.kind().name());
            ByteBufIOUtil.writeUTF(buf, reason.message());
            return buf;
        });
    }
//    private static final @NotNull MessageProto InvalidId = MessageProto.composeMessage(ResponseState.DataError, "Id");
//    private static final @NotNull MessageProto InvalidFile = MessageProto.composeMessage(ResponseState.DataError, "Content");

    public static void initialize() {
        ServerHandlerManager.register(OperationType.ListFiles, OperateFilesHandler.doListFiles);
        ServerHandlerManager.register(OperationType.RefreshDirectory, OperateFilesHandler.doRefreshDirectory);
        ServerHandlerManager.register(OperationType.GetFileOrDirectory, OperateFilesHandler.doGetFileOrDirectory);
        ServerHandlerManager.register(OperationType.DeleteFileOrDirectory, OperateFilesHandler.doDeleteFileOrDirectory);
        ServerHandlerManager.register(OperationType.CreateDirectory, OperateFilesHandler.doCreateDirectory);
//        ServerHandlerManager.register(OperationType.RenameFile, OperateFilesHandler.doRenameFile);
//        ServerHandlerManager.register(OperationType.RequestDownloadFile, OperateFilesHandler.doRequestDownloadFile);
//        ServerHandlerManager.register(OperationType.DownloadFile, OperateFilesHandler.doDownloadFile);
//        ServerHandlerManager.register(OperationType.CancelDownloadFile, OperateFilesHandler.doCancelDownloadFile);
//        ServerHandlerManager.register(OperationType.RequestUploadFile, OperateFilesHandler.doRequestUploadFile);
//        ServerHandlerManager.register(OperationType.UploadFile, OperateFilesHandler.doUploadFile);
//        ServerHandlerManager.register(OperationType.CancelUploadFile, OperateFilesHandler.doCancelUploadFile);
//        ServerHandlerManager.register(OperationType.CopyFile, OperateFilesHandler.doCopyFile);
//        ServerHandlerManager.register(OperationType.MoveFile, OperateFilesHandler.doMoveFile);
    }

    /**
     * @see com.xuxiaocheng.WList.Client.Operations.OperateFilesHelper#listFiles(WListClientInterface, String, FileLocation, Options.FilterPolicy, LinkedHashMap, long, int)
     */
    public static final @NotNull ServerHandler doListFiles = (channel, buffer) -> {
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
        return () -> RootSelector.list(directory, filter, orders.getT(), position, limit, p -> {
            if (p == null) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.LocationNotAvailable);
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(buf -> {
                ByteBufIOUtil.writeVariableLenLong(buf, p.getT().total());
                ByteBufIOUtil.writeVariableLenLong(buf, p.getT().filtered());
                ByteBufIOUtil.writeVariableLenInt(buf, p.getT().informationList().size());
                for (final FileInformation information: p.getT().informationList())
                    information.dumpVisible(buf);
                return buf;
            }));
        });
    };

    public static final @NotNull ServerHandler doRefreshDirectory = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FilesBuildIndex);
        final FileLocation directory = FileLocation.parse(buffer);
        ServerHandler.logOperation(channel, OperationType.RefreshDirectory, user, () -> ParametersMap.create()
                .add("directory", directory));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> RootSelector.refreshDirectory(directory, p -> {
            if (p == null) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.LocationNotAvailable);
                return;
            }
            if (p.isFailure()) {
                channel.pipeline().fireExceptionCaught(p.getE());
                return;
            }
            WListServer.ServerChannelHandler.write(channel, p.getT().booleanValue() ? MessageProto.Success : OperateFilesHandler.LocationNotFound);
        });
    };

    public static final @NotNull ServerHandler doGetFileOrDirectory = (channel, buffer) -> {
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
        return () -> {
            final FileInformation information = RootSelector.info(location, isDirectory);
            if (information == null) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.LocationNotAvailable);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(information::dumpVisible));
        };
    };

    public static final @NotNull ServerHandler doDeleteFileOrDirectory = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FileDelete);
        final FileLocation location = FileLocation.parse(buffer);
        final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
        ServerHandler.logOperation(channel, OperationType.DeleteFileOrDirectory, user, () -> ParametersMap.create()
                .add("location", location).add("isDirectory", isDirectory));
        if (user.isFailure()) {
            WListServer.ServerChannelHandler.write(channel, user.getE());
            return null;
        }
        return () -> {
            if (RootSelector.delete(location, isDirectory)) {
                HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted.", ServerHandler.user(null, user.getT()),
                        ParametersMap.create().add("location", location).add("isDirectory", isDirectory));
                WListServer.ServerChannelHandler.write(channel, MessageProto.Success);
                return;
            }
            WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.LocationNotAvailable);
        };
    };

    public static final @NotNull ServerHandler doCreateDirectory = (channel, buffer) -> {
        final String token = ByteBufIOUtil.readUTF(buffer);
        final UnionPair<UserInformation, MessageProto> user = OperateSelfHandler.checkToken(token, UserPermission.FilesList, UserPermission.FileUpload);
        final FileLocation parent = FileLocation.parse(buffer);
        final String directoryName = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy policy = Options.DuplicatePolicy.of(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, OperationType.DeleteFileOrDirectory, user, () -> ParametersMap.create()
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
        return () -> {
            final UnionPair<FileInformation, FailureReason> information = RootSelector.createDirectory(parent, directoryName, policy);
            if (information.isFailure()) {
                WListServer.ServerChannelHandler.write(channel, OperateFilesHandler.Failure(information.getE()));
                return;
            }
            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Created directory.", ServerHandler.user(null, user.getT()),
                    ParametersMap.create().add("directory", information.getT()));
            WListServer.ServerChannelHandler.write(channel, MessageProto.successMessage(information.getT()::dumpVisible));
        };
    };

//    public static final @NotNull ServerHandler doRenameFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList, UserPermission.FileDownload, UserPermission.FileUpload, UserPermission.FileDelete);
//        final FileLocation location = FileLocation.parse(buffer);
//        final String name = ByteBufIOUtil.readUTF(buffer);
//        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, OperationType.RenameFile, user, () -> ParametersMap.create()
//                .add("location", location).add("name", name).add("duplicatePolicy", duplicatePolicy));
//        if (user.isFailure())
//            return user.getE();
//        if (duplicatePolicy == null)
//            return MessageProto.WrongParameters;
//        final UnionPair<FileInformation, FailureReason> file;
//        try {
//            file = RootSelector.getInstance().rename(location, name, duplicatePolicy);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (file.isFailure())
//            return switch (file.getE().kind()) {
//                case FailureReason.InvalidFilename -> OperateFilesHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFilesHandler.DuplicateError;
//                case FailureReason.NoSuchFile -> OperateFilesHandler.FileNotFound;
//                default -> throw new ServerException("Unknown failure reason. " + file.getE(), file.getE().throwable());
//            };
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Renamed.", ServerHandler.buildUserString(user.getT().id(), user.getT().username()),
//                ParametersMap.create().add("location", location).add("name", name));
//        return MessageProto.successMessage(buf -> {
//            FileInformation.dumpVisible(buf, file.getT());
//            return buf;
//        });
//    };
//
//    public static final @NotNull ServerHandler doRequestDownloadFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList, UserPermission.FileDownload);
//        final FileLocation location = FileLocation.parse(buffer);
//        final long from = ByteBufIOUtil.readVariableLenLong(buffer);
//        final long to = ByteBufIOUtil.readVariable2LenLong(buffer);
//        ServerHandler.logOperation(channel, OperationType.RequestDownloadFile, user, () -> ParametersMap.create()
//                .add("location", location).add("from", from).add("to", to));
//        if (user.isFailure())
//            return user.getE();
//        if (from < 0 || from >= to)
//            return MessageProto.WrongParameters;
//        final UnionPair<DownloadMethods, FailureReason> url;
//        try {
//            url = RootSelector.getInstance().download(location, from, to);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (url.isFailure()) {
//            if (FailureReason.NoSuchFile.equals(url.getE().kind()))
//                return OperateFilesHandler.FileNotFound;
//            throw new ServerException("Unknown failure reason. " + url.getE(), url.getE().throwable());
//        }
//        final String id = DownloadIdHelper.generateId(url.getT(), user.getT().username());
//        HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed download id for user: '", user.getT().username(), "' file: '", location, "' (", from, '-', to, ") id: ", id);
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeVariable2LenLong(buf, url.getT().total());
//            ByteBufIOUtil.writeUTF(buf, id);
//            return buf;
//        });
//    };
//
//    public static final @NotNull ServerHandler doDownloadFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FileDownload);
//        final String id = ByteBufIOUtil.readUTF(buffer);
//        final int chunk = ByteBufIOUtil.readVariableLenInt(buffer);
//        ServerHandler.logOperation(channel, OperationType.DownloadFile, user, () -> ParametersMap.create()
//                .add("id", id).add("chunk", chunk));
//        if (user.isFailure())
//            return user.getE();
//        final ByteBuf file;
//        try {
//            file = DownloadIdHelper.download(id, user.getT().username(), chunk);
//        } catch (final ServerException exception) {
//            throw exception;
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (file == null)
//            return OperateFilesHandler.InvalidId;
//        return new MessageProto(ResponseState.Success, buf ->
//                ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, buf, file));
//    };
//
//    public static final @NotNull ServerHandler doCancelDownloadFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FileDownload);
//        final String id = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, OperationType.CancelDownloadFile, user, () -> ParametersMap.create()
//                .add("id", id));
//        if (user.isFailure())
//            return user.getE();
//        return DownloadIdHelper.cancel(id, user.getT().username()) ? MessageProto.Success : MessageProto.DataError;
//    };
//
//    public static final @NotNull ServerHandler doRequestUploadFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList, UserPermission.FileUpload);
//        final FileLocation parentLocation = FileLocation.parse(buffer);
//        final String filename = ByteBufIOUtil.readUTF(buffer);
//        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
//        final String md5 = ByteBufIOUtil.readUTF(buffer);
//        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, OperationType.RequestUploadFile, user, () -> ParametersMap.create()
//                .add("parentLocation", parentLocation).add("filename", filename).add("size", size).add("md5", md5).add("duplicatePolicy", duplicatePolicy)
//                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().groupName().permissions().contains(UserPermission.FileDelete)));
//        if (user.isFailure())
//            return user.getE();
//        if (size < 0 || !HMessageDigestHelper.MD5.pattern.matcher(md5).matches() || duplicatePolicy == null)
//            return MessageProto.WrongParameters;
//        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().groupName().permissions().contains(UserPermission.FileDelete))
//            return MessageProto.NoPermission.apply(UserPermission.FileDelete);
//        final UnionPair<UploadMethods, FailureReason> methods;
//        try {
//            methods = RootSelector.getInstance().upload(parentLocation, filename, size, md5, duplicatePolicy);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (methods.isFailure())
//            return switch (methods.getE().kind()) {
//                case FailureReason.InvalidFilename -> OperateFilesHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFilesHandler.DuplicateError;
//                case FailureReason.ExceedMaxSize -> OperateFilesHandler.ExceedSize;
//                default -> throw new ServerException("Unknown failure reason. " + methods.getE(), methods.getE().throwable());
//            };
//        if (methods.getT().methods().isEmpty()) { // (reuse / empty file)
//            final FileInformation file;
//            try {
//                file = methods.getT().supplier().get();
//            } catch (final Exception exception) {
//                throw new ServerException(exception);
//            } finally {
//                methods.getT().finisher().run();
//            }
//            if (file == null)
//                return OperateFilesHandler.FileNotFound;
//            return MessageProto.successMessage(buf -> {
//                ByteBufIOUtil.writeBoolean(buf, true);
//                FileInformation.dumpVisible(buf, file);
//                return buf;
//            });
//        }
//        assert methods.getT().methods().size() == MiscellaneousUtil.calculatePartCount(size, NetworkTransmission.FileTransferBufferSize);
//        final String id = UploadIdHelper.generateId(methods.getT(), size, user.getT().username());
//        HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed upload id for user: '", user.getT().username(), "' parent: ", parentLocation, ", name: '", filename, "' (", size, "B) id: ", id);
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeBoolean(buf, false);
//            ByteBufIOUtil.writeUTF(buf, id);
//            return buf;
//        });
//    };
//
//    public static final @NotNull ServerHandler doUploadFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FileUpload);
//        final String id = ByteBufIOUtil.readUTF(buffer);
//        final int chunk = ByteBufIOUtil.readVariableLenInt(buffer);
//        ServerHandler.logOperation(channel, OperationType.UploadFile, user, () -> ParametersMap.create()
//                .add("id", id).add("chunk", chunk));
//        if (user.isFailure())
//            return user.getE();
//        final UnionPair<FileInformation, Boolean> information;
//        try {
//            information = UploadIdHelper.upload(id, user.getT().username(), buffer.duplicate(), chunk);
//        } catch (final ServerException exception) {
//            throw exception;
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (information == null)
//            return OperateFilesHandler.InvalidId;
//        if (information.isFailure() && information.getE().booleanValue())
//            return OperateFilesHandler.InvalidFile;
//        buffer.readerIndex(buffer.writerIndex());
//        final FileInformation file = information.isSuccess() ? information.getT() : null;
//        if (file != null)
//            HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Uploaded.", ServerHandler.buildUserString(user.getT().id(), user.getT().username()),
//                    ParametersMap.create().add("file", file));
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeBoolean(buf, file == null);
//            if (file != null)
//                FileInformation.dumpVisible(buf, file);
//            return buf;
//        });
//    };
//
//    public static final @NotNull ServerHandler doCancelUploadFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FileUpload);
//        final String id = ByteBufIOUtil.readUTF(buffer);
//        ServerHandler.logOperation(channel, OperationType.CancelUploadFile, user, () -> ParametersMap.create()
//                .add("id", id));
//        if (user.isFailure())
//            return user.getE();
//        return UploadIdHelper.cancel(id, user.getT().username()) ? MessageProto.Success : MessageProto.DataError;
//    };
//
//    public static final @NotNull ServerHandler doCopyFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList, UserPermission.FileUpload, UserPermission.FileDownload);
//        final FileLocation source = FileLocation.parse(buffer);
//        final FileLocation targetParent = FileLocation.parse(buffer);
//        final String filename = ByteBufIOUtil.readUTF(buffer);
//        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, OperationType.CopyFile, user, () -> ParametersMap.create()
//                .add("source", source).add("targetParent", targetParent).add("filename", filename).add("duplicatePolicy", duplicatePolicy)
//                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().groupName().permissions().contains(UserPermission.FileDelete)));
//        if (user.isFailure())
//            return user.getE();
//        if (duplicatePolicy == null)
//            return MessageProto.WrongParameters;
//        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().groupName().permissions().contains(UserPermission.FileDelete))
//            return MessageProto.NoPermission.apply(UserPermission.FileDelete);
//        final UnionPair<FileInformation, FailureReason> file;
//        try {
//            file = RootSelector.getInstance().copy(source, targetParent, filename, duplicatePolicy);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (file.isFailure())
//            return switch (file.getE().kind()) {
//                case FailureReason.InvalidFilename -> OperateFilesHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFilesHandler.DuplicateError;
//                case FailureReason.NoSuchFile -> OperateFilesHandler.FileNotFound;
//                default -> throw new ServerException("Unknown failure reason. " + file.getE(), file.getE().throwable());
//            };
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Copied.", ServerHandler.buildUserString(user.getT().id(), user.getT().username()),
//                ParametersMap.create().add("source", source).add("file", file));
//        return MessageProto.successMessage(buf -> {
//            FileInformation.dumpVisible(buf, file.getT());
//            return buf;
//        });
//    };
//
//    public static final @NotNull ServerHandler doMoveFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList, UserPermission.FileDownload, UserPermission.FileUpload, UserPermission.FileDelete);
//        final FileLocation source = FileLocation.parse(buffer);
//        final FileLocation target = FileLocation.parse(buffer);
//        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, OperationType.MoveFile, user, () -> ParametersMap.create()
//                .add("source", source).add("target", target).add("duplicatePolicy", duplicatePolicy));
//        if (user.isFailure())
//            return user.getE();
//        if (duplicatePolicy == null)
//            return MessageProto.WrongParameters;
//        final UnionPair<FileInformation, FailureReason> file;
//        try {
//            file = RootSelector.getInstance().move(source, target, duplicatePolicy);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (file.isFailure())
//            return switch (file.getE().kind()) {
//                case FailureReason.InvalidFilename -> OperateFilesHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFilesHandler.DuplicateError;
//                case FailureReason.NoSuchFile -> OperateFilesHandler.FileNotFound;
//                default -> throw new ServerException("Unknown failure reason. " + file.getE(), file.getE().throwable());
//            };
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Moved.", ServerHandler.buildUserString(user.getT().id(), user.getT().username()),
//                ParametersMap.create().add("source", source).add("file", file));
//        return MessageProto.successMessage(buf -> {
//            FileInformation.dumpVisible(buf, file.getT());
//            return buf;
//        });
//    };
}
