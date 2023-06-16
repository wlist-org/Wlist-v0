use std::io;
use std::io::ErrorKind::InvalidData;
use std::io::{Read, Write};
use crate::bytes::bytes_util;
use crate::operations::permissions::Permission;

pub struct UserGroupInformation {
    id: u64,
    name: String,
    permissions: Vec<Permission>,
}

impl UserGroupInformation {
    pub fn id(&self) -> u64 {
        self.id
    }

    pub fn name(&self) -> &String {
        &self.name
    }

    pub fn permissions(&self) -> &Vec<Permission> {
        &self.permissions
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

impl UserGroupInformation {
    pub fn parse(source: &mut impl Read) -> Result<UserGroupInformation, io::Error> {
        let id = bytes_util::read_variable_u64(source)?;
        let name = bytes_util::read_string(source)?;
        let permissions = parse_permissions(&bytes_util::read_string(source)?);
        Ok(UserGroupInformation{ id, name, permissions })
    }

    pub fn dump(&self, target: &mut impl Write) -> Result<(), io::Error> {
        bytes_util::write_variable_u64(target, self.id)?;
        bytes_util::write_string(target, &self.name)?;
        bytes_util::write_string(target, &dump_permissions(&self.permissions))?;
        Ok(())
    }
}
