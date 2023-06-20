use std::{io, thread};
use std::io::{BufReader, ErrorKind};
use std::net::TcpStream;
use aes::Aes256;
use aes::cipher::{BlockEncryptMut, Iv, Key, KeyIvInit};
use aes::cipher::block_padding::Pkcs7;
use aes::cipher::consts::{U16, U32};
use aes::cipher::generic_array::GenericArray;
use cbc::{Encryptor};
use chrono::Local;
use log::debug;
use rsa::{BigUint, Pkcs1v15Encrypt, RsaPublicKey};
use rsa::rand_core::{OsRng, RngCore};
use crate::bytes::bytes_util::{read_string, read_u8_vec, write_u8_array};
use crate::bytes::index_reader::IndexReader;
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::network::codecs::{cipher_decode, cipher_encode, length_based_receiver, length_based_sender};

pub struct WListClient {
    stream: BufReader<TcpStream>,
    key: GenericArray<u8, U32>,
    vector: GenericArray<u8, U16>,
}

static DEFAULT_HEADER: &str = "WList/Ciphers/Initializing";
static DEFAULT_TAILOR: &str = "Checking";

impl WListClient {
    pub fn new(address: &String) -> Result<WListClient, io::Error> {
        let mut client = BufReader::new(TcpStream::connect(address)?);
        let mut receiver = VecU8Reader::new(length_based_receiver(&mut client)?);
        let header = read_string(&mut receiver)?;
        if header != DEFAULT_HEADER {
            return Err(io::Error::new(ErrorKind::InvalidData, format!("Invalid header: {}", header)));
        }
        let rsa_modulus = BigUint::from_bytes_be(&read_u8_vec(&mut receiver)?);
        let rsa_exponent = BigUint::from_bytes_be(&read_u8_vec(&mut receiver)?);
        let rsa_pub_key = match RsaPublicKey::new(rsa_modulus, rsa_exponent) {
            Ok(k) => k,
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Invalid rsa public key. {}", e))),
        };
        assert_eq!(receiver.readable(), 0);
        let mut sender = Vec::new();
        let mut aes_key = [0; 117];
        OsRng.try_fill_bytes(&mut aes_key)?;
        let rsa_encrypted = match rsa_pub_key.encrypt(&mut OsRng::default(), Pkcs1v15Encrypt/*NoPadding*/, &aes_key) {
            Ok(v) => v,
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to encrypt aes keys. {}", e)))
        };
        assert_eq!(rsa_encrypted.len(), 128);
        write_u8_array(&mut sender, &rsa_encrypted)?;
        let key = Key::<Encryptor<Aes256>>::from_slice(&aes_key[50..82]);
        let vector = Iv::<Encryptor<Aes256>>::from_slice(&aes_key[82..98]);
        let encipher = Encryptor::<Aes256>::new(key, vector);
        let mut buffer = Vec::new();
        buffer.extend_from_slice(DEFAULT_TAILOR.as_bytes());
        buffer.resize(DEFAULT_TAILOR.len() + 8, 0);
        let aes_encrypted = match encipher.encrypt_padded_mut::<Pkcs7>(&mut buffer, DEFAULT_TAILOR.len()) {
            Ok(b) => b,
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to encrypt tailor. {}", e))),
        };
        write_u8_array(&mut sender, aes_encrypted)?;
        length_based_sender(client.get_mut(), &mut VecU8Reader::new(sender))?;
        Ok(WListClient { stream: client, key: GenericArray::clone_from_slice(key), vector: GenericArray::clone_from_slice(vector) })
    }
    
    pub fn no_send(&mut self) -> Result<Vec<u8>, io::Error> {
        let receiver = length_based_receiver(self.stream.get_mut())?;
        let message = cipher_decode(&receiver, self.key, self.vector)?;
        debug!("{} {}: Read: {} len: {} cipher: {}",
                Local::now().format("%.9f"),
                thread::current().name().unwrap_or("Unknown"),
                self.stream.get_ref().peer_addr()?,
                message.len(), message[0]);
        Ok(message)
    }

    pub fn send(&mut self, message: Box<dyn IndexReader>) -> Result<Vec<u8>, io::Error> {
        debug!("{} {}: Write: {} len: {} cipher: {}",
                Local::now().format("%.9f"),
                thread::current().name().unwrap_or("Unknown"),
                self.stream.get_ref().peer_addr()?,
                message.readable(), message.get(0).unwrap_or(0));
        let mut sender = cipher_encode(message, self.key, self.vector)?;
        length_based_sender(self.stream.get_mut(), sender.as_mut())?;
        self.no_send()
    }

    pub fn send_vec(&mut self, message: Vec<u8>) -> Result<Box<dyn IndexReader>, io::Error> {
        Ok(Box::new(VecU8Reader::new(self.send(Box::new(VecU8Reader::new(message)))?)))
    }
}
