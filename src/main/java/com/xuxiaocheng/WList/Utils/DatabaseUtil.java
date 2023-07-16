package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.AndroidSupport.ARandomHelper;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Helper.HFileHelper;
import com.xuxiaocheng.HeadLibs.Helper.HRandomHelper;
import io.netty.util.IllegalReferenceCountException;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

// TODO SQLCipher
// TODO Optimise
public class DatabaseUtil implements DatabaseInterface {
    private static @Nullable DatabaseUtil instance;

    public static synchronized void initialize(final @NotNull File database) throws SQLException {
        if (DatabaseUtil.instance != null)
            throw new IllegalStateException("Database util is initialized." + ParametersMap.create().add("instance", DatabaseUtil.instance));
        DatabaseUtil.instance = new DatabaseUtil(new PooledDatabaseConfig(database,
                1, 1, 2,/* 2, 3, 10, */false, false, Connection.TRANSACTION_READ_COMMITTED
        ));
    }

    public static synchronized @NotNull DatabaseUtil getInstance() {
        if (DatabaseUtil.instance == null)
            throw new IllegalStateException("Database util is not initialized.");
        return DatabaseUtil.instance;
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
            throw new IllegalStateException("Init connection count > max count." + ParametersMap.create().add("config", config));
        if (!HFileHelper.ensureFileExist(config.source))
            throw new SQLException("Cannot create database file." + ParametersMap.create().add("path", config.source));
        final SQLiteDataSource database = new SQLiteDataSource();
        database.setUrl(JDBC.PREFIX + config.source.getPath());
        database.setJournalMode(SQLiteConfig.JournalMode.WAL.getValue());
        this.dataSource = database;
        for (int i = 0; i < this.config.initSize; ++i)
            this.freeConnections.add(this.createNewConnection());
        assert this.freeConnections.size() == this.config.initSize;
        assert this.createdSize.get() == this.config.initSize;
    }

    protected final @NotNull ReferencedConnection createNewConnection() throws SQLException {
        if (this.createdSize.getAndIncrement() < this.config.maxSize) {
            final Connection rawConnection = this.dataSource.getConnection();
            if (rawConnection == null) {
                this.createdSize.getAndDecrement();
                throw new SQLException("Failed to get connection with sqlite database source." + ParametersMap.create().add("config", this.config));
            }
            this.resetConnection(rawConnection);
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
        void retain(); // void close() throws SQLException;
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
        private @Nullable String allow = null;

        private PooledConnectionProxy(final @NotNull Connection connection, final @NotNull DatabaseUtil util) {
            super();
            this.connection = connection;
            this.util = util;
        }

        @Override
        public synchronized @Nullable Object invoke(final @NotNull Object proxy, final @NotNull Method method, final Object @Nullable [] args) throws SQLException {
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
                    if (--this.referenceCounter < 1) {
                        this.allow = null;
                        this.util.recycleConnection(this.id);
                    }
                    return null;
                }
                case "inside": return this.connection;
                case "commit": {
                    if (this.referenceCounter != 1)
                        return null;
                }
            }
            if (this.allow != null)
                throw new SQLWarning("Something went wrong on this connection (" + this.id + "). Usually caused in other threads", this.allow);
            try {
                return method.invoke(this.connection, args);
            } catch (final IllegalAccessException exception) {
                throw new RuntimeException(exception);
            } catch (final InvocationTargetException exception) {
                if (exception.getTargetException() instanceof SQLException sqlException) {
                    this.allow = sqlException.getMessage();
                    throw sqlException;
                }
                throw new RuntimeException(exception);
            }
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

    public @NotNull Connection getExplicitConnection(final @NotNull String id) throws SQLException {
        final ReferencedConnection connection;
        try {
            connection = this.activeConnections.computeIfAbsent(id, HExceptionWrapper.wrapFunction(k -> {
                ReferencedConnection newConnection = this.freeConnections.poll();
                if (newConnection == null)
                    newConnection = this.createNewConnection();
                newConnection.setId(id);
                return newConnection;
            }));
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, SQLException.class);
        }
        connection.retain();
        assert id.equals(connection.id());
        return connection;
    }

    public @NotNull Connection getNewConnection(final @Nullable Consumer<? super @NotNull String> idSaver) throws SQLException {
        ReferencedConnection connection = this.freeConnections.poll();
        if (connection == null)
            connection = this.createNewConnection();
        final String id = MiscellaneousUtil.randomKeyAndPut(this.activeConnections,
                () -> ARandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 16, HRandomHelper.DefaultWords),
                connection);
        connection.setId(id);
        if (idSaver != null)
            idSaver.accept(id);
        connection.retain();
        return connection;
    }

    @Override
    public @NotNull Connection getConnection(final @Nullable String _connectionId, final @Nullable AtomicReference<? super String> connectionId) throws SQLException {
        if (_connectionId == null)
            return this.getNewConnection(connectionId == null ? null : connectionId::set);
        if (connectionId != null)
            connectionId.set(_connectionId);
        return this.getExplicitConnection(_connectionId);
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

    protected record PooledDatabaseConfig(@NotNull File source, int initSize, int averageSize, int maxSize, boolean walMode, boolean autoCommit, int transactionIsolationLevel) {
    }
}
