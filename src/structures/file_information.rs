use std::fmt::{Display, Formatter};
use std::io;
use std::io::{Read, Write};

use crate::bytes::bytes_util;

pub struct FileInformation {
    path: String,
    is_dir: bool,
    size: u64,
    create_time: Option<String>,
    update_time: Option<String>,
    md5: String,
}

impl FileInformation {
    pub fn path(&self) -> &String {
        &self.path
    }

    pub fn is_dir(&self) -> bool {
        self.is_dir
    }

    pub fn size(&self) -> u64 {
        self.size
    }

    pub fn create_time(&self) -> &Option<String> {
        &self.create_time
    }

    pub fn update_time(&self) -> &Option<String> {
        &self.update_time
    }

    pub fn md5(&self) -> &String {
        &self.md5
    }

    pub fn parse(source: &mut impl Read) -> Result<FileInformation, io::Error> {
        let path = bytes_util::read_string(source)?;
        let is_dir = bytes_util::read_bool_be(source)?;
        let size = bytes_util::read_variable_u64(source)?;
        let create_time = bytes_util::read_string_nullable(source)?;
        let update_time = bytes_util::read_string_nullable(source)?;
        let md5 = bytes_util::read_string(source)?;
        Ok(FileInformation{ path, is_dir, size, create_time, update_time, md5 })
    }

    pub fn dump(&self, target: &mut impl Write) -> Result<(), io::Error> {
        bytes_util::write_string(target, &self.path)?;
        bytes_util::write_bool_be(target, self.is_dir)?;
        bytes_util::write_variable_u64(target, self.size)?;
        bytes_util::write_string_nullable(target, &self.create_time)?;
        bytes_util::write_string_nullable(target, &self.update_time)?;
        bytes_util::write_string(target, &self.md5)?;
        Ok(())
    }
}

impl Display for FileInformation {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "FileInformation(path=\'{}\', is_dir={}, size={}, create_time={}, update_time={}, md5={})",
            self.path, self.is_dir, self.size, match &self.create_time { Some(s) => s, None => "None",},
               match &self.update_time { Some(s) => s, None => "None",}, self.md5)
    }
}
