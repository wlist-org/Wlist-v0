use std::fmt::{Display, Formatter};

pub enum OrderPolicy {
    FileName,
    Size,
    CreateTime,
    UpdateTime,
}

impl Display for OrderPolicy {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", String::from(self))
    }
}

impl From<&OrderPolicy> for String {
    fn from(value: &OrderPolicy) -> Self {
        String::from(match value {
            OrderPolicy::FileName => "FileName",
            OrderPolicy::Size => "Size",
            OrderPolicy::CreateTime => "CreateTime",
            OrderPolicy::UpdateTime => "UpdateTime",
        })
    }
}
