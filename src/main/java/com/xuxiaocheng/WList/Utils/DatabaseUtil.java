package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseUtil {
    private static @Nullable DatabaseUtil Instance;

    public static synchronized @NotNull DatabaseUtil getInstance() throws SQLException {
        if (DatabaseUtil.Instance == null)
            DatabaseUtil.Instance = new DatabaseUtil(new PooledDatabaseConfig(
                    new File(GlobalConfiguration.getInstance().databasePath()),
                    3, 2, 10, false, true, Connection.TRANSACTION_READ_COMMITTED
            ));
        return DatabaseUtil.Instance;
    }

    protected final @NotNull DataSource dataSource;
    protected final @NotNull PooledDatabaseConfig config;
    protected final @NotNull AtomicInteger createdSize = new AtomicInteger(0);
    protected final @NotNull Queue<@NotNull Connection> freeConnections = new ConcurrentLinkedQueue<>();
    protected final @NotNull Map<@NotNull Thread, Pair.@NotNull ImmutablePair<@NotNull Connection, @NotNull AtomicInteger>> threadConnections = new ConcurrentHashMap<>();
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
            final Connection connection = this.createNewConnection();
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

    protected final @NotNull Connection createNewConnection() throws SQLException {
        if (this.createdSize.getAndIncrement() < this.config.maxSize) {
            final Connection rawConnection = this.dataSource.getConnection();
            if (rawConnection == null) {
                this.createdSize.getAndDecrement();
                throw new SQLException("Failed to get connection with sqlite database source.");
            }
            return (Connection) Proxy.newProxyInstance(rawConnection.getClass().getClassLoader(),
                    DatabaseUtil.ConnectionProxy, new PooledConnectionProxy(rawConnection, this));
        }
        this.createdSize.getAndDecrement();
        Connection connection = null;
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

    private static final Class<?>[] ConnectionProxy = new Class[] {Connection.class};

    protected record PooledConnectionProxy(@NotNull Connection connection, @NotNull DatabaseUtil util) implements InvocationHandler {
        @Override
        public @Nullable Object invoke(final @NotNull Object proxy, final @NotNull Method method, final Object @Nullable [] args) throws IllegalAccessException, InvocationTargetException, SQLException {
            if (!"close".equals(method.getName()))
                return method.invoke(this.connection, args);
            this.util.recycleConnection(this.connection);
            return null;
        }
    }

    // Due to thread dependency, synchronization is not required.
    public @NotNull Connection getConnection() throws SQLException {
        final Thread current = Thread.currentThread();
        final Pair.ImmutablePair<Connection, AtomicInteger> pair = this.threadConnections.get(current);
        if (pair != null) {
            pair.getSecond().getAndIncrement();
            return pair.getFirst();
        }
        Connection connection = this.freeConnections.poll();
        if (connection == null)
            connection = this.createNewConnection();
        this.threadConnections.put(current, Pair.ImmutablePair.makeImmutablePair(connection, new AtomicInteger(1)));
        return connection;
    }

    // Due to thread dependency, synchronization is not required.
    public void recycleConnection(final @NotNull Connection connection) throws SQLException {
        final Thread current = Thread.currentThread();
        final Pair.ImmutablePair<Connection, AtomicInteger> pair = this.threadConnections.get(current);
        assert pair != null && pair.getFirst() == connection;
        if (pair.getSecond().getAndDecrement() > 1)
            return;
        this.threadConnections.remove(current);
        this.resetConnection(connection);
        if (this.createdSize.get() > this.config.averageSize) {
            connection.close();
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
                ", threadConnections=" + this.threadConnections +
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
    public static @NotNull Connection requireConnection(final @Nullable Connection _connection, final @NotNull DatabaseUtil util) throws SQLException {
        if (_connection != null)
            return _connection;
        // Objects.requireNonNullElseGet(_connection, DatabaseUtil.getIndexInstance()::getConnection); (With SQLException)
        return util.getConnection();
    }
}
