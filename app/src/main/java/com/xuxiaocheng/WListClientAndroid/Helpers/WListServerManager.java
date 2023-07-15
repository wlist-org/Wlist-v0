package com.xuxiaocheng.WListClientAndroid.Helpers;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class WListServerManager extends Service {
    @NonNull private final Thread ServerMainThread = new Thread(HExceptionWrapper.wrapRunnable(() ->
            WList.main("-path:" + this.getExternalFilesDir("server")), () -> this.stopSelf()), "ServerMain");

    @Override
    public void onCreate() {
        super.onCreate();
        if (WList.getMainStageAPI() != -1)
            throw new IllegalStateException("Internal WList Server has already started.");
        HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.FINE, "Starting Internal WList Server.");
        this.ServerMainThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.FINE, "Stopping Internal WList Server.");
        switch (WList.getMainStageAPI()) {
            case 0 -> this.ServerMainThread.interrupt();
            case 1 -> WListServer.getInstance().stop();
            default -> {}
        }
    }

    @Override
    @NonNull public IBinder onBind(@NonNull final Intent intent) {
        return ServerBinder.instance;
    }

    public static final class ServerBinder extends Binder {
        private static final IBinder instance = new ServerBinder();

        @Nullable
        public SocketAddress getAddress() throws InterruptedException {
            if (this.waitForMainStage(1)) return null;
            // TODO
            return new InetSocketAddress(5212);
        }

        public boolean waitForMainStage(final int stage) throws InterruptedException {
            int current = WList.getMainStageAPI();
            if (current == -1)
                return true;
            synchronized (WList.mainStageAPIChanger) {
                while (current != stage) {
                    WList.mainStageAPIChanger.wait();
                    current = WList.getMainStageAPI();
                    if (current == 3)
                        break;
                }
            }
            if (current == 3 && stage == 3)
                return false;
            return current == 3;
        }
    }
}
