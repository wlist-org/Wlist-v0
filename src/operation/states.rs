use std::fmt::{Display, Formatter};

pub enum State {
    Undefined,
    Success,
    Broadcast,
    ServerError,
    Unsupported,
    NoPermission,
    DataError,
    FormatError,
}

impl Display for State {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", String::from(self))
    }
}

impl From<&State> for String {
    fn from(value: &State) -> Self {
        String::from(match value {
            State::Undefined => "Undefined",
            State::Success => "Success",
            State::Broadcast => "Broadcast",
            State::ServerError => "ServerError",
            State::Unsupported => "Unsupported",
            State::NoPermission => "NoPermission",
            State::DataError => "DataError",
            State::FormatError => "FormatError",
        })
    }
}

impl From<&String> for State {
    fn from(value: &String) -> Self {
        match value {
            v if v == "Success" => State::Success,
            v if v == "Broadcast" => State::Broadcast,
            v if v == "ServerError" => State::ServerError,
            v if v == "Unsupported" => State::Unsupported,
            v if v == "NoPermission" => State::NoPermission,
            v if v == "DataError" => State::DataError,
            v if v == "FormatError" => State::FormatError,
            _ => State::Undefined,
        }
    }
}
