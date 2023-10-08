package com.xuxiaocheng.WListTest.Storage.Real;

import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.Real.Lanzou.LanzouProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.Providers.StorageTypes;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WListTest.Operations.ProvidersWrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class LanzouTest extends RealAbstractTest<LanzouConfiguration> {
    @BeforeAll
    public static void initialize() throws Exception {
        ProvidersWrapper.initialize();
        StorageManager.addStorage("test", StorageTypes.Lanzou, null);
    }

    @AfterAll
    public static void uninitialize() throws IOException {
        StorageManager.removeStorage("test", false);
    }

    @Test
    @Disabled
    public void login() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final ProviderInterface<LanzouConfiguration> provider = this.provider();
        final Method method = LanzouProvider.class.getDeclaredMethod("loginIfNot0");
        method.setAccessible(true);
        provider.getConfiguration().setToken(null);
        provider.getConfiguration().setTokenExpire(null);
        method.invoke(provider);
    }

    @Nested
    @Disabled
    public class DownloadTest extends AbstractDownloadTest {
    }

    @Nested
    @Disabled
    public class UploadTest extends AbstractUploadTest {
    }
}
