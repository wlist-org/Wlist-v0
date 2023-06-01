use std::io;

use crate::bytes::bytes_util::{read_string, write_bool_be};
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::handlers::common_handler::{handle_broadcast_state, handle_state, operate, operate_with_token};
use crate::network::client::WListClient;
use crate::operations::states::State;
use crate::operations::types::Type;
use crate::operations::wrong_state_error::WrongStateError;

pub fn close_server(client: &mut WListClient, token: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let sender = operate_with_token(&Type::CloseServer, token)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn wait_broadcast(client: &mut WListClient) -> Result<Result<(String, VecU8Reader), WrongStateError>, io::Error> {
    // TODO: (String, Vec<u8>)
    let mut receiver = VecU8Reader::new(client.no_send()?);
    Ok(match handle_broadcast_state(&mut receiver)? {
        Ok(()) => Ok((read_string(&mut receiver)?, receiver)),
        Err(e) => Err(e),
    })
}

pub fn broadcast(client: &mut WListClient, token: &String, message: &[u8]) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::Broadcast, token)?;
    sender.extend_from_slice(message);
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn set_broadcast_mode(client: &mut WListClient, allow: bool) -> Result<Result<(), WrongStateError>, io::Error> {
    let mut sender = operate(&Type::Broadcast)?;
    write_bool_be(&mut sender, allow)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(()),
        Ok(false) => Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())),
        Err(e) => Err(e),
    })
}
