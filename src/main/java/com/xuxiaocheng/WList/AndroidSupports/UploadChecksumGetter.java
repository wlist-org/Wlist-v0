package com.xuxiaocheng.WList.AndroidSupports;

import com.xuxiaocheng.WList.Commons.Beans.UploadChecksum;
import org.jetbrains.annotations.NotNull;

/**
 * @see UploadChecksum
 */
public final class UploadChecksumGetter {
    public long start(final @NotNull UploadChecksum checksum) {
        return checksum.start();
    }

    public long end(final @NotNull UploadChecksum checksum) {
        return checksum.end();
    }

    public @NotNull String algorithm(final @NotNull UploadChecksum checksum) {
        return checksum.algorithm();
    }
}
