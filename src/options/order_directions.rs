use std::fmt::{Display, Formatter};

pub enum OrderDirection {
    ASCEND,
    DESCEND,
}

impl Display for OrderDirection {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", String::from(self))
    }
}

impl From<&OrderDirection> for String {
    fn from(value: &OrderDirection) -> Self {
        String::from(match value {
            OrderDirection::ASCEND => "ASCEND",
            OrderDirection::DESCEND => "DESCEND",
        })
    }
}
