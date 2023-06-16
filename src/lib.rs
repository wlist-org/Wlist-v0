pub mod bytes;
pub mod network;
pub mod operations;
pub mod options;
pub mod structures;
pub mod handlers;

#[cfg(test)]
mod tests {
    use std::io;

    use crate::handlers::user_handler::login;
    use crate::network::client::WListClient;


    static ADDRESS: &str = "127.0.0.1:5212";

    #[test]
    fn client() -> Result<(), io::Error> {
        let mut client = WListClient::new(&String::from(ADDRESS))?;
        let token = login(&mut client, &String::from(""), &String::from(""))??.unwrap();
        println!("Signed in. token: {}.", token);
        Ok(())
    }
}
