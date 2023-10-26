package com.xuxiaocheng.WListAndroid.Services.InternalServer;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
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
    }

    public static final int Code = 2168877; // IBinder.FIRST_CALL_TRANSACTION <= Code=-"InternalServerBinder".hashCode()/100 <= IBinder.LAST_CALL_TRANSACTION

    public static @Nullable InetSocketAddress getAddress(final @NotNull IBinder iService) throws RemoteException {
        final InetSocketAddress[] address = new InetSocketAddress[1];
        InternalServerBinder.sendTransact(iService, TransactOperate.GetAddress, p -> {
            final int success = p.readInt();
            if (success == 1) // Usually, the server is closing.
                return;
            if (success != 0)
                throw new IllegalStateException("Failed to get internal server address." + ParametersMap.create().add("code", success));
            final String hostname = p.readString();
            final int port = p.readInt();
            address[0] = new InetSocketAddress(hostname, port);
        });
        return address[0];
    }

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

    @Override
    protected boolean onTransact(final int code, final @NotNull Parcel data, final @Nullable Parcel reply, final int flags) throws RemoteException {
        if (code != InternalServerBinder.Code)
            return super.onTransact(code, data, reply, flags);
        assert reply != null;
        try {
            WList.waitStarted();
        } catch (final InterruptedException ignore) {
            reply.writeInt(-1);
            return true;
        }
        switch (TransactOperate.valueOf(data.readString())) {
            case GetAddress -> {
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
                final String password = DatabaseSupporter.getAndDeleteDefaultAdminPassword();
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
