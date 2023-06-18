use std::io;
use std::io::Read;
use crate::bytes::index_reader::IndexReader;

pub struct CompositeReader {
    reader1: Box<dyn IndexReader>,
    reader2: Box<dyn IndexReader>,
}

impl CompositeReader {
    pub fn composite(reader1: Box<dyn IndexReader>, reader2: Box<dyn IndexReader>) -> CompositeReader {
        CompositeReader { reader1, reader2 }
    }
}

impl Read for CompositeReader {
    fn read(&mut self, buf: &mut [u8]) -> Result<usize, io::Error> {
        let mut index = 0;
        while self.reader1.readable() > 0 && index < buf.len() {
            index += self.reader1.read(&mut buf[index..])?;
        }
        while self.reader2.readable() > 0 && index < buf.len() {
            index += self.reader2.read(&mut buf[index..])?;
        }
        Ok(index)
    }
}

impl IndexReader for CompositeReader {
    fn readable(&self) -> usize {
        self.reader1.readable() + self.reader2.readable()
    }

    fn length(&self) -> usize {
        self.reader1.length() + self.reader2.length()
    }

    fn get(&self, index: usize) -> Result<u8, ()> {
        let len1 = self.reader1.length();
        if index < self.reader1.length() {
            return self.reader1.get(index);
        }
        if index - len1 < self.reader2.length() {
            return self.reader2.get(index - len1);
        }
        Err(())
    }
}
