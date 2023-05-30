use std::fmt::{Display, Formatter};

pub enum Permission {
    Undefined,
    ServerOperate,
    Broadcast,
    UsersList,
    UsersOperate,
    FilesList,
    FileDownload,
    FileUpload,
    FileDelete,
}

impl Display for Permission {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", String::from(self))
    }
}

impl From<&Permission> for String {
    fn from(value: &Permission) -> Self {
        String::from(match value {
            Permission::Undefined => "Undefined",
            Permission::ServerOperate => "ServerOperate",
            Permission::Broadcast => "Broadcast",
            Permission::UsersList => "UsersList",
            Permission::UsersOperate => "UsersOperate",
            Permission::FilesList => "FilesList",
            Permission::FileDownload => "FileDownload",
            Permission::FileUpload => "FileUpload",
            Permission::FileDelete => "FileDelete",
        })
    }
}

impl From<&String> for Permission {
    fn from(value: &String) -> Self {
        match value {
            v if v == "ServerOperate" => Permission::ServerOperate,
            v if v == "Broadcast" => Permission::Broadcast,
            v if v == "UsersList" => Permission::UsersList,
            v if v == "UsersOperate" => Permission::UsersOperate,
            v if v == "FilesList" => Permission::FilesList,
            v if v == "FileDownload" => Permission::FileDownload,
            v if v == "FileUpload" => Permission::FileUpload,
            v if v == "FileDelete" => Permission::FileDelete,
            _ => Permission::Undefined,
        }
    }
}

