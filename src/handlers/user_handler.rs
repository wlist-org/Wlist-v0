use std::io;
use crate::bytes::bytes_util::write_string;
use crate::handlers::client::WListClient;
use crate::handlers::codecs::DEFAULT_CIPHER;

pub fn login(client: &mut WListClient, username: &String, password: &String) -> Result<String, io::Error> {
    let mut sender = Vec::new();
    sender.push(DEFAULT_CIPHER);
    write_string(&mut sender, &String::from("Login"))?;
    write_string(&mut sender, username)?;
    write_string(&mut sender, password)?;
    client.
}
