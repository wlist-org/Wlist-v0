use std::fmt::{Display, Formatter};
use crate::operation::states::State;

pub struct WrongStateError {
    state: State,
}

impl WrongStateError {
    pub fn new(state: State) -> WrongStateError {
        WrongStateError { state }
    }
}

impl Display for WrongStateError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "Wrong state: {}", self.state)
    }
}
