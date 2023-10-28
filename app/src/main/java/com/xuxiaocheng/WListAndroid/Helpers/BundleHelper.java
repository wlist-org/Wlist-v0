package com.xuxiaocheng.WListAndroid.Helpers;

import android.os.Bundle;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

public final class BundleHelper {
    private BundleHelper() {
        super();
    }

    private static void saveAddress(final @NotNull InetSocketAddress address, final @NotNull Bundle bundle) {
        bundle.putString("host", address.getHostString());
        bundle.putInt("port", address.getPort());
    }
    private static @Nullable InetSocketAddress restoreAddress(final @NotNull Bundle bundle) {
        final String host = bundle.getString("host");
        final int port = bundle.getInt("port", -1);
        return host != null && -1 < port && port < 65536 ? new InetSocketAddress(host, port) : null;
    }
    private static void saveUsername(final @NotNull String username, final @NotNull Bundle bundle) {
        bundle.putString("username", username);
    }
    private static @Nullable String restoreUsername(final @NotNull Bundle bundle) {
        return bundle.getString("username");
    }

    public static void saveClient(final @Nullable InetSocketAddress address, final @Nullable String username, final @NotNull Bundle bundle) {
        if (address != null && username != null) {
            BundleHelper.saveAddress(address, bundle);
            BundleHelper.saveUsername(username, bundle);
        }
    }
    public static void saveClient(final @NotNull HInitializer<? extends InetSocketAddress> address, final @NotNull HInitializer<String> username, final @NotNull Bundle bundle, final @Nullable BiConsumer<? super @NotNull InetSocketAddress, ? super @NotNull String> callback) {
        final InetSocketAddress socketAddress = address.getInstanceNullable();
        final String name = username.getInstanceNullable();
        if (socketAddress != null && name != null) {
            BundleHelper.saveAddress(socketAddress, bundle);
            BundleHelper.saveUsername(name, bundle);
            if (callback != null)
                callback.accept(socketAddress, name);
        }
    }
    public static void restoreClient(final @NotNull Bundle bundle, final @NotNull HInitializer<? super InetSocketAddress> address, final @NotNull HInitializer<? super String> username, final @Nullable BiConsumer<? super @NotNull InetSocketAddress, ? super @NotNull String> callback) {
        final InetSocketAddress socketAddress = BundleHelper.restoreAddress(bundle);
        final String name = BundleHelper.restoreUsername(bundle);
        if (socketAddress != null && name != null) {
            address.reinitialize(socketAddress);
            username.reinitialize(name);
            if (callback != null)
                callback.accept(socketAddress, name);
        }
    }
}
