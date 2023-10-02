package com.xuxiaocheng.WList.Client.Assistants;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClient;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Commons.Utils.I18NUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.buffer.ByteBuf;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @see com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager
 */
public final class BroadcastAssistant {
    private BroadcastAssistant() {
        super();
    }

    public static final @NotNull EventExecutorGroup CallbackExecutors = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory("CallbackExecutors"));

    protected static class CallbackSet<T> {
        protected final @NotNull Set<Consumer<T>> callbacks = ConcurrentHashMap.newKeySet();
        public void register(final @NotNull Consumer<T> callback) {
            this.callbacks.add(callback);
        }
        public void unregister(final @NotNull Consumer<T> callback) {
            this.callbacks.remove(callback);
        }
        protected void callback(final @NotNull T t) throws InterruptedException {
            HMultiRunHelper.runConsumers(BroadcastAssistant.CallbackExecutors, this.callbacks, c -> c.accept(t));
        }
        @Override
        public @NotNull String toString() {
            return "CallbackSet{" +
                    "callbacks=" + this.callbacks +
                    '}';
        }
    }

    public static final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull String/*sender*/, @NotNull String/*message*/>> UserBroadcast = new CallbackSet<>();

    public static final @NotNull CallbackSet<@NotNull VisibleUserInformation/*information*/> UserLogon = new CallbackSet<>();
    public static final @NotNull CallbackSet<@NotNull Long/*id*/> UserLogoff = new CallbackSet<>();
    public static final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, @NotNull String/*newName*/, @NotNull ZonedDateTime/*updateTime*/>> UserChangeName = new CallbackSet<>();
    public static final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull Long/*id*/, @NotNull ZonedDateTime/*updateTime*/>> UserChangePassword = new CallbackSet<>();

    public static final @NotNull CallbackSet<@NotNull VisibleUserGroupInformation/*information*/> UserGroupAdded = new CallbackSet<>();
    public static final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, @NotNull String/*newName*/, @NotNull ZonedDateTime/*updateTime*/>> UserGroupChangeName = new CallbackSet<>();
    public static final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, @NotNull Set<@NotNull UserPermission>/*newPermissions*/, @NotNull ZonedDateTime/*updateTime*/>> UserGroupChangePermissions = new CallbackSet<>();
    public static final @NotNull CallbackSet<@NotNull Long/*id*/> UserGroupDeleted = new CallbackSet<>();

    public static final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, Pair.@NotNull ImmutablePair<@NotNull Long/*groupId*/, @NotNull String/*groupName*/>, @NotNull ZonedDateTime/*updateTime*/>> UserChangeGroup = new CallbackSet<>();
    public static final @NotNull CallbackSet<@NotNull Long/*groupId*/> UsersLogoff = new CallbackSet<>();

    public static final @NotNull CallbackSet<@NotNull String/*name*/> ProviderInitialized = new CallbackSet<>();
    public static final @NotNull CallbackSet<@NotNull String/*name*/> ProviderUninitialized = new CallbackSet<>();

    public static final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull FileLocation/*location*/, @NotNull Boolean/*isDirectory*/>> FileTrash = new CallbackSet<>();
    public static final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull FileLocation/*location*/, @NotNull Boolean/*isDirectory*/>> FileUpdate = new CallbackSet<>();
    public static final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull String/*storage*/, @NotNull VisibleFileInformation/*information*/>> FileUpload = new CallbackSet<>();
    public static final @NotNull CallbackSet<@NotNull FileLocation/*directory*/> DirectoryRefresh = new CallbackSet<>();

    private static void handleBroadcast(final @NotNull OperationType type, final @NotNull ByteBuf buffer) throws IOException, InterruptedException {
        switch (type) {
            case Logon -> {
                final VisibleUserInformation information = VisibleUserInformation.parse(buffer);
                BroadcastAssistant.UserLogon.callback(information);
            }
            case Logoff -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                BroadcastAssistant.UserLogoff.callback(id);
            }
            case ChangeUsername -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final String username = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                BroadcastAssistant.UserChangeName.callback(Triad.ImmutableTriad.makeImmutableTriad(id, username, updateTime));
            }
            case ChangePassword -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                BroadcastAssistant.UserChangePassword.callback(Pair.ImmutablePair.makeImmutablePair(id, updateTime));
            }

            case AddGroup -> {
                final VisibleUserGroupInformation information = VisibleUserGroupInformation.parse(buffer);
                BroadcastAssistant.UserGroupAdded.callback(information);
            }
            case ChangeGroupName -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final String name = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                BroadcastAssistant.UserGroupChangeName.callback(Triad.ImmutableTriad.makeImmutableTriad(id, name, updateTime));
            }
            case ChangeGroupPermissions -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final String permission = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                final Set<UserPermission> permissions = UserPermission.parse(permission);
                if (permissions == null)
                    throw new IllegalStateException("Invalid permissions." + ParametersMap.create().add("id", id).add("permission", permission).add("updateTime", updateTime));
                BroadcastAssistant.UserGroupChangePermissions.callback(Triad.ImmutableTriad.makeImmutableTriad(id, permissions, updateTime));
            }
            case DeleteGroup -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                BroadcastAssistant.UserGroupDeleted.callback(id);
            }

            case ChangeUserGroup -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
                final String groupName = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                BroadcastAssistant.UserChangeGroup.callback(Triad.ImmutableTriad.makeImmutableTriad(id, Pair.ImmutablePair.makeImmutablePair(groupId, groupName), updateTime));
            }
            case DeleteUsersInGroup -> {
                final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
                BroadcastAssistant.UsersLogoff.callback(groupId);
            }

            case AddProvider -> {
                final String name = ByteBufIOUtil.readUTF(buffer);
                BroadcastAssistant.ProviderInitialized.callback(name);
            }
            case RemoveProvider -> {
                final String name = ByteBufIOUtil.readUTF(buffer);
                BroadcastAssistant.ProviderUninitialized.callback(name);
            }

            case TrashFileOrDirectory -> {
                final FileLocation location = FileLocation.parse(buffer);
                final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
                BroadcastAssistant.FileTrash.callback(Pair.ImmutablePair.makeImmutablePair(location, isDirectory));
            }
            case GetFileOrDirectory -> {
                final FileLocation location = FileLocation.parse(buffer);
                final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
                BroadcastAssistant.FileUpdate.callback(Pair.ImmutablePair.makeImmutablePair(location, isDirectory));
            }
            case UploadFile -> {
                final String storage = ByteBufIOUtil.readUTF(buffer);
                final VisibleFileInformation information = VisibleFileInformation.parse(buffer);
                BroadcastAssistant.FileUpload.callback(Pair.ImmutablePair.makeImmutablePair(storage, information));
            }
            case RefreshDirectory -> {
                final FileLocation directory = FileLocation.parse(buffer);
                BroadcastAssistant.DirectoryRefresh.callback(directory);
            }
            default -> throw new IllegalStateException("Invalid broadcast type." + ParametersMap.create().add("type", type).add("buffer", buffer));
        }
    }

    private static final @NotNull Map<@NotNull SocketAddress, @NotNull WListClientInterface> receiver = new ConcurrentHashMap<>();

    public static void start(final @NotNull SocketAddress address) {
        //noinspection resource
        BroadcastAssistant.receiver.computeIfAbsent(address, k -> {
            final WListClientInterface client = new WListClient(address);
            BroadcastAssistant.CallbackExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                boolean flag = true;
                try (client) {
                    client.open();
                    OperateServerHelper.setBroadcastMode(client, true);
                    while (client.isActive()) {
                        final UnionPair<Pair.ImmutablePair<OperationType, ByteBuf>, Pair.ImmutablePair<String, String>> pair = OperateServerHelper.waitBroadcast(client);
                        BroadcastAssistant.CallbackExecutors.submit(HExceptionWrapper.wrapRunnable(() -> {
                            if (pair.isFailure()) {
                                BroadcastAssistant.UserBroadcast.callback(pair.getE());
                                return;
                            }
                            final ByteBuf buffer = pair.getT().getSecond();
                            try {
                                BroadcastAssistant.handleBroadcast(pair.getT().getFirst(), buffer);
                            } finally {
                                buffer.release();
                            }
                        })).addListener(MiscellaneousUtil.exceptionListener());
                    }
                } catch (final IOException exception) {
                    if (!exception.getMessage().equals(I18NUtil.get("client.network.closed_client", address)))
                        throw exception;
                    flag = false;
                } finally {
                    BroadcastAssistant.stop(address);
                    if (flag)
                        BroadcastAssistant.start(address);
                }
            })).addListener(MiscellaneousUtil.exceptionListener());
            return client;
        });
    }

    public static void stop(final @NotNull SocketAddress address) {
        final WListClientInterface client = BroadcastAssistant.receiver.remove(address);
        if (client != null)
            client.close();
    }
}
