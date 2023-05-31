use std::io;

use crate::bytes::bytes_util::{read_string, read_variable_u32, read_variable_u64, write_string, write_variable_u32};
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::handlers::common_handler::{handle_state, operate, operate_with_token};
use crate::network::client::WListClient;
use crate::operations::permissions::Permission;
use crate::operations::states::State;
use crate::operations::types::Type;
use crate::operations::wrong_state_error::WrongStateError;
use crate::options::order_directions::OrderDirection;
use crate::structures::user_information::{dump_permissions, UserInformation};

pub fn login(client: &mut WListClient, username: &String, password: &String) -> Result<Result<Option<String>, WrongStateError>, io::Error> {
    let mut sender = operate(&Type::Login)?;
    write_string(&mut sender, username)?;
    write_string(&mut sender, password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(read_string(&mut receiver)?)),
        Ok(false) => Ok(None),
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

pub fn list_users(client: &mut WListClient, token: &String, limit: u32, page: u32, direction: &OrderDirection) -> Result<Result<(u64, Vec<UserInformation>), WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::ListUsers, token)?;
    write_variable_u32(&mut sender, limit)?;
    write_variable_u32(&mut sender, page)?;
    write_string(&mut sender, &direction.to_string())?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    if match handle_state(&mut receiver)? {
        Ok(s) => s,
        Err(e) => return Ok(Err(e)),
    } {
        let total = read_variable_u64(&mut receiver)?;
        let count = read_variable_u32(&mut receiver)?;
        let mut infos = Vec::new();
        for _ in 0..count {
            infos.push(UserInformation::parse(&mut receiver)?);
        }
        return Ok(Ok((total, infos)));
    }
    Ok(Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())))
}

pub fn delete_user(client: &mut WListClient, token: &String, username: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::DeleteUser, token)?;
    write_string(&mut sender, username)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn change_permission(client: &mut WListClient, token: &String, username: &String, add: bool, permissions: &Vec<Permission>) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(if add { &Type::AddPermission } else { &Type::ReducePermission }, token)?;
    write_string(&mut sender, username)?;
    write_string(&mut sender, &dump_permissions(permissions))?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(true),
        Ok(false) => if read_string(&mut receiver)? == "Permissions"
        { Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) } else { Ok(false) },
        Err(e) => Err(e),
    })
}
