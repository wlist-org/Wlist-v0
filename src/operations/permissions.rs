use std::fmt::{Display, Formatter};
use std::io;
use std::io::ErrorKind::InvalidData;

#[derive(Debug)]
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
            Permission::ServerOperate => 1 << 1,
            Permission::Broadcast => 1 << 2,
            Permission::UsersList => 1 << 3,
            Permission::UsersOperate => 1 << 4,
            Permission::DriverOperate => 1 << 5,
            Permission::FilesList => 1 << 6,
            Permission::FileDownload => 1 << 7,
            Permission::FileUpload => 1 << 8,
            Permission::FileDelete => 1 << 9,
        }
    }
}

impl From<u64> for Permission {
    fn from(value: u64) -> Self {
        match value {
            v if v == 1 << 1 => Permission::ServerOperate,
            v if v == 1 << 2 => Permission::Broadcast,
            v if v == 1 << 3 => Permission::UsersList,
            v if v == 1 << 4 => Permission::UsersOperate,
            v if v == 1 << 5 => Permission::DriverOperate,
            v if v == 1 << 6 => Permission::FilesList,
            v if v == 1 << 7 => Permission::FileDownload,
            v if v == 1 << 8 => Permission::FileUpload,
            v if v == 1 << 9 => Permission::FileDelete,
            _ => Permission::Undefined,
        }
    }
}


fn e_x36(ch: u8) -> Result<char, io::Error> {
    Ok(match ch {
         0 => '0',  1 => '1',  2 => '2',  3 => '3',  4 => '4',  5 => '5',
         6 => '6',  7 => '7',  8 => '8',  9 => '9', 10 => 'a', 11 => 'b',
        12 => 'c', 13 => 'd', 14 => 'e', 15 => 'f', 16 => 'g', 17 => 'h',
        18 => 'i', 19 => 'j', 20 => 'k', 21 => 'l', 22 => 'm', 23 => 'n',
        24 => 'o', 25 => 'p', 26 => 'q', 27 => 'r', 28 => 's', 29 => 't',
        30 => 'u', 31 => 'v', 32 => 'w', 33 => 'x', 34 => 'y', 35 => 'z',
        _ => return Err(io::Error::new(InvalidData, "Unsupported x36 encode.")),
    })
}

fn d_x36(ch: char) -> Result<u8, io::Error> {
    Ok(match ch {
        '0' =>  0, '1' =>  1, '2' =>  2, '3' =>  3, '4' =>  4, '5' =>  5,
        '6' =>  6, '7' =>  7, '8' =>  8, '9' =>  9, 'a' => 10, 'b' => 11,
        'c' => 12, 'd' => 13, 'e' => 14, 'f' => 15, 'g' => 16, 'h' => 17,
        'i' => 18, 'j' => 19, 'k' => 20, 'l' => 21, 'm' => 22, 'n' => 23,
        'o' => 24, 'p' => 25, 'q' => 26, 'r' => 27, 's' => 28, 't' => 29,
        'u' => 30, 'v' => 31, 'w' => 32, 'x' => 33, 'y' => 34, 'z' => 35,
        _ => return Err(io::Error::new(InvalidData, "Unsupported x36 decode.")),
    })
}

pub fn dump_permissions(permissions: &Vec<Permission>) -> String {
    let mut p = 0;
    for permission in permissions {
        p |= u64::from(permission);
    }
    let mut res = Vec::new();
    while p > 0 {
        res.push(e_x36((p % 36) as u8).unwrap());
        p /= 36;
    }
    res.reverse();
    res.into_iter().collect()
}

pub fn parse_permissions(permissions: &String) -> Vec<Permission> {
    let mut p = 0;
    let mut len = permissions.len();
    for ch in permissions.chars() {
        len -= 1;
        p += 36_u64.pow(len as u32) * (d_x36(ch).unwrap() as u64);
    }
    let mut res = Vec::new();
    while p > 0 {
        let low_bit = p & (p ^ (p-1));
        res.push(Permission::from(low_bit));
        p -= low_bit;
    }
    res
}
