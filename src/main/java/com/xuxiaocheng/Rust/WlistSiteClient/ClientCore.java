package com.xuxiaocheng.Rust.WlistSiteClient;

import com.xuxiaocheng.Rust.NativeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("NativeMethod")
public final class ClientCore {
    private ClientCore() {
        super();
    }

    static native @Nullable String getLastError();

    private static native boolean initializeTokioRuntime();
    private static native boolean uninitializeTokioRuntime();

    public static void load() {
    } static {
        NativeUtil.load("wlist_site_client");
        ClientCore.initialize();
    }

    public static void initialize() {
        if (!ClientCore.initializeTokioRuntime())
            throw new NativeUtil.NativeException("Failed to initialize Tokio runtime: " + ClientCore.getLastError());
    }

    public static void uninitialize() {
        if (!ClientCore.uninitializeTokioRuntime())
            throw new NativeUtil.NativeException("Failed to uninitialize Tokio runtime: " + ClientCore.getLastError());
    }

    private static native @Nullable String getVersion();

    public static @NotNull String getVersionString() {
        final String version = ClientCore.getVersion();
        if (version == null)
            throw new NativeUtil.NativeException("Failed to get version string: " + ClientCore.getLastError());
        return version;
    }


    public static class WlistSiteClient implements AutoCloseable {
        protected final long ptr;
        protected final @NotNull AtomicBoolean closed = new AtomicBoolean(false);

        protected WlistSiteClient(final long ptr) {
            super();
            this.ptr = ptr;
        }

        @Override
        public void close() {
            if (this.closed.compareAndSet(false, true))
                ClientCore.disconnect(this);
        }

        @Override
        public @NotNull String toString() {
            return "WlistSiteClient{" +
                    "ptr=" + this.ptr +
                    ", closed=" + this.closed +
                    '}';
        }
    }

    private static native long connect(final @NotNull String address);

    public static @NotNull WlistSiteClient connect(final @NotNull InetSocketAddress address) {
        final String addr = address.getHostString() + ":" + address.getPort();
        final long ptr = ClientCore.connect(addr);
        if (ptr == 0)
            throw new NativeUtil.NativeException("Failed to connect to " + addr + ": " + ClientCore.getLastError());
        return new WlistSiteClient(ptr);
    }

    private static native boolean disconnect(final long ptr);

    public static void disconnect(final @NotNull WlistSiteClient client) {
        ClientCore.disconnect(client.ptr);
    }


    private static native @Nullable String selectLink(final long pinging);

    public static @Nullable InetSocketAddress selectLink() {
        final String address = ClientCore.selectLink(10);
        if (address == null) {
            final String error = ClientCore.getLastError();
            if (error == null)
                return null;
            throw new NativeUtil.NativeException("Failed to select link: " + error);
        }
        if (!address.startsWith("tcp://"))
            return null; // Unreachable.
        final String url = address.substring("tcp://".length());
        final int index = url.lastIndexOf(':');
        return new InetSocketAddress(url.substring(0, index), Integer.parseInt(url.substring(index + 1)));
    }
}
