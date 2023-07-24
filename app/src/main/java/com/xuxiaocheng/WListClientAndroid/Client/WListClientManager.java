package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.HeadLibs.Initializer.HMultiInitializers;
import com.xuxiaocheng.WList.Utils.AndroidSupport;
import com.xuxiaocheng.WListClient.Client.WListClient;
import com.xuxiaocheng.WListClient.Client.WListClientInterface;
import io.netty.buffer.ByteBuf;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WListClientManager implements Closeable {
    @NonNull public static final HMultiInitializers<SocketAddress, WListClientManager> instances = new HMultiInitializers<>("WListClientManagers");

    public static void quicklyInitialize(@NonNull final WListClientManager manager) {
        WListClientManager.instances.initializeIfNot(manager.clientConfig.address, () -> {
            manager.open();
            return manager;
        });
    }

    public static void quicklyUninitialize(@NonNull final SocketAddress address) {
        final WListClientManager manager = WListClientManager.instances.uninitialize(address);
        if (manager != null)
            manager.close();
    }

    @NonNull public static WListClientInterface quicklyGetClient(@NonNull final SocketAddress address) {
        return WListClientManager.instances.getInstance(address).getClient();
    }

    @NonNull protected final GenericObjectPoolConfig<WListClient> poolConfig;
    @NonNull protected final ClientManagerConfig clientConfig;
    @NonNull protected final HInitializer<GenericObjectPool<WListClient>> clientPool = new HInitializer<>("ClientPool");

    public static class ClientManagerConfig {
        @NonNull protected final SocketAddress address;

        public ClientManagerConfig(@NonNull final SocketAddress address) {
            super();
            this.address = address;
        }

        @Override
        @NonNull public String toString() {
            return "ClientManagerConfig{" +
                    "address=" + this.address +
                    '}';
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (!(o instanceof final ClientManagerConfig that)) return false;
            return this.address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.address);
        }
    }

    public WListClientManager(@NonNull final GenericObjectPoolConfig<WListClient> poolConfig, @NonNull final ClientManagerConfig clientConfig) {
        super();
        this.poolConfig = poolConfig;
        this.clientConfig = clientConfig;
    }

    @NonNull public static WListClientManager getDefault(@NonNull final SocketAddress address) {
        final GenericObjectPoolConfig<WListClient> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setJmxEnabled(AndroidSupport.jmxEnable);
        poolConfig.setTestOnBorrow(true);
        return new WListClientManager(poolConfig, new ClientManagerConfig(address));
    }

    public void open() {
        this.clientPool.initialize(new GenericObjectPool<>(new PooledClientFactory(this.clientConfig), this.poolConfig));
    }

    @Override
    public void close() {
        final GenericObjectPool<WListClient> pool = this.clientPool.uninitialize();
        if (pool != null)
            pool.close();
        for (final WrappedClient client: this.activeClients)
            client.closePool();
        assert this.activeClients.isEmpty();
    }

    protected static class PooledClientFactory implements PooledObjectFactory<WListClient> {
        @NonNull protected final ClientManagerConfig configuration;

        protected PooledClientFactory(@NonNull final ClientManagerConfig configuration) {
            super();
            this.configuration = configuration;
        }

        @Override
        public PooledObject<WListClient> makeObject() throws InterruptedException, ConnectException {
            return new DefaultPooledObject<>(new WListClient(this.configuration.address));
        }

        @Override
        public void destroyObject(@NonNull final PooledObject<WListClient> p) {
            p.getObject().close();
        }

        @Override
        public void activateObject(@NonNull final PooledObject<WListClient> p) {
        }

        @Override
        public void passivateObject(@NonNull final PooledObject<WListClient> p) {
        }

        @Override
        public boolean validateObject(@NonNull final PooledObject<WListClient> p) {
            return p.getObject().isActive();
        }

        @Override
        @NonNull public String toString() {
            return "PooledClientFactory{" +
                    "configuration=" + this.configuration +
                    '}';
        }
    }

    @NonNull protected final Set<WrappedClient> activeClients = ConcurrentHashMap.newKeySet();

    @NonNull public WListClientInterface getClient() {
        final WrappedClient client;
        try {
            client = new WrappedClient(this.clientPool.getInstance().borrowObject(), this);
        } catch (final Exception exception) {
            throw new RuntimeException("Unreachable!", exception);
        }
        this.activeClients.add(client);
        return client;
    }

    protected static class WrappedClient implements WListClientInterface {
        @NonNull protected final WListClient client;
        @NonNull protected final WListClientManager manager;

        protected WrappedClient(@NonNull final WListClient client, @NonNull final WListClientManager manager) {
            super();
            this.client = client;
            this.manager = manager;
        }

        @Override
        @NonNull public SocketAddress getAddress() {
            return this.client.getAddress();
        }

        @Override
        @NonNull public ByteBuf send(@Nullable final ByteBuf msg) throws InterruptedException {
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
                    ", manager=" + this.manager +
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
