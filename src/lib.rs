pub mod bytes;
pub mod network;
pub mod operation;
pub mod handlers;

#[cfg(test)]
mod tests {
    use std::io;
    use crate::network::client::WListClient;
    use crate::handlers::user_handler::login;
    use crate::operation::states::State;

    static ADDRESS: &str = "127.0.0.1:5212";

    #[test]
    fn client() -> Result<(), io::Error> {
        let mut client = WListClient::new(&String::from(ADDRESS))?;
        let token = login(&mut client, &String::from("admin"), &String::from("Gj0-rBZ4"))?;
        println!("Signed in. token: {}.", token);
        Ok(())
    }
}
