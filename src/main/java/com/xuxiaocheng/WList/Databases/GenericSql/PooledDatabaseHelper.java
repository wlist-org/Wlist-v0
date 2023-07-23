package com.xuxiaocheng.WList.Databases.GenericSql;

import com.xuxiaocheng.HeadLibs.AndroidSupport.ARandomHelper;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import io.netty.util.IllegalReferenceCountException;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

// TODO SQLCipher
public class PooledDatabaseHelper implements PooledDatabaseInterface {
    protected final @NotNull GenericObjectPoolConfig<Connection> poolConfig;
    protected final @NotNull PooledDatabaseConfig connectionConfig;
    protected final @NotNull HInitializer<GenericObjectPool<@NotNull Connection>> connectionPool = new HInitializer<>("ConnectionPool");

    public record PooledDatabaseConfig(@NotNull File source, boolean autoCommit, SQLiteConfig.@NotNull JournalMode journalMode, int transactionIsolationLevel) {
    }

    public PooledDatabaseHelper(final @NotNull GenericObjectPoolConfig<Connection> poolConfig, final @NotNull PooledDatabaseHelper.PooledDatabaseConfig connectionConfig) {
        super();
        this.poolConfig = poolConfig;
        this.connectionConfig = connectionConfig;
    }

    public static @NotNull PooledDatabaseHelper getDefault(final @NotNull File database) {
        final GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setTestOnBorrow(true);
        return new PooledDatabaseHelper(poolConfig, new PooledDatabaseHelper.PooledDatabaseConfig(database, false,
                SQLiteConfig.JournalMode.WAL, Connection.TRANSACTION_READ_COMMITTED));
    }

    @Override
    public void open() throws SQLException {
        final File path = this.connectionConfig.source();
        if (!HFileHelper.ensureFileExist(path))
            throw new SQLException("Cannot create database file." + ParametersMap.create().add("path", path));
        final SQLiteDataSource database = new SQLiteDataSource();
        database.setUrl(JDBC.PREFIX + path.getPath());
        database.setJournalMode(this.connectionConfig.journalMode().getValue());
        this.connectionPool.initialize(new GenericObjectPool<>(new PooledConnectionFactory(database, this.connectionConfig, this), this.poolConfig));
    }

    @Override
    public void close() {
        final GenericObjectPool<Connection> pool = this.connectionPool.uninitialize();
        if (pool != null)
            pool.close();
    }

    protected record PooledConnectionFactory(@NotNull DataSource source, @NotNull PooledDatabaseConfig configuration, @NotNull PooledDatabaseHelper database) implements PooledObjectFactory<Connection> {
        @Override
        public @NotNull PooledObject<@NotNull Connection> makeObject() throws SQLException {
            final Connection connection = this.source.getConnection();
            if (connection == null)
                throw new SQLException("Failed to get connection with sqlite database source." + ParametersMap.create().add("source", this.source));
            return new DefaultPooledObject<>(connection);
        }

        @Override
        public void destroyObject(final @NotNull PooledObject<@NotNull Connection> p) throws SQLException {
            p.getObject().close();
        }

        @SuppressWarnings("MagicConstant")
        @Override
        public void activateObject(@NotNull final PooledObject<@NotNull Connection> p) throws SQLException {
            final Connection connection = p.getObject();
            connection.clearWarnings();
            if (connection.getAutoCommit() != this.configuration.autoCommit())
                connection.setAutoCommit(this.configuration.autoCommit());
            if (connection.getTransactionIsolation() != this.configuration.transactionIsolationLevel())
                connection.setTransactionIsolation(this.configuration.transactionIsolationLevel());
        }

        @Override
        public void passivateObject(final @NotNull PooledObject<@NotNull Connection> p) throws SQLException {
            final Connection connection = p.getObject();
            if (!connection.getAutoCommit())
                connection.rollback();
        }

        @Override
        public boolean validateObject(final @NotNull PooledObject<@NotNull Connection> p) {
            try {
                return p.getObject().isValid(0);
            } catch (final SQLException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
        }
    }

    protected final @NotNull Map<@NotNull String, @NotNull ReferencedConnection> activeConnections = new ConcurrentHashMap<>();

    protected static final Class<?>[] ConnectionProxy = new Class[] {ReferencedConnection.class};
    protected interface ReferencedConnection extends Connection {
        void setId(final @NotNull String id);
        void retain();
        // void close() throws SQLException;
    }
    protected static final class ReferencedConnectionProxy implements InvocationHandler {
        private @Nullable String id;
        private final @NotNull Connection rawConnection;
        private final @NotNull PooledDatabaseHelper databaseHelper;

        private int referenceCounter = 0;
        private @Nullable String allow = null;

        private ReferencedConnectionProxy(final @Nullable String id, final @NotNull Connection rawConnection, final @NotNull PooledDatabaseHelper databaseHelper) {
            super();
            this.id = id;
            this.rawConnection = rawConnection;
            this.databaseHelper = databaseHelper;
        }

        @Override
        public synchronized @Nullable Object invoke(final @NotNull Object proxy, final @NotNull Method method, final Object @Nullable [] args) throws SQLException {
            if (this.referenceCounter < 0)
                throw new IllegalReferenceCountException(this.referenceCounter);
            switch (method.getName()) {
                case "setId" -> {
                    assert args != null && args.length == 1;
                    this.id = (String) args[0];
                    return null;
                }
                case "retain" -> {
                    ++this.referenceCounter;
                    return null;
                }
                case "close" -> {
                    if (this.referenceCounter < 1)
                        throw new IllegalReferenceCountException(0);
                    if (--this.referenceCounter < 1) {
                        this.allow = null;
                        assert this.id != null;
                        this.databaseHelper.activeConnections.remove(this.id, (ReferencedConnection) proxy);
                        this.databaseHelper.connectionPool.getInstance().returnObject(this.rawConnection);
                    }
                    return null;
                }
                case "commit" -> {
                    if (this.referenceCounter != 1)
                        return null;
                }
            }
            if (this.allow != null)
                throw new SQLWarning("Something went wrong on this connection (" + this.rawConnection + "). Usually caused in other threads.", this.allow);
            try {
                return method.invoke(this.rawConnection, args);
            } catch (final IllegalAccessException exception) {
                throw new RuntimeException(exception);
            } catch (final InvocationTargetException exception) {
                if (exception.getCause() instanceof SQLException sqlException) {
                    this.allow = sqlException.getMessage();
                    throw sqlException;
                }
                throw new RuntimeException(exception.getCause());
            }
        }

        @Override
        public synchronized @NotNull String toString() {
            return "ReferencedConnectionProxy{" +
                    "id='" + this.id + '\'' +
                    ", rawConnection=" + this.rawConnection +
                    ", databaseHelper=" + this.databaseHelper +
                    ", referenceCounter=" + this.referenceCounter +
                    ", allow='" + this.allow + '\'' +
                    '}';
        }
    }

    protected @NotNull ReferencedConnection createNewConnection(final @Nullable String id) throws SQLException {
        final Connection rawConnection;
        try {
            rawConnection = this.connectionPool.getInstance().borrowObject();
        } catch (final Exception exception) {
            throw new RuntimeException(HExceptionWrapper.unwrapException(exception, SQLException.class));
        }
        return (ReferencedConnection) Proxy.newProxyInstance(rawConnection.getClass().getClassLoader(),
                PooledDatabaseHelper.ConnectionProxy, new ReferencedConnectionProxy(id, rawConnection, this));
    }

    @Override
    public @NotNull Connection getExplicitConnection(final @NotNull String id) throws SQLException {
        if (!this.connectionPool.isInitialized() || this.connectionPool.getInstance().isClosed())
            throw new IllegalStateException("Closed connection pool.");
        final ReferencedConnection connection;
        try {
            connection = this.activeConnections.computeIfAbsent(id, HExceptionWrapper.wrapFunction(this::createNewConnection));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
        connection.retain();
        return connection;
    }

    @Override
    public @NotNull Connection getNewConnection(final @Nullable Consumer<? super @NotNull String> idSaver) throws SQLException {
        if (!this.connectionPool.isInitialized() || this.connectionPool.getInstance().isClosed())
            throw new IllegalStateException("Closed connection pool.");
        final ReferencedConnection connection = this.createNewConnection(null);
        connection.retain();
        final String id = MiscellaneousUtil.randomKeyAndPut(this.activeConnections,
                () -> ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, HRandomHelper.DefaultWords),
                connection);
        connection.setId(id);
        if (idSaver != null)
            idSaver.accept(id);
        return connection;
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        if (!this.connectionPool.isInitialized() || this.connectionPool.getInstance().isClosed())
            throw new IllegalStateException("Closed connection pool.");
        if (_connectionId == null)
            return this.getNewConnection(connectionId == null ? null : connectionId::set);
        if (connectionId != null)
            connectionId.set(_connectionId);
        return this.getExplicitConnection(_connectionId);
    }

    @Override
    public @NotNull String toString() {
        return "PooledDatabaseHelper{" +
                "connectionPool=" + this.connectionPool +
                ", activeConnections=" + this.activeConnections +
                '}';
    }
}
