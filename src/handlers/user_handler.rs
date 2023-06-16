use std::io;
use crate::bytes::bytes_util;
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::handlers::common_handler::{handle_state, operate, operate_with_token};
use crate::network::client::WListClient;
use crate::operations::permissions::Permission;
use crate::operations::states::State;
use crate::operations::types::Type;
use crate::operations::wrong_state_error::WrongStateError;
use crate::options::order_directions::OrderDirection;
use crate::structures::user_group_information::{dump_permissions, UserGroupInformation};
use crate::structures::user_information::UserInformation;

pub fn register(client: &mut WListClient, username: &String, password: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate(&Type::Register)?;
    bytes_util::write_string(&mut sender, username)?;
    bytes_util::write_string(&mut sender, password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn login(client: &mut WListClient, username: &String, password: &String) -> Result<Result<Option<String>, WrongStateError>, io::Error> {
    let mut sender = operate(&Type::Login)?;
    bytes_util::write_string(&mut sender, username)?;
    bytes_util::write_string(&mut sender, password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(bytes_util::read_string(&mut receiver)?)),
        Ok(false) => Ok(None),
        Err(e) => Err(e),
    })
}

pub fn get_permissions(client: &mut WListClient, token: &String) -> Result<Result<Option<UserGroupInformation>, WrongStateError>, io::Error> {
    let sender = operate_with_token(&Type::GetPermissions, token)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(UserGroupInformation::parse(&mut receiver)?)),
        Ok(false) => Ok(None),
        Err(e) => Err(e),
    })
}

pub fn change_username(client: &mut WListClient, token: &String, new_username: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::ChangeUsername, token)?;
    bytes_util::write_string(&mut sender, new_username)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn change_password(client: &mut WListClient, token: &String, old_password: &String, new_password: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::ChangePassword, token)?;
    bytes_util::write_string(&mut sender, old_password)?;
    bytes_util::write_string(&mut sender, new_password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn logoff(client: &mut WListClient, token: &String, password: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::Logoff, token)?;
    bytes_util::write_string(&mut sender, password)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn list_users(client: &mut WListClient, token: &String, limit: u32, page: u32, direction: &OrderDirection) -> Result<Result<(u64, Vec<UserInformation>), WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::ListUsers, token)?;
    bytes_util::write_variable_u32(&mut sender, limit)?;
    bytes_util::write_variable_u32(&mut sender, page)?;
    bytes_util::write_string(&mut sender, &String::from(direction))?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => {
            let total = bytes_util::read_variable_u64(&mut receiver)?;
            let count = bytes_util::read_variable_u32(&mut receiver)?;
            let mut infos = Vec::new();
            for _ in 0..count {
                infos.push(UserInformation::parse(&mut receiver)?);
            }
            Ok((total, infos))
        },
        Ok(false) => Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())),
        Err(e) => Err(e),
    })
}

pub fn delete_user(client: &mut WListClient, token: &String, username: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::DeleteUser, token)?;
    bytes_util::write_string(&mut sender, username)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn list_groups(client: &mut WListClient, token: &String, limit: u32, page: u32, direction: &OrderDirection) -> Result<Result<(u64, Vec<UserGroupInformation>), WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::ListGroups, token)?;
    bytes_util::write_variable_u32(&mut sender, limit)?;
    bytes_util::write_variable_u32(&mut sender, page)?;
    bytes_util::write_string(&mut sender, &String::from(direction))?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => {
            let total = bytes_util::read_variable_u64(&mut receiver)?;
            let count = bytes_util::read_variable_u32(&mut receiver)?;
            let mut infos = Vec::new();
            for _ in 0..count {
                infos.push(UserGroupInformation::parse(&mut receiver)?);
            }
            Ok((total, infos))
        },
        Ok(false) => Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())),
        Err(e) => Err(e),
    })
}

pub fn add_group(client: &mut WListClient, token: &String, group_name: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::AddGroup, token)?;
    bytes_util::write_string(&mut sender, group_name)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn delete_group(client: &mut WListClient, token: &String, group_name: &String) -> Result<Result<Option<bool>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::DeleteGroup, token)?;
    bytes_util::write_string(&mut sender, group_name)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(true)),
        Ok(false) => Ok(if bytes_util::read_string(&mut receiver)? == "Users" { Some(false) } else { None }),
        Err(e) => Err(e),
    })
}

pub fn change_group(client: &mut WListClient, token: &String, username: &String, group_name: &String) -> Result<Result<Option<bool>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::ChangeGroup, token)?;
    bytes_util::write_string(&mut sender, username)?;
    bytes_util::write_string(&mut sender, group_name)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(true)),
        Ok(false) => Ok(if bytes_util::read_string(&mut receiver)? == "User" { Some(false) } else { None }),
        Err(e) => Err(e),
    })
}

pub fn change_permission(client: &mut WListClient, token: &String, group_name: &String, add: bool, permissions: &Vec<Permission>) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(if add { &Type::AddPermission } else { &Type::RemovePermission }, token)?;
    bytes_util::write_string(&mut sender, group_name)?;
    bytes_util::write_string(&mut sender, &dump_permissions(permissions))?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(true),
        Ok(false) => if bytes_util::read_string(&mut receiver)? == "Group" { Ok(false) } else {
            Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) },
        Err(e) => Err(e),
    })
}
