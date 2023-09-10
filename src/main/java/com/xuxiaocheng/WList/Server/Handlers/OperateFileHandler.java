package com.xuxiaocheng.WList.Server.Handlers;

import com.xuxiaocheng.WList.Commons.Operations.ResponseState;
import com.xuxiaocheng.WList.Server.MessageProto;
import org.jetbrains.annotations.NotNull;

public final class OperateFileHandler {
    private OperateFileHandler() {
        super();
    }

    public static final @NotNull MessageProto FileNotFound = MessageProto.composeMessage(ResponseState.DataError, "File");
    public static final @NotNull MessageProto InvalidFilename = MessageProto.composeMessage(ResponseState.DataError, "Filename");
    public static final @NotNull MessageProto DuplicateError = MessageProto.composeMessage(ResponseState.DataError, "Duplicate");
    public static final @NotNull MessageProto ExceedSize = MessageProto.composeMessage(ResponseState.DataError, "Size");
    public static final @NotNull MessageProto InvalidId = MessageProto.composeMessage(ResponseState.DataError, "Id");
    public static final @NotNull MessageProto InvalidFile = MessageProto.composeMessage(ResponseState.DataError, "Content");

    public static void initialize() {
//        ServerHandlerManager.register(OperationType.ListFiles, OperateFileHandler.doListFiles);
//        ServerHandlerManager.register(OperationType.CreateDirectory, OperateFileHandler.doCreateDirectory);
//        ServerHandlerManager.register(OperationType.DeleteFile, OperateFileHandler.doDeleteFile);
//        ServerHandlerManager.register(OperationType.RenameFile, OperateFileHandler.doRenameFile);
//        ServerHandlerManager.register(OperationType.RequestDownloadFile, OperateFileHandler.doRequestDownloadFile);
//        ServerHandlerManager.register(OperationType.DownloadFile, OperateFileHandler.doDownloadFile);
//        ServerHandlerManager.register(OperationType.CancelDownloadFile, OperateFileHandler.doCancelDownloadFile);
//        ServerHandlerManager.register(OperationType.RequestUploadFile, OperateFileHandler.doRequestUploadFile);
//        ServerHandlerManager.register(OperationType.UploadFile, OperateFileHandler.doUploadFile);
//        ServerHandlerManager.register(OperationType.CancelUploadFile, OperateFileHandler.doCancelUploadFile);
//        ServerHandlerManager.register(OperationType.CopyFile, OperateFileHandler.doCopyFile);
//        ServerHandlerManager.register(OperationType.MoveFile, OperateFileHandler.doMoveFile);
    }

//    public static final @NotNull ServerHandler doListFiles = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList);
//        final FileLocation location = FileLocation.parse(buffer);
//        final Options.DirectoriesOrFiles filter = Options.valueOfDirectoriesOrFiles(ByteBufIOUtil.readByte(buffer));
//        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
//        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
//        final Options.OrderPolicy orderPolicy = Options.valueOfOrderPolicy(ByteBufIOUtil.readUTF(buffer));
//        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
//        final boolean refresh = ByteBufIOUtil.readBoolean(buffer);
//        ServerHandler.logOperation(channel, OperationType.ListFiles, user, () -> ParametersMap.create()
//                .add("location", location).add("filter", filter).add("limit", limit).add("page", page)
//                .add("orderPolicy", orderPolicy).add("orderDirection", orderDirection).add("refresh", refresh)
//                .optionallyAddSupplier(refresh && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(UserPermission.FilesBuildIndex)));
//        if (user.isFailure())
//            return user.getE();
//        if (limit < 1 || limit > ServerConfiguration.getInstance().maxLimitPerPage()
//                || page < 0 || orderPolicy == null || orderDirection == null || filter == null)
//            return MessageProto.WrongParameters;
//        if (refresh && !user.getT().group().permissions().contains(UserPermission.FilesBuildIndex))
//            return MessageProto.NoPermission.apply(UserPermission.FilesBuildIndex);
//        final Triad.ImmutableTriad<Long, Long, List<FileInformation>> list;
//        try {
//            if (refresh)
//                RootSelector.getInstance().forceRefreshDirectory(location);
//            // TODO with groups
//            list = RootSelector.getInstance().list(location, filter, limit, page, orderPolicy, orderDirection);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (list == null)
//            return OperateFileHandler.FileNotFound;
//        return MessageProto.successMessage(buf -> {
//            ByteBufIOUtil.writeVariableLenLong(buf, list.getA().longValue());
//            ByteBufIOUtil.writeVariableLenLong(buf, list.getB().longValue());
//            ByteBufIOUtil.writeVariableLenInt(buf, list.getC().size());
//            for (final FileInformation information: list.getC())
//                FileInformation.dumpVisible(buf, information);
//            return buf;
//        });
//    };
//
//    public static final @NotNull ServerHandler doCreateDirectory = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList, UserPermission.FileUpload);
//        final FileLocation parentLocation = FileLocation.parse(buffer);
//        final String directoryName = ByteBufIOUtil.readUTF(buffer);
//        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
//        ServerHandler.logOperation(channel, OperationType.CreateDirectory, user, () -> ParametersMap.create()
//                .add("parentLocation", parentLocation).add("directoryName", directoryName).add("duplicatePolicy", duplicatePolicy)
//                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(UserPermission.FileDelete)));
//        if (user.isFailure())
//            return user.getE();
//        if (duplicatePolicy == null)
//            return MessageProto.WrongParameters;
//        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(UserPermission.FileDelete))
//            return MessageProto.NoPermission.apply(UserPermission.FileDelete);
//        final UnionPair<FileInformation, FailureReason> directory;
//        try {
//            directory = RootSelector.getInstance().createDirectory(parentLocation, directoryName, duplicatePolicy);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        if (directory.isFailure())
//            return switch (directory.getE().kind()) {
//                case FailureReason.InvalidFilename -> OperateFileHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFileHandler.DuplicateError;
//                default -> throw new ServerException("Unknown failure reason. " + directory.getE(), directory.getE().throwable());
//            };
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Created directory.", ServerHandler.buildUserString(user.getT().id(), user.getT().username()),
//                ParametersMap.create().add("directory", directory.getT()));
//        return MessageProto.successMessage(buf -> {
//            FileInformation.dumpVisible(buf, directory.getT());
//            return buf;
//        });
//    };
//
//    public static final @NotNull ServerHandler doDeleteFile = (channel, buffer) -> {
//        final UnionPair<UserInformation, MessageProto> user = OperateUsersHandler.checkToken(buffer, UserPermission.FilesList, UserPermission.FileDelete);
//        final FileLocation location = FileLocation.parse(buffer);
//        ServerHandler.logOperation(channel, OperationType.DeleteFile, user, () -> ParametersMap.create()
//                .add("location", location));
//        if (user.isFailure())
//            return user.getE();
//        try {
//            RootSelector.getInstance().delete(location);
//        } catch (final UnsupportedOperationException exception) {
//            return MessageProto.Unsupported.apply(exception);
//        } catch (final Exception exception) {
//            throw new ServerException(exception);
//        }
//        HLog.getInstance("ServerLogger").log(HLogLevel.FINE, "Deleted.", ServerHandler.buildUserString(user.getT().id(), user.getT().username()),
//                ParametersMap.create().add("location", location));
//        return MessageProto.Success;
//    };
//
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
//                case FailureReason.InvalidFilename -> OperateFileHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFileHandler.DuplicateError;
//                case FailureReason.NoSuchFile -> OperateFileHandler.FileNotFound;
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
//                return OperateFileHandler.FileNotFound;
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
//            return OperateFileHandler.InvalidId;
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
//                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(UserPermission.FileDelete)));
//        if (user.isFailure())
//            return user.getE();
//        if (size < 0 || !HMessageDigestHelper.MD5.pattern.matcher(md5).matches() || duplicatePolicy == null)
//            return MessageProto.WrongParameters;
//        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(UserPermission.FileDelete))
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
//                case FailureReason.InvalidFilename -> OperateFileHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFileHandler.DuplicateError;
//                case FailureReason.ExceedMaxSize -> OperateFileHandler.ExceedSize;
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
//                return OperateFileHandler.FileNotFound;
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
//            return OperateFileHandler.InvalidId;
//        if (information.isFailure() && information.getE().booleanValue())
//            return OperateFileHandler.InvalidFile;
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
//                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(UserPermission.FileDelete)));
//        if (user.isFailure())
//            return user.getE();
//        if (duplicatePolicy == null)
//            return MessageProto.WrongParameters;
//        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(UserPermission.FileDelete))
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
//                case FailureReason.InvalidFilename -> OperateFileHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFileHandler.DuplicateError;
//                case FailureReason.NoSuchFile -> OperateFileHandler.FileNotFound;
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
//                case FailureReason.InvalidFilename -> OperateFileHandler.InvalidFilename;
//                case FailureReason.DuplicatePolicyError -> OperateFileHandler.DuplicateError;
//                case FailureReason.NoSuchFile -> OperateFileHandler.FileNotFound;
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
