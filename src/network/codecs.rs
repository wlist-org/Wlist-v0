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
use crate::bytes::composite_reader::CompositeReader;
use crate::bytes::index_reader::IndexReader;
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::network::{DO_AES, DO_GZIP, MAX_SIZE_PER_PACKET};

pub fn length_based_sender(target: &mut impl Write, message: &mut dyn IndexReader) -> Result<usize, io::Error> {
    if message.readable() > MAX_SIZE_PER_PACKET {
        return Err(io::Error::new(ErrorKind::InvalidInput, "Message is too long when encoding."));
    }
    let mut size = bytes_util::write_u32_be(target, message.readable() as u32)?;
    let mut buffer = vec![0; message.readable()];
    message.read_exact(&mut buffer)?;
    size += target.write(&buffer)?;
    Ok(size)
}

pub fn length_based_receiver(source: &mut impl Read) -> Result<Vec<u8>, io::Error> {
    let length = bytes_util::read_u32_be(source)? as usize;
    if length > MAX_SIZE_PER_PACKET {
        return Err(io::Error::new(ErrorKind::InvalidInput, "Message is too long when decoding."));
    }
    let mut message = vec![0; length];
    source.read_exact(&mut message)?;
    Ok(message)
}

pub fn cipher_encode(mut source: Box<dyn IndexReader>, key: GenericArray<u8, U32>, vector: GenericArray<u8, U16>) -> Result<Box<dyn IndexReader>, io::Error> {
    if source.readable() <= 1 {
        return Err(io::Error::new(ErrorKind::UnexpectedEof, "Need cipher flags and message."));
    }
    let mut flags = [0];
    source.read_exact(&mut flags)?;
    let flags = flags[0];
    let aes = flags & DO_AES > 0;
    let gzip = flags & DO_GZIP > 0;
    let mut message = Vec::new();
    bytes_util::write_u8(&mut message, flags)?;
    bytes_util::write_variable_u32(&mut message, source.readable() as u32)?;
    let message = VecU8Reader::new(message);
    if !aes && !gzip {
        return Ok(Box::new(CompositeReader::composite(Box::new(message), source)));
    }
    let mut message_buffer = vec![0; source.readable()];
    source.read_exact(&mut message_buffer)?;
    let mut aes_buffer = Vec::new();
    if aes {
        aes_buffer.resize(message_buffer.len() + 32, 0);
        let cipher = Encryptor::<Aes256>::new(&key, &vector);
        let len = match cipher.encrypt_padded_b2b_mut::<Pkcs7>(&message_buffer, &mut aes_buffer) {
            Ok(b) => b.len(),
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to encrypt. {}", e))),
        };
        aes_buffer.truncate(len);
        message_buffer = aes_buffer;
    }
    if gzip {
        let mut writer = GzEncoder::new(Vec::new(), Compression::default());
        writer.write_all(&message_buffer)?;
        message_buffer = writer.finish()?;
    }
    Ok(Box::new(CompositeReader::composite(Box::new(message), Box::new(VecU8Reader::new(message_buffer)))))
}

pub fn cipher_decode(source: &Vec<u8>, key: GenericArray<u8, U32>, vector: GenericArray<u8, U16>) -> Result<Vec<u8>, io::Error> {
    if source.len() < 7 {
        return Err(io::Error::new(ErrorKind::UnexpectedEof, "Too short message."));
    }
    let flags = source[0];
    let aes = flags & DO_AES > 0;
    let gzip = flags & DO_GZIP > 0;
    let mut start = 1;
    let len = bytes_util::read_variable_u32_buf(source, &mut start)? as usize;
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
    assert_eq!(message_buffer.len(), len);
    message.write_all(message_buffer)?;
    Ok(message)
}
