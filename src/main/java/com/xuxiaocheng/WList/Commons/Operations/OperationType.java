package com.xuxiaocheng.WList.Commons.Operations;

import org.jetbrains.annotations.NotNull;

public enum OperationType {
    Undefined,
    // Self
    Logon,
    Login,
    Logoff,
    ChangeUsername,
    ChangePassword,
    GetSelfGroup,
    // State
    SetBroadcastMode,
    CloseServer,
    Broadcast,
    ResetConfiguration,
    // Groups
    AddGroup,
    ChangeGroupName,
    ChangeGroupPermissions,
    GetGroup,
    ListGroups,
    ListGroupsInPermissions,
    DeleteGroup,
    SearchGroupRegex,
    SearchGroupName,
    // Users
    ChangeUserGroup,
    GetUser,
    ListUsers,
    ListUsersInGroups,
    DeleteUser,
    DeleteUsersInGroup,
    SearchUserRegex,
    SearchUserName,
    //
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
    ;

    public static @NotNull OperationType of(final @NotNull String type) {
        try {
            return OperationType.valueOf(type);
        } catch (final IllegalArgumentException exception) {
            return OperationType.Undefined;
        }
    }
}
