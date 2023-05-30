use std::{io, vec};
use std::io::{ErrorKind, Read, Write};
use aes::Aes256;
use aes::cipher::{BlockDecryptMut, BlockEncryptMut, KeyIvInit};
use aes::cipher::block_padding::Pkcs7;
use aes::cipher::consts::{U16, U32};
use aes::cipher::generic_array::GenericArray;
use cbc::{Decryptor, Encryptor};
use flate2::Compression;
use flate2::write::{GzDecoder, GzEncoder};

use crate::bytes::bytes_util;
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::network::{DO_AES, DO_GZIP, MAX_SIZE_PER_PACKET};

pub fn length_based_encode(target: &mut impl Write, message: &Vec<u8>) -> Result<usize, io::Error> {
    if message.len() > MAX_SIZE_PER_PACKET {
        return Err(io::Error::new(ErrorKind::InvalidInput, "Message is too long when encoding."));
    }
    let mut size = bytes_util::write_u32_be(target, message.len() as u32)?;
    size += target.write(message)?;
    Ok(size)
}

pub fn length_based_decode(source: &mut impl Read) -> Result<Vec<u8>, io::Error> {
    let length = bytes_util::read_u32_be(source)? as usize;
    if length > MAX_SIZE_PER_PACKET {
        return Err(io::Error::new(ErrorKind::InvalidInput, "Message is too long when decoding."));
    }
    let mut message = vec![0; length];
    source.read_exact(&mut message)?;
    Ok(message)
}

pub fn cipher_encode(source: &Vec<u8>, key: GenericArray<u8, U32>, vector: GenericArray<u8, U16>) -> Result<Vec<u8>, io::Error> {
    if source.len() <= 1 {
        return Err(io::Error::new(ErrorKind::UnexpectedEof, "Need cipher flags and message."));
    }
    let flags = source[0];
    let aes = flags & DO_AES > 0;
    let gzip = flags & DO_GZIP > 0;
    let mut message = Vec::new();
    bytes_util::write_u8(&mut message, flags)?;
    let len = source.len() - 1;
    bytes_util::write_variable_u32(&mut message, len as u32)?;
    let mut message_buffer = &source[1..];
    let mut aes_buffer = Vec::new();
    if aes {
        aes_buffer.resize(message_buffer.len() + 32, 0);
        let cipher = Encryptor::<Aes256>::new(&key, &vector);
        message_buffer = match cipher.encrypt_padded_b2b_mut::<Pkcs7>(message_buffer, &mut aes_buffer) {
            Ok(b) => b,
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to encrypt. {}", e))),
        };
    }
    let mut gzip_buffer = Vec::new();
    if gzip {
        let mut writer = GzEncoder::new(&mut gzip_buffer, Compression::default());
        writer.write_all(message_buffer)?;
        message_buffer = writer.finish()?;
    }
    message.write_all(message_buffer)?;
    Ok(message)
}

pub fn cipher_decode(source: &Vec<u8>, key: GenericArray<u8, U32>, vector: GenericArray<u8, U16>) -> Result<Vec<u8>, io::Error> {
    if source.len() < 7 {
        return Err(io::Error::new(ErrorKind::UnexpectedEof, "Too short message."));
    }
    let flags = source[0];
    let aes = flags & DO_AES > 0;
    let gzip = flags & DO_GZIP > 0;
    let mut len = Vec::with_capacity(6);
    len.write_all(&source[1..7])?;
    let mut len_reader = VecU8Reader::new(len);
    let len = bytes_util::read_variable_u32(&mut len_reader)? as usize;
    if len <= 1 {
        return Err(io::Error::new(ErrorKind::InvalidData, "Need message."));
    }
    let start = len_reader.index() + 1;
    let mut message = Vec::new();
    bytes_util::write_u8(&mut message, flags)?;
    let mut message_buffer = &source[start..];
    let mut gzip_buffer = Vec::new();
    if gzip {
        let mut writer = GzDecoder::new(&mut gzip_buffer);
        writer.write_all(message_buffer)?;
        message_buffer = writer.finish()?;
    }
    let mut aes_buffer = Vec::new();
    if aes {
        aes_buffer.resize(message_buffer.len(), 0);
        let cipher = Decryptor::<Aes256>::new(&key, &vector);
        message_buffer = match cipher.decrypt_padded_b2b_mut::<Pkcs7>(message_buffer, &mut aes_buffer) {
            Ok(b) => b,
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to decrypt. {}", e))),
        };
    }
    message.write_all(message_buffer)?;
    Ok(message)
}
