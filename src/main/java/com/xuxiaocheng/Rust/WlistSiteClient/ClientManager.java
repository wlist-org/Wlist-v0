package com.xuxiaocheng.Rust.WlistSiteClient;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClientManager implements AutoCloseable {
    public static final @NotNull HInitializer<ClientManager> instance = new HInitializer<>("SiteClientManager");

    public static void quicklyInitialize(final @NotNull ClientManager manager) {
        ClientManager.instance.initializeIfNot(() -> {
            manager.open();
            return manager;
        });
    }

    public static void quicklyUninitialize() {
        final ClientManager manager = ClientManager.instance.uninitializeNullable();
        if (manager != null)
            manager.close();
    }

    public static ClientCore.@NotNull WlistSiteClient quicklyGetClient() throws IOException {
        return ClientManager.instance.getInstance().getClient();
    }

    public static @NotNull ClientManager getDefault() {
        final GenericObjectPoolConfig<ClientCore.WlistSiteClient> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setJmxEnabled(AndroidSupporter.jmxEnable);
        return new ClientManager(poolConfig);
    }

    protected final @NotNull GenericObjectPoolConfig<ClientCore.WlistSiteClient> poolConfig;
    protected final @NotNull HInitializer<GenericObjectPool<ClientCore.WlistSiteClient>> clientPool = new HInitializer<>("SiteClientPool");

    public ClientManager(final @NotNull GenericObjectPoolConfig<ClientCore.WlistSiteClient> poolConfig) {
        super();
        this.poolConfig = poolConfig;
    }

    public void open() {
        this.clientPool.initialize(new GenericObjectPool<>(new PooledSiteClientFactory(this), this.poolConfig));
    }

    @Override
    public void close() {
        final GenericObjectPool<ClientCore.WlistSiteClient> pool = this.clientPool.uninitializeNullable();
        if (pool != null)
            pool.close();
    }

    public @NotNull ClientCore.WlistSiteClient getClient() throws IOException {
        try {
            return this.clientPool.getInstance().borrowObject();
        } catch (final IOException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new IOException(exception);
        }
    }

    protected record PooledSiteClientFactory(@NotNull ClientManager manager) implements PooledObjectFactory<ClientCore.WlistSiteClient> {
        @Override
        public @NotNull PooledObject<ClientCore.@NotNull WlistSiteClient> makeObject() throws IOException {
            final ClientCore.WlistSiteClient client = ClientCore.tryConnect();
            if (client == null)
                throw new IOException("Failed to connect to server.");
            return new DefaultPooledObject<>(client);
        }

        @Override
        public void destroyObject(final @NotNull PooledObject<ClientCore.@NotNull WlistSiteClient> pooledObject) {
            pooledObject.getObject().close();
        }

        @Override
        public void activateObject(final @NotNull PooledObject<ClientCore.@NotNull WlistSiteClient> pooledObject) {
        }

        @Override
        public void passivateObject(final @NotNull PooledObject<ClientCore.@NotNull WlistSiteClient> pooledObject) {
        }

        @Override
        public boolean validateObject(final @NotNull PooledObject<ClientCore.@NotNull WlistSiteClient> pooledObject) {
            return true;
        }

        @Override
        public String toString() {
            return "PooledSiteClientFactory{}";
        }
    }

    @Override
    public @NotNull String toString() {
        return "ClientManager{" +
                "poolConfig=" + this.poolConfig +
                ", clientPool=" + this.clientPool +
                '}';
    }
}
