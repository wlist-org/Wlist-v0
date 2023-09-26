package com.xuxiaocheng.WListTest.Databases;

import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqliteHelper;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.StaticLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.TimeUnit;

@Execution(ExecutionMode.CONCURRENT)
public class FilesTest {
    @TempDir(cleanup = CleanupMode.ALWAYS)
    private static File directory;

    @BeforeAll
    public static void initialize() {
        StaticLoader.load();
    }

    protected static final ThreadLocal<File> database = new ThreadLocal<>();
    protected static final ThreadLocal<String> provider = new ThreadLocal<>();

    @BeforeEach
    public void load() throws SQLException, InterruptedException {
        synchronized (FilesTest.class) {
            FilesTest.database.set(new File(FilesTest.directory, System.currentTimeMillis() + ".db"));
            TimeUnit.MILLISECONDS.sleep(10);
        }
        FilesTest.provider.set(HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 32, null));
        FileManager.quicklyInitialize(FilesTest.provider.get(), SqlDatabaseManager.quicklyOpen(FilesTest.database.get()), 0, null);
    }

    @AfterEach
    public void unload() throws SQLException {
        FileManager.quicklyUninitialize(FilesTest.provider.get(), null);
        SqlDatabaseManager.quicklyClose(FilesTest.database.get());
    }

    public FileManager manager() {
        return FileManager.getInstance(FilesTest.provider.get());
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
        this.manager().insertIterator(Collections.emptyIterator(), 0, null);
        this.manager().insertFileOrDirectory(new FileInformation(1, 0, "1-file", false, 123, null, null, null), null);
        this.manager().insertFileOrDirectory(new FileInformation(2, 0, "2-directory", true, 0, null, null, null), null);
        this.manager().insertFileOrDirectory(new FileInformation(3, 2, "2-file", false, 234, null, null, null), null);
        Assertions.assertEquals(357/*123 + 234*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        Assertions.assertEquals(234, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
    }

    @Test
    public void insertIterator() throws SQLException {
        this.manager().insertIterator(List.of(
                new FileInformation(1, 0, "1-directory1", true, -1, null, null, null),
                new FileInformation(2, 0, "1-directory2", true, -1, null, null, null),
                new FileInformation(3, 0, "1-file", false, 123, null, null, null)
        ).iterator(), 0, null);
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().insertIterator(List.of(
                new FileInformation(4, 1, "2-file", false, 234, null, null, null)
        ).iterator(), 1, null);
        Assertions.assertEquals(234, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().insertIterator(List.of(
                new FileInformation(15, 2, "2-file", false, 345, null, null, null)
        ).iterator(), 2, null);
        Assertions.assertEquals(345, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
        Assertions.assertEquals(702/*123 + 234 + 345*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
    }

    @Test
    public void updateOrInsertFile() throws SQLException {
        this.manager().insertIterator(List.of(
                new FileInformation(1, 0, "1-directory", true, -1, null, null, null),
                new FileInformation(2, 0, "1-file", false, 123, null, null, null)
        ).iterator(), 0, null);
        this.manager().insertIterator(List.of(
                new FileInformation(3, 1, "2-file1", false, 234, null, null, null),
                new FileInformation(4, 1, "2-file2", false, 345, null, null, null)
        ).iterator(), 1, null);
        this.manager().updateOrInsertFile(new FileInformation(5, 1, "2-file3", false, 456, null, null, null), null);
        Assertions.assertEquals(1035/*234 + 345 + 456*/, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(1158/*123 + 234 + 345 + 456*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().updateOrInsertFile(new FileInformation(3, 0, "3-file1", false, 567, null, null, null), null);
        Assertions.assertEquals(801/*345 + 456*/, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(1491/*123 + 345 + 456 + 567*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().updateOrInsertFile(new FileInformation(5, 0, "4-file3", false, 678, null, null, null), null);
        Assertions.assertEquals(345, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(1713/*123 + 345 + 567 + 678*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
    }

    @Test
    public void updateOrInsertDirectory() throws SQLException {
        this.manager().insertIterator(List.of(
                new FileInformation(1, 0, "1-directory1", true, -1, null, null, null),
                new FileInformation(2, 0, "1-directory2", true, -1, null, null, null),
                new FileInformation(3, 0, "1-file", false, 123, null, null, null)
        ).iterator(), 0, null);
        this.manager().insertIterator(List.of(
                new FileInformation(4, 1, "2-file1", false, 234, null, null, null),
                new FileInformation(5, 1, "2-file2", false, 345, null, null, null)
        ).iterator(), 1, null);
        Assertions.assertEquals(579,/*234 + 345,*/ Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().updateOrInsertDirectory(new FileInformation(2, 1, "3-directory2", true, -1, null, null, null), null);
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().updateOrInsertDirectory(new FileInformation(2, 0, "3-directory2", true, -1, null, null, null), null);
        Assertions.assertEquals(579/*234 + 345*/, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
        Assertions.assertEquals(-1, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().insertIterator(List.of(
                new FileInformation(6, 2, "2-file3", false, 456, null, null, null)
        ).iterator(), 2, null);
        Assertions.assertEquals(456, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
        Assertions.assertEquals(1158/*123 + 234 + 345 + 456*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().updateOrInsertDirectory(new FileInformation(1, 2, "4-directory1", true, -1, null, null, null), null);
        Assertions.assertEquals(1035/*234 + 345 + 456*/, Objects.requireNonNull(this.manager().selectInfo(2, true, null)).size());
        Assertions.assertEquals(1158/*123 + 234 + 345 + 456*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
    }


    @Test
    public void deleteFile() throws SQLException {
        this.manager().insertIterator(List.of(
                new FileInformation(1, 0, "1-directory", true, -1, null, null, null),
                new FileInformation(2, 0, "1-file", false, 123, null, null, null)
        ).iterator(), 0, null);
        this.manager().insertIterator(List.of(
                new FileInformation(3, 1, "2-file1", false, 234, null, null, null),
                new FileInformation(4, 1, "2-file2", false, 345, null, null, null)
        ).iterator(), 1, null);
        Assertions.assertEquals(579/*234 + 345*/, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(702/*123 + 234 + 345*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().deleteFile(4, null);
        Assertions.assertEquals(234, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(357/*123 + 234*/, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().deleteFile(3, null);
        Assertions.assertEquals(0, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(123, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
    }

    @Test
    public void deleteDirectoryRecursively() throws SQLException {
        this.manager().insertIterator(List.of(
                new FileInformation(1, 0, "1-directory", true, -1, null, null, null),
                new FileInformation(2, 0, "1-file", false, 123, null, null, null)
        ).iterator(), 0, null);
        this.manager().insertIterator(List.of(
                new FileInformation(3, 1, "2-file1", false, 234, null, null, null),
                new FileInformation(4, 1, "2-file2", false, 345, null, null, null)
        ).iterator(), 1, null);
        Assertions.assertEquals(579, Objects.requireNonNull(this.manager().selectInfo(1, true, null)).size());
        Assertions.assertEquals(702, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().deleteDirectoryRecursively(1, null);
        Assertions.assertNull(this.manager().selectInfo(1, true, null));
        Assertions.assertEquals(123, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().deleteFile(2, null);
        Assertions.assertEquals(0, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
        this.manager().deleteDirectoryRecursively(0, null);
        Assertions.assertEquals(0, Objects.requireNonNull(this.manager().selectInfo(0, true, null)).size());
    }
}
