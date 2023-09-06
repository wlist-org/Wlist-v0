package com.xuxiaocheng.WListClient.Server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;

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
        GetPermissions,
        ChangeUsername,
        ChangePassword,
        Logoff,
        ListUsers,
        DeleteUser,
        ListGroups,
        AddGroup,
        DeleteGroup,
        ChangeGroup,
        AddPermission,
        RemovePermission,
        BuildIndex,
        ListFiles,
        CreateDirectory,
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
        Undefined,
        ServerOperate,
        Broadcast,
        UsersList,
        UsersOperate,
        DriverOperate,
        FilesBuildIndex,
        FilesList,
        FileDownload,
        FileUpload,
        FileDelete,
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

    public static @NotNull EnumSet<Permission> emptyPermissions() {
        return EnumSet.noneOf(Permission.class);
    }

    public static @NotNull EnumSet<Permission> defaultPermissions() {
        return EnumSet.of(Permission.FilesList);
    }

    public static @NotNull EnumSet<Permission> allPermissions() {
        final EnumSet<Permission> permissions = EnumSet.allOf(Permission.class);
        permissions.remove(Permission.Undefined);
        return permissions;
    }

    public static @NotNull String dumpPermissions(final @NotNull Iterable<Permission> permissions) {
        long p = 0;
        for (final Permission permission: permissions)
            p |= 1L << permission.ordinal();
        return Long.toString(p, 36);
    }

    public static @Nullable EnumSet<Permission> parsePermissions(final @NotNull String permissions) {
        try {
            final EnumSet<Permission> permissionsSet = Operation.emptyPermissions();
            long p = Long.valueOf(permissions, 36).longValue();
            while (p != 0) {
                final long current = p & -p;
                p -= current;
                permissionsSet.add(Permission.values()[Long.numberOfTrailingZeros(current)]);
            }
            return permissionsSet;
        } catch (final NumberFormatException | IndexOutOfBoundsException exception) {
            return null;
        }
    }

    public static @NotNull EnumSet<Permission> parsePermissionsNotNull(final @NotNull String permissions) {
        return Objects.requireNonNullElseGet(Operation.parsePermissions(permissions), Operation::emptyPermissions);
    }
}
