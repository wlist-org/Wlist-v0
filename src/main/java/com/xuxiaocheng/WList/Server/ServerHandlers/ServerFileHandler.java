package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Server.Databases.User.UserSqlInformation;
import com.xuxiaocheng.WList.Server.Driver.RootDriver;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.Polymers.MessageProto;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Server.ServerCodecs.MessageCiphers;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ServerFileHandler {
    private ServerFileHandler() {
        super();
    }

    static @NotNull Map<String, Object> getVisibleInfo(final @NotNull FileSqlInformation f) {
        final Map<String, Object> map = new HashMap<>(6);
        map.put("path", f.path().getPath());
        map.put("is_dir", f.is_dir());
        map.put("size", f.size());
        if (f.createTime() != null)
            map.put("create_time", f.createTime().format(DateTimeFormatter.ISO_DATE_TIME));
        if (f.updateTime() != null)
            map.put("update_time", f.updateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        map.put("md5", f.md5());
        return map;
    }

    public static final @NotNull MessageProto FileNotFound = ServerHandler.composeMessage(Operation.State.DataError, "File");

    public static final @NotNull ServerHandler doListFiles = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final OrderDirection orderDirection = OrderDirection.Map.get(ByteBufIOUtil.readUTF(buffer));
        final OrderPolicy orderPolicy = OrderPolicy.Map.get(ByteBufIOUtil.readUTF(buffer));
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage()
                || page < 0 || orderDirection == null || orderPolicy == null)
            return ServerHandler.WrongParameters;
        final Pair.ImmutablePair<Long, List<FileSqlInformation>> list;
        try {
            list = RootDriver.getInstance().list(path, limit, page, orderDirection, orderPolicy);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (list == null)
            return ServerFileHandler.FileNotFound;
        final String json = JSON.toJSONString(list.getSecond().stream()
                .map(ServerFileHandler::getVisibleInfo).collect(Collectors.toList()));
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, list.getFirst().longValue());
            ByteBufIOUtil.writeUTF(buf, json);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doMakeDirectories = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final FileSqlInformation dir;
        try {
            dir = RootDriver.getInstance().mkdirs(path);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (dir == null)
            return ServerFileHandler.FileNotFound;
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(dir)));
    };

    public static final @NotNull ServerHandler doDeleteFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDelete);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        try {
            RootDriver.getInstance().delete(path);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        return ServerHandler.Success;
    };

    public static final @NotNull ServerHandler doRenameFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload, Operation.Permission.FileUpload, Operation.Permission.FileDelete);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final String name = ByteBufIOUtil.readUTF(buffer);
        final FileSqlInformation file;
        try {
            file = RootDriver.getInstance().rename(path, name);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerFileHandler.FileNotFound;
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    };

    public static final @NotNull ServerHandler doRequestDownloadFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final long from = ByteBufIOUtil.readVariableLenLong(buffer);
        final long to = ByteBufIOUtil.readVariableLenLong(buffer);
        if (from < 0 || from >= to)
            return ServerHandler.WrongParameters;
        // TODO: download methods.
        final Pair.ImmutablePair<InputStream, Long> url;
        try {
            url = RootDriver.getInstance().download(path, from, to);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (url == null)
            return ServerFileHandler.FileNotFound;
        final String id = FileDownloadIdHelper.generateId(url.getFirst(), user.getT().username());
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, url.getSecond().longValue());
            ByteBufIOUtil.writeVariableLenInt(buf, (int) Math.ceil((double) url.getSecond().longValue() / WListServer.FileTransferBufferSize));
            ByteBufIOUtil.writeUTF(buf, id);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doDownloadFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        final Pair.ImmutablePair<Integer, ByteBuf> file;
        try {
            file = FileDownloadIdHelper.download(id, user.getT().username());
        } catch (final InterruptedException | IOException | ExecutionException exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerHandler.DataError;
        return new MessageProto(MessageCiphers.defaultDoGZip, Operation.State.Success, (buf) -> {
            ByteBufIOUtil.writeVariableLenInt(buf, file.getFirst().intValue());
            final CompositeByteBuf composite = ByteBufAllocator.DEFAULT.compositeBuffer(2);
            composite.addComponents(true, buf, file.getSecond());
            return composite;
        });
    };

    public static final @NotNull ServerHandler doCancelDownloadFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        return FileDownloadIdHelper.cancel(id, user.getT().username()) ? ServerHandler.Success : ServerHandler.DataError;
    };

    public static final @NotNull ServerHandler doRequestUploadFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final long size = ByteBufIOUtil.readVariableLenLong(buffer);
        final String md5 = ByteBufIOUtil.readUTF(buffer);
        if (size < 0 || !MiscellaneousUtil.md5Pattern.matcher(md5).matches())
            return ServerHandler.WrongParameters;
        final UploadMethods methods;
        try {
            methods = RootDriver.getInstance().upload(path, size, md5);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (methods == null)
            return ServerFileHandler.FileNotFound;
        if (methods.methods().isEmpty()) { // (reuse / empty file)
            try {
                final FileSqlInformation info;
                info = methods.supplier().get();
                if (info != null)
                    RootDriver.getInstance().completeUpload(info);
            } catch (final Exception exception) {
                throw new ServerException(exception);
            } finally {
                methods.finisher().run();
            }
            return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
                ByteBufIOUtil.writeBoolean(buf, true);
                return buf;
            });
        }
        final String id = FileUploadIdHelper.generateId(methods, md5, user.getT().username());
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeBoolean(buf, false);
            ByteBufIOUtil.writeUTF(buf, id);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doUploadFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int chunk = ByteBufIOUtil.readVariableLenInt(buffer);
        try {
            final SupplierE<FileSqlInformation> supplier = FileUploadIdHelper.upload(id, user.getT().username(), buffer, chunk);
            if (supplier == null)
                return ServerHandler.DataError;
            buffer.retain();
            final FileSqlInformation info = supplier.get();
            if (info == null)
                return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
                    ByteBufIOUtil.writeBoolean(buf, true);
                    return buf;
                });
            RootDriver.getInstance().completeUpload(info);
            final String json = JSON.toJSONString(ServerFileHandler.getVisibleInfo(info));
            return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
                ByteBufIOUtil.writeBoolean(buf, false);
                ByteBufIOUtil.writeUTF(buf, json);
                return buf;
            });
        } catch (final ServerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
    };

    public static final @NotNull ServerHandler doCancelUploadFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        return FileUploadIdHelper.cancel(id, user.getT().username()) ? ServerHandler.Success : ServerHandler.DataError;
    };

    public static final @NotNull ServerHandler doCopyFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final DrivePath source = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final DrivePath target = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final FileSqlInformation file;
        try {
            file = RootDriver.getInstance().copy(source, target);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerFileHandler.FileNotFound;
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    };

    public static final @NotNull ServerHandler doMoveFile = buffer -> {
        final UnionPair<UserSqlInformation, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload, Operation.Permission.FileDelete);
        if (user.isFailure())
            return user.getE();
        final DrivePath sourceFile = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final DrivePath targetDirectory = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final FileSqlInformation file;
        try {
            file = RootDriver.getInstance().move(sourceFile, targetDirectory);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerFileHandler.FileNotFound;
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    };
}
