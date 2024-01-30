package com.xuxiaocheng.Rust.WlistSiteClient;

import com.xuxiaocheng.Rust.NativeUtil;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("NativeMethod")
public final class ClientVersion {
    private ClientVersion() {
        super();
    }

    public static final byte VERSION_LATEST = 3;
    public static final byte VERSION_AVAILABLE = 1;

    private static native byte isAvailable(final long client);
    private static byte version = Byte.MAX_VALUE;

    public static byte isAvailable(final ClientCore.@NotNull WlistSiteClient client) {
        if (ClientVersion.version != Byte.MAX_VALUE) return ClientVersion.version;
        final byte res = ClientVersion.isAvailable(client.ptr);
        if (res == Byte.MAX_VALUE)
            throw new NativeUtil.NativeException("Failed to check wlist site client version status: " + ClientCore.getLastError());
        ClientVersion.version = res;
        return res;
    }
}
