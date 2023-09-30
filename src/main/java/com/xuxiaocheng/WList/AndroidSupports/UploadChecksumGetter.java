package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import org.jetbrains.annotations.NotNull;

/**
 * @see UploadChecksum
 */
public final class UploadChecksumGetter {
    private UploadChecksumGetter() {
        super();
    }

    public static long start(final @NotNull UploadChecksum checksum) {
        return checksum.start();
    }

    public static long end(final @NotNull UploadChecksum checksum) {
        return checksum.end();
    }

    public static @NotNull String algorithm(final @NotNull UploadChecksum checksum) {
        return checksum.algorithm();
    }
}
