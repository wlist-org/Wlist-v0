package com.xuxiaocheng.WList.Server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SortedSet;
import java.util.TreeSet;

public final class Operation {
    private Operation() {
        super();
    }

    public enum Type {
        Undefined,
        CloseServer,
        Broadcast,
        SetBroadcastMode,
        Register,
        Login,
        ChangePassword,
        Logoff,
        ListUsers,
        DeleteUser,
        AddPermission,
        ReducePermission,
        ListFiles,
        MakeDirectories,
        DeleteFile,
        RenameFile,
        RequestDownloadFile,
        DownloadFile,
        CancelDownloadFile,
        RequestUploadFile,
        UploadFile,
        CancelUploadFile,
        CopyFile,
        MoveFile,
    }

    public enum Permission {
        Undefined(0),
        ServerOperate(1),
        Broadcast(1 << 1),
        UsersList(1 << 2),
        UsersOperate(1 << 3),
        FilesList(1 << 4), // TODO permissions for each file
        FileDownload(1 << 5),
        FileUpload(1 << 6),
        FileDelete(1 << 7);
        private final long index;
        Permission(final long index) {
            this.index = index;
        }
        @Override
        public @NotNull String toString() {
            return super.toString() + '(' + this.index + ')';
        }
    }

    public enum State {
        Undefined,
        Success,
        Broadcast,
        ServerError,
        Unsupported,
        NoPermission,
        DataError,
        FormatError,
    }

    public static @NotNull Type valueOfType(final @NotNull String type) {
        try {
            return Type.valueOf(type);
        } catch (final IllegalArgumentException exception) {
            return Type.Undefined;
        }
    }

    public static @NotNull Permission valueOfPermission(final @NotNull String permission) {
        try {
            return Permission.valueOf(permission);
        } catch (final IllegalArgumentException exception) {
            return Permission.Undefined;
        }
    }

    public static @NotNull State valueOfState(final @NotNull String state) {
        try {
            return State.valueOf(state);
        } catch (final IllegalArgumentException exception) {
            return State.Undefined;
        }
    }

    public static @NotNull String dumpPermissions(final @NotNull Iterable<@NotNull Permission> permissions) {
        long p = 0;
        for (final Permission permission: permissions)
            p |= permission.index;
        return Long.toString(p, 36);
    }

    public static @Nullable SortedSet<@NotNull Permission> parsePermissions(final @NotNull String permissions) {
        try {
            final long p = Long.valueOf(permissions, 36).longValue();
            final SortedSet<Permission> permissionsSet = new TreeSet<>();
            for (final Permission permission: Permission.values())
                if ((p & permission.index) > 0)
                    permissionsSet.add(permission);
            return permissionsSet;
        } catch (final NumberFormatException exception) {
            return null;
        }
    }
}
