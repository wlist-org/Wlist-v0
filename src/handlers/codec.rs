use std::io;
use std::io::{Read, Write};
use crate::handlers::bytes_util;
use crate::handlers::bytes_util::VecU8Reader;

pub fn length_based_decode(source: &mut impl Read) -> Result<VecU8Reader, io::Error> {
    let length = bytes_util::read_u32_be(source)?;
    let mut message = Vec::new();
    message.resize(length as usize, 0);
    source.read_exact(&mut message)?;
    Ok(VecU8Reader::new(message))
}

pub fn length_based_encode(target: &mut impl Write, message: &mut Vec<u8>) -> Result<usize, io::Error> {
    let mut size = bytes_util::write_u32_be(target, message.len() as u32)?;
    size += target.write(message)?;
    Ok(size)
}

pub static DO_AES: u8 = 1;
pub static DEFAULT_DO_AES: u8 = DO_AES;
pub static DO_GZIP: u8 = 1 << 1;
pub static DEFAULT_DO_GZIP: u8 = DO_GZIP;

pub static DEFAULT_CIPHER: u8 = DEFAULT_DO_AES | DEFAULT_DO_GZIP;

pub fn cipher_encode(target: &mut impl Write, message: Vec<u8>) -> Result<usize, io::Error> {
    let mut stream = VecU8Reader::new(message);
    let cipher = bytes_util::read_u8(&mut stream)?;
    let mut size = bytes_util::write_u8(target, cipher)?;
    size += bytes_util::write_variable_u32(target, stream.readable_bytes() as u32)?;
    let aes = cipher & DO_AES > 0;
    let gzip = cipher & DO_GZIP > 0;
    if !aes && !gzip {
        size += target.write(stream.left_bytes_slice())?;
        return Ok(size);
    }

    Ok(size)
}
