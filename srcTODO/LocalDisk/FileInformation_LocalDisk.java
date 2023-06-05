package com.xuxiaocheng.WList.WebDrivers.LocalDisk;

import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.WList.Server.Databases.File.FileSqlInformation;
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

    static @Nullable FileSqlInformation create(final @NotNull Path root, final @NotNull Path child, final @Nullable BasicFileAttributes attributes) throws IOException {
        if (attributes == null)
            return null;
        if (!child.startsWith(root))
            return null;
        try {
            return new FileSqlInformation(-1, LocalDiskManager.getDrivePath(root, child),
                    !attributes.isRegularFile(), attributes.size(),
                    LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault()),
                    LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault()),
                    "", attributes.isSymbolicLink() ? "S" : attributes.isOther() ? "O" : "");
        } catch (final RuntimeException exception) {
            throw HExceptionWrapper.unwrapException(exception, IOException.class);
        }
    }
}
