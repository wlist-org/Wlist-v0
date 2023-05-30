use std::io;
use std::io::{ErrorKind, Read};

use crate::bytes::bytes_util::{read_string, read_u8, write_string, write_u8};
use crate::network::DEFAULT_CIPHER;
use crate::operation::states::State;
use crate::operation::types::Type;
use crate::operation::wrong_state_error::WrongStateError;

pub fn handle_state(receiver: &mut impl Read) -> Result<Result<bool, WrongStateError>, io::Error> {
    let _cipher = read_u8(receiver)?;
    let state = State::from(&read_string(receiver)?);
    Ok(match state {
        State::FormatError => return Err(io::Error::new(ErrorKind::InvalidData, "Format error.")),
        State::Success => Ok(true),
        State::DataError => Ok(false),
        State::NoPermission => Err(WrongStateError::new(State::NoPermission, "No permission.".to_string())),
        State::Broadcast => Err(WrongStateError::new(State::Broadcast, "Unsupported broadcast.".to_string())),
        State::ServerError => Err(WrongStateError::new(State::ServerError, read_string(receiver)?)),
        State::Unsupported => Err(WrongStateError::new(State::Unsupported, read_string(receiver)?)),
        State::Undefined => Err(WrongStateError::new(State::Undefined, "Undefined operation.".to_string())),
    })
}

pub fn handle_broadcast_state(receiver: &mut impl Read) -> Result<Result<(), WrongStateError>, io::Error> {
    let _cipher = read_u8(receiver)?;
    let state = State::from(&read_string(receiver)?);
    Ok(match state {
        State::FormatError => return Err(io::Error::new(ErrorKind::InvalidData, "Format error.")),
        State::Success => Err(WrongStateError::new(State::Success, "Unreachable! Received Success.".to_string())),
        State::DataError => Err(WrongStateError::new(State::DataError, "Unreachable! Received DataError.".to_string())),
        State::NoPermission => Err(WrongStateError::new(State::NoPermission, "No permission.".to_string())),
        State::ServerError => Err(WrongStateError::new(State::ServerError, read_string(receiver)?)),
        State::Unsupported => Err(WrongStateError::new(State::Unsupported, read_string(receiver)?)),
        State::Undefined => Err(WrongStateError::new(State::Undefined, "Undefined operation.".to_string())),
        State::Broadcast => Ok(()),
    })
}

pub fn operate(operation: &Type) -> Result<Vec<u8>, io::Error> {
    let mut sender = Vec::new();
    write_u8(&mut sender, DEFAULT_CIPHER)?;
    write_string(&mut sender, &operation.to_string())?;
    Ok(sender)
}

pub fn operate_with_token(operation: &Type, token: &String) -> Result<Vec<u8>, io::Error> {
    let mut sender = operate(operation)?;
    write_string(&mut sender, &token.to_string())?;
    Ok(sender)
}
