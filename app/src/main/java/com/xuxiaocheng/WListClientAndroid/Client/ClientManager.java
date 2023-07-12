package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.ConsumerE;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WListClient.Client.WListClient;
import com.xuxiaocheng.WListClient.Utils.MiscellaneousUtil;
import com.xuxiaocheng.WListClientAndroid.Utils.ARandomHelper;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ClientManager {
    @NonNull public static final EventExecutorGroup ThreadPool = new DefaultEventExecutorGroup(32);

    @Nullable private static ClientManager Instance;

    public static synchronized void initialize(@NonNull final ClientManagerConfig config) throws InterruptedException, ConnectException {
        if (ClientManager.Instance != null)
            throw new IllegalStateException("Client manager is initialized." + ParametersMap.create()
                    .add("instance", ClientManager.Instance).add("config", config));
        ClientManager.Instance = new ClientManager(config);
    }

    @NonNull public static synchronized ClientManager getInstance() {
        if (ClientManager.Instance == null)
            throw new IllegalStateException("Client manager is not initialized.");
        return ClientManager.Instance;
    }

    @NonNull protected final ClientManagerConfig config;
    @NonNull protected final AtomicInteger createdSize = new AtomicInteger(0);
    @NonNull protected final BlockingQueue<ReferencedClient> freeClients = new LinkedBlockingQueue<>();
    @NonNull protected final ConcurrentMap<String, ReferencedClient> activeClients = new ConcurrentHashMap<>();
    @NonNull protected final Object needIdleClient = new Object();

    protected ClientManager(@NonNull final ClientManagerConfig config) throws InterruptedException, ConnectException {
        super();
        this.config = config;
        if (this.config.initSize > this.config.maxSize)
            throw new IllegalStateException("Init client count > max count. config: " + this.config);
        for (int i = 0; i < this.config.initSize; ++i)
            this.freeClients.add(this.createNewClient());
        assert this.freeClients.size() == this.config.initSize;
        assert this.createdSize.get() == this.config.initSize;
    }

    protected static class ClientManagerConfig {
        @NonNull protected final SocketAddress address;
        protected final int initSize;
        protected final int averageSize;
        protected final int maxSize;

        public ClientManagerConfig(@NonNull final SocketAddress address, final int initSize, final int averageSize, final int maxSize) {
            super();
            this.address = address;
            this.initSize = initSize;
            this.averageSize = averageSize;
            this.maxSize = maxSize;
        }

        @Override
        @NonNull public String toString() {
            return "ClientManagerConfig{" +
                    "address=" + this.address +
                    ", initSize=" + this.initSize +
                    ", averageSize=" + this.averageSize +
                    ", maxSize=" + this.maxSize +
                    '}';
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (!(o instanceof final ClientManagerConfig that)) return false;
            return this.initSize == that.initSize && this.averageSize == that.averageSize && this.maxSize == that.maxSize && this.address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.address, this.initSize, this.averageSize, this.maxSize);
        }
    }

    @NonNull protected final ReferencedClient createNewClient() throws InterruptedException, ConnectException {
        if (this.createdSize.getAndIncrement() < this.config.maxSize)
            return new ReferencedClient(this.config.address, this);
        this.createdSize.getAndDecrement();
        ReferencedClient client = null;
        synchronized (this.needIdleClient) {
            while (client == null) {
                this.needIdleClient.wait();
                client = this.freeClients.poll();
            }
        }
        return client;
    }

    protected static final class ReferencedClient extends WListClient {
        @NonNull private final ClientManager manager;
        private int referenceCounter = 0;
        @NonNull private String id = "";

        public ReferencedClient(@NonNull final SocketAddress address, @NonNull final ClientManager manager) throws InterruptedException, ConnectException {
            super(address);
            this.manager = manager;
        }

        private void retain() {
            if (this.referenceCounter < 0)
                throw new IllegalReferenceCountException(this.referenceCounter);
            ++this.referenceCounter;
        }

        @Override
        public void close() {
            if (this.referenceCounter < 1)
                throw new IllegalReferenceCountException(this.referenceCounter);
            if (--this.referenceCounter < 1)
                this.manager.recycleClient(this.id);
        }

        private void closeInside() {
            super.close();
        }

        @Override
        @NonNull public String toString() {
            return "ReferencedClient{" +
                    "manager=" + this.manager +
                    ", referenceCounter=" + this.referenceCounter +
                    ", id='" + this.id + '\'' +
                    ", super=" + super.toString() +
                    '}';
        }
    }

    @NonNull public WListClient getExplicitClient(@NonNull final String id) throws SQLException, InterruptedException, ConnectException {
        final ReferencedClient client;
        try {
            client = this.activeClients.computeIfAbsent(id, HExceptionWrapper.wrapFunction(k -> {
                ReferencedClient newClient = this.freeClients.poll();
                if (newClient == null)
                    newClient = this.createNewClient();
                newClient.id = id;
                return newClient;
            }));
            assert id.equals(client.id);
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(
                    HExceptionWrapper.unwrapException(
                            HExceptionWrapper.unwrapException(exception,
                                    SQLException.class),
                            InterruptedException.class),
                    ConnectException.class);
        }
        client.retain();
        return client;
    }

    @NonNull public WListClient getNewClient(@Nullable final Consumer<? super String> idSaver) throws InterruptedException, ConnectException {
        ReferencedClient client = this.freeClients.poll();
        if (client == null)
            client = this.createNewClient();
        final String id = MiscellaneousUtil.randomKeyAndPut(this.activeClients,
                () -> ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, HRandomHelper.DefaultWords), client);
        client.id = id;
        if (idSaver != null)
            idSaver.accept(id);
        client.retain();
        return client;
    }

    @NonNull public WListClient getNewClient() throws InterruptedException, ConnectException {
        return this.getNewClient(ConsumerE.emptyConsumer());
    }

    @NonNull public WListClient getNewClient(@NonNull final AtomicReference<? super String> clientId) throws InterruptedException, ConnectException {
        return this.getNewClient(clientId::set);
    }

    @NonNull public WListClient getClient(@Nullable final String id, @NonNull final AtomicReference<? super String> clientId) throws SQLException, InterruptedException, ConnectException {
        if (id == null)
            return this.getNewClient(clientId);
        clientId.set(id);
        return this.getExplicitClient(id);
    }

    protected void recycleClient(@NonNull final String id) {
        final ReferencedClient client = this.activeClients.remove(id);
        if (client == null)
            return;
        if (this.createdSize.get() > this.config.averageSize || !client.isActive()) {
            client.closeInside();
            this.createdSize.getAndDecrement();
            return;
        }
        synchronized (this.needIdleClient) {
            this.freeClients.add(client);
            this.needIdleClient.notify();
        }
    }

    @Override
    @NonNull public String toString() {
        return "ClientManager{" +
                "config=" + this.config +
                ", createdSize=" + this.createdSize +
                ", freeClients=" + this.freeClients +
                ", activeClients=" + this.activeClients +
                '}';
    }
}
