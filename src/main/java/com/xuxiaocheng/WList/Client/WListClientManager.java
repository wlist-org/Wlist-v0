package com.xuxiaocheng.WList.Client;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HMultiRunHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WList.Server.Storage.Helpers.BackgroundTaskManager;
import io.netty.buffer.ByteBuf;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WListClientManager implements Closeable {
    public static final @NotNull HMultiInitializers<@NotNull SocketAddress, @NotNull WListClientManager> instances = new HMultiInitializers<>("WListClientManager");
    protected static final @NotNull Map<@NotNull SocketAddress, @NotNull List<@NotNull Consumer<@NotNull Boolean>>> listeners = new ConcurrentHashMap<>();

    public static void addListener(final @NotNull SocketAddress address, final @NotNull ConsumerE<? super @NotNull Boolean> listener) {
        WListClientManager.listeners.compute(address, (k, v) -> Objects.requireNonNullElseGet(v, LinkedList::new)).add(
                HExceptionWrapper.wrapConsumer(listener, MiscellaneousUtil.exceptionCallback, true));
    }

    public static void removeAllListeners(final @NotNull SocketAddress address) {
        WListClientManager.listeners.remove(address);
    }

    public static void quicklyInitialize(final @NotNull WListClientManager manager) {
        if (WListClientManager.instances.initializeIfNot(manager.clientConfig.address, () -> {
            manager.open();
            return manager;
        })) {
            final List<Consumer<Boolean>> list = WListClientManager.listeners.get(manager.clientConfig.address);
            if (list != null)
                BackgroundTaskManager.BackgroundExecutors.submit(HExceptionWrapper.wrapRunnable(() ->
                                HMultiRunHelper.runConsumers(BackgroundTaskManager.BackgroundExecutors, list, c -> c.accept(Boolean.TRUE))))
                        .addListener(MiscellaneousUtil.exceptionListener());
        }
    }

    public static void quicklyUninitialize(final @NotNull SocketAddress address) {
        final WListClientManager manager = WListClientManager.instances.uninitializeNullable(address);
        if (manager != null) {
            final GenericObjectPool<WListClientInterface> pool = manager.clientPool.uninitializeNullable();
            if (pool != null)
                pool.close();
            for (final WrappedClient client: manager.activeClients)
                client.closeInside();
            assert manager.activeClients.isEmpty();
            final List<Consumer<Boolean>> list = WListClientManager.listeners.get(address);
            if (list != null)
                BackgroundTaskManager.BackgroundExecutors.submit(HExceptionWrapper.wrapRunnable(() ->
                                HMultiRunHelper.runConsumers(BackgroundTaskManager.BackgroundExecutors, list, c -> c.accept(Boolean.FALSE))))
                        .addListener(MiscellaneousUtil.exceptionListener());
        }
    }

    public static @NotNull WListClientInterface quicklyGetClient(final @NotNull SocketAddress address) throws IOException {
        return WListClientManager.instances.getInstance(address).getClient();
    }

    protected final @NotNull GenericObjectPoolConfig<WListClientInterface> poolConfig;
    protected final @NotNull ClientManagerConfig clientConfig;
    protected final @NotNull HInitializer<GenericObjectPool<@NotNull WListClientInterface>> clientPool = new HInitializer<>("ClientPool");

    public record ClientManagerConfig(@NotNull SocketAddress address) {
    }

    public static @NotNull WListClientManager getDefault(final @NotNull SocketAddress address) {
        final GenericObjectPoolConfig<WListClientInterface> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setJmxEnabled(AndroidSupporter.jmxEnable);
        poolConfig.setTestOnBorrow(true);
        return new WListClientManager(poolConfig, new ClientManagerConfig(address));
    }

    /**
     * @see #getDefault(SocketAddress)
     */
    public WListClientManager(final @NotNull GenericObjectPoolConfig<WListClientInterface> poolConfig, final @NotNull ClientManagerConfig clientConfig) {
        super();
        this.poolConfig = poolConfig;
        this.clientConfig = clientConfig;
    }

    public void open() {
        this.clientPool.initialize(new GenericObjectPool<>(new PooledClientFactory(this.clientConfig, this), this.poolConfig));
    }

    @Override
    public void close() {
        WListClientManager.quicklyUninitialize(this.clientConfig.address);
    }

    protected record PooledClientFactory(@NotNull ClientManagerConfig configuration, @NotNull WListClientManager manager) implements PooledObjectFactory<WListClientInterface> {
        @Override
        public @NotNull PooledObject<@NotNull WListClientInterface> makeObject() throws IOException {
            final WListClient client = new WListClient(this.configuration.address);
            try {
                client.open();
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                client.close();
                throw exception;
            }
            return new DefaultPooledObject<>(new WrappedClient(client, this.manager));
        }

        @Override
        public void destroyObject(final @NotNull PooledObject<@NotNull WListClientInterface> p) {
            ((WrappedClient) p.getObject()).closeInside();
        }

        @Override
        public void activateObject(final @NotNull PooledObject<@NotNull WListClientInterface> p) {
        }

        @Override
        public void passivateObject(final @NotNull PooledObject<@NotNull WListClientInterface> p) {
        }

        @Override
        public boolean validateObject(final @NotNull PooledObject<@NotNull WListClientInterface> p) {
            return p.getObject().isActive();
        }

        @Override
        public @NotNull String toString() {
            return "PooledClientFactory{" +
                    "configuration=" + this.configuration +
                    '}';
        }
    }

    protected final @NotNull Set<@NotNull WrappedClient> activeClients = ConcurrentHashMap.newKeySet();

    public @NotNull WListClientInterface getClient() throws IOException {
        final WrappedClient real;
        try {
            real = (WrappedClient) this.clientPool.getInstance().borrowObject();
        } catch (final IOException exception) {
            this.close();
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        this.activeClients.add(real);
        return real;
    }

    protected record WrappedClient(@NotNull WListClient client, @NotNull WListClientManager manager) implements WListClientInterface {
        @Override
        public void open() {
            throw new IllegalStateException("Cannot open wrapped client.");
        }

        @Override
        public @NotNull SocketAddress getAddress() {
                return this.client.getAddress();
            }

        @Override
        public @NotNull ByteBuf send(final @Nullable ByteBuf msg) throws IOException, InterruptedException {
            return this.client.send(msg);
        }

        @Override
        public void close() {
            this.manager.activeClients.remove(this);
            final GenericObjectPool<WListClientInterface> pool = this.manager.clientPool.getInstanceNullable();
            if (pool != null) // The pool may be closed before each active client had closed.
                pool.returnObject(this);
        }

        public void closeInside() {
            this.manager.activeClients.remove(this);
            this.client.close();
        }

        @Override
        public boolean isActive() {
            return this.client.isActive();
        }

        @Override
        public @NotNull String toString() {
            return "WrappedClient{" +
                    "client=" + this.client +
                    '}';
        }
    }

    @Override
    public @NotNull String toString() {
        return "WListClientManager{" +
                "poolConfig=" + this.poolConfig +
                ", clientConfig=" + this.clientConfig +
                ", clientPool=" + this.clientPool +
                ", activeClients=" + this.activeClients +
                '}';
    }
}
