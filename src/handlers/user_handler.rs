use std::io;

use crate::bytes::bytes_util::{read_string, write_string};
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::handlers::common_handler::{handle_state, operate, operate_with_token};
use crate::network::client::WListClient;
use crate::operations::types::Type;
use crate::operations::wrong_state_error::WrongStateError;

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

pub fn register(client: &mut WListClient, username: &String, password: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate(&Type::Register)?;
    write_string(&mut sender, username)?;
    write_string(&mut sender, password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn change_password(client: &mut WListClient, token: &String, old_password: &String, new_password: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::Register, token)?;
    write_string(&mut sender, old_password)?;
    write_string(&mut sender, new_password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn logoff(client: &mut WListClient, token: &String, password: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::Register, token)?;
    write_string(&mut sender, password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}
