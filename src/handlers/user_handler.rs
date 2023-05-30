use std::io;
use std::io::Read;
use crate::bytes::bytes_util::write_string;
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::network::client::WListClient;
use crate::network::DEFAULT_CIPHER;

pub fn login(client: &mut WListClient, username: &String, password: &String) -> Result<String, io::Error> {
    let mut sender = Vec::new();
    sender.push(DEFAULT_CIPHER);
    write_string(&mut sender, &String::from("Login"))?;
    write_string(&mut sender, username)?;
    write_string(&mut sender, password)?;
    let receiver = VecU8Reader::new(client.send(sender)?);
    todo!()
}
