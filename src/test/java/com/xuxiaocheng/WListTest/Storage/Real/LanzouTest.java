package com.xuxiaocheng.WListTest.Storage.Real;

import com.xuxiaocheng.WList.Commons.Beans.VisibleFileInformation;
import com.xuxiaocheng.WList.Commons.Options.Options;
import com.xuxiaocheng.WList.Server.Databases.File.FileInformation;
import com.xuxiaocheng.WList.Server.ServerConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouConfiguration;
import com.xuxiaocheng.WList.Server.Storage.Providers.Lanzou.LanzouProvider;
import com.xuxiaocheng.WList.Server.Storage.Providers.ProviderInterface;
import com.xuxiaocheng.WList.Server.Storage.StorageManager;
import com.xuxiaocheng.WListTest.StaticLoader;
import com.xuxiaocheng.WListTest.Storage.ProviderHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Execution(ExecutionMode.SAME_THREAD)
public class LanzouTest {
    @BeforeAll
    public static void initialize() throws IOException {
        StaticLoader.load();
        ServerConfiguration.set(ServerConfiguration.parse(new ByteArrayInputStream("providers:\n  test: lanzou\n".getBytes(StandardCharsets.UTF_8))));
        StorageManager.initialize(new File("run/configs"), new File("run/caches"));
    }

    @AfterAll
    public static void uninitialize() throws IOException {
        StorageManager.removeStorage("test", false);
    }

    public @NotNull ProviderInterface<?> provider() {
        return Objects.requireNonNull(StorageManager.getProvider("test"));
    }

    public long root() {
        return this.provider().getConfiguration().getRootDirectoryId();
    }

    @Test
    @Disabled
    @SuppressWarnings("unchecked")
    public void login() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final ProviderInterface<LanzouConfiguration> provider = (ProviderInterface<LanzouConfiguration>) this.provider();
        final Method method = LanzouProvider.class.getDeclaredMethod("loginIfNot");
        method.setAccessible(true);
        provider.getConfiguration().setToken(null);
        provider.getConfiguration().setTokenExpire(null);
        method.invoke(provider);
    }

    @BeforeEach
    public void checkEmpty() throws Exception {
        Assumptions.assumeTrue(ProviderHelper.list(this.provider(), this.root(), Options.FilterPolicy.Both, VisibleFileInformation.emptyOrder(), 0, 10).getT().total() == 0);
    }

    @Test
    public void curd() throws Exception {
        final FileInformation directory = ProviderHelper.create(this.provider(), this.root(), "Curd test.", Options.DuplicatePolicy.ERROR).getT();


    }
}
