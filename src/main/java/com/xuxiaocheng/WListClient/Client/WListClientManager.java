package com.xuxiaocheng.WListClient.Client;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Initializers.HMultiInitializers;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WListClientManager implements Closeable {
    public static final @NotNull HMultiInitializers<@NotNull SocketAddress, @NotNull WListClientManager> instances = new HMultiInitializers<>("WListClientManagers");

    public static void quicklyInitialize(final @NotNull WListClientManager manager) {
        WListClientManager.instances.initializeIfNot(manager.clientConfig.address, () -> {
            manager.open();
            return manager;
        });
    }

    public static void quicklyUninitialize(final @NotNull SocketAddress address) {
        final WListClientManager manager = WListClientManager.instances.uninitialize(address);
        if (manager != null) {
            final GenericObjectPool<WListClient> pool = manager.clientPool.uninitialize();
            if (pool != null)
                pool.close();
            for (final WrappedClient client: manager.activeClients)
                client.closePool();
            assert manager.activeClients.isEmpty();
        }
    }

    public static @NotNull WListClientInterface quicklyGetClient(final @NotNull SocketAddress address) throws IOException, InterruptedException {
        return WListClientManager.instances.getInstance(address).getClient();
    }

    protected final @NotNull GenericObjectPoolConfig<WListClient> poolConfig;
    protected final @NotNull ClientManagerConfig clientConfig;
    protected final @NotNull HInitializer<GenericObjectPool<@NotNull WListClient>> clientPool = new HInitializer<>("ClientPool");

    public record ClientManagerConfig(@NotNull SocketAddress address) {
    }

    public static @NotNull WListClientManager getDefault(final @NotNull SocketAddress address) {
        final GenericObjectPoolConfig<WListClient> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setJmxEnabled(AndroidSupporter.jmxEnable);
        poolConfig.setTestOnBorrow(true);
        return new WListClientManager(poolConfig, new ClientManagerConfig(address));
    }

    public WListClientManager(final @NotNull GenericObjectPoolConfig<WListClient> poolConfig, final @NotNull ClientManagerConfig clientConfig) {
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

    protected record PooledClientFactory(@NotNull ClientManagerConfig configuration, @NotNull WListClientManager manager) implements PooledObjectFactory<WListClient> {
        @Override
        public @NotNull PooledObject<@NotNull WListClient> makeObject() throws IOException, InterruptedException {
            final WListClient client = new WListClient(this.configuration.address);
            try {
                client.open();
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                client.close();
                throw exception;
            }
            return new DefaultPooledObject<>(client);
        }

        @Override
        public void destroyObject(final @NotNull PooledObject<@NotNull WListClient> p) {
            p.getObject().close();
        }

        @Override
        public void activateObject(final @NotNull PooledObject<@NotNull WListClient> p) {
        }

        @Override
        public void passivateObject(final @NotNull PooledObject<@NotNull WListClient> p) {
        }

        @Override
        public boolean validateObject(final @NotNull PooledObject<@NotNull WListClient> p) {
            return p.getObject().isActive();
        }
    }

    protected final @NotNull Set<@NotNull WrappedClient> activeClients = ConcurrentHashMap.newKeySet();

    public @NotNull WListClientInterface getClient() throws IOException, InterruptedException {
        final WListClient real;
        try {
            real = this.clientPool.getInstance().borrowObject();
        } catch (final IOException | InterruptedException exception) {
            this.close();
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        final WrappedClient client = new WrappedClient(real, this);
        this.activeClients.add(client);
        return client;
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
            final GenericObjectPool<WListClient> pool = this.manager.clientPool.getInstanceNullable();
            if (pool != null)
                pool.returnObject(this.client);
        }

        public void closePool() {
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
