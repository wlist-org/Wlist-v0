package com.xuxiaocheng.WList.WebDrivers.Driver_123pan;

import com.alibaba.fastjson2.JSONObject;
import com.xuxiaocheng.WList.Driver.DrivePath;
import com.xuxiaocheng.WList.Driver.Exceptions.IllegalParametersException;
import com.xuxiaocheng.WList.Utils.SQLiteUtil;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class DriverSQLHelper_123pan {
    private DriverSQLHelper_123pan() {
        super();
    }

    private static @NotNull String getTableName(final @NotNull String name) {
        return "Driver_123pan_" + name;
    }

    static void init(final @NotNull String name) throws SQLException {
        final String table = DriverSQLHelper_123pan.getTableName(name);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try(final PreparedStatement statement = SQLiteUtil.getIndexInstance().prepareStatement("""
                CREATE TABLE IF NOT EXISTS ? (
                    path        TEXT    PRIMARY KEY
                                        UNIQUE
                                        NOT NULL,
                    name        TEXT    NOT NULL,
                    directory   INTEGER NOT NULL
                                        DEFAULT (0),
                    id          INTEGER NOT NULL,
                    size                NOT NULL
                                        DEFAULT (0),
                    create_time INTEGER NOT NULL,
                    update_time INTEGER NOT NULL,
                    etag        TEXT    NOT NULL,
                    s3key       TEXT    NOT NULL,
                    node        TEXT    NOT NULL
                );
                """)) {
            statement.setString(1, table);
            statement.executeUpdate();
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }

    static void insertFile(final @NotNull String name, final @NotNull DrivePath path, final @NotNull JSONObject info) throws SQLException, IllegalParametersException {
        final String table = DriverSQLHelper_123pan.getTableName(name);
        SQLiteUtil.getIndexInstance().getLock(table).writeLock().lock();
        try(final PreparedStatement statement = SQLiteUtil.getIndexInstance().prepareStatement("""
                INSERT INTO ? (path, name, directory, id, size, create_time, update_time, etag, s3key, node)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)) {
            statement.setString(1, table);
            statement.setString(2, path.getPath());
            statement.setString(3, path.getName());
            statement.setInt(4, info.getIntValue("Type", 0));
            statement.setLong(5, info.getLongValue("FileId"));
            statement.setLong(6, info.getLongValue("Size", -1));
            statement.setLong(7, DriverHelper_123pan.parseServerTimeWithZone(info.getString("CreateAt")));
            statement.setLong(8, DriverHelper_123pan.parseServerTimeWithZone(info.getString("UpdateAt")));
            statement.setString(9, info.getString("Etag"));
            statement.setString(10, info.getString("S3KeyFlag"));
            statement.setString(11, info.getString("StorageNode"));
            statement.executeUpdate();
        } finally {
            SQLiteUtil.getIndexInstance().getLock(table).writeLock().unlock();
        }
    }
}
