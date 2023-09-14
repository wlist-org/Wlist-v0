package com.xuxiaocheng.WListTest.Databases;

import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.sql.SQLException;

@Execution(ExecutionMode.SAME_THREAD)
public class FilesTest {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static File directory;

    @BeforeAll
    public static void initialize() throws SQLException {
        StaticLoader.load();
        final File file = new File(FilesTest.directory, "data.db");
        final SqlDatabaseInterface database = SqlDatabaseManager.quicklyOpen(file);
        FileManager.quicklyInitialize("test", database, 0, null);
    }

    @AfterAll
    public static void uninitialize() throws SQLException {
        FileManager.quicklyUninitialize("test", null);
        final File file = new File(FilesTest.directory, "data.db");
        SqlDatabaseManager.quicklyClose(file);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 0, -1, -2, -3})
    public void doubleId(final long id) {
        final long directory = FileSqliteHelper.getDoubleId(id, true);
        final long file = FileSqliteHelper.getDoubleId(id, false);
        Assertions.assertEquals(id, FileSqliteHelper.getRealId(directory));
        Assertions.assertEquals(id, FileSqliteHelper.getRealId(file));
        Assertions.assertTrue(FileSqliteHelper.isDirectory(directory));
        Assertions.assertFalse(FileSqliteHelper.isDirectory(file));
        Assertions.assertEquals(directory, file - 1);
    }
}
