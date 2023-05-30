pub mod handlers;

#[cfg(test)]
mod tests {
    use std::io;
    use crate::handlers::client::WListClient;

    static ADDRESS: &str = "127.0.0.1:5212";

    #[test]
    fn client() -> Result<(), io::Error> {
        WListClient::new(&String::from(ADDRESS))?;
        Ok(())
    }

    // fn send_and_receive(send: Vec<u8>) -> Result<Vec<u8>, io::Error> {
    //     let mut stream = get_client().unwrap();
        // stream.write(&get_4u8_length(send.len())).unwrap();
        // stream.write(send.borrow()).unwrap();
        // let mut length = [0;4];
        // stream.read_exact(&mut length)?;
        // let mut receive = Vec::new();
        // receive.with_capacity(length);
        // stream.read_buf_exact(&mut receive);
        // Ok(receive)
    // }

    #[test]
    fn login_test() {
        // let send = login(String::from("admin"), String::from("123456"));
        // let receive = send_and_receive(send).unwrap();

    }
}
