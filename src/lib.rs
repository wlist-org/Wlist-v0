pub mod bytes;
pub mod handlers;

#[cfg(test)]
mod tests {
    use std::io;
    use crate::handlers::client::WListClient;
    use crate::handlers::user_handler::login;

    static ADDRESS: &str = "127.0.0.1:5212";

    #[test]
    fn client() -> Result<(), io::Error> {
        let mut client = WListClient::new(&String::from(ADDRESS))?;
        WListClient::send(&mut client, login(&String::from("admin"), &String::from("Gj0-rBZ4")))?;
        Ok(())
    }
}
