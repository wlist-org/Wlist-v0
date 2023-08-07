package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Helpers.HMessageDigestHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.FileLocation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadIdHelper;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadIdHelper;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ServerFileHandler {
    private ServerFileHandler() {
        super();
    }

    public static final @NotNull MessageProto FileNotFound = ServerHandler.composeMessage(Operation.State.DataError, "File");
    public static final @NotNull MessageProto InvalidFilename = ServerHandler.composeMessage(Operation.State.DataError, "Filename");
    public static final @NotNull MessageProto DuplicateError = ServerHandler.composeMessage(Operation.State.DataError, "Duplicate");
    public static final @NotNull MessageProto ExceedSize = ServerHandler.composeMessage(Operation.State.DataError, "Size");
    public static final @NotNull MessageProto InvalidId = ServerHandler.composeMessage(Operation.State.DataError, "Id");
    public static final @NotNull MessageProto InvalidFile = ServerHandler.composeMessage(Operation.State.DataError, "Content");

    public static void initialize() {
        ServerHandlerManager.register(Operation.Type.ListFiles, ServerFileHandler.doListFiles);
        ServerHandlerManager.register(Operation.Type.MakeDirectories, ServerFileHandler.doMakeDirectories);
        ServerHandlerManager.register(Operation.Type.DeleteFile, ServerFileHandler.doDeleteFile);
        ServerHandlerManager.register(Operation.Type.RenameFile, ServerFileHandler.doRenameFile);
        ServerHandlerManager.register(Operation.Type.RequestDownloadFile, ServerFileHandler.doRequestDownloadFile);
        ServerHandlerManager.register(Operation.Type.DownloadFile, ServerFileHandler.doDownloadFile);
        ServerHandlerManager.register(Operation.Type.CancelDownloadFile, ServerFileHandler.doCancelDownloadFile);
        ServerHandlerManager.register(Operation.Type.RequestUploadFile, ServerFileHandler.doRequestUploadFile);
        ServerHandlerManager.register(Operation.Type.UploadFile, ServerFileHandler.doUploadFile);
        ServerHandlerManager.register(Operation.Type.CancelUploadFile, ServerFileHandler.doCancelUploadFile);
        ServerHandlerManager.register(Operation.Type.CopyFile, ServerFileHandler.doCopyFile);
        ServerHandlerManager.register(Operation.Type.MoveFile, ServerFileHandler.doMoveFile);
    }

    public static final @NotNull ServerHandler doListFiles = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList);
        final FileLocation location = FileLocation.parse(buffer);
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final Options.OrderPolicy orderPolicy = Options.valueOfOrderPolicy(ByteBufIOUtil.readUTF(buffer));
        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
        final Options.DirectoriesOrFiles filter = Options.valueOfDirectoriesOrFiles(ByteBufIOUtil.readByte(buffer));
        final boolean refresh = ByteBufIOUtil.readBoolean(buffer);
        ServerHandler.logOperation(channel, Operation.Type.ListFiles, user, () -> ParametersMap.create()
                .add("location", location).add("limit", limit).add("page", page)
                .add("orderPolicy", orderPolicy).add("orderDirection", orderDirection).add("filter", filter).add("refresh", refresh)
                .optionallyAddSupplier(refresh && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FilesBuildIndex)));
        if (user.isFailure())
            return user.getE();
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage()
                || page < 0 || orderPolicy == null || orderDirection == null || filter == null)
            return ServerHandler.WrongParameters;
        if (refresh && !user.getT().group().permissions().contains(Operation.Permission.FilesBuildIndex))
            return ServerHandler.NoPermission;
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> list;
        try {
            if (refresh)
                RootDriver.getInstance().forceRefreshDirectory(location);
            // TODO with groups
            list = RootDriver.getInstance().list(location, limit, page, orderPolicy, orderDirection, filter);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (list == null)
            return ServerFileHandler.FileNotFound;
        return ServerHandler.successMessage(buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
            ByteBufIOUtil.writeVariableLenInt(buf, list.getSecond().size());
            for (final FileSqlInformation information: list.getSecond())
                FileSqlInformation.dumpVisible(buf, information);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doMakeDirectories = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        final FileLocation parentLocation = FileLocation.parse(buffer);
        final String directoryName = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, Operation.Type.MakeDirectories, user, () -> ParametersMap.create()
                .add("parentLocation", parentLocation).add("directoryName", directoryName).add("duplicatePolicy", duplicatePolicy)
                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FileDelete)));
        if (user.isFailure())
            return user.getE();
        if (duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(Operation.Permission.FileDelete))
            return ServerHandler.NoPermission;
        final UnionPair<FileSqlInformation, FailureReason> dir;
        try {
            dir = RootDriver.getInstance().createDirectory(parentLocation, directoryName, duplicatePolicy);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (dir.isFailure())
            return switch (dir.getE().kind()) {
                case FailureReason.InvalidFilename -> ServerFileHandler.InvalidFilename;
                case FailureReason.DuplicatePolicyError -> ServerFileHandler.DuplicateError;
                default -> throw new ServerException("Unknown failure reason. " + dir.getE(), dir.getE().throwable());
            };
        return ServerHandler.successMessage(buf -> {
            FileSqlInformation.dumpVisible(buf, dir.getT());
            return buf;
        });
    };

    public static final @NotNull ServerHandler doDeleteFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDelete);
        final FileLocation location = FileLocation.parse(buffer);
        ServerHandler.logOperation(channel, Operation.Type.DeleteFile, user, () -> ParametersMap.create()
                .add("location", location));
        if (user.isFailure())
            return user.getE();
        try {
            RootDriver.getInstance().delete(location);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doRenameFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload, Operation.Permission.FileUpload, Operation.Permission.FileDelete);
        final FileLocation location = FileLocation.parse(buffer);
        final String name = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, Operation.Type.RenameFile, user, () -> ParametersMap.create()
                .add("location", location).add("name", name).add("duplicatePolicy", duplicatePolicy));
        if (user.isFailure())
            return user.getE();
        if (duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        final UnionPair<FileSqlInformation, FailureReason> file;
        try {
            file = RootDriver.getInstance().rename(location, name, duplicatePolicy);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file.isFailure())
            return switch (file.getE().kind()) {
                case FailureReason.InvalidFilename -> ServerFileHandler.InvalidFilename;
                case FailureReason.DuplicatePolicyError -> ServerFileHandler.DuplicateError;
                case FailureReason.NoSuchFile -> ServerFileHandler.FileNotFound;
                default -> throw new ServerException("Unknown failure reason. " + file.getE(), file.getE().throwable());
            };
        return ServerHandler.successMessage(buf -> {
            FileSqlInformation.dumpVisible(buf, file.getT());
            return buf;
        });
    };

    public static final @NotNull ServerHandler doRequestDownloadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload);
        final FileLocation location = FileLocation.parse(buffer);
        final long from = ByteBufIOUtil.readVariableLenLong(buffer);
        final long to = ByteBufIOUtil.readVariable2LenLong(buffer);
        ServerHandler.logOperation(channel, Operation.Type.RequestDownloadFile, user, () -> ParametersMap.create()
                .add("location", location).add("from", from).add("to", to));
        if (user.isFailure())
            return user.getE();
        if (from < 0 || from >= to)
            return ServerHandler.WrongParameters;
        final UnionPair<DownloadMethods, FailureReason> url;
        try {
            url = RootDriver.getInstance().download(location, from, to);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (url.isFailure()) {
            if (FailureReason.NoSuchFile.equals(url.getE().kind()))
                return ServerFileHandler.FileNotFound;
            throw new ServerException("Unknown failure reason. " + url.getE(), url.getE().throwable());
        }
        final String id = DownloadIdHelper.generateId(url.getT(), user.getT().username());
        HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed download id for user: '", user.getT().username(), "' file: '", location, "' (", from, '-', to, ") id: ", id);
        return ServerHandler.successMessage(buf -> {
            ByteBufIOUtil.writeVariable2LenLong(buf, url.getT().total());
            ByteBufIOUtil.writeUTF(buf, id);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doDownloadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileDownload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int chunk = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, Operation.Type.DownloadFile, user, () -> ParametersMap.create()
                .add("id", id).add("chunk", chunk));
        if (user.isFailure())
            return user.getE();
        final ByteBuf file;
        try {
            file = DownloadIdHelper.download(id, user.getT().username(), chunk);
        } catch (final ServerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerFileHandler.InvalidId;
        return new MessageProto(ServerHandler.defaultFileCipher, Operation.State.Success, buf ->
                ByteBufAllocator.DEFAULT.compositeBuffer(2).addComponents(true, buf, file));
    };

    public static final @NotNull ServerHandler doCancelDownloadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileDownload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, Operation.Type.CancelDownloadFile, user, () -> ParametersMap.create()
                .add("id", id));
        if (user.isFailure())
            return user.getE();
        return DownloadIdHelper.cancel(id, user.getT().username()) ? ServerHandler.Success : ServerHandler.DataError;
    };

    public static final @NotNull ServerHandler doRequestUploadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        final FileLocation parentLocation = FileLocation.parse(buffer);
        final String filename = ByteBufIOUtil.readUTF(buffer);
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, Operation.Type.RequestUploadFile, user, () -> ParametersMap.create()
                .add("parentLocation", parentLocation).add("filename", filename).add("size", size).add("md5", md5).add("duplicatePolicy", duplicatePolicy)
                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FileDelete)));
        if (user.isFailure())
            return user.getE();
        if (size < 0 || !HMessageDigestHelper.MD5.pattern.matcher(md5).matches() || duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(Operation.Permission.FileDelete))
            return ServerHandler.NoPermission;
        final UnionPair<UploadMethods, FailureReason> methods;
        try {
            methods = RootDriver.getInstance().upload(parentLocation, filename, size, md5, duplicatePolicy);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (methods.isFailure())
            return switch (methods.getE().kind()) {
                case FailureReason.InvalidFilename -> ServerFileHandler.InvalidFilename;
                case FailureReason.DuplicatePolicyError -> ServerFileHandler.DuplicateError;
                case FailureReason.ExceedMaxSize -> ServerFileHandler.ExceedSize;
                default -> throw new ServerException("Unknown failure reason. " + methods.getE(), methods.getE().throwable());
            };
        if (methods.getT().methods().isEmpty()) { // (reuse / empty file)
            final FileSqlInformation file;
            try {
                file = methods.getT().supplier().get();
            } catch (final Exception exception) {
                throw new ServerException(exception);
            } finally {
                methods.getT().finisher().run();
            }
            if (file == null)
                return ServerFileHandler.FileNotFound;
            return ServerHandler.successMessage(buf -> {
                ByteBufIOUtil.writeBoolean(buf, true);
                FileSqlInformation.dumpVisible(buf, file);
                return buf;
            });
        }
        assert methods.getT().methods().size() == MiscellaneousUtil.calculatePartCount(size, WListServer.FileTransferBufferSize);
        final String id = UploadIdHelper.generateId(methods.getT(), size, user.getT().username());
        HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed upload id for user: '", user.getT().username(), "' parent: ", parentLocation, ", name: '", filename, "' (", size, "B) id: ", id);
        return ServerHandler.successMessage(buf -> {
            ByteBufIOUtil.writeBoolean(buf, false);
            ByteBufIOUtil.writeUTF(buf, id);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doUploadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileUpload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int chunk = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel, Operation.Type.UploadFile, user, () -> ParametersMap.create()
                .add("id", id).add("chunk", chunk));
        if (user.isFailure())
            return user.getE();
        final UnionPair<FileSqlInformation, Boolean> information;
        try {
            information = UploadIdHelper.upload(id, user.getT().username(), buffer.duplicate(), chunk);
        } catch (final ServerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (information == null)
            return ServerFileHandler.InvalidId;
        if (information.isFailure() && information.getE().booleanValue())
            return ServerFileHandler.InvalidFile;
        buffer.readerIndex(buffer.writerIndex());
        final FileSqlInformation file = information.isSuccess() ? information.getT() : null;
        return ServerHandler.successMessage(buf -> {
            ByteBufIOUtil.writeBoolean(buf, file == null);
            if (file != null)
                FileSqlInformation.dumpVisible(buf, file);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doCancelUploadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileUpload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        ServerHandler.logOperation(channel, Operation.Type.CancelUploadFile, user, () -> ParametersMap.create()
                .add("id", id));
        if (user.isFailure())
            return user.getE();
        return UploadIdHelper.cancel(id, user.getT().username()) ? ServerHandler.Success : ServerHandler.DataError;
    };

    public static final @NotNull ServerHandler doCopyFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload);
        final FileLocation source = FileLocation.parse(buffer);
        final FileLocation targetParent = FileLocation.parse(buffer);
        final String filename = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, Operation.Type.CopyFile, user, () -> ParametersMap.create()
                .add("source", source).add("targetParent", targetParent).add("filename", filename).add("duplicatePolicy", duplicatePolicy)
                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FileDelete)));
        if (user.isFailure())
            return user.getE();
        if (duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(Operation.Permission.FileDelete))
            return ServerHandler.NoPermission;
        final UnionPair<FileSqlInformation, FailureReason> file;
        try {
            file = RootDriver.getInstance().copy(source, targetParent, filename, duplicatePolicy);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file.isFailure())
            return switch (file.getE().kind()) {
                case FailureReason.InvalidFilename -> ServerFileHandler.InvalidFilename;
                case FailureReason.DuplicatePolicyError -> ServerFileHandler.DuplicateError;
                case FailureReason.NoSuchFile -> ServerFileHandler.FileNotFound;
                default -> throw new ServerException("Unknown failure reason. " + file.getE(), file.getE().throwable());
            };
        return ServerHandler.successMessage(buf -> {
            FileSqlInformation.dumpVisible(buf, file.getT());
            return buf;
        });
    };

    public static final @NotNull ServerHandler doMoveFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload, Operation.Permission.FileUpload, Operation.Permission.FileDelete);
        final FileLocation source = FileLocation.parse(buffer);
        final FileLocation target = FileLocation.parse(buffer);
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel, Operation.Type.MoveFile, user, () -> ParametersMap.create()
                .add("source", source).add("target", target).add("duplicatePolicy", duplicatePolicy));
        if (user.isFailure())
            return user.getE();
        if (duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        final UnionPair<FileSqlInformation, FailureReason> file;
        try {
            file = RootDriver.getInstance().move(source, target, duplicatePolicy);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file.isFailure())
            return switch (file.getE().kind()) {
                case FailureReason.InvalidFilename -> ServerFileHandler.InvalidFilename;
                case FailureReason.DuplicatePolicyError -> ServerFileHandler.DuplicateError;
                case FailureReason.NoSuchFile -> ServerFileHandler.FileNotFound;
                default -> throw new ServerException("Unknown failure reason. " + file.getE(), file.getE().throwable());
            };
        return ServerHandler.successMessage(buf -> {
            FileSqlInformation.dumpVisible(buf, file.getT());
            return buf;
        });
    };
}
