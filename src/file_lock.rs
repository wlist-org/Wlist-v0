use std::fs::File;
use std::io;
use fs2::FileExt;

pub struct FileLock<'a> {
    file: &'a File,
}

impl <'a> FileLock<'a> {
    pub fn lock_exclusive(file: &'a File) -> Result<FileLock<'a>, io::Error> {
        file.lock_exclusive()?;
        Ok(FileLock { file })
    }

    pub fn lock_shared(file: &'a File) -> Result<FileLock<'a>, io::Error> {
        file.lock_shared()?;
        Ok(FileLock { file })
    }
}

impl<'a> Drop for FileLock<'a> {
    fn drop(&mut self) {
        self.file.unlock().unwrap_or_else(|_| panic!("Unlocking file. {:?}", self.file));
    }
}
