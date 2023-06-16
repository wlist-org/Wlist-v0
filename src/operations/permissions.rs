use std::fmt::{Display, Formatter};

pub enum Permission {
    Undefined,
    ServerOperate,
    Broadcast,
    UsersList,
    UsersOperate,
    DriverOperate,
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
            Permission::DriverOperate => "DriverOperate",
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
            v if v == "DriverOperate" => Permission::DriverOperate,
            v if v == "FilesList" => Permission::FilesList,
            v if v == "FileDownload" => Permission::FileDownload,
            v if v == "FileUpload" => Permission::FileUpload,
            v if v == "FileDelete" => Permission::FileDelete,
            _ => Permission::Undefined,
        }
    }
}

impl From<&Permission> for u64 {
    fn from(value: &Permission) -> Self {
        match value {
            Permission::Undefined => 0,
            Permission::ServerOperate => 1 << 0,
            Permission::Broadcast => 1 << 1,
            Permission::UsersList => 1 << 2,
            Permission::UsersOperate => 1 << 3,
            Permission::DriverOperate => 1 << 4,
            Permission::FilesList => 1 << 5,
            Permission::FileDownload => 1 << 6,
            Permission::FileUpload => 1 << 7,
            Permission::FileDelete => 1 << 8,
        }
    }
}

impl From<u64> for Permission {
    fn from(value: u64) -> Self {
        match value {
            v if v == 1 << 0 => Permission::ServerOperate,
            v if v == 1 << 1 => Permission::Broadcast,
            v if v == 1 << 2 => Permission::UsersList,
            v if v == 1 << 3 => Permission::UsersOperate,
            v if v == 1 << 4 => Permission::DriverOperate,
            v if v == 1 << 5 => Permission::FilesList,
            v if v == 1 << 6 => Permission::FileDownload,
            v if v == 1 << 7 => Permission::FileUpload,
            v if v == 1 << 8 => Permission::FileDelete,
            _ => Permission::Undefined,
        }
    }
}
