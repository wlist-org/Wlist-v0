package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Helpers.DriverSqlHelper;
import com.xuxiaocheng.WList.Driver.Utils.DrivePath;
import com.xuxiaocheng.WList.Driver.Utils.FileInformation;
import com.xuxiaocheng.WList.Utils.DataBaseUtil;
import com.xuxiaocheng.WList.Utils.MiscellaneousUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@SuppressWarnings("SameParameterValue")
public final class LocalDiskManager {
    private LocalDiskManager() {
        super();
    }

    static @NotNull File toFile(final @NotNull File root, final @NotNull DrivePath child) {
        return new File(root, child.getPath());
    }

    static @NotNull DrivePath getDrivePath(final @NotNull Path root, final @NotNull Path full) {
        return new DrivePath(full.toString().substring(root.toString().length()));
    }

    static @Nullable FileInformation getFileInformation(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable Connection _connection) throws IOException, SQLException {
        if (useCache) {
            final FileInformation info = DriverSqlHelper.getFile(configuration.getLocalSide().getName(), path, _connection);
            if (info != null)
                return info;
        }
        final Path file = LocalDiskManager.toFile(configuration.getWebSide().getRootDirectoryPath(), path).toPath();
        final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        return FileInformation_LocalDisk.create(configuration.getWebSide().getRootDirectoryPath().toPath(), file, attributes);
    }

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileInformation>> listFileNoCache(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath directoryPath, final int limit, final int page, final @Nullable Connection _connection) throws IOException, SQLException {
        final File root = LocalDiskManager.toFile(configuration.getWebSide().getRootDirectoryPath(), directoryPath);
        final File[] children = root.listFiles();
        if (children == null)
            return Pair.ImmutablePair.makeImmutablePair(0, new LinkedList<>());
        final List<FileInformation> list = new LinkedList<>();
        for (int i = (page - 1) * limit; list.size() < limit && i < children.length; ++i) {
            directoryPath.child(children[i].getName());
            try {
                final FileInformation information = LocalDiskManager.getFileInformation(configuration, directoryPath, false, _connection);
                if (information != null)
                    list.add(information);
            } finally {
                directoryPath.parent();
            }
        }
        DriverSqlHelper.insertFilesIgnoreId(configuration.getLocalSide().getName(), list, _connection);
        return Pair.ImmutablePair.makeImmutablePair(children.length, list);
    }

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull Iterator<@NotNull FileInformation>> listAllFilesNoCache(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection, final @NotNull ExecutorService threadPool) throws IOException, SQLException {
        final Pair.ImmutablePair<Integer, List<FileInformation>> files = LocalDiskManager.listFileNoCache(configuration, directoryPath, Integer.MAX_VALUE, 1, _connection);
        return Pair.ImmutablePair.makeImmutablePair(files.getFirst(), files.getSecond().iterator());
    }

    public static void recursiveRefreshDirectory(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath directoryPath, final @Nullable Connection _connection) throws IOException, SQLException {
        final Connection connection = MiscellaneousUtil.requireConnection(_connection, DataBaseUtil.getIndexInstance());
        try {
            if (_connection == null)
                connection.setAutoCommit(false);
            final Path root = new File(configuration.getWebSide().getRootDirectoryPath(), directoryPath.getPath()).toPath();
            final Collection<FileInformation> list = new LinkedList<>();
            Files.walkFileTree(root, Set.of(), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
//                    DriverSqlHelper.getFileByParentPath()
                    try {
                        DriverSqlHelper.deleteFileByParentPath(configuration.getLocalSide().getName(), LocalDiskManager.getDrivePath(root, dir), connection);
                        final FileInformation information = FileInformation_LocalDisk.create(root, dir, attrs);
                        if (information != null)
                            list.add(information);
                    } catch (final SQLException exception) {
                        throw new IOException(exception);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    final FileInformation information = FileInformation_LocalDisk.create(root, file, attrs);
                    if (information != null)
                        list.add(information);
                    return FileVisitResult.CONTINUE;
                }
            });
            DriverSqlHelper.insertFilesIgnoreId(configuration.getLocalSide().getName(), list, connection);
            if (_connection == null)
                connection.commit();
        } finally {
            if (_connection == null)
                connection.close();
        }
    }
}
