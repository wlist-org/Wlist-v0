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
}

impl Read for VecU8Reader {
    fn read(&mut self, buf: &mut [u8]) -> io::Result<usize> {
        if self.bytes.len() < self.index + buf.len() {
            return Err(io::Error::new(ErrorKind::UnexpectedEof,
                                      format!("Out of index. Total: {}, Index: {}, Require: {}",
                                      self.bytes.len(), self.index, buf.len())));
        }
        let start = self.index;
        for i in 0..buf.len() {
            buf[i] = self.bytes[self.index];
            self.index += 1;
        }
        Ok(self.index - start)
    }
}

pub fn read_u8(source: &mut impl Read) -> Result<u8, io::Error> {
    let mut bytes = [0;1];
    source.read_exact(&mut bytes)?;
    Ok(u8::from_le_bytes(bytes))
}
pub fn read_u8_be(source: &mut impl Read) -> Result<u8, io::Error> {
    let mut bytes = [0;1];
    source.read_exact(&mut bytes)?;
    Ok(u8::from_be_bytes(bytes))
}
pub fn write_u8(target: &mut impl Write, message: u8) -> Result<usize, io::Error> {
    target.write(&mut u8::to_le_bytes(message))
}
pub fn write_u8_be(target: &mut impl Write, message: u8) -> Result<usize, io::Error> {
    target.write(&mut u8::to_be_bytes(message))
}

pub fn read_i8(source: &mut impl Read) -> Result<i8, io::Error> {
    let mut bytes = [0;1];
    source.read_exact(&mut bytes)?;
    Ok(i8::from_le_bytes(bytes))
}
pub fn read_i8_be(source: &mut impl Read) -> Result<i8, io::Error> {
    let mut bytes = [0;1];
    source.read_exact(&mut bytes)?;
    Ok(i8::from_be_bytes(bytes))
}
pub fn write_i8(target: &mut impl Write, message: i8) -> Result<usize, io::Error> {
    target.write(&mut i8::to_le_bytes(message))
}
pub fn write_i8_be(target: &mut impl Write, message: i8) -> Result<usize, io::Error> {
    target.write(&mut i8::to_be_bytes(message))
}

pub fn read_bool(source: &mut impl Read) -> Result<bool, io::Error> {
    Ok(read_u8(source)? != 0)
}
pub fn read_bool_be(source: &mut impl Read) -> Result<bool, io::Error> {
    Ok(read_u8_be(source)? != 0)
}
pub fn write_bool(target: &mut impl Write, message: bool) -> Result<usize, io::Error> {
    write_u8(target, if message { 1 } else { 0 })
}
pub fn write_bool_be(target: &mut impl Write, message: bool) -> Result<usize, io::Error> {
    write_u8_be(target, if message { 1 } else { 0 })
}

pub fn read_u16(source: &mut impl Read) -> Result<u16, io::Error> {
    let mut bytes = [0;2];
    source.read_exact(&mut bytes)?;
    Ok(u16::from_le_bytes(bytes))
}
pub fn read_u16_be(source: &mut impl Read) -> Result<u16, io::Error> {
    let mut bytes = [0;2];
    source.read_exact(&mut bytes)?;
    Ok(u16::from_be_bytes(bytes))
}
pub fn write_u16(target: &mut impl Write, message: u16) -> Result<usize, io::Error> {
    target.write(&mut u16::to_le_bytes(message))
}
pub fn write_u16_be(target: &mut impl Write, message: u16) -> Result<usize, io::Error> {
    target.write(&mut u16::to_be_bytes(message))
}

pub fn read_i16(source: &mut impl Read) -> Result<i16, io::Error> {
    let mut bytes = [0;2];
    source.read_exact(&mut bytes)?;
    Ok(i16::from_le_bytes(bytes))
}
pub fn read_i16_be(source: &mut impl Read) -> Result<i16, io::Error> {
    let mut bytes = [0;2];
    source.read_exact(&mut bytes)?;
    Ok(i16::from_be_bytes(bytes))
}
pub fn write_i16(target: &mut impl Write, message: i16) -> Result<usize, io::Error> {
    target.write(&mut i16::to_le_bytes(message))
}
pub fn write_i16_be(target: &mut impl Write, message: i16) -> Result<usize, io::Error> {
    target.write(&mut i16::to_be_bytes(message))
}

pub fn read_u32(source: &mut impl Read) -> Result<u32, io::Error> {
    let mut bytes = [0;4];
    source.read_exact(&mut bytes)?;
    Ok(u32::from_le_bytes(bytes))
}
pub fn read_u32_be(source: &mut impl Read) -> Result<u32, io::Error> {
    let mut bytes = [0;4];
    source.read_exact(&mut bytes)?;
    Ok(u32::from_be_bytes(bytes))
}
pub fn write_u32(target: &mut impl Write, message: u32) -> Result<usize, io::Error> {
    target.write(&mut u32::to_le_bytes(message))
}
pub fn write_u32_be(target: &mut impl Write, message: u32) -> Result<usize, io::Error> {
    target.write(&mut u32::to_be_bytes(message))
}

pub fn read_i32(source: &mut impl Read) -> Result<i32, io::Error> {
    let mut bytes = [0;4];
    source.read_exact(&mut bytes)?;
    Ok(i32::from_le_bytes(bytes))
}
pub fn read_i32_be(source: &mut impl Read) -> Result<i32, io::Error> {
    let mut bytes = [0;4];
    source.read_exact(&mut bytes)?;
    Ok(i32::from_be_bytes(bytes))
}
pub fn write_i32(target: &mut impl Write, message: i32) -> Result<usize, io::Error> {
    target.write(&mut i32::to_le_bytes(message))
}
pub fn write_i32_be(target: &mut impl Write, message: i32) -> Result<usize, io::Error> {
    target.write(&mut i32::to_be_bytes(message))
}

pub fn read_u64(source: &mut impl Read) -> Result<u64, io::Error> {
    let mut bytes = [0;8];
    source.read_exact(&mut bytes)?;
    Ok(u64::from_le_bytes(bytes))
}
pub fn read_u64_be(source: &mut impl Read) -> Result<u64, io::Error> {
    let mut bytes = [0;8];
    source.read_exact(&mut bytes)?;
    Ok(u64::from_be_bytes(bytes))
}
pub fn write_u64(target: &mut impl Write, message: u64) -> Result<usize, io::Error> {
    target.write(&mut u64::to_le_bytes(message))
}
pub fn write_u64_be(target: &mut impl Write, message: u64) -> Result<usize, io::Error> {
    target.write(&mut u64::to_be_bytes(message))
}

pub fn read_i64(source: &mut impl Read) -> Result<i64, io::Error> {
    let mut bytes = [0;8];
    source.read_exact(&mut bytes)?;
    Ok(i64::from_le_bytes(bytes))
}
pub fn read_i64_be(source: &mut impl Read) -> Result<i64, io::Error> {
    let mut bytes = [0;8];
    source.read_exact(&mut bytes)?;
    Ok(i64::from_be_bytes(bytes))
}
pub fn write_i64(target: &mut impl Write, message: i64) -> Result<usize, io::Error> {
    target.write(&mut i64::to_le_bytes(message))
}
pub fn write_i64_be(target: &mut impl Write, message: i64) -> Result<usize, io::Error> {
    target.write(&mut i64::to_be_bytes(message))
}

pub fn read_u128(source: &mut impl Read) -> Result<u128, io::Error> {
    let mut bytes = [0;16];
    source.read_exact(&mut bytes)?;
    Ok(u128::from_le_bytes(bytes))
}
pub fn read_u128_be(source: &mut impl Read) -> Result<u128, io::Error> {
    let mut bytes = [0;16];
    source.read_exact(&mut bytes)?;
    Ok(u128::from_be_bytes(bytes))
}
pub fn write_u128(target: &mut impl Write, message: u128) -> Result<usize, io::Error> {
    target.write(&mut u128::to_le_bytes(message))
}
pub fn write_u128_be(target: &mut impl Write, message: u128) -> Result<usize, io::Error> {
    target.write(&mut u128::to_be_bytes(message))
}

pub fn read_i128(source: &mut impl Read) -> Result<i128, io::Error> {
    let mut bytes = [0;16];
    source.read_exact(&mut bytes)?;
    Ok(i128::from_le_bytes(bytes))
}
pub fn read_i128_be(source: &mut impl Read) -> Result<i128, io::Error> {
    let mut bytes = [0;16];
    source.read_exact(&mut bytes)?;
    Ok(i128::from_be_bytes(bytes))
}
pub fn write_i128(target: &mut impl Write, message: i128) -> Result<usize, io::Error> {
    target.write(&mut i128::to_le_bytes(message))
}
pub fn write_i128_be(target: &mut impl Write, message: i128) -> Result<usize, io::Error> {
    target.write(&mut i128::to_be_bytes(message))
}
