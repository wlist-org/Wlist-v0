package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.WList.Server.Configuration.GlobalConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Deque;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class DataBaseUtil {
    protected static @Nullable DataBaseUtil DataDB;
    protected static @Nullable DataBaseUtil IndexDB;

    public static @NotNull DataBaseUtil getDataInstance() throws SQLException {
        if (DataBaseUtil.DataDB == null)
            DataBaseUtil.DataDB = new DataBaseUtil(new PooledDatabaseConfig(
                    new File(GlobalConfiguration.getInstance().getData_db()),
                    2, 4, 10, true, Connection.TRANSACTION_READ_COMMITTED
            ));
        return DataBaseUtil.DataDB;
    }

    public static @NotNull DataBaseUtil getIndexInstance() throws SQLException {
        if (DataBaseUtil.IndexDB == null)
            DataBaseUtil.IndexDB = new DataBaseUtil(new PooledDatabaseConfig(
                    new File(GlobalConfiguration.getInstance().getIndex_db()),
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
        if (!path.exists() && !path.getParentFile().mkdirs() && !path.getParentFile().exists())
            throw new SQLException("Cannot create database directory.");
        this.sqliteDataSource = new SQLiteDataSource();
        this.sqliteDataSource.setUrl(org.sqlite.JDBC.PREFIX + path.getPath());
        for (int i = 0; i < this.config.initSize; ++i)
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
//        BusyHandler.setHandler(connection, new BusyHandler() {
//            @Override
//            protected int callback(final int n) throws SQLException {
//                if (n > 2 && (n < 5 || n % 5 == 0))
//                    HLog.getInstance("DefaultLogger").log(HLogLevel.WARN, "SQLITE BUSY!!! Retry time: ", n);
//                if (n > 99) // Connection may leak.
//                    return 0;
//                try {
//                    TimeUnit.MILLISECONDS.sleep(100);
//                } catch (final InterruptedException exception) {
//                    throw new SQLException(exception);
//                }
//                return 1;
//            }
//        });
//        // Regex fixer
//        Function.create(connection, "REGEXP", new Function() {
//            @Override
//            protected void xFunc() throws SQLException {
//                final String expression = this.value_text(0);
//                final String value = Objects.requireNonNullElse(this.value_text(1), "");
//                this.result(Pattern.compile(expression).matcher(value).find() ? 1 : 0);
//            }
//        });
        return new PooledSqliteConnection(connection, this);
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
        assert connection instanceof PooledSqliteConnection;
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

    protected record PooledSqliteConnection(@NotNull Connection connection, @NotNull DataBaseUtil util) implements Connection {
        @Override
        public void close() throws SQLException {
            this.util.recycleConnection(this);
        }


        @Override
        public Statement createStatement() throws SQLException {
            return this.connection.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(final String sql) throws SQLException {
            return this.connection.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(final String sql) throws SQLException {
            return this.connection.prepareCall(sql);
        }

        @Override
        public String nativeSQL(final String sql) throws SQLException {
            return this.connection.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(final boolean autoCommit) throws SQLException {
            this.connection.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return this.connection.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            this.connection.commit();
        }

        @Override
        public void rollback() throws SQLException {
            this.connection.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return this.connection.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return this.connection.getMetaData();
        }

        @Override
        public void setReadOnly(final boolean readOnly) throws SQLException {
            this.connection.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return this.connection.isReadOnly();
        }

        @Override
        public void setCatalog(final String catalog) throws SQLException {
            this.connection.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return this.connection.getCatalog();
        }

        @Override
        public void setTransactionIsolation(final int level) throws SQLException {
            this.connection.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return this.connection.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return this.connection.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            this.connection.clearWarnings();
        }

        @Override
        public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
            return this.connection.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
            return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
            return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return this.connection.getTypeMap();
        }

        @Override
        public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
            this.connection.setTypeMap(map);
        }

        @Override
        public void setHoldability(final int holdability) throws SQLException {
            this.connection.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return this.connection.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return this.connection.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(final String name) throws SQLException {
            return this.connection.setSavepoint(name);
        }

        @Override
        public void rollback(final Savepoint savepoint) throws SQLException {
            this.connection.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
            this.connection.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
            return this.connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
            return this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
            return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
            return this.connection.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
            return this.connection.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
            return this.connection.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return this.connection.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return this.connection.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return this.connection.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return this.connection.createSQLXML();
        }

        @Override
        public boolean isValid(final int timeout) throws SQLException {
            return this.connection.isValid(timeout);
        }

        @Override
        public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
            this.connection.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(final Properties properties) throws SQLClientInfoException {
            this.connection.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(final String name) throws SQLException {
            return this.connection.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return this.connection.getClientInfo();
        }

        @Override
        public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
            return this.connection.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
            return this.connection.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(final String schema) throws SQLException {
            this.connection.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return this.connection.getSchema();
        }

        @Override
        public void abort(final Executor executor) throws SQLException {
            this.connection.abort(executor);
        }

        @Override
        public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
            this.connection.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return this.connection.getNetworkTimeout();
        }

        @Override
        public void beginRequest() throws SQLException {
            this.connection.beginRequest();
        }

        @Override
        public void endRequest() throws SQLException {
            this.connection.endRequest();
        }

        @Override
        public boolean setShardingKeyIfValid(final ShardingKey shardingKey, final ShardingKey superShardingKey, final int timeout) throws SQLException {
            return this.connection.setShardingKeyIfValid(shardingKey, superShardingKey, timeout);
        }

        @Override
        public boolean setShardingKeyIfValid(final ShardingKey shardingKey, final int timeout) throws SQLException {
            return this.connection.setShardingKeyIfValid(shardingKey, timeout);
        }

        @Override
        public void setShardingKey(final ShardingKey shardingKey, final ShardingKey superShardingKey) throws SQLException {
            this.connection.setShardingKey(shardingKey, superShardingKey);
        }

        @Override
        public void setShardingKey(final ShardingKey shardingKey) throws SQLException {
            this.connection.setShardingKey(shardingKey);
        }

        @Override
        public <T> T unwrap(final Class<T> iFace) throws SQLException {
            return this.connection.unwrap(iFace);
        }

        @Override
        public boolean isWrapperFor(final Class<?> iFace) throws SQLException {
            return this.connection.isWrapperFor(iFace);
        }
    }
}
