use std::{io, vec};
use std::io::{ErrorKind, Read, Write};
use aes::Aes256;
use aes::cipher::{BlockDecryptMut, BlockEncryptMut, KeyIvInit};
use aes::cipher::block_padding::Pkcs7;
use aes::cipher::consts::{U16, U32};
use aes::cipher::generic_array::GenericArray;
use cbc::{Decryptor, Encryptor};
use flate2::Compression;
use flate2::read::GzDecoder;
use flate2::write::GzEncoder;

use crate::bytes::bytes_util;
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::network::{DO_AES, DO_GZIP};

pub static FILE_TRANSFER_BUFFER_SIZE: usize = 4 << 20;
pub static MAX_SIZE_PER_PACKET: usize = (64 << 10) + FILE_TRANSFER_BUFFER_SIZE;

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
    source.read_exact(message.as_mut_slice())?;
    Ok(message)
}

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

pub fn cipher_decode(source: &mut impl Read, key: GenericArray<u8, U32>, vector: GenericArray<u8, U16>) -> Result<Vec<u8>, io::Error> {
    let flags = bytes_util::read_u8(source)?;
    let mut len = bytes_util::read_variable_u32(source)? as usize;
    if len <= 1 {
        return Err(io::Error::new(ErrorKind::UnexpectedEof, "Need message."));
    }
    let aes = flags & DO_AES > 0;
    let gzip = flags & DO_GZIP > 0;
    let mut message = vec![0; len];
    if aes {
        let cipher = Decryptor::<Aes256>::new(&key, &vector);
        len = match cipher.decrypt_padded_mut::<Pkcs7>(message.as_mut_slice()) {
            Ok(b) => b.len(),
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to decrypt. {}", e))),
        };
    }
    if gzip {
        let mut reader = GzDecoder::new(VecU8Reader::new(message));
        let mut buffer = Vec::new();
        reader.read_to_end(&mut buffer)?;
        return Ok(buffer);
    }
    Ok(message)
}
