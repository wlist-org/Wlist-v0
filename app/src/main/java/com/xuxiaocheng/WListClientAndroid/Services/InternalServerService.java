package com.xuxiaocheng.WListClientAndroid.Services;

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
import com.xuxiaocheng.HeadLibs.Logger.HLog;
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
        HLogManager.initialize(this, "Server");
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
        final HLog logger = HLogManager.getInstance("DefaultLogger");
        final int stage = WList.getMainStageAPI();
        logger.log(HLogLevel.FINE, "Internal WList Server is stopping.", ParametersMap.create().add("stage", stage));
        switch (stage) {
            case 0 -> this.ServerMainThread.interrupt();
            case 1 -> WListServer.getInstance().stop();
            default -> {}
        }
        //noinspection CallToSystemExit
        System.exit(0); // Require JVM exit to reboot WList class.
    }

    @Override
    @NonNull public IBinder onBind(@NonNull final Intent intent) {
        return new ServerBinder();
    }

    private enum TransactOperate {
        GetAddress,
        GetAndDeleteAdminPassword,
    }

    @NonNull public static InetSocketAddress getAddress(@NonNull final IBinder iService) throws RemoteException {
        final InetSocketAddress[] address = new InetSocketAddress[1];
        InternalServerService.sendTransact(iService, InternalServerService.TransactOperate.GetAddress, null, p -> {
            final int success = p.readInt();
            if (success != 0)
                throw new IllegalStateException("Failed to get internal server address." + ParametersMap.create().add("code", success));
            final String hostname = p.readString();
            final int port = p.readInt();
            address[0] = new InetSocketAddress(hostname, port);
        });
        return address[0];
    }

    @Nullable public static String getAndDeleteAdminPassword(@NonNull final IBinder iService) throws RemoteException {
        final String[] password = new String[1];
        InternalServerService.sendTransact(iService, TransactOperate.GetAndDeleteAdminPassword, null, p -> {
            final int success = p.readInt();
            if (success != 0 && success != 1)
                throw new IllegalStateException("Failed to get default admin password." + ParametersMap.create().add("code", success));
            if (success == 0)
                password[0] = p.readString();
        });
        return password[0];
    }

    private static void sendTransact(@NonNull final IBinder iService, @NonNull final TransactOperate operate, @Nullable final Consumer<? super Parcel> dataCallback, @Nullable final Consumer<? super Parcel> replyCallback) throws RemoteException {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(operate.name());
            if (dataCallback != null)
                dataCallback.accept(data);
            iService.transact(operate.ordinal() + 1, data, reply, 0);
            if (replyCallback != null)
                replyCallback.accept(reply);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static boolean waitStart(@NonNull final Parcel reply) {
        try {
            if (WList.waitMainStageAPI(1, false))
                return false;
        } catch (final InterruptedException ignore) {
        }
        reply.writeInt(-1);
        return true;
    }

    private static final class ServerBinder extends Binder {
        @Override
        protected boolean onTransact(final int code, @NonNull final Parcel data, @Nullable final Parcel reply, final int flags) throws RemoteException {
            if (code < 1 || TransactOperate.values().length < code)
                return super.onTransact(code, data, reply, flags);
            final TransactOperate operate = TransactOperate.values()[code - 1];
            data.enforceInterface(operate.name());
            assert reply != null;
            switch (operate) {
                case GetAddress -> {
                    if (InternalServerService.waitStart(reply)) break;
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
                    if (InternalServerService.waitStart(reply)) break;
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
