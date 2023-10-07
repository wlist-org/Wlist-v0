package com.xuxiaocheng.Rust;

import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@SuppressWarnings("ErrorNotRethrown")
public final class NativeUtil {
    private NativeUtil() {
        super();
    }

    private static final @NotNull HLog logger = HLog.create("SystemLogger");

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

    public static final @NotNull HInitializer<UnaryOperator<@NotNull String>> ExtraPathGetterCore = new HInitializer<>("NativeUtil.ExtraPathGetterCore", l -> "rust/" + l);

    private static void tryLoad(final @NotNull String name) {
        try {
            System.loadLibrary(name);
        } catch (final UnsatisfiedLinkError error) {
            final String library = System.mapLibraryName(name);
            final String path = NativeUtil.ExtraPathGetterCore.getInstance().apply(library);
            final String prefix, suffix;
            final Path temp;
            try (final InputStream stream = NativeUtil.class.getClassLoader().getResourceAsStream(path)) {
                if (stream == null)
                    throw new FileNotFoundException(path);
                final int index = library.lastIndexOf('.');
                prefix = library.substring(0, index);
                suffix = library.substring(index);
                temp = Files.createTempFile(prefix, suffix).toAbsolutePath();
                Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
                System.load(temp.toString());
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final IOException exception) {
                error.addSuppressed(exception);
                throw error;
            }
            try (final Stream<Path> stream = Files.list(temp.getParent())) {
                stream.map(Path::toFile).filter(f -> f.isFile() && f.getName().startsWith(prefix) && f.getName().endsWith(suffix))
                        .forEach(File::deleteOnExit);
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") final Throwable exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            }
        }
    }
}
