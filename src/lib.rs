pub mod bytes; // mod bytes;
pub mod network;
pub mod operations;
pub mod options;
pub mod structures;
pub mod handlers;

#[cfg(test)]
mod tests {
    use std::io;
    use crate::handlers::file_handler::copy_file;
    use crate::handlers::user_handler::login;
    use crate::network::client::WListClient;
    use crate::options::duplicate_policies::DuplicatePolicy;

    static ADDRESS: &str = "127.0.0.1:5212";

    #[test]
    fn client() -> Result<(), io::Error> {
        let mut client = WListClient::new(&String::from(ADDRESS))?;
        let token = login(&mut client, &String::from("admin"), &String::from("9wS5mVBd"))??.unwrap();
        println!("Signed in. token: {}.", token);
        let f = copy_file(&mut client, &token, &"/123pan/test/t.txt".to_string(), &"/123pan/test/q.txt".to_string(), &DuplicatePolicy::ERROR)??.unwrap();
        println!("Copied. info: {}.", f);
        Ok(())
    }
}
