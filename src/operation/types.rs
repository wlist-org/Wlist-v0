use std::fmt::{Display, Formatter};

pub enum Type {
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

impl Display for Type {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", String::from(self))
    }
}

impl From<&Type> for String {
    fn from(value: &Type) -> Self {
        String::from(match value {
            Type::Undefined => "Undefined",
            Type::CloseServer => "CloseServer",
            Type::Broadcast => "Broadcast",
            Type::SetBroadcastMode => "SetBroadcastMode",
            Type::Register => "Register",
            Type::Login => "Login",
            Type::ChangePassword => "ChangePassword",
            Type::Logoff => "Logoff",
            Type::ListUsers => "ListUsers",
            Type::DeleteUser => "DeleteUser",
            Type::AddPermission => "AddPermission",
            Type::ReducePermission => "ReducePermission",
            Type::ListFiles => "ListFiles",
            Type::MakeDirectories => "MakeDirectories",
            Type::DeleteFile => "DeleteFile",
            Type::RenameFile => "RenameFile",
            Type::RequestDownloadFile => "RequestDownloadFile",
            Type::DownloadFile => "DownloadFile",
            Type::CancelDownloadFile => "CancelDownloadFile",
            Type::RequestUploadFile => "RequestUploadFile",
            Type::UploadFile => "UploadFile",
            Type::CancelUploadFile => "CancelUploadFile",
            Type::CopyFile => "CopyFile",
            Type::MoveFile => "MoveFile",
        })
    }
}
