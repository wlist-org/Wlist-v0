package com.xuxiaocheng.WListClient.AndroidSupports;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class AndroidSupport {
    private AndroidSupport() {
        super();
    }

    static final int DEFAULT_BUFFER_SIZE = 8192; // InputStream#DEFAULT_BUFFER_SIZE

    public static void transferTo(final @NotNull InputStream inputStream, final @NotNull OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[AndroidSupport.DEFAULT_BUFFER_SIZE];
        int read;
        while (true) {
            read = inputStream.read(buffer, 0, AndroidSupport.DEFAULT_BUFFER_SIZE);
            if (read < 0)
                break;
            outputStream.write(buffer, 0, read);
        };
    }
}
