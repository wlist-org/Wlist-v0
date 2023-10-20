package com.xuxiaocheng.WListTest.Databases;

import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Server.Databases.SqlDatabaseManager;
import com.xuxiaocheng.StaticLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    @TempDir
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
        final long directory = FileSqlInterface.getDoubleId(id, true);
        final long file = FileSqlInterface.getDoubleId(id, false);
        Assertions.assertEquals(id, FileSqlInterface.getRealId(directory));
        Assertions.assertEquals(id, FileSqlInterface.getRealId(file));
        Assertions.assertTrue(FileSqlInterface.isDirectory(directory));
        Assertions.assertFalse(FileSqlInterface.isDirectory(file));
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
    public void isInDirectoryRecursively() throws SQLException {
        this.manager().insertIterator(List.of(
                new FileInformation(1, 0, "1-directory1", true, -1, null, null, null),
                new FileInformation(10, 0, "1-directory2", true, -1, null, null, null),
                new FileInformation(2, 0, "1-file", false, 123, null, null, null)
        ).iterator(), 0, null);
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(1, true, 0, null));
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(2, false, 0, null));
        Assertions.assertFalse(this.manager().isInDirectoryRecursively(2, true, 1, null));
        Assertions.assertFalse(this.manager().isInDirectoryRecursively(2, false, 1, null));

        this.manager().insertIterator(List.of(
                new FileInformation(3, 1, "2-file", false, 234, null, null, null),
                new FileInformation(4, 1, "2-directory", true, -1, null, null, null)
        ).iterator(), 1, null);
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(3, false, 1, null));
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(3, false, 0, null));
        Assertions.assertFalse(this.manager().isInDirectoryRecursively(0, true, 1, null));

        this.manager().insertIterator(List.of(
                new FileInformation(5, 4, "3-file", false, 345, null, null, null)
        ).iterator(), 4, null);
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(5, false, 4, null));
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(5, false, 1, null));
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(5, false, 0, null));
        Assertions.assertFalse(this.manager().isInDirectoryRecursively(5, false, 10, null));
        Assertions.assertFalse(this.manager().isInDirectoryRecursively(0, true, 4, null));

        Assertions.assertTrue(this.manager().isInDirectoryRecursively(1, true, 1, null));

        // Not exist.
        Assertions.assertFalse(this.manager().isInDirectoryRecursively(9, false, 2, null));
        Assertions.assertTrue(this.manager().isInDirectoryRecursively(3, false, 2, null));
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
