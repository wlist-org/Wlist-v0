use super::client::DEFAULT_CIPHER;

pub fn login(username: String, password: String) -> Vec<u8> {
    let mut client = Vec::new();
    client.push(DEFAULT_CIPHER);
    client.extend(username.as_bytes());
    client.extend(password.as_bytes());
    client
}
