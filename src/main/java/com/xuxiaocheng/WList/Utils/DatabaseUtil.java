package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import io.netty.util.IllegalReferenceCountException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.JDBC;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DatabaseUtil {
    private static @Nullable DatabaseUtil Instance;

    public static synchronized @NotNull DatabaseUtil getInstance() throws SQLException {
        if (DatabaseUtil.Instance == null)
            DatabaseUtil.Instance = new DatabaseUtil(new PooledDatabaseConfig(
                    new File(GlobalConfiguration.getInstance().databasePath()),
                    2, 3, 10, false, true, Connection.TRANSACTION_READ_COMMITTED
            ));
        return DatabaseUtil.Instance;
    }

    protected final @NotNull DataSource dataSource;
    protected final @NotNull PooledDatabaseConfig config;
    protected final @NotNull AtomicInteger createdSize = new AtomicInteger(0);
    protected final @NotNull BlockingQueue<@NotNull ReferencedConnection> freeConnections = new LinkedBlockingQueue<>();
    protected final @NotNull ConcurrentMap<@NotNull String, @NotNull ReferencedConnection> activeConnections = new ConcurrentHashMap<>();
    protected final @NotNull Object needIdleConnection = new Object();

    protected DatabaseUtil(final @NotNull PooledDatabaseConfig config) throws SQLException {
        super();
        this.config = config;
        if (config.initSize > config.maxSize)
            throw new IllegalStateException("Init connection count > max count. config: " + config);
        final File path = this.config.path.getAbsoluteFile();
        if (!HFileHelper.ensureFileExist(path))
            throw new SQLException("Cannot create database file.");
        this.dataSource = new SQLiteDataSource();
        ((SQLiteDataSource) this.dataSource).setUrl(JDBC.PREFIX + path.getPath());
        if (config.walMode) {
            final ReferencedConnection connection = this.createNewConnection();
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA journal_mode = WAL;");
            }
            this.freeConnections.add(connection);
        }
        for (int i = config.walMode ? 1 : 0; i < this.config.initSize; ++i)
            this.freeConnections.add(this.createNewConnection());
        assert this.freeConnections.size() == this.config.initSize;
        assert this.createdSize.get() == this.config.initSize;
    }

    protected final @NotNull ReferencedConnection createNewConnection() throws SQLException {
        if (this.createdSize.getAndIncrement() < this.config.maxSize) {
            final Connection rawConnection = this.dataSource.getConnection();
            if (rawConnection == null) {
                this.createdSize.getAndDecrement();
                throw new SQLException("Failed to get connection with sqlite database source.");
            }
            return (ReferencedConnection) Proxy.newProxyInstance(rawConnection.getClass().getClassLoader(),
                    PooledConnectionProxy.ConnectionProxy, new PooledConnectionProxy(rawConnection, this));
        }
        this.createdSize.getAndDecrement();
        ReferencedConnection connection = null;
        try {
            synchronized (this.needIdleConnection) {
                while (connection == null) {
                    this.needIdleConnection.wait();
                    connection = this.freeConnections.poll();
                }
            }
        } catch (final InterruptedException exception) {
            throw new SQLException(exception);
        }
        return connection;
    }

    protected interface ReferencedConnection extends Connection {
        void retain();
        @NotNull String id();
        void setId(final @NotNull String id);
        @NotNull Connection inside();
    }

    protected static final class PooledConnectionProxy implements InvocationHandler {
        private static final Class<?>[] ConnectionProxy = new Class[] {ReferencedConnection.class};

        private final @NotNull Connection connection;
        private final @NotNull DatabaseUtil util;
        private int referenceCounter = 0;
        private @NotNull String id = "";

        private PooledConnectionProxy(final @NotNull Connection connection, final @NotNull DatabaseUtil util) {
            super();
            this.connection = connection;
            this.util = util;
        }

        @Override
        public synchronized @Nullable Object invoke(final @NotNull Object proxy, final @NotNull Method method, final Object @Nullable [] args) throws IllegalAccessException, InvocationTargetException, SQLException {
            if (this.referenceCounter < 0)
                throw new IllegalReferenceCountException(this.referenceCounter);
            switch (method.getName()) {
                case "retain": {
                    ++this.referenceCounter;
                    return null;
                }
                case "id": {
                    if (this.referenceCounter < 1)
                        throw new IllegalReferenceCountException(0);
                    return this.id;
                }
                case "setId": {
                    if (this.referenceCounter > 0)
                        throw new IllegalReferenceCountException(this.referenceCounter);
                    assert args != null && args.length == 1;
                    this.id = (String) args[0];
                    return null;
                }
                case "close": {
                    if (this.referenceCounter < 1)
                        throw new IllegalReferenceCountException(0);
                    if (--this.referenceCounter < 1)
                        this.util.recycleConnection(this.id);
                    return null;
                }
                case "inside": return this.connection;
                case "commit": {
                    if (this.referenceCounter != 1)
                        return null;
                }
            }
            return method.invoke(this.connection, args);
        }

        @Override
        public synchronized @NotNull String toString() {
            return "PooledConnectionProxy{" +
                    "connection=" + this.connection +
                    ", util=" + this.util +
                    ", referenceCounter=" + this.referenceCounter +
                    ", id='" + this.id + '\'' +
                    '}';
        }
    }

    public @NotNull Connection getConnection(final @Nullable String connectionId) throws SQLException {
        if (connectionId == null)
            return this.getNewConnection(null);
        final ReferencedConnection connection;
        try {
            connection = this.activeConnections.computeIfAbsent(connectionId, HExceptionWrapper.wrapFunction(k -> {
                ReferencedConnection newConnection = this.freeConnections.poll();
                if (newConnection == null)
                    newConnection = this.createNewConnection();
                newConnection.setId(connectionId);
                return newConnection;
            }));
            assert connectionId.equals(connection.id());
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
        connection.retain();
        return connection;
    }

    public @NotNull Connection getNewConnection(final @Nullable Consumer<? super @NotNull String> indexSaver) throws SQLException {
        ReferencedConnection connection = this.freeConnections.poll();
        if (connection == null)
            connection = this.createNewConnection();
        final String id = MiscellaneousUtil.randomKeyAndPut(this.activeConnections,
                () -> HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, HRandomHelper.DefaultWords),
                connection);
        connection.setId(id);
        if (indexSaver != null)
            indexSaver.accept(id);
        connection.retain();
        return connection;
    }

    protected void recycleConnection(final @NotNull String id) throws SQLException {
        final ReferencedConnection connection = this.activeConnections.remove(id);
        this.resetConnection(connection);
        if (this.createdSize.get() > this.config.averageSize) {
            connection.inside().close();
            this.createdSize.getAndDecrement();
            return;
        }
        synchronized (this.needIdleConnection) {
            this.freeConnections.add(connection);
            this.needIdleConnection.notify();
        }
    }

    protected void resetConnection(final @NotNull Connection connection) throws SQLException {
        if (connection.isClosed())
            throw new SQLException("Closed connection.");
        connection.clearWarnings();
        if (!connection.getAutoCommit())
            connection.rollback();
        if (connection.getAutoCommit() != this.config.autoCommit)
            connection.setAutoCommit(this.config.autoCommit);
        if (connection.getTransactionIsolation() != this.config.transactionIsolationLevel)
            connection.setTransactionIsolation(this.config.transactionIsolationLevel);
    }

    @Override
    public @NotNull String toString() {
        return "DatabaseUtil{" +
                "config=" + this.config +
                ", createdSize=" + this.createdSize +
                ", freeConnections=" + this.freeConnections +
                ", activeConnections=" + this.activeConnections +
                '}';
    }

    protected record PooledDatabaseConfig(@NotNull File path, int initSize, int averageSize, int maxSize, boolean walMode, boolean autoCommit, int transactionIsolationLevel) {
    }

    /**
     * Automatically get not null connection with DatabaseUtil.
     * Standard code: @code {
     *  final Connection connection = DatabaseUtil.requireConnection(_connection, databaseUtil);
     *  try {
     *      // use connection.
     *  } finally {
     *      if (_connection == null)
     *          connection.close();
     *  }
     * }
     */
    @Deprecated
    public static @NotNull Connection requireConnection(final @Nullable String id, final @NotNull DatabaseUtil util) throws SQLException {
        // Objects.requireNonNullElseGet(_connection, DatabaseUtil.getIndexInstance()::getConnection); (With SQLException)
        return util.getConnection(id);
    }
}
