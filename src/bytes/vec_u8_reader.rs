use std::io;
use std::io::{ErrorKind, Read, Write};

pub struct VecU8Reader {
    bytes: Vec<u8>,
    index: usize
}

impl VecU8Reader {
    pub fn new(bytes: Vec<u8>) -> VecU8Reader {
        VecU8Reader { bytes, index: 0 }
    }

    pub fn index(&self) -> usize {
        self.index
    }

    pub fn readable_bytes(&self) -> usize {
        self.bytes.len() - self.index
    }
}

impl Read for VecU8Reader {
    fn read(&mut self, mut buf: &mut [u8]) -> io::Result<usize> {
        let len = buf.len();
        if self.bytes.len() < self.index + len {
            return Err(io::Error::new(ErrorKind::UnexpectedEof,
                                      format!("Out of index. Total: {}, Index: {}, Require: {}",
                                              self.bytes.len(), self.index, buf.len())));
        }
        self.index += len;
        buf.write(&self.bytes[self.index - len..self.index])
    }
}
