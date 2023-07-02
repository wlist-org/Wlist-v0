package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Driver.FailureReason;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Server.Driver.RootDriver;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.DownloadMethods;
import com.xuxiaocheng.WList.Server.MessageProto;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.UploadMethods;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.FileDownloadIdHelper;
import com.xuxiaocheng.WList.Server.ServerHandlers.Helpers.FileUploadIdHelper;
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
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final Options.OrderPolicy orderPolicy = Options.valueOfOrderPolicy(ByteBufIOUtil.readUTF(buffer));
        final Options.OrderDirection orderDirection = Options.valueOfOrderDirection(ByteBufIOUtil.readUTF(buffer));
        final boolean refresh = ByteBufIOUtil.readBoolean(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.ListFiles, user, () -> ParametersMap.<String, Object>create()
                .add("path", path).add("limit", limit).add("page", page).add("orderPolicy", orderPolicy).add("orderDirection", orderDirection).add("refresh", refresh)
                .optionallyAddSupplier(refresh && user.isSuccess(), () -> "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FilesBuildIndex)));
        if (user.isFailure())
            return user.getE();
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage()
                || page < 0 || orderPolicy == null || orderDirection == null)
            return ServerHandler.WrongParameters;
        if (refresh && !user.getT().group().permissions().contains(Operation.Permission.FilesBuildIndex))
            return ServerHandler.NoPermission;
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> list;
        try {
            if (refresh)
                RootDriver.getInstance().forceRefreshDirectory(path);
            // TODO with groups
            list = RootDriver.getInstance().list(path, limit, page, orderPolicy, orderDirection);
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
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.MakeDirectories, user, () -> ParametersMap.<String, Object>create()
                .add("path", path).add("duplicatePolicy", duplicatePolicy)
                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), () -> "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FileDelete)));
        if (user.isFailure())
            return user.getE();
        if (duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(Operation.Permission.FileDelete))
            return ServerHandler.NoPermission;
        final UnionPair<FileSqlInformation, FailureReason> dir;
        try {
            dir = RootDriver.getInstance().mkdirs(path, duplicatePolicy);
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
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.DeleteFile, user, () -> ParametersMap.<String, Object>create()
                .add("path", path));
        if (user.isFailure())
            return user.getE();
        try {
            RootDriver.getInstance().delete(path);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doRenameFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload, Operation.Permission.FileUpload, Operation.Permission.FileDelete);
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final String name = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.RenameFile, user, () -> ParametersMap.<String, Object>create()
                .add("path", path).add("name", name).add("duplicatePolicy", duplicatePolicy));
        if (user.isFailure())
            return user.getE();
        if (duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        final UnionPair<FileSqlInformation, FailureReason> file;
        try {
            file = RootDriver.getInstance().rename(path, name, duplicatePolicy);
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
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final long from = ByteBufIOUtil.readVariableLenLong(buffer);
        final long to = ByteBufIOUtil.readVariable2LenLong(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.RequestDownloadFile, user, () -> ParametersMap.<String, Object>create()
                .add("path", path).add("from", from).add("to", to));
        if (user.isFailure())
            return user.getE();
        if (from < 0 || from >= to)
            return ServerHandler.WrongParameters;
        final DownloadMethods url;
        try {
            url = RootDriver.getInstance().download(path, from, to);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.Unsupported.apply(exception);
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (url == null)
            return ServerFileHandler.FileNotFound;
        final String id = FileDownloadIdHelper.generateId(url, user.getT().username());
        HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed download id for user: ", user.getT().username(), " file: '", path, "' (", from, '-', to, ") id: ", id);
        return ServerHandler.successMessage(buf -> {
            ByteBufIOUtil.writeVariable2LenLong(buf, url.total());
            ByteBufIOUtil.writeUTF(buf, id);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doDownloadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileDownload);
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int chunk = ByteBufIOUtil.readVariableLenInt(buffer);
        ServerHandler.logOperation(channel.id(), Operation.Type.DownloadFile, user, () -> ParametersMap.<String, Object>create()
                .add("id", id).add("chunk", chunk));
        if (user.isFailure())
            return user.getE();
        final ByteBuf file;
        try {
            file = FileDownloadIdHelper.download(id, user.getT().username(), chunk);
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
        ServerHandler.logOperation(channel.id(), Operation.Type.CancelDownloadFile, user, () -> ParametersMap.<String, Object>create()
                .add("id", id));
        if (user.isFailure())
            return user.getE();
        return FileDownloadIdHelper.cancel(id, user.getT().username()) ? ServerHandler.Success : ServerHandler.DataError;
    };

    public static final @NotNull ServerHandler doRequestUploadFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final long size = ByteBufIOUtil.readVariable2LenLong(buffer);
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.RequestUploadFile, user, () -> ParametersMap.<String, Object>create()
                .add("path", path).add("size", size).add("md5", md5).add("duplicatePolicy", duplicatePolicy)
                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), () -> "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FileDelete)));
        if (user.isFailure())
            return user.getE();
        if (size < 0 || !MiscellaneousUtil.md5Pattern.matcher(md5).matches() || duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(Operation.Permission.FileDelete))
            return ServerHandler.NoPermission;
        final UnionPair<UploadMethods, FailureReason> methods;
        try {
            methods = RootDriver.getInstance().upload(path, size, md5, duplicatePolicy);
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
        final String id = FileUploadIdHelper.generateId(methods.getT(), size, user.getT().username());
        HLog.getInstance("ServerLogger").log(HLogLevel.LESS, "Signed upload id for user: ", user.getT().username(), " file: '", path, "' (", size, "B) id: ", id);
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
        ServerHandler.logOperation(channel.id(), Operation.Type.UploadFile, user, () -> ParametersMap.<String, Object>create()
                .add("id", id).add("chunk", chunk));
        if (user.isFailure())
            return user.getE();
        final UnionPair<FileSqlInformation, Boolean> information;
        try {
            information = FileUploadIdHelper.upload(id, user.getT().username(), buffer.duplicate(), chunk);
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
        ServerHandler.logOperation(channel.id(), Operation.Type.CancelUploadFile, user, () -> ParametersMap.<String, Object>create()
                .add("id", id));
        if (user.isFailure())
            return user.getE();
        return FileUploadIdHelper.cancel(id, user.getT().username()) ? ServerHandler.Success : ServerHandler.DataError;
    };

    public static final @NotNull ServerHandler doCopyFile = (channel, buffer) -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload);
        final DrivePath source = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final DrivePath target = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.CopyFile, user, () -> ParametersMap.<String, Object>create()
                .add("source", source).add("target", target).add("duplicatePolicy", duplicatePolicy)
                .optionallyAddSupplier(duplicatePolicy == Options.DuplicatePolicy.OVER && user.isSuccess(), () -> "allow", () -> user.getT().group().permissions().contains(Operation.Permission.FileDelete)));
        if (user.isFailure())
            return user.getE();
        if (duplicatePolicy == null)
            return ServerHandler.WrongParameters;
        if (duplicatePolicy == Options.DuplicatePolicy.OVER && !user.getT().group().permissions().contains(Operation.Permission.FileDelete))
            return ServerHandler.NoPermission;
        final UnionPair<FileSqlInformation, FailureReason> file;
        try {
            file = RootDriver.getInstance().copy(source, target, duplicatePolicy);
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
        final DrivePath source = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final DrivePath target = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final Options.DuplicatePolicy duplicatePolicy = Options.valueOfDuplicatePolicy(ByteBufIOUtil.readUTF(buffer));
        ServerHandler.logOperation(channel.id(), Operation.Type.MoveFile, user, () -> ParametersMap.<String, Object>create()
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
