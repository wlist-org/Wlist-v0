package com.xuxiaocheng.WListAndroid.Helpers;

import android.os.Bundle;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import com.xuxiaocheng.WList.AndroidSupports.FileLocationGetter;
import com.xuxiaocheng.WList.Commons.Beans.FileLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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


    private static @NotNull Bundle saveLocation(final @NotNull FileLocation location) {
        final Bundle bundle = new Bundle(2);
        bundle.putString("storage", FileLocationGetter.storage(location));
        bundle.putLong("id", FileLocationGetter.id(location));
        return bundle;
    }
    private static @Nullable FileLocation restoreLocation(final @NotNull Bundle bundle) {
        final String storage = bundle.getString("storage");
        final long id = bundle.getLong("id");
        if (storage == null || (id == 0L && !bundle.containsKey("id")))
            return null;
        return new FileLocation(storage, id);
    }

    public static void saveLocation(final @NotNull FileLocation location, final @NotNull Bundle bundle, final @NotNull String key) {
        final Bundle value = BundleHelper.saveLocation(location);
        bundle.putBundle(key, value);
    }
    public static void saveLocation(final @NotNull AtomicReference<FileLocation> location, final @NotNull Bundle bundle, final @NotNull String key, final @Nullable Consumer<? super @NotNull FileLocation> callback) {
        final FileLocation fileLocation = location.get();
        if (fileLocation != null) {
            BundleHelper.saveLocation(fileLocation, bundle, key);
            if (callback != null)
                callback.accept(fileLocation);
        }
    }
    public static void restoreLocation(final @NotNull Bundle bundle, final @NotNull String key, final @NotNull AtomicReference<? super FileLocation> location, final @Nullable Consumer<? super @NotNull FileLocation> callback) {
        final Bundle value = bundle.getBundle(key);
        if (value != null) {
            final FileLocation fileLocation = BundleHelper.restoreLocation(value);
            if (fileLocation != null) {
                location.set(fileLocation);
                if (callback != null)
                    callback.accept(fileLocation);
            }
        }
    }
}
