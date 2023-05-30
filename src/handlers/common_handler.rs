use std::io;
use std::io::Read;
use crate::operation::wrong_state_error::WrongStateError;

fn handle_state(receiver: &mut impl Read) -> Result<bool, Result<WrongStateError, io::Error>> {

    Ok(true)
}
