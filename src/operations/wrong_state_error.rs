use std::fmt::{Display, Formatter};
use std::io;
use std::io::ErrorKind;

use crate::operations::states::State;

pub struct WrongStateError {
    state: State,
    message: String,
}

impl WrongStateError {
    pub fn new(state: State, message: String) -> WrongStateError {
        WrongStateError { state, message }
    }
}

impl Display for WrongStateError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "Wrong state: {} ({})", self.state, self.message)
    }
}

impl From<WrongStateError> for io::Error {
    fn from(value:WrongStateError) -> Self {
        io::Error::new(ErrorKind::InvalidData, value.to_string())
    }
}
