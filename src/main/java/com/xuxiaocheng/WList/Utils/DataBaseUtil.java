package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.WList.Server.GlobalConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.JDBC;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class DataBaseUtil {
    private static final boolean WalMode = false;

    protected static @Nullable DataBaseUtil DataDB;
    protected static @Nullable DataBaseUtil IndexDB;

    public static @NotNull DataBaseUtil getDataInstance() throws SQLException {
        if (DataBaseUtil.DataDB == null)
            DataBaseUtil.DataDB = new DataBaseUtil(new PooledDatabaseConfig(
                    new File(GlobalConfiguration.getInstance().dataDBPath()),
                    2, 4, 10, true, Connection.TRANSACTION_READ_COMMITTED
            ));
        return DataBaseUtil.DataDB;
    }

    public static @NotNull DataBaseUtil getIndexInstance() throws SQLException {
        if (DataBaseUtil.IndexDB == null)
            DataBaseUtil.IndexDB = new DataBaseUtil(new PooledDatabaseConfig(
                    new File(GlobalConfiguration.getInstance().indexDBPath()),
                    2, 4, 10, true, Connection.TRANSACTION_READ_COMMITTED
            ));
        return DataBaseUtil.IndexDB;
    }

    protected final @NotNull SQLiteDataSource sqliteDataSource;
    protected final @NotNull PooledDatabaseConfig config;
    protected final @NotNull AtomicInteger createdSize = new AtomicInteger();
    protected final @NotNull Deque<@NotNull Connection> sqliteConnections = new ConcurrentLinkedDeque<>();
    protected final @NotNull Object needIdleConnection = new Object();

    protected DataBaseUtil(final @NotNull PooledDatabaseConfig config) throws SQLException {
        super();
        this.config = config;
        final File path = this.config.path.getAbsoluteFile();
        if (!HFileHelper.ensureFileExist(path))
            throw new SQLException("Cannot create database file.");
        this.sqliteDataSource = new SQLiteDataSource();
        this.sqliteDataSource.setUrl(JDBC.PREFIX + path.getPath());
        if (DataBaseUtil.WalMode) {
            final Connection connection = this.createNewConnection();
            try (final Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA journal_mode = WAL");
            }
            this.sqliteConnections.push(connection);
        }
        for (int i = DataBaseUtil.WalMode ? 1 : 0; i < this.config.initSize; ++i)
            this.sqliteConnections.push(this.createNewConnection());
        assert this.createdSize.get() == this.config.initSize;
    }

    public @NotNull PooledDatabaseConfig getConfig() {
        return this.config;
    }

    protected final @NotNull Connection createNewConnection() throws SQLException {
        final Connection connection;
        synchronized (this.createdSize) {
            connection = this.sqliteDataSource.getConnection();
            if (connection == null)
                throw new SQLException("Failed to get connection with sqlite database.");
            this.createdSize.incrementAndGet();
        }
        return (Connection) Proxy.newProxyInstance(connection.getClass().getClassLoader(),
                PooledConnectionProxy.proxy, new PooledConnectionProxy(connection, this));
    }

    public @NotNull Connection getConnection() throws SQLException {
        Connection connection = this.sqliteConnections.pollFirst();
        if (connection != null)
            return connection;
        synchronized (this.createdSize) {
            if (this.createdSize.get() < this.config.maxSize)
                return this.createNewConnection();
        }
        try {
            synchronized (this.needIdleConnection) {
                while (connection == null) {
                    this.needIdleConnection.wait();
                    connection = this.sqliteConnections.pollFirst();
                }
            }
        } catch (final InterruptedException exception) {
            throw new SQLException(exception);
        }
        return connection;
    }

    public void recycleConnection(final @NotNull Connection connection) throws SQLException {
        this.resetConnection(connection);
        synchronized (this.needIdleConnection) {
            if (this.sqliteConnections.offerLast(connection)) {
                this.needIdleConnection.notify();
                return;
            }
        }
        // Close redundant connections.
        synchronized (this.createdSize) {
            if (this.createdSize.get() > this.config.minSize) {
                connection.close();
                this.createdSize.decrementAndGet();
            }
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
        return "DataBaseUtil{" +
                "config=" + this.config +
                ", createdSize=" + this.createdSize +
                '}';
    }

    protected record PooledDatabaseConfig(@NotNull File path, int initSize, int minSize, int maxSize, boolean autoCommit, int transactionIsolationLevel) {
    }

    protected record PooledConnectionProxy(@NotNull Connection connection, @NotNull DataBaseUtil util) implements InvocationHandler {
        private static final Class<?>[] proxy = new Class[] {Connection.class};

        @Override
        public @Nullable Object invoke(final @NotNull Object proxy, final @NotNull Method method, final Object @Nullable [] args) throws IllegalAccessException, InvocationTargetException, SQLException {
            if (!"close".equals(method.getName()))
                return method.invoke(this.connection, args);
            this.util.recycleConnection(this.connection);
            return null;
        }
    }
}
