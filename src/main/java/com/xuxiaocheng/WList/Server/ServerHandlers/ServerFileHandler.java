package com.xuxiaocheng.WList.Server.ServerHandlers;

import com.alibaba.fastjson2.JSON;
import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.DriverInterface;
import com.xuxiaocheng.WList.Driver.DriverUtil;
import com.xuxiaocheng.WList.Driver.FileInformation;
import com.xuxiaocheng.WList.Driver.Options.OrderDirection;
import com.xuxiaocheng.WList.Driver.Options.OrderPolicy;
import com.xuxiaocheng.WList.Exceptions.ServerException;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import com.xuxiaocheng.WList.Server.Driver.RootDriver;
import com.xuxiaocheng.WList.Server.FileDownloadIdHelper;
import com.xuxiaocheng.WList.Server.Operation;
import com.xuxiaocheng.WList.Utils.ByteBufIOUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ServerFileHandler {
    public static @NotNull Map<String, Object> getVisibleInfo(final @NotNull FileInformation f) {
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
        if (limit < 1 || limit > GlobalConfiguration.getInstance().getMaxLimitPerPage() || page < 0
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
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeVariableLenInt(buffer, list.getFirst().intValue());
        ByteBufIOUtil.writeUTF(buffer, JSON.toJSONString(list.getSecond().stream().map(ServerFileHandler::getVisibleInfo).collect(Collectors.toSet())));
        channel.writeAndFlush(buffer);
    }

    public static void doRequestDownloadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileDownload) == null)
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
        final String id = FileDownloadIdHelper.generateId(url.getFirst());
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
        ByteBufIOUtil.writeVariableLenLong(buffer, url.getSecond().longValue());
        ByteBufIOUtil.writeUTF(buffer, id);
        channel.writeAndFlush(buffer);
    }

    public static void doDownloadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        final Triad.ImmutableTriad<String, String, SortedSet<Operation.Permission>> user = ServerUserHandler.checkToken(buf, channel, Operation.Permission.FileDownload);
        if (user == null)
            return;
        final String id = ByteBufIOUtil.readUTF(buf);
        try {
            final Pair.ImmutablePair<Integer, ByteBuf> file = FileDownloadIdHelper.download(id);
            if (file == null) {
                ServerHandler.writeMessage(channel, Operation.State.DataError, null);
                return;
            }
            final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
            ByteBufIOUtil.writeUTF(buffer, Operation.State.Success.name());
            ByteBufIOUtil.writeVariableLenInt(buffer, file.getFirst().intValue());
            ByteBufIOUtil.writeVariableLenInt(buffer, file.getSecond().readableBytes());
            final CompositeByteBuf composite = ByteBufAllocator.DEFAULT.compositeBuffer(2);
            composite.addComponent(true, buffer);
            composite.addComponent(true, file.getSecond());
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
        if (FileDownloadIdHelper.cancel(id))
            ServerHandler.writeMessage(channel, Operation.State.Success, null);
        else
            ServerHandler.writeMessage(channel, Operation.State.DataError, null);
    }

    public static void doMakeDirectories(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        Pair.@Nullable ImmutablePair<@NotNull DriverInterface<?>, @NotNull DrivePath> path = null;
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload) != null) {
            final DrivePath path1 = new DrivePath(ByteBufIOUtil.readUTF(buf));
            path = Pair.ImmutablePair.makeImmutablePair(RootDriver.getInstance(), path1);
        }
        if (path == null)
            return;
        final FileInformation dir;
        try {
            dir = path.getFirst().mkdirs(path.getSecond());
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
        ServerHandler.writeMessage(channel, Operation.State.Success, JSON.toJSONString(getVisibleInfo(dir)));
    }

    public static void doDeleteFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        Pair.@Nullable ImmutablePair<@NotNull DriverInterface<?>, @NotNull DrivePath> path = null;
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileDelete) != null) {
            final DrivePath path1 = new DrivePath(ByteBufIOUtil.readUTF(buf));
            path = Pair.ImmutablePair.makeImmutablePair(RootDriver.getInstance(), path1);
        }
        if (path == null)
            return;
        try {
            path.getFirst().delete(path.getSecond());
        } catch (final UnsupportedOperationException exception) {
            ServerHandler.writeMessage(channel, Operation.State.Unsupported, exception.getMessage());
            return;
        } catch (final Exception exception) {
            throw new ServerException(exception);
        }
        ServerHandler.writeMessage(channel, Operation.State.Success, null);
    }

    public static void doRenameFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        Pair.@Nullable ImmutablePair<@NotNull DriverInterface<?>, @NotNull DrivePath> path = null;
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload, Operation.Permission.FileDelete) != null) {
            final DrivePath path1 = new DrivePath(ByteBufIOUtil.readUTF(buf));
            path = Pair.ImmutablePair.makeImmutablePair(RootDriver.getInstance(), path1);
        }
        if (path == null)
            return;
        final String name = ByteBufIOUtil.readUTF(buf);
        final FileInformation file;
        try {
            file = path.getFirst().rename(path.getSecond(), name);
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
        ServerHandler.writeMessage(channel, Operation.State.Success, JSON.toJSONString(getVisibleInfo(file)));
    }

    public static void doRequestUploadFile(final @NotNull ByteBuf buf, final @NotNull Channel channel) throws IOException, ServerException {
        Pair.@Nullable ImmutablePair<@NotNull DriverInterface<?>, @NotNull DrivePath> path = null;
        if (ServerUserHandler.checkToken(buf, channel, Operation.Permission.FilesList, Operation.Permission.FileUpload) != null) {
            final DrivePath path1 = new DrivePath(ByteBufIOUtil.readUTF(buf));
            path = Pair.ImmutablePair.makeImmutablePair(RootDriver.getInstance(), path1);
        }
        if (path == null)
            return;
        final long size = ByteBufIOUtil.readVariableLenLong(buf);
        final String tag = ByteBufIOUtil.readUTF(buf);
        final List<String> tags = JSON.parseArray(ByteBufIOUtil.readUTF(buf), String.class);
        if (size < 0 || size <= (long) (tags.size() - 1) * (4 << 20) || (long) tags.size() * (4 << 20) < size) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Size");
            return;
        }
        if (!DriverUtil.tagPredication.test(tag) || !tags.stream().allMatch(DriverUtil.tagPredication)) {
            ServerHandler.writeMessage(channel, Operation.State.DataError, "Tag");
            return;
        }
        //todo
        ServerHandler.writeMessage(channel, Operation.State.Unsupported, null);
    }

    // TODO
}
