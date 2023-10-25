package com.xuxiaocheng.WListAndroid.Services.InternalServer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NativeUtil;
import com.xuxiaocheng.WList.Server.Operations.ServerHandler;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import com.xuxiaocheng.WListAndroid.Main;
import com.xuxiaocheng.WListAndroid.Utils.HLogManager;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ClassHasNoToStringMethod")
public final class InternalServerService extends Service {
    private final @NotNull Thread ServerMainThread = new Thread(HExceptionWrapper.wrapRunnable(WList::main, this::stopSelf), "ServerMain");

    @Override
    public void onCreate() {
        super.onCreate();
        if (HLogManager.initialize(this, HLogManager.ProcessType.Server)) {
            HLogManager.getInstance("DefaultLogger").log(HLogLevel.FAULT, "Incompatible service instance.", ParametersMap.create().add("pid", Process.myPid()));
            Main.runOnBackgroundThread(null, () -> {
                this.onDestroy(); // Force stop.
                this.stopSelf();
            }, 300, TimeUnit.MILLISECONDS);
            return;
        }
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.INFO, "Internal WList Server is starting.", ParametersMap.create().add("pid", Process.myPid()));
        WList.RuntimePath.reinitialize(this.getExternalFilesDir("server"));
        NativeUtil.ExtraPathGetterCore.reinitialize(l -> {
            final String arch = PlatformDependent.normalizedArch();
            throw new IllegalStateException("Unknown architecture: " + ("unknown".equals(arch) ? System.getProperty("os.arch") : arch));
        }); // Normally is unreachable.
        ServerHandler.AllowLogOn.set(false);
        this.ServerMainThread.start();
    }

    private final @NotNull AtomicBoolean called = new AtomicBoolean(false);
    @Override
    public void onDestroy() {
        if (!this.called.compareAndSet(false, true)) return;
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.FINE, "Internal WList Server is stopping.", ParametersMap.create().add("pid", Process.myPid()));
        Main.runOnBackgroundThread(null, HExceptionWrapper.wrapRunnable(() -> {
            WListServer.getInstance().stop();
            this.ServerMainThread.join();
            //noinspection CallToSystemExit
            System.exit(0); // Require JVM exit to reboot WList class.
        }));
    }

    @Override
    public @NotNull IBinder onBind(final @NotNull Intent intent) {
        return new InternalServerBinder();
    }
}