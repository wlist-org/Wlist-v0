package com.xuxiaocheng.Rust;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStreams;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@SuppressWarnings("ErrorNotRethrown")
public final class NativeUtil {
    private NativeUtil() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("SystemLogger", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, false, true, HMergedStreams.getFileOutputStreamNoException(null));

    public static void load(final @NotNull String name) {
        NativeUtil.logger.log(HLogLevel.VERBOSE, "Loading native library: ", name);
        try {
            NativeUtil.tryLoad(name + '_' + PlatformDependent.normalizedArch());
        } catch (final UnsatisfiedLinkError e1) {
            try {
                NativeUtil.tryLoad(name);
                NativeUtil.logger.log(HLogLevel.WARN, "Loaded unknown platform native lib: ", name);
            } catch (final UnsatisfiedLinkError e2) {
                e1.addSuppressed(e2);
                throw e1;
            }
        }
    }

    private static void tryLoad(final @NotNull String name) {
        try {
            System.loadLibrary(name);
        } catch (final UnsatisfiedLinkError error) {
            final String library = System.mapLibraryName(name);
            final String path = "rust/" + library;
            try (final InputStream stream = NativeUtil.class.getClassLoader().getResourceAsStream(path)) {
                if (stream == null)
                    throw new FileNotFoundException(path);
                final int index = library.lastIndexOf('.');
                final Path temp = Files.createTempFile(library.substring(0, index), library.substring(index));
                Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
                temp.toFile().deleteOnExit();
                System.load(temp.toAbsolutePath().toString());
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final IOException exception) {
                error.addSuppressed(exception);
                throw error;
            }
        }
    }
}
