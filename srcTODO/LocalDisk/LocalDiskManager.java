package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.DataStructures.Pair;
import com.xuxiaocheng.WList.Driver.Helpers.DrivePath;
import com.xuxiaocheng.WList.Server.Databases.File.FileManager;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformationWithGroups;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SuppressWarnings("SameParameterValue")
public final class LocalDiskManager {
    // TODO
    private LocalDiskManager() {
        super();
    }

    static @NotNull File toFile(final @NotNull File root, final @NotNull DrivePath child) {
        return new File(root, child.getPath());
    }

    static @NotNull DrivePath getDrivePath(final @NotNull Path root, final @NotNull Path full) {
        return new DrivePath(full.toString().substring(root.toString().length()));
    }

    static @Nullable FileSqlInformationWithGroups getFileInformation(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath path, final boolean useCache, final @Nullable String connectionId) throws IOException, SQLException {
        if (useCache) {
            final FileSqlInformationWithGroups info = FileManager.selectFileByPathWithGroups(configuration.getLocalSide().getName(), path, connectionId);
            if (info != null)
                return info;
        }
        final Path file = LocalDiskManager.toFile(configuration.getWebSide().getRootDirectoryPath(), path).toPath();
        final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        return FileInformation_LocalDisk.create(configuration.getWebSide().getRootDirectoryPath().toPath(), file, attributes);
    }

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull List<@NotNull FileSqlInformationWithGroups>> listFileNoCache(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath directoryPath, final int limit, final int page, final @Nullable String connectionId
    ) throws IOException, SQLException {
        final File root = LocalDiskManager.toFile(configuration.getWebSide().getRootDirectoryPath(), directoryPath);
        final File[] children = root.listFiles();
        if (children == null)
            return Pair.ImmutablePair.makeImmutablePair(0, new LinkedList<>());
        final List<FileSqlInformationWithGroups> list = new LinkedList<>();
        for (int i = (page - 1) * limit; list.size() < limit && i < children.length; ++i) {
            directoryPath.child(children[i].getName());
            try {
                final FileSqlInformationWithGroups information = LocalDiskManager.getFileInformation(configuration, directoryPath, false, connectionId);
                if (information != null)
                    list.add(information);
            } finally {
                directoryPath.parent();
            }
        }
//        FileManager.insertFilesIgnoreId(configuration.getLocalSide().getName(), list, _connection);
        return Pair.ImmutablePair.makeImmutablePair(children.length, list);
    }

    static Pair.@NotNull ImmutablePair<@NotNull Integer, @NotNull Iterator<@NotNull FileSqlInformationWithGroups>> listAllFilesNoCache(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath directoryPath, final @Nullable String connectionId, final @NotNull ExecutorService threadPool) throws IOException, SQLException {
        final Pair.ImmutablePair<Integer, List<FileSqlInformationWithGroups>> files = LocalDiskManager.listFileNoCache(configuration, directoryPath, Integer.MAX_VALUE, 1, connectionId);
        return Pair.ImmutablePair.makeImmutablePair(files.getFirst(), files.getSecond().iterator());
    }

    public static void recursiveRefreshDirectory(final @NotNull LocalDiskConfiguration configuration, final @NotNull DrivePath directoryPath, final @NotNull String connectionId) throws IOException, SQLException {
//        try (final Connection connection = FileManager.DefaultDatabaseUtil.getExplicitConnection(connectionId)) {
//            connection.setAutoCommit(false);
//            final Path root = new File(configuration.getWebSide().getRootDirectoryPath(), directoryPath.getPath()).toPath();
//            final Collection<FileSqlInformationWithGroups> list = new LinkedList<>();
            // TODO
//            FileManager.deleteFileByParentPathRecursively(configuration.getLocalSide().getName(), LocalDiskManager.getDrivePath(root, dir), connectionId);
//            Files.walkFileTree(root, Set.of(), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
//                @Override
//                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
//                    FileManager.getFileByParentPath()
//                    final FileSqlInformationWithGroups information = FileInformation_LocalDisk.create(root, dir, attrs);
//                    if (information != null)
//                        list.add(information);
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public @NotNull FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
//                    final FileSqlInformationWithGroups information = FileInformation_LocalDisk.create(root, file, attrs);
//                    if (information != null)
//                        list.add(information);
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//            FileManager.insertFilesIgnoreId(configuration.getLocalSide().getName(), list, connection);
//            connection.commit();
//        }
    }
}
