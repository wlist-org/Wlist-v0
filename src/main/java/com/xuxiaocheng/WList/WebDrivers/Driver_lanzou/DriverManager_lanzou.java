package com.xuxiaocheng.WList.WebDrivers.Driver_lanzou;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Databases.File.FileManager;
import com.xuxiaocheng.WList.Databases.File.FileSqlInformation;
import com.xuxiaocheng.WList.Databases.File.FileSqlInterface;
import com.xuxiaocheng.WList.Server.InternalDrivers.RootDriver;
import com.xuxiaocheng.WList.Server.WListServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public final class DriverManager_lanzou {
    private DriverManager_lanzou() {
        super();
    }

    // User Reader

    static void loginIfNot(final @NotNull DriverConfiguration_lanzou configuration) throws IOException {
        DriverHelper_lanzou.loginIfNot(configuration);
    }

    // File Reader

    static @Nullable List<@NotNull FileSqlInformation> listAllFilesNoCache(final @NotNull DriverConfiguration_lanzou configuration, final long directoryId, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        final CompletableFuture<List<FileSqlInformation>> directoriesFuture = CompletableFuture.supplyAsync(HExceptionWrapper.wrapSupplier(() ->
                DriverHelper_lanzou.listAllDirectory(configuration, directoryId)), WListServer.IOExecutors);
        final CompletableFuture<List<FileSqlInformation>> filesFuture = CompletableFuture.supplyAsync(HExceptionWrapper.wrapSupplier(() ->
                DriverHelper_lanzou.listAllFiles(configuration, directoryId)), WListServer.IOExecutors);
        final List<FileSqlInformation> information;
        try {
            information = directoriesFuture.get();
            information.addAll(filesFuture.get());
        } catch (final ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException)
                throw HExceptionWrapper.unwrapException(HExceptionWrapper.unwrapException(runtimeException, IOException.class), InterruptedException.class);
            throw new RuntimeException(exception.getCause());
        } finally {
            directoriesFuture.cancel(true);
            filesFuture.cancel(true);
        }
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final Set<Long> deletedIds = FileManager.selectFileIdByParentId(configuration.getName(), directoryId, connectionId.get());
            deletedIds.removeAll(information.stream().map(FileSqlInformation::id).collect(Collectors.toSet()));
            FileManager.deleteFilesRecursively(configuration.getName(), deletedIds, connectionId.get());
            FileManager.insertOrUpdateFiles(configuration.getName(), information, connectionId.get());
            connection.commit();
        }
        return information;
    }

    public static @Nullable FileSqlInformation getFileInformation(final @NotNull DriverConfiguration_lanzou configuration, final long id, final @Nullable FileSqlInformation parentInformation, final @Nullable String _connectionId) throws IOException, SQLException, InterruptedException {
        if (id == configuration.getWebSide().getRootDirectoryId()) return RootDriver.getDriverInformation(configuration);
        if (id == -1) return null; // Out of Root File Tree.
        final AtomicReference<String> connectionId = new AtomicReference<>();
        try (final Connection connection = FileManager.getConnection(configuration.getName(), _connectionId, connectionId)) {
            final FileSqlInformation cachedInformation = FileManager.selectFile(configuration.getName(), id, connectionId.get());
            if (cachedInformation != null) return cachedInformation;
            if (parentInformation == null)
                throw new UnsupportedOperationException("Cannot get a file information without parent information." + ParametersMap.create().add("id", id));
            if (parentInformation.type() != FileSqlInterface.FileSqlType.Directory)
                return null;
            final long count = FileManager.selectFileCountByParentId(configuration.getName(), parentInformation.id(), connectionId.get());
            if (count > 0)
                return null;
            final List<FileSqlInformation> list = DriverManager_lanzou.listAllFilesNoCache(configuration, parentInformation.id(), connectionId.get());
            if (list == null)
                FileManager.deleteFileRecursively(configuration.getName(), parentInformation.id(), connectionId.get());
            else if (list.isEmpty())
                FileManager.insertOrUpdateFile(configuration.getName(), parentInformation.getAsEmptyDirectory(), connectionId.get());
            connection.commit();
            if (list == null || list.isEmpty())
                return null;
            for (final FileSqlInformation information: list)
                if (information.id() == id)
                    return information;
            return null;
        }
    }

}
