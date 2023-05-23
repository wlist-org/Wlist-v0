package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.WList.DataAccessObjects.FileInformation;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Driver.RootDriver;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.Polymers.MessageProto;
import com.xuxiaocheng.WList.Server.Polymers.UploadMethods;
import com.xuxiaocheng.WList.Server.Polymers.UserTokenInfo;
import com.xuxiaocheng.WList.Server.ServerCodecs.MessageCiphers;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
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
import java.util.stream.Collectors;

public final class ServerFileHandler {
    private ServerFileHandler() {
        super();
    }

    static @NotNull Map<String, Object> getVisibleInfo(final @NotNull FileInformation f) {
        final Map<String, Object> map = new HashMap<>(6);
        map.put("path", f.path().getPath());
        map.put("is_dir", f.is_dir());
        map.put("size", f.size());
        if (f.createTime() != null)
            map.put("create_time", f.createTime().format(DateTimeFormatter.ISO_DATE_TIME));
        if (f.updateTime() != null)
            map.put("update_time", f.updateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        map.put("tag", f.tag());
        return map;
    }

    public static final @NotNull ServerHandler doListFiles = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final int limit = ByteBufIOUtil.readVariableLenInt(buffer);
        final int page = ByteBufIOUtil.readVariableLenInt(buffer);
        final String direction = ByteBufIOUtil.readUTF(buffer);
        final String policy = ByteBufIOUtil.readUTF(buffer);
        final OrderDirection orderDirection = OrderDirection.Map.get(direction);
        final OrderPolicy orderPolicy = OrderPolicy.Map.get(policy);
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0
                || (orderDirection == null && !"D".equals(direction))
                || (orderPolicy == null && !"D".equals(policy)))
            return ServerHandler.composeMessage(Operation.State.DataError, "Parameters");
        final Pair.ImmutablePair<Integer, List<FileInformation>> list;
        try {
            list = RootDriver.getInstance().list(path, limit, page, orderDirection, orderPolicy);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (list == null)
            return ServerHandler.composeMessage(Operation.State.DataError, "File");
        final String json = JSON.toJSONString(list.getSecond().stream()
                .map(ServerFileHandler::getVisibleInfo).collect(Collectors.toList()));
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeVariableLenInt(buf, list.getFirst().intValue());
            ByteBufIOUtil.writeUTF(buf, json);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doMakeDirectories = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final FileInformation dir;
        try {
            dir = RootDriver.getInstance().mkdirs(path);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (dir == null)
            return ServerHandler.composeMessage(Operation.State.DataError, "Name");
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(dir)));
    };

    public static final @NotNull ServerHandler doDeleteFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDelete);
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
        return ServerHandler.composeMessage(Operation.State.Success, null);
    };

    public static final @NotNull ServerHandler doRenameFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload, Operation.Permission.FileUpload, Operation.Permission.FileDelete);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final String name = ByteBufIOUtil.readUTF(buffer);
        final FileInformation file;
        try {
            file = RootDriver.getInstance().rename(path, name);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerHandler.composeMessage(Operation.State.DataError, "Name");
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    };

    public static final @NotNull ServerHandler doRequestDownloadFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final long from = ByteBufIOUtil.readVariableLenLong(buffer);
        final long to = ByteBufIOUtil.readVariableLenLong(buffer);
        if (from < 0 || from >= to)
            return ServerHandler.composeMessage(Operation.State.DataError, "Parameters");
        final Pair.ImmutablePair<InputStream, Long> url;
        try {
            url = RootDriver.getInstance().download(path, from, to);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (url == null)
            return ServerHandler.composeMessage(Operation.State.DataError, "File");
        final String id = FileDownloadIdHelper.generateId(url.getFirst(), user.getT().username());
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeVariableLenLong(buf, url.getSecond().longValue());
            ByteBufIOUtil.writeVariableLenInt(buf, WListServer.FileTransferBufferSize);
            ByteBufIOUtil.writeUTF(buf, id);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doDownloadFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        try {
            final Pair.ImmutablePair<Integer, ByteBuf> file = FileDownloadIdHelper.download(id, user.getT().username());
            if (file == null)
                return ServerHandler.composeMessage(Operation.State.DataError, null);
            return new MessageProto(MessageCiphers.defaultDoGZip, Operation.State.Success, (buf) -> {
                ByteBufIOUtil.writeVariableLenInt(buf, file.getFirst().intValue());
                final CompositeByteBuf composite = ByteBufAllocator.DEFAULT.compositeBuffer(2);
                composite.addComponents(true, buf, file.getSecond());
                return composite;
            });
        } catch (final InterruptedException | IOException | ExecutionException exception) {
            throw new ServerException(exception);
        }
    };

    public static final @NotNull ServerHandler doCancelDownloadFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        if (FileDownloadIdHelper.cancel(id, user.getT().username()))
            return ServerHandler.composeMessage(Operation.State.Success, null);
        else
            return ServerHandler.composeMessage(Operation.State.DataError, null);
    };

    public static final @NotNull ServerHandler doRequestUploadFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final long size = ByteBufIOUtil.readVariableLenLong(buffer);
        final String tag = ByteBufIOUtil.readUTF(buffer);
        if (size < 0)
            return ServerHandler.composeMessage(Operation.State.DataError, "Size");
        if (!DriverUtil.tagPredication.test(tag))
            return ServerHandler.composeMessage(Operation.State.DataError, "Tag");
        final UploadMethods methods;
        try {
            methods = RootDriver.getInstance().upload(path, size, tag);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (methods == null)
            return ServerHandler.composeMessage(Operation.State.DataError, "File");
        if (methods.methods().isEmpty()) { // (reuse/empty)
            try {
                final FileInformation info;
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
        final String id = FileUploadIdHelper.generateId(methods, tag, user.getT().username());
        return new MessageProto(ServerHandler.defaultCipher, Operation.State.Success, buf -> {
            ByteBufIOUtil.writeBoolean(buf, false);
            ByteBufIOUtil.writeUTF(buf, id);
            return buf;
        });
    };

    public static final @NotNull ServerHandler doUploadFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        final int chunk = ByteBufIOUtil.readVariableLenInt(buffer);
        try {
            final SupplierE<FileInformation> supplier = FileUploadIdHelper.upload(id, user.getT().username(), buffer, chunk);
            if (supplier == null)
                return ServerHandler.composeMessage(Operation.State.DataError, null);
            buffer.retain();
            final FileInformation info = supplier.get();
            if (info != null)
                RootDriver.getInstance().completeUpload(info);
            return ServerHandler.composeMessage(Operation.State.Success, null);
        } catch (final ServerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
    };

    public static final @NotNull ServerHandler doCancelUploadFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FileUpload);
        if (user.isFailure())
            return user.getE();
        final String id = ByteBufIOUtil.readUTF(buffer);
        boolean success = FileUploadIdHelper.cancel(id, user.getT().username());
        return ServerHandler.composeMessage(success ? Operation.State.Success : Operation.State.DataError, null);
    };

    public static final @NotNull ServerHandler doCopyFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload);
        if (user.isFailure())
            return user.getE();
        final DrivePath source = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final DrivePath target = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final FileInformation file;
        try {
            file = RootDriver.getInstance().copy(source, target);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerHandler.composeMessage(Operation.State.DataError, null);
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    };

    public static final @NotNull ServerHandler doMoveFile = buffer -> {
        final UnionPair<UserTokenInfo, MessageProto> user = ServerUserHandler.checkToken(buffer, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload, Operation.Permission.FileDelete);
        if (user.isFailure())
            return user.getE();
        final DrivePath sourceFile = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final DrivePath targetDirectory = new DrivePath(ByteBufIOUtil.readUTF(buffer));
        final FileInformation file;
        try {
            file = RootDriver.getInstance().move(sourceFile, targetDirectory);
        } catch (final UnsupportedOperationException exception) {
            return ServerHandler.composeMessage(Operation.State.Unsupported, exception.getMessage());
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null)
            return ServerHandler.composeMessage(Operation.State.DataError, null);
        return ServerHandler.composeMessage(Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    };
}
