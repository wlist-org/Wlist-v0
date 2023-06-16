use std::fmt::{Display, Formatter};
use std::io;
use std::io::{Read, Write};
use crate::bytes::bytes_util;

pub struct UserInformation {
    id: u64,
    username: String,
    group: String,
}

impl UserInformation {
    pub fn id(&self) -> u64 {
        self.id
    }

    pub fn username(&self) -> &String {
        &self.username
    }

    pub fn group(&self) -> &String {
        &self.group
    }

    pub fn parse(source: &mut impl Read) -> Result<UserInformation, io::Error> {
        let id = bytes_util::read_variable_u64(source)?;
        let username = bytes_util::read_string(source)?;
        let group = bytes_util::read_string(source)?;
        Ok(UserInformation{ id, username, group })
    }

    pub fn dump(&self, target: &mut impl Write) -> Result<(), io::Error> {
        bytes_util::write_variable_u64(target, self.id)?;
        bytes_util::write_string(target, &self.username)?;
        bytes_util::write_string(target, &self.group)?;
        Ok(())
    }
}

impl Display for UserInformation {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "UserInformation(id={}, username='{}', group='{}')", self.id, self.username, self.group)
    }
}
