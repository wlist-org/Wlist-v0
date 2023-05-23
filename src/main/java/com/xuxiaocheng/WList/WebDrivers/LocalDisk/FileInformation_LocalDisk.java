package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.WList.DataAccessObjects.FileInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class FileInformation_LocalDisk {
    private FileInformation_LocalDisk() {
        super();
    }

    static @Nullable FileInformation create(final @NotNull Path root, final @NotNull Path child, final @Nullable BasicFileAttributes attributes) throws IOException {
        if (attributes == null)
            return null;
        if (!child.startsWith(root))
            return null;
        try {
            return new FileInformation(-1, LocalDiskManager.getDrivePath(root, child),
                    !attributes.isRegularFile(), attributes.size(),
                    LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault()),
                    LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault()),
                    "", attributes.isSymbolicLink() ? "S" : attributes.isOther() ? "O" : "");
        } catch (final RuntimeException exception) {
            if (exception.getCause() instanceof IOException ioException)
                throw ioException;
            throw exception;
        }
    }
}
