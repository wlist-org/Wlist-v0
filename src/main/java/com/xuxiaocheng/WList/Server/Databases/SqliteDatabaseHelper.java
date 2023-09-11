package com.xuxiaocheng.WList.Server.Databases;

import com.xuxiaocheng.HeadLibs.AndroidSupport.AndroidSupporter;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helpers.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.Commons.Utils.MiscellaneousUtil;
import io.netty.util.IllegalReferenceCountException;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.Function;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
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

// TODO SQLCipher (io.github.willena:sqlite-jdbc)
public class SqliteDatabaseHelper implements SqlDatabaseInterface {
    protected final @NotNull GenericObjectPoolConfig<Connection> poolConfig;
    protected final @NotNull PooledDatabaseConfig connectionConfig;
    protected final @NotNull HInitializer<GenericObjectPool<@NotNull Connection>> connectionPool = new HInitializer<>("ConnectionPool");

    public static @NotNull SqliteDatabaseHelper getDefault(final @NotNull File database) {
        final GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setJmxEnabled(AndroidSupporter.jmxEnable); // default: true
        poolConfig.setTestOnBorrow(true);
        return new SqliteDatabaseHelper(poolConfig, new SqliteDatabaseHelper.PooledDatabaseConfig(database,
                SQLiteConfig.JournalMode.PERSIST, Connection.TRANSACTION_SERIALIZABLE));
    }

    public record PooledDatabaseConfig(@NotNull File path, SQLiteConfig.@NotNull JournalMode journalMode, int transactionIsolationLevel) implements SqlDatabaseManager.PooledDatabaseIConfiguration {
    }

    public SqliteDatabaseHelper(final @NotNull GenericObjectPoolConfig<Connection> poolConfig, final SqliteDatabaseHelper.@NotNull PooledDatabaseConfig connectionConfig) {
        super();
        this.poolConfig = poolConfig;
        this.connectionConfig = connectionConfig;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String sqlLanguage() {
        return "Sqlite";
    }

    @Override
    public void openIfNot() throws SQLException {
        if (this.connectionPool.isInitialized()) return;
        final File file = this.connectionConfig.path();
        try {
            HFileHelper.ensureFileAccessible(file, true);
        } catch (final IOException exception) {
            throw new SQLException("Cannot create database file." + ParametersMap.create().add("file", file), exception);
        }
        final SQLiteDataSource database = new SQLiteDataSource();
        database.setUrl(JDBC.PREFIX + file.getPath());
        database.setJournalMode(this.connectionConfig.journalMode().getValue());
        this.connectionPool.initialize(new GenericObjectPool<>(new PooledConnectionFactory(database, this.connectionConfig, this), this.poolConfig));
    }

    @Override
    public void close() throws SQLException {
        final GenericObjectPool<Connection> pool = this.connectionPool.uninitializeNullable();
        if (pool != null)
            pool.close();
        for (final ReferencedConnection connection: this.activeConnections.values())
            connection.closePool();
        assert this.activeConnections.isEmpty();
    }

    protected record PooledConnectionFactory(@NotNull DataSource source, @NotNull PooledDatabaseConfig configuration, @NotNull SqliteDatabaseHelper database) implements PooledObjectFactory<Connection> {
        @Override
        public @NotNull PooledObject<@NotNull Connection> makeObject() throws SQLException {
            final Connection connection = this.source.getConnection();
            if (connection == null)
                throw new SQLException("Failed to get connection with sqlite database source." + ParametersMap.create().add("source", this.source));
            // REGEXP fixer
            Function.create(connection, "regexp", new Function() {
                @Override
                protected void xFunc() throws SQLException {
                    final int args = this.args();
                    if (args != 2)
                        throw new SQLException("REGEXP requires two arguments.");
                    final String pattern = this.value_text(0);
                    final String value = this.value_text(1);
                    this.result(value.matches(pattern) ? 1 : 0);
                }
            });
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
            connection.setAutoCommit(false);
            if (connection.getTransactionIsolation() != this.configuration.transactionIsolationLevel())
                connection.setTransactionIsolation(this.configuration.transactionIsolationLevel());
        }

        @Override
        public void passivateObject(final @NotNull PooledObject<@NotNull Connection> p) throws SQLException {
            p.getObject().rollback();
        }

        @Override
        public boolean validateObject(final @NotNull PooledObject<@NotNull Connection> p) {
            try {
                return p.getObject().isValid(0);
            } catch (final SQLException exception) {
                throw new RuntimeException("Unreachable!", exception);
            }
        }

        @Override
        public @NotNull String toString() {
            return "PooledConnectionFactory{" +
                    "source=" + this.source +
                    ", configuration=" + this.configuration +
                    '}';
        }
    }

    protected final @NotNull Map<@NotNull String, @NotNull ReferencedConnection> activeConnections = new ConcurrentHashMap<>();

    protected static final Class<?>[] ConnectionProxy = new Class[] {ReferencedConnection.class};
    protected interface ReferencedConnection extends Connection {
        void setId(final @NotNull String id);
        void retain();
        // void close() throws SQLException;
        void closePool() throws SQLException;
    }
    protected static final class ReferencedConnectionProxy implements InvocationHandler {
        private @Nullable String id;
        private final @NotNull Connection rawConnection;
        private final @NotNull SqliteDatabaseHelper databaseHelper;

        private int referenceCounter = 0;
        private @Nullable String allow = null;

        private ReferencedConnectionProxy(final @Nullable String id, final @NotNull Connection rawConnection, final @NotNull SqliteDatabaseHelper databaseHelper) {
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
                        final GenericObjectPool<Connection> pool = this.databaseHelper.connectionPool.getInstanceNullable();
                        if (pool != null)
                            pool.returnObject(this.rawConnection);
                    }
                    return null;
                }
                case "closePool" -> {
                    this.databaseHelper.activeConnections.remove(this.id, (ReferencedConnection) proxy);
                    this.rawConnection.close();
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
                SqliteDatabaseHelper.ConnectionProxy, new ReferencedConnectionProxy(id, rawConnection, this));
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
                () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 32, HRandomHelper.AnyWords),
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
        return "SqliteDatabaseHelper{" +
                "poolConfig=" + this.poolConfig +
                ", connectionConfig=" + this.connectionConfig +
                ", connectionPool=" + this.connectionPool +
                ", activeConnections=" + this.activeConnections.size() +
                '}';
    }
}
