use std::io;
use std::io::{Read, Write};
use crate::handlers::bytes_util;
use crate::handlers::bytes_util::VecU8Reader;

pub fn length_based_decode(source: &mut impl Read) -> Result<VecU8Reader, io::Error> {
    let length = bytes_util::read_u32(source)?;
    let mut message = Vec::new();
    message.resize(length as usize, 0);
    source.read_exact(&mut message)?;
    Ok(VecU8Reader::new(message))
}

pub fn length_based_encode(target: &mut impl Write, message: &mut Vec<u8>) -> Result<(), io::Error> {
    bytes_util::write_u32(target, message.len() as u32)?;
    target.write(message)?;
    Ok(())
}

pub static DEFAULT_HEADER: &str = "WList/Ciphers-Initializing";
pub static DEFAULT_CIPHER: u8 = 1;
