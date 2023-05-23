package com.xuxiaocheng.WList.Server;

import com.alibaba.fastjson2.JSON;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
        Undefined,
        ServerOperate,
        Broadcast,
        UsersList,
        UsersOperate,
        FilesList, // TODO permissions for each file
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

    public static @Nullable Type valueOfType(final @NotNull String type) {
        try {
            return Type.valueOf(type);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public static @Nullable Permission valueOfPermission(final @NotNull String permission) {
        try {
            return Permission.valueOf(permission);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public static @Nullable State valueOfState(final @NotNull String state) {
        try {
            return State.valueOf(state);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public static @NotNull String dumpPermissions(final @NotNull Collection<@NotNull Permission> permissions) {
        return JSON.toJSONString(permissions.stream().map(Enum::name).collect(Collectors.toCollection(TreeSet::new)));
    }

    public static @NotNull SortedSet<@NotNull Permission> parsePermissions(final @NotNull String permissions) {
        return new TreeSet<>(JSON.parseArray(permissions).stream().map(Object::toString).map(Operation::valueOfPermission).filter(Objects::nonNull).toList());
    }
}
