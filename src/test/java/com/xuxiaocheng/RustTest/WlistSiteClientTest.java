package com.xuxiaocheng.RustTest;

import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NativeUtil;
import com.xuxiaocheng.Rust.WlistSiteClient.ClientCore;
import com.xuxiaocheng.Rust.WlistSiteClient.ClientVersion;
import com.xuxiaocheng.StaticLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

public class WlistSiteClientTest {
    @BeforeAll
    public static void initialize() {
        StaticLoader.load();
        ClientCore.load();
        HLog.LoggerCreateCore.reinitialize(n -> HLog.createInstance(n, HLogLevel.DEBUG.getLevel(), false));
        HLog.create("DefaultLogger");
    }

    @AfterAll
    public static void uninitialize() {
        ClientCore.uninitialize();
    }

    @Test
    public void initializeTwice() {
        Assertions.assertThrowsExactly(NativeUtil.NativeException.class, ClientCore::initialize);
    }

    @Test
    public void selectLink() {
        Assertions.assertNull(ClientCore.selectLink());
    }

    @Test
    @Disabled
    public void selectLink2() {
        final InetSocketAddress address = ClientCore.selectLink();
        Assertions.assertNotNull(address);
        ClientCore.connect(address).close();
    }

    @Test
    public void versionIsAvailable() {
        try (final ClientCore.WlistSiteClient client = ClientCore.connect(new InetSocketAddress("localhost", 25565))) {
            final byte res = ClientVersion.isAvailable(client);
            Assertions.assertEquals(ClientVersion.VERSION_LATEST | ClientVersion.VERSION_AVAILABLE, res);
        }
    }
}
