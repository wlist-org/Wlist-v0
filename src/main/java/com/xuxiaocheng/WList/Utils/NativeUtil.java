package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.HeadLibs.Logger.HMergedStreams;
import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

public final class NativeUtil {
    private NativeUtil() {
        super();
    }

    private static final @NotNull HLog logger = HLog.createInstance("SystemLogger", HLog.isDebugMode() ? Integer.MIN_VALUE : HLogLevel.DEBUG.getLevel() + 1, false, true, HMergedStreams.getFileOutputStreamNoException(null));

    // META-INF/native/lib_name
    @SuppressWarnings("ErrorNotRethrown")
    public static void load(final @NotNull String name) {
        // see netty: Native#loadNativeLibrary()
        NativeUtil.logger.log(HLogLevel.VERBOSE, "Loading native library: ", name);
        final String staticLibName = "lib_" + name;
        final String sharedLibName = staticLibName + '_' + PlatformDependent.normalizedArch();
        final ClassLoader loader = PlatformDependent.getClassLoader(NativeUtil.class);
        try {
            NativeLibraryLoader.load(sharedLibName, loader);
        } catch (final UnsatisfiedLinkError e1) {
            try {
                NativeLibraryLoader.load(staticLibName, loader);
                NativeUtil.logger.log(HLogLevel.WARN, "Failed to load shared native lib: ", name);
            } catch (final UnsatisfiedLinkError e2) {
                e1.addSuppressed(e2);
                throw e1;
            }
        }
    }
}
