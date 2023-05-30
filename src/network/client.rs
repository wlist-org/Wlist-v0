use std::io;
use std::io::{BufReader, ErrorKind};
use std::net::TcpStream;
use aes::Aes256;
use aes::cipher::{BlockEncryptMut, Iv, Key, KeyIvInit};
use aes::cipher::block_padding::Pkcs7;
use aes::cipher::consts::{U16, U32};
use aes::cipher::generic_array::GenericArray;
use cbc::{Encryptor};
use rsa::{BigUint, Pkcs1v15Encrypt, RsaPublicKey};
use rsa::rand_core::{OsRng, RngCore};

use crate::bytes::bytes_util::{read_string, read_u8_vec, write_u8_array};
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::network::codecs::{cipher_decode, cipher_encode, length_based_decode, length_based_encode};

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
        let mut receiver = VecU8Reader::new(length_based_decode(&mut client)?);
        let header = read_string(&mut receiver)?;
        if header != DEFAULT_HEADER {
            return Err(io::Error::new(ErrorKind::InvalidData, format!("Invalid header: {}", header)));
        }
        let rsa_modulus = BigUint::from_bytes_be(read_u8_vec(&mut receiver)?.as_slice());
        let rsa_exponent = BigUint::from_bytes_be(read_u8_vec(&mut receiver)?.as_slice());
        let rsa_pub_key = match RsaPublicKey::new(rsa_modulus, rsa_exponent) {
            Ok(k) => k,
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Invalid rsa public key. {}", e))),
        };
        assert_eq!(receiver.readable_bytes(), 0);
        let mut sender = Vec::new();
        let mut aes_key = [0; 48];
        OsRng.try_fill_bytes(&mut aes_key)?;
        let encrypted = match rsa_pub_key.encrypt(&mut OsRng::default(), Pkcs1v15Encrypt/*NoPadding*/, &aes_key) {
            Ok(v) => v,
            Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Failed to encrypt aes keys. {}", e)))
        };
        assert_eq!(encrypted.len(), 128);
        write_u8_array(&mut sender, &encrypted)?;
        let key = Key::<Encryptor<Aes256>>::from_slice(&aes_key[0..32]);
        let vector = Iv::<Encryptor<Aes256>>::from_slice(&aes_key[32..48]);
        let encipher = Encryptor::<Aes256>::new(key, vector);
        let mut buffer = Vec::new();
        buffer.extend_from_slice(DEFAULT_TAILOR.as_bytes());
        buffer.resize(DEFAULT_TAILOR.len() + 8, 0);
        let k = encipher.encrypt_padded_mut::<Pkcs7>(buffer.as_mut_slice(), DEFAULT_TAILOR.len()).unwrap();
        write_u8_array(&mut sender, k)?;
        length_based_encode(client.get_mut(), &sender)?;
        Ok(WListClient { stream: client, key: GenericArray::clone_from_slice(key), vector: GenericArray::clone_from_slice(vector) })
    }

    pub fn send(&mut self, message: Vec<u8>) -> Result<Vec<u8>, io::Error> {
        let mut sender = Vec::new();
        cipher_encode(&mut sender, self.key, self.vector, message)?;
        length_based_encode(self.stream.get_mut(), &sender)?;
        let mut receiver = VecU8Reader::new(length_based_decode(self.stream.get_mut())?);
        cipher_decode(&mut receiver, self.key, self.vector)
    }
}
