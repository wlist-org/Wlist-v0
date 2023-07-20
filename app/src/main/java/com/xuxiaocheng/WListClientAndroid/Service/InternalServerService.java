package com.xuxiaocheng.WListClientAndroid.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

@SuppressWarnings("ClassHasNoToStringMethod")
public final class InternalServerService extends Service {
    @NonNull private final Thread ServerMainThread = new Thread(() -> {
        try {
            WList.main("-path:" + this.getExternalFilesDir("server"));
        } finally {
            this.stopSelf();
        }
    }, "ServerMain");

    @Override
    public void onCreate() {
        super.onCreate();
        if (WList.getMainStageAPI() == 3)
            HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.ERROR, "Internal WList Server has already stopped.", ParametersMap.create().add("pid", Process.myPid()));
        else if (WList.getMainStageAPI() != -1)
            HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.MISTAKE, "Internal WList Server has already started.", ParametersMap.create().add("pid", Process.myPid()));
        else
            HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.FINE, "Internal WList Server is starting.", ParametersMap.create().add("pid", Process.myPid()));
        this.ServerMainThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HLogManager.getInstance(this, "DefaultLogger").log(HLogLevel.FINE, "Internal WList Server is stopping.");
        switch (WList.getMainStageAPI()) {
            case 0 -> this.ServerMainThread.interrupt();
            case 1 -> WListServer.getInstance().stop();
            default -> {}
        }
        //noinspection CallToSystemExit
        System.exit(1); // Require JVM exit to reboot WList class.
    }

    @Override
    @NonNull public IBinder onBind(@NonNull final Intent intent) {
        return new ServerBinder();
    }

    public enum TransactOperate {
        GetAddress,
        GetAndDeleteAdminPassword,
        ;
        private final int code;
        TransactOperate() {
            this.code = this.ordinal() + 1;
        }
        public int getCode() {
            return this.code;
        }
        @Override
        @NonNull public String toString() {
            return "TransactOperate{" +
                    "name='" + this.name() + '\'' +
                    ", code=" + this.code +
                    '}';
        }
    }

    public static void sendTransact(@NonNull final IBinder iService, @NonNull final TransactOperate operate, @Nullable final Consumer<? super Parcel> dataCallback, @Nullable final Consumer<? super Parcel> replyCallback) throws RemoteException {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(operate.name());
            if (dataCallback != null)
                dataCallback.accept(data);
            iService.transact(operate.code, data, reply, 0);
            if (replyCallback != null)
                replyCallback.accept(reply);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public static final class ServerBinder extends Binder {
        private static boolean waitStage(final int stage, @NonNull final Parcel reply) {
            try {
                if (WList.waitMainStageAPI(stage))
                    return false;
            } catch (final InterruptedException ignore) {
            }
            reply.writeInt(-1);
            return true;
        }

        @Override
        protected boolean onTransact(final int code, @NonNull final Parcel data, @Nullable final Parcel reply, final int flags) throws RemoteException {
            if (code < 1 || TransactOperate.values().length < code)
                return super.onTransact(code, data, reply, flags);
            final TransactOperate operate = TransactOperate.values()[code - 1];
            data.enforceInterface(operate.name());
            assert reply != null;
            switch (operate) {
                case GetAddress -> {
                    if (ServerBinder.waitStage(1, reply)) break;
                    final InetSocketAddress address = WListServer.getInstance().getAddress().getInstanceNullable();
                    if (address == null)
                        reply.writeInt(1);
                    else {
                        reply.writeInt(0);
                        reply.writeString(address.getHostName());
                        reply.writeInt(address.getPort());
                    }
                }
                case GetAndDeleteAdminPassword -> {
                    if (ServerBinder.waitStage(1, reply)) break;
                    final String password = UserManager.getAndDeleteDefaultAdminPasswordAPI();
                    if (password == null)
                        reply.writeInt(1);
                    else {
                        reply.writeInt(0);
                        reply.writeString(password);
                    }
                }
            }
            return true;
        }
    }
}
