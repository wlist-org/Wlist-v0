package com.xuxiaocheng.WList.Client.Assistants;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.DataStructures.UnionPair;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Client.Operations.OperateServerHelper;
import com.xuxiaocheng.WList.Client.WListClient;
import com.xuxiaocheng.WList.Client.WListClientInterface;
import com.xuxiaocheng.WList.Client.WListClientManager;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserGroupInformation;
import com.xuxiaocheng.WList.Commons.Beans.VisibleUserInformation;
import com.xuxiaocheng.WList.Commons.Operations.OperationType;
import com.xuxiaocheng.WList.Commons.Operations.UserPermission;
import com.xuxiaocheng.WList.Commons.Utils.ByteBufIOUtil;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Operations.Helpers.BroadcastManager;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @see BroadcastManager
 */
public final class BroadcastAssistant {
    private BroadcastAssistant() {
        super();
    }

    public static final @NotNull EventExecutorGroup CallbackExecutors = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory("CallbackExecutors"));

    public static final @NotNull AtomicBoolean LogBroadcastEvent = new AtomicBoolean(true);

    public static class CallbackSet<T> {
        protected final @NotNull String name;
        public CallbackSet(final @NotNull String name) {
            super();
            this.name = name;
        }
        protected final @NotNull Map<@NotNull String, @NotNull Consumer<T>> callbacks = new ConcurrentHashMap<>();
        public @NotNull Map<@NotNull String, @NotNull Consumer<T>> getCallbacks() {
            return this.callbacks;
        }
        protected void callback(final @NotNull T t) throws InterruptedException {
            if (this.callbacks.isEmpty()) {
                HLog.getInstance("ClientLogger").log(HLogLevel.WARN, "No broadcast callbacks registered.", ParametersMap.create().add("name", this.name).add("event", t));
                return;
            }
            if (BroadcastAssistant.LogBroadcastEvent.get())
                HLog.getInstance("ClientLogger").log(HLogLevel.LESS, "Broadcast callback: ", ParametersMap.create().add("name", this.name).add("event", t));
            HMultiRunHelper.runConsumers(BroadcastAssistant.CallbackExecutors, this.callbacks.values(), c -> c.accept(t));
        }
        @Override
        public @NotNull String toString() {
            return "CallbackSet{" +
                    "name=" + this.name +
                    ", callbacks=" + this.callbacks +
                    '}';
        }
    }

    @SuppressWarnings("PublicField")
    public static class BroadcastSet {
        protected BroadcastSet() {
            super();
        }

        public final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull String/*sender*/, @NotNull String/*message*/>> UserBroadcast = new CallbackSet<>("UserBroadcast");

        public final @NotNull CallbackSet<@NotNull Long/*userId*/> ServerClose = new CallbackSet<>("ServerClose");

        public final @NotNull CallbackSet<@NotNull VisibleUserInformation/*information*/> UserLogon = new CallbackSet<>("UserLogon");
        public final @NotNull CallbackSet<@NotNull Long/*id*/> UserLogoff = new CallbackSet<>("UserLogoff");
        public final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, @NotNull String/*newName*/, @NotNull ZonedDateTime/*updateTime*/>> UserChangeName = new CallbackSet<>("UserChangeName");
        public final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull Long/*id*/, @NotNull ZonedDateTime/*updateTime*/>> UserChangePassword = new CallbackSet<>("UserChangePassword");

        public final @NotNull CallbackSet<@NotNull VisibleUserGroupInformation/*information*/> UserGroupAdded = new CallbackSet<>("UserGroupAdded");
        public final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, @NotNull String/*newName*/, @NotNull ZonedDateTime/*updateTime*/>> UserGroupChangeName = new CallbackSet<>("UserGroupChangeName");
        public final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, @NotNull Set<@NotNull UserPermission>/*newPermissions*/, @NotNull ZonedDateTime/*updateTime*/>> UserGroupChangePermissions = new CallbackSet<>("UserGroupChangePermissions");
        public final @NotNull CallbackSet<@NotNull Long/*id*/> UserGroupDeleted = new CallbackSet<>("UserGroupDeleted");

        public final @NotNull CallbackSet<Triad.@NotNull ImmutableTriad<@NotNull Long/*id*/, Pair.@NotNull ImmutablePair<@NotNull Long/*groupId*/, @NotNull String/*groupName*/>, @NotNull ZonedDateTime/*updateTime*/>> UserChangeGroup = new CallbackSet<>("UserChangeGroup");
        public final @NotNull CallbackSet<@NotNull Long/*groupId*/> UsersLogoff = new CallbackSet<>("UsersLogoff");

        public final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull String/*storage*/, @NotNull Long/*id*/>> ProviderInitialized = new CallbackSet<>("ProviderInitialized");
        public final @NotNull CallbackSet<@NotNull String/*storage*/> ProviderUninitialized = new CallbackSet<>("ProviderUninitialized");
        public final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull String/*storage*/, @NotNull Boolean/*enter*/>> ProviderLogin = new CallbackSet<>("ProviderLogin");

        public final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull FileLocation/*location*/, @NotNull Boolean/*isDirectory*/>> FileTrash = new CallbackSet<>("FileTrash");
        public final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull FileLocation/*location*/, @NotNull Boolean/*isDirectory*/>> FileUpdate = new CallbackSet<>("FileUpdate");
        public final @NotNull CallbackSet<Pair.@NotNull ImmutablePair<@NotNull String/*storage*/, @NotNull VisibleFileInformation/*information*/>> FileUpload = new CallbackSet<>("FileUpload");

        @Override
        public @NotNull String toString() {
            return "BroadcastSet{" +
                    "UserBroadcast=" + this.UserBroadcast +
                    ", UserLogon=" + this.UserLogon +
                    ", UserLogoff=" + this.UserLogoff +
                    ", UserChangeName=" + this.UserChangeName +
                    ", UserChangePassword=" + this.UserChangePassword +
                    ", UserGroupAdded=" + this.UserGroupAdded +
                    ", UserGroupChangeName=" + this.UserGroupChangeName +
                    ", UserGroupChangePermissions=" + this.UserGroupChangePermissions +
                    ", UserGroupDeleted=" + this.UserGroupDeleted +
                    ", UserChangeGroup=" + this.UserChangeGroup +
                    ", UsersLogoff=" + this.UsersLogoff +
                    ", ProviderInitialized=" + this.ProviderInitialized +
                    ", ProviderUninitialized=" + this.ProviderUninitialized +
                    ", ProviderLogin=" + this.ProviderLogin +
                    ", FileTrash=" + this.FileTrash +
                    ", FileUpdate=" + this.FileUpdate +
                    ", FileUpload=" + this.FileUpload +
                    '}';
        }
    }

    private static final @NotNull HMultiInitializers<@NotNull SocketAddress, @NotNull BroadcastSet> map = new HMultiInitializers<>("BroadcastSets");

    public static @NotNull BroadcastSet get(final @NotNull SocketAddress address) {
        return BroadcastAssistant.map.getInstance(address);
    }

    private static void handleBroadcast(final @NotNull BroadcastSet set, final @NotNull OperationType type, final @NotNull ByteBuf buffer) throws IOException, InterruptedException {
        switch (type) {
            case CloseServer -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                set.ServerClose.callback(id);
            }
            case Logon -> {
                final VisibleUserInformation information = VisibleUserInformation.parse(buffer);
                set.UserLogon.callback(information);
            }
            case Logoff -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                set.UserLogoff.callback(id);
            }
            case ChangeUsername -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final String username = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                set.UserChangeName.callback(Triad.ImmutableTriad.makeImmutableTriad(id, username, updateTime));
            }
            case ChangePassword -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                set.UserChangePassword.callback(Pair.ImmutablePair.makeImmutablePair(id, updateTime));
            }

            case AddGroup -> {
                final VisibleUserGroupInformation information = VisibleUserGroupInformation.parse(buffer);
                set.UserGroupAdded.callback(information);
            }
            case ChangeGroupName -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final String name = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                set.UserGroupChangeName.callback(Triad.ImmutableTriad.makeImmutableTriad(id, name, updateTime));
            }
            case ChangeGroupPermissions -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final String permission = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                final Set<UserPermission> permissions = UserPermission.parse(permission);
                if (permissions == null)
                    throw new IllegalStateException("Invalid permissions." + ParametersMap.create().add("id", id).add("permission", permission).add("updateTime", updateTime));
                set.UserGroupChangePermissions.callback(Triad.ImmutableTriad.makeImmutableTriad(id, permissions, updateTime));
            }
            case DeleteGroup -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                set.UserGroupDeleted.callback(id);
            }

            case ChangeUserGroup -> {
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
                final String groupName = ByteBufIOUtil.readUTF(buffer);
                final ZonedDateTime updateTime = ZonedDateTime.parse(ByteBufIOUtil.readUTF(buffer), DateTimeFormatter.ISO_DATE_TIME);
                set.UserChangeGroup.callback(Triad.ImmutableTriad.makeImmutableTriad(id, Pair.ImmutablePair.makeImmutablePair(groupId, groupName), updateTime));
            }
            case DeleteUsersInGroup -> {
                final long groupId = ByteBufIOUtil.readVariableLenLong(buffer);
                set.UsersLogoff.callback(groupId);
            }

            case AddProvider -> {
                final String storage = ByteBufIOUtil.readUTF(buffer);
                final long id = ByteBufIOUtil.readVariableLenLong(buffer);
                set.ProviderInitialized.callback(Pair.ImmutablePair.makeImmutablePair(storage, id));
            }
            case RemoveProvider -> {
                final String storage = ByteBufIOUtil.readUTF(buffer);
                set.ProviderUninitialized.callback(storage);
            }
            case Login -> {
                final String storage = ByteBufIOUtil.readUTF(buffer);
                final boolean enter = ByteBufIOUtil.readBoolean(buffer);
                set.ProviderLogin.callback(Pair.ImmutablePair.makeImmutablePair(storage, enter));
            }

            case TrashFileOrDirectory -> {
                final FileLocation location = FileLocation.parse(buffer);
                final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
                set.FileTrash.callback(Pair.ImmutablePair.makeImmutablePair(location, isDirectory));
            }
            case GetFileOrDirectory -> {
                final FileLocation location = FileLocation.parse(buffer);
                final boolean isDirectory = ByteBufIOUtil.readBoolean(buffer);
                set.FileUpdate.callback(Pair.ImmutablePair.makeImmutablePair(location, isDirectory));
            }
            case UploadFile -> {
                final String storage = ByteBufIOUtil.readUTF(buffer);
                final VisibleFileInformation information = VisibleFileInformation.parse(buffer);
                set.FileUpload.callback(Pair.ImmutablePair.makeImmutablePair(storage, information));
            }
            default -> throw new IllegalStateException("Invalid broadcast type." + ParametersMap.create().add("type", type).add("buffer", buffer));
        }
    }

    private static final @NotNull Map<@NotNull SocketAddress, @NotNull WListClientInterface> receiver = new ConcurrentHashMap<>();

    public static void start(final @NotNull SocketAddress address) {
        if (BroadcastAssistant.map.isInitialized(address))
            return;
        //noinspection resource
        BroadcastAssistant.receiver.computeIfAbsent(address, k -> {
            final WListClientInterface client = new WListClient(address);
            new Thread(HExceptionWrapper.wrapRunnable(() -> {
                boolean flag = true;
                try (client) {
                    client.open();
                    OperateServerHelper.setBroadcastMode(client, true);
                    while (client.isActive()) {
                        final UnionPair<Pair.ImmutablePair<OperationType, ByteBuf>, Pair.ImmutablePair<String, String>> pair = OperateServerHelper.waitBroadcast(client);
                        final Runnable runner = HExceptionWrapper.wrapRunnable(() -> {
                            if (pair.isFailure()) {
                                BroadcastAssistant.get(address).UserBroadcast.callback(pair.getE());
                                return;
                            }
                            final ByteBuf buffer = pair.getT().getSecond();
                            try {
                                BroadcastAssistant.handleBroadcast(BroadcastAssistant.get(address), pair.getT().getFirst(), buffer);
                            } finally {
                                buffer.release();
                            }
                        }, MiscellaneousUtil.exceptionCallback, true);
                        if (pair.isFailure() || !BroadcastManager.OrderedBroadcastType.contains(pair.getT().getFirst()))
                            BroadcastAssistant.CallbackExecutors.submit(runner);
                        else
                            runner.run();
                    }
                } catch (final IOException | IllegalStateException exception) {
                    HLog.getInstance("ClientLogger").log(HLogLevel.MISTAKE, "Broadcast receiver '", Thread.currentThread().getName(), "' close. ", exception.getLocalizedMessage());
                    flag = false;
                    WListClientManager.quicklyUninitialize(address);
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                    HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
                } finally {
                    BroadcastAssistant.stop(address);
                    if (flag)
                        BroadcastAssistant.start(address);
                }
            }), "BroadcastReceiverLoop@" + address).start();
            BroadcastAssistant.map.initializeIfNot(address, BroadcastSet::new);
            return client;
        });
    }

    public static void stop(final @NotNull SocketAddress address) {
        final WListClientInterface client = BroadcastAssistant.receiver.remove(address);
        if (client != null) {
            client.close();
            BroadcastAssistant.map.uninitialize(address);
        }
    }
}
