package com.xuxiaocheng.WListClientAndroid.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.Functions.HExceptionWrapper;
import com.xuxiaocheng.HeadLibs.Logger.HLogLevel;
import com.xuxiaocheng.WList.Databases.User.UserManager;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import com.xuxiaocheng.WListClientAndroid.Utils.HLogManager;

import java.net.InetSocketAddress;

public final class InternalServerService extends Service {
    @NonNull private final Thread ServerMainThread = new Thread(HExceptionWrapper.wrapRunnable(() -> WList.main("-path:" + this.getExternalFilesDir("server")), e -> {
        if (e != null) {
            HLogManager.getInstance(this, "ServerLogger").log(HLogLevel.FAULT, "Something went wrong in server thread.", e);
            Toast.makeText(this.getApplicationContext(), "Something went wrong in internal server.\r\n" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        this.stopSelf();
    }, true), "ServerMain");

    @Override
    public void onCreate() {
        super.onCreate();
        if (WList.getMainStageAPI() != -1)
            throw new IllegalStateException("Internal WList Server has already started.");
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

    public static final class ServerBinder extends Binder {
        private static boolean waitStage(final int stage, @NonNull final Parcel reply) {
            try {
                if (!WList.waitMainStageAPI(stage)) {
                    reply.writeInt(-2);
                    return true;
                }
                return false;
            } catch (final InterruptedException exception) {
                reply.writeInt(-1);
                reply.writeException(exception);
                return true;
            }
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
