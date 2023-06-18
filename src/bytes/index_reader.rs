use std::io::Read;

pub trait IndexReader: Read {
    fn readable(&self) -> usize;
    fn length(&self) -> usize;
    fn get(&self, index: usize) -> Result<u8, ()>;
}
