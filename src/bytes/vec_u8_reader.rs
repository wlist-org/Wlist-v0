use std::cmp::min;
use std::io;
use std::io::{Read, Write};
use crate::bytes::index_reader::IndexReader;

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
}

impl Read for VecU8Reader {
    fn read(&mut self, mut buf: &mut [u8]) -> Result<usize, io::Error> {
        let len = min(buf.len(), self.readable());
        let len = buf.write(&self.bytes[self.index..self.index+len])?;
        self.index += len;
        Ok(len)
    }
}

impl IndexReader for VecU8Reader {
    fn readable(&self) -> usize {
        self.bytes.len() - self.index
    }

    fn length(&self) -> usize {
        self.bytes.len()
    }

    fn get(&self, index: usize) -> Result<u8, ()> {
        if index >= self.bytes.len() {
            return Err(());
        }
        Ok(self.bytes[index])
    }
}
