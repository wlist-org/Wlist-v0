package com.xuxiaocheng.WListTest.Databases;

import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Execution(ExecutionMode.SAME_THREAD)
public class FilesTest {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static File directory;

    public FileManager manager() {
        return FileManager.getInstance("test");
    }

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

    @Test
    public void insertFileOrDirectory() throws SQLException {
        FileManager.getInstance("test").insertIterator(Collections.emptyIterator(), 0, null);
        this.manager().insertFileOrDirectory(new FileInformation(1, 0, "1-file", false, 1024, null, null, null), null);
        this.manager().insertFileOrDirectory(new FileInformation(2, 0, "2-directory", true, 0, null, null, null), null);
        this.manager().insertFileOrDirectory(new FileInformation(3, 2, "2-file", false, 204, null, null, null), null);
        Assertions.assertEquals(1024 + 204, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        Assertions.assertEquals(204, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
    }

    @Test
    public void insertFilesSameDirectory() throws SQLException {
        FileManager.getInstance("test").insertIterator(List.of(
                new FileInformation(11, 0, "1-directory1", true, -1, null, null, null),
                new FileInformation(12, 0, "1-directory2", true, -1, null, null, null),
                new FileInformation(13, 0, "1-file", false, 123, null, null, null)
        ).iterator(), 0, null);
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        FileManager.getInstance("test").insertIterator(List.of(
                new FileInformation(14, 11, "2-file", false, 234, null, null, null)
        ).iterator(), 11, null);
        Assertions.assertEquals(234, Objects.requireNonNull(this.manager().selectInfo(11, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        FileManager.getInstance("test").insertIterator(List.of(
                new FileInformation(15, 12, "2-file", false, 345, null, null, null)
        ).iterator(), 12, null);
        Assertions.assertEquals(345, Objects.requireNonNull(this.manager().selectInfo(12, true, null)).size());
        Assertions.assertEquals(123 + 234 + 345, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
    }
}
