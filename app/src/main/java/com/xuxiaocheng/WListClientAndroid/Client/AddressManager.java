package com.xuxiaocheng.WListClientAndroid.Client;

import androidx.annotation.NonNull;
import com.xuxiaocheng.HeadLibs.Initializer.HInitializer;

import java.net.SocketAddress;

public final class AddressManager {
    private AddressManager() {
        super();
    }

    @NonNull public static final HInitializer<SocketAddress> internalServerAddress = new HInitializer<>("InternalServerAddress");
}
