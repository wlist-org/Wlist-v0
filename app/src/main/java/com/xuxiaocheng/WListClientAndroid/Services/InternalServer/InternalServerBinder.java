package com.xuxiaocheng.WListClientAndroid.Services.InternalServer;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import androidx.annotation.WorkerThread;
import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.WList.AndroidSupports.DatabaseSupporter;
import com.xuxiaocheng.WList.Server.WListServer;
import com.xuxiaocheng.WList.WList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public final class InternalServerBinder extends Binder {
    private enum TransactOperate {
        GetAddress,
        GetAndDeleteAdminPassword,
        GetMainStage,
    }

    public static final int Code = "InternalServerBinder".hashCode();

    @WorkerThread
    public static @NotNull InetSocketAddress getAddress(final @NotNull IBinder iService) throws RemoteException {
        final InetSocketAddress[] address = new InetSocketAddress[1];
        InternalServerBinder.sendTransact(iService, TransactOperate.GetAddress, p -> {
            final int success = p.readInt();
            if (success != 0)
                throw new IllegalStateException("Failed to get internal server address." + ParametersMap.create().add("code", success));
            final String hostname = p.readString();
            final int port = p.readInt();
            address[0] = new InetSocketAddress(hostname, port);
        });
        return address[0];
    }

    @WorkerThread
    public static @Nullable String getAndDeleteAdminPassword(final @NotNull IBinder iService) throws RemoteException {
        final String[] password = new String[1];
        InternalServerBinder.sendTransact(iService, TransactOperate.GetAndDeleteAdminPassword, p -> {
            final int success = p.readInt();
            if (success != 0 && success != 1)
                throw new IllegalStateException("Failed to get default admin password." + ParametersMap.create().add("code", success));
            if (success == 0)
                password[0] = p.readString();
        });
        return password[0];
    }

    @WorkerThread
    public static int getMainStage(final @NotNull IBinder iService) throws RemoteException {
        final int[] stage = new int[1];
        InternalServerBinder.sendTransact(iService, TransactOperate.GetMainStage, p -> stage[0] = p.readInt());
        return stage[0];
    }

    private static void sendTransact(final @NotNull IBinder iService, final @NotNull TransactOperate operate, final @Nullable Consumer<? super Parcel> replyCallback) throws RemoteException {
        final Parcel data = Parcel.obtain();
        final Parcel reply = Parcel.obtain();
        try {
            data.writeString(operate.name());
            iService.transact(InternalServerBinder.Code, data, reply, 0);
            if (replyCallback != null)
                replyCallback.accept(reply);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static boolean waitStart(final @NotNull Parcel reply) {
        try {
            if (WList.waitMainStageAPI(1, false))
                return false;
        } catch (final InterruptedException ignore) {
        }
        reply.writeInt(-1);
        return true;
    }

    @Override
    protected boolean onTransact(final int code, final @NotNull Parcel data, final @Nullable Parcel reply, final int flags) throws RemoteException {
        if (code != InternalServerBinder.Code)
            return super.onTransact(code, data, reply, flags);
        assert reply != null;
        switch (TransactOperate.valueOf(data.readString())) {
            case GetAddress -> {
                if (InternalServerBinder.waitStart(reply)) break;
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
                if (InternalServerBinder.waitStart(reply)) break;
                final String password = DatabaseSupporter.getAndDeleteDefaultAdminPassword();
                if (password == null)
                    reply.writeInt(1);
                else {
                    reply.writeInt(0);
                    reply.writeString(password);
                }
            }
            case GetMainStage -> reply.writeInt(WList.getMainStageAPI());
        }
        return true;
    }
}
