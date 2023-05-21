package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.SupplierE;
import com.xuxiaocheng.WList.Driver.Helpers.DriverUtil;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Driver.Utils.FileInformation;
import com.xuxiaocheng.WList.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.Driver.RootDriver;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
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

    public static void doListFiles(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList) == null)
            return;
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final int limit = ByteBufIOUtil.readVariableLenInt(buf);
        final int page = ByteBufIOUtil.readVariableLenInt(buf);
        final String direction = ByteBufIOUtil.readUTF(buf);
        final String policy = ByteBufIOUtil.readUTF(buf);
        final OrderDirection orderDirection = OrderDirection.Map.get(direction);
        final OrderPolicy orderPolicy = OrderPolicy.Map.get(policy);
        if (limit < 1 || limit > GlobalConfiguration.getInstance().maxLimitPerPage() || page < 0
                || (orderDirection == null && !"D".equals(direction))
                || (orderPolicy == null && !"D".equals(policy))) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Parameters");
            return;
        }
        final Pair.ImmutablePair<Integer, List<FileInformation>> list;
        try {
            list = RootDriver.getInstance().list(path, limit, page, orderDirection, orderPolicy);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (list == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "File");
            return;
        }
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(buffer, ServerHandler.defaultCipher);
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeVariableLenInt(buffer, list.getFirst().intValue());
        ByteBufIOUtil.writeUTF(buffer, JSON.toJSONString(list.getSecond().stream().map(ServerFileHandler::getVisibleInfo).collect(Collectors.toSet())));
        channel.writeAndFlush(buffer);
    }

    public static void doMakeDirectories(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload) == null)
            return;
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final FileInformation dir;
        try {
            dir = RootDriver.getInstance().mkdirs(path);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (dir == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Name");
            return;
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(dir)));
    }

    public static void doDeleteFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileDelete) == null)
            return;
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buf));
        try {
            RootDriver.getInstance().delete(path);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    public static void doRenameFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDelete) == null)
            return;
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final String name = ByteBufIOUtil.readUTF(buf);
        final FileInformation file;
        try {
            file = RootDriver.getInstance().rename(path, name);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Name");
            return;
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    }

    public static void doRequestDownloadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileDownload);
        if (user == null)
            return;
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final long from = ByteBufIOUtil.readVariableLenLong(buf);
        final long to = ByteBufIOUtil.readVariableLenLong(buf);
        if (from < 0 || from >= to) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Parameters");
            return;
        }
        final Pair.ImmutablePair<InputStream, Long> url;
        try {
            url = RootDriver.getInstance().download(path, from, to);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (url == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "File");
            return;
        }
        final String id = FileDownloadIdHelper.generateId(url.getFirst(), user.getA());
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(buffer, ServerHandler.defaultCipher);
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeVariableLenLong(buffer, url.getSecond().longValue());
        ByteBufIOUtil.writeVariableLenInt(buffer, WListServer.FileTransferBufferSize);
        ByteBufIOUtil.writeUTF(buffer, id);
        channel.writeAndFlush(buffer);
    }

    public static void doDownloadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.FileDownload);
        if (user == null)
            return;
        final String id = ByteBufIOUtil.readUTF(buf);
        try {
            final Pair.ImmutablePair<Integer, ByteBuf> file = FileDownloadIdHelper.download(id, user.getA());
            if (file == null) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, null);
                return;
            }
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeByte(buffer, AesCipher.defaultDoGZip);
            ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
            ByteBufIOUtil.writeVariableLenInt(buffer, file.getFirst().intValue());
            final CompositeByteBuf composite = ByteBufAllocator.DEFAULT.compositeBuffer(2);
            composite.addComponents(true, buffer, file.getSecond());
            channel.writeAndFlush(composite);
        } catch (final InterruptedException | IOException | ExecutionException exception) {
            throw new ServerException(exception);
        }
    }

    public static void doCancelDownloadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.FileDownload);
        if (user == null)
            return;
        final String id = ByteBufIOUtil.readUTF(buf);
        if (FileDownloadIdHelper.cancel(id, user.getA()))
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        else
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
    }

    public static void doRequestUploadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload);
        if (user == null)
            return;
        final DrivePath path = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final long size = ByteBufIOUtil.readVariableLenLong(buf);
        final String tag = ByteBufIOUtil.readUTF(buf);
        if (size < 0) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Size");
            return;
        }
        if (!DriverUtil.tagPredication.test(tag)) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Tag");
            return;
        }
        final Triad.ImmutableTriad<List<Pair.ImmutablePair<Integer, ConsumerE<ByteBuf>>>, SupplierE<FileInformation>, Runnable> methods;
        try {
            methods = RootDriver.getInstance().upload(path, size, tag);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (methods == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "File");
            return;
        }
        final boolean reuse = methods.getA().isEmpty();
        if (reuse) {
            try {
                final FileInformation info;
                info = methods.getB().get();
                if (info != null)
                    RootDriver.getInstance().completeUpload(info);
            } catch (final Exception exception) {
                throw new ServerException(exception);
            } finally {
                methods.getC().run();
            }
        } else { // assert
            int all = 0;
            for (final Pair.ImmutablePair<Integer, ConsumerE<ByteBuf>> p : methods.getA())
                all += p.getFirst().intValue();
            if (all != size)
                throw new ServerException(new IllegalParametersException("Assert error. All size in {methods.first(list)} should equal {size}.",
                        Map.of("all", all, "size", size, "tag", tag, "username", user.getA())));
        }
        final String id = reuse ? null : FileUploadIdHelper.generateId(methods, tag, user.getA());
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeByte(buffer, ServerHandler.defaultCipher);
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeBoolean(buffer, reuse);
        if (!reuse)
            ByteBufIOUtil.writeUTF(buffer, id);
        channel.writeAndFlush(buffer);
    }

    public static void doUploadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.FileUpload);
        if (user == null)
            return;
        final String id = ByteBufIOUtil.readUTF(buf);
        final int chunk = ByteBufIOUtil.readVariableLenInt(buf);
        try {
            final Pair.ImmutablePair<Boolean, SupplierE<FileInformation>> pair = FileUploadIdHelper.upload(id, user.getA(), buf, chunk);
            if (!pair.getFirst().booleanValue()) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, null);
                return;
            }
            buf.retain();
            if (pair.getSecond() != null) {
                final FileInformation info = pair.getSecond().get();
                if (info != null)
                    RootDriver.getInstance().completeUpload(info);
            }
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        } catch (final ServerException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
    }

    public static void doCancelUploadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.FileUpload);
        if (user == null)
            return;
        final String id = ByteBufIOUtil.readUTF(buf);
        if (FileUploadIdHelper.cancel(id, user.getA()))
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        else
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
    }

    public static void doCopyFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload) == null)
            return;
        final DrivePath source = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final DrivePath target = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final FileInformation file;
        try {
            file = RootDriver.getInstance().copy(source, target);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return;
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    }

    public static void doMoveFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDownload, Operation.Permission.FileDelete) == null)
            return;
        final DrivePath sourceFile = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final DrivePath targetDirectory = new DrivePath(ByteBufIOUtil.readUTF(buf));
        final FileInformation file;
        try {
            file = RootDriver.getInstance().move(sourceFile, targetDirectory);
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        if (file == null) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
            return;
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, JSON.toJSONString(ServerFileHandler.getVisibleInfo(file)));
    }
}
