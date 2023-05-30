use std::io;

use crate::bytes::bytes_util::{read_string, write_string};
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::handlers::common_handler::{handle_state, operate};
use crate::network::client::WListClient;
use crate::operation::types::Type;
use crate::operation::wrong_state_error::WrongStateError;

pub fn login(client: &mut WListClient, username: &String, password: &String) -> Result<Result<Option<String>, WrongStateError>, io::Error> {
    let mut sender = operate(&Type::Login)?;
    write_string(&mut sender, username)?;
    write_string(&mut sender, password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(s) if s => Ok(Some(read_string(&mut receiver)?)),
        Ok(_) => Ok(None),
        Err(e) => Err(e),
    })
}
