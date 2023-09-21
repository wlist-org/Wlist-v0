package com.xuxiaocheng.WListTest.Operations;

import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import org.junit.jupiter.api.BeforeAll;

@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class ProvidersWrapper extends ServerWrapper {
    @BeforeAll
    public static void initialize() throws Exception {
        ServerWrapper.initialize();
        StorageManager.addProvider("test", ProviderTypes.Lanzou, null);
    }
}
