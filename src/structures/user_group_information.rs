use std::fmt::{Display, Formatter};
use std::io;

use std::io::{Read, Write};
use crate::bytes::bytes_util;
use crate::operations::permissions;
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

impl UserGroupInformation {
    pub fn parse(source: &mut impl Read) -> Result<UserGroupInformation, io::Error> {
        let id = bytes_util::read_variable_u64(source)?;
        let name = bytes_util::read_string(source)?;
        let permissions = permissions::parse_permissions(&bytes_util::read_string(source)?);
        Ok(UserGroupInformation{ id, name, permissions })
    }

    pub fn dump(&self, target: &mut impl Write) -> Result<(), io::Error> {
        bytes_util::write_variable_u64(target, self.id)?;
        bytes_util::write_string(target, &self.name)?;
        bytes_util::write_string(target, &permissions::dump_permissions(&self.permissions))?;
        Ok(())
    }
}

impl Display for UserGroupInformation {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "UserGroupInformation(id={}, name='{}', permissions={:?})", self.id, self.name, self.permissions)
    }
}
