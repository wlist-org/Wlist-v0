use std::io;
use std::io::{ErrorKind, Read, Write};
use aes::Aes256;
use aes::cipher::{BlockEncryptMut, KeyIvInit};
use aes::cipher::block_padding::Pkcs7;
use aes::cipher::consts::{U16, U32};
use aes::cipher::generic_array::GenericArray;
use cbc::Encryptor;
use flate2::Compression;
use flate2::write::GzEncoder;
use crate::bytes::bytes_util;


pub fn length_based_encode(target: &mut impl Write, message: &Vec<u8>) -> Result<usize, io::Error> {
    let mut size = bytes_util::write_u32_be(target, message.len() as u32)?;
    size += target.write(message)?;
    Ok(size)
}

pub fn length_based_decode(source: &mut impl Read) -> Result<Vec<u8>, io::Error> {
    let length = bytes_util::read_u32_be(source)?;
    let mut message = Vec::new();
    message.resize(length as usize, 0);
    source.read_exact(&mut message)?;
    Ok(message)
}

pub static DO_AES: u8 = 1;
pub static DEFAULT_DO_AES: u8 = DO_AES;
pub static DO_GZIP: u8 = 1 << 1;
pub static DEFAULT_DO_GZIP: u8 = DO_GZIP;

pub static DEFAULT_CIPHER: u8 = DEFAULT_DO_AES | DEFAULT_DO_GZIP;

pub fn cipher_encode(target: &mut impl Write, key: GenericArray<u8, U32>, vector: GenericArray<u8, U16>, mut message: Vec<u8>) -> Result<(), io::Error> {
    if message.len() <= 1 {
        return Err(io::Error::new(ErrorKind::UnexpectedEof, "Need cipher flags and message."));
    }
    let flags = message[0];
    let aes = flags & DO_AES > 0;
    let gzip = flags & DO_GZIP > 0;
    bytes_util::write_u8(target, flags)?;
    let mut len = message.len() - 1;
    bytes_util::write_variable_u32(target, len as u32)?;
    if aes {
        let cipher = Encryptor::<Aes256>::new(&key, &vector);
        message.extend_from_slice(&[0; 32]);
        len = match cipher.encrypt_padded_mut::<Pkcs7>(&mut message[1..], len) {
            Ok(b) => b.len(),
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to encrypt. {}", e))),
        };
    }
    if gzip {
        let mut writer = GzEncoder::new(target, Compression::default());
        writer.write_all(&message[1..len + 1])?;
        writer.try_finish()?;
        return Ok(());
    }
    let _ = target.write(&message[1..len + 1])?;
    Ok(())
}
