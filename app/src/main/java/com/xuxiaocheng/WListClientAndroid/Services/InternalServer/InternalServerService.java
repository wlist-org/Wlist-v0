package com.xuxiaocheng.WListClientAndroid.Services.InternalServer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.Rust.NativeUtil;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import com.xuxiaocheng.WListClientAndroid.Main;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ClassHasNoToStringMethod")
public final class InternalServerService extends Service {
    private final @NotNull Thread ServerMainThread = new Thread(HExceptionWrapper.wrapRunnable(WList::main, this::stopSelf), "ServerMain");

    @Override
    public void onCreate() {
        super.onCreate();
        HLogManager.initialize(this, HLogManager.ProcessType.Server);
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.FINE, "Internal WList Server is starting.", ParametersMap.create().add("pid", Process.myPid()));
        WList.RuntimePath.reinitialize(this.getExternalFilesDir("server"));
        NativeUtil.ExtraPathGetterCore.reinitialize(l -> {
            final String arch = PlatformDependent.normalizedArch();
            throw new IllegalStateException("Unknown architecture: " + ("unknown".equals(arch) ? System.getProperty("os.arch") : arch));
        }); // Normally is unreachable.
        this.ServerMainThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.FINE, "Internal WList Server is stopping.");
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
