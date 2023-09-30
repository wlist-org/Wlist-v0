package com.xuxiaocheng.WListClientAndroid.Services.InternalServer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Helpers.HUncaughtExceptionHelper;
import com.xuxiaocheng.HeadLibs.Logger.HLog;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ClassHasNoToStringMethod")
public final class InternalServerService extends Service {
    private final @NotNull Thread ServerMainThread = new Thread(() -> {
        try {
            InternalServerHooker.hookBefore();
            WList.main("-path:" + this.getExternalFilesDir("server"));
        } finally {
            this.stopSelf();
        }
    }, "ServerMain");

    @Override
    public void onCreate() {
        super.onCreate();
        HLogManager.initialize(this, HLogManager.ProcessType.Server);
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        if (WList.getMainStageAPI() == 3)
            logger.log(HLogLevel.ERROR, "Internal WList Server has already stopped.", ParametersMap.create().add("pid", Process.myPid()));
        else if (WList.getMainStageAPI() != -1)
            logger.log(HLogLevel.MISTAKE, "Internal WList Server has already started.", ParametersMap.create().add("pid", Process.myPid()));
        else
            logger.log(HLogLevel.FINE, "Internal WList Server is starting.", ParametersMap.create().add("pid", Process.myPid()));
        this.ServerMainThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance("DefaultLogger").log(HLogLevel.FINE, "Internal WList Server is stopping.");
        InternalServerHooker.hookFinish();
        final int stage = WList.getMainStageAPI();
        switch (stage) {
            case 0 -> this.ServerMainThread.interrupt();
            case 1 -> WListServer.getInstance().stop();
            default -> {}
        }
        if (stage > 0)
            try {
                WListServer.getInstance().awaitStop();
            } catch (final InterruptedException exception) {
                HUncaughtExceptionHelper.uncaughtException(Thread.currentThread(), exception);
            }
        //noinspection CallToSystemExit
        System.exit(0); // Require JVM exit to reboot WList class.
    }

    @Override
    public @NotNull IBinder onBind(final @NotNull Intent intent) {
        return new InternalServerBinder();
    }
}
