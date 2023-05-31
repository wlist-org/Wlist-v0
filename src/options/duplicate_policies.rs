use std::fmt::{Display, Formatter};

pub enum DuplicatePolicy {
    ERROR,
    OVER,
    KEEP,
}

impl Display for DuplicatePolicy {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", String::from(self))
    }
}

impl From<&DuplicatePolicy> for String {
    fn from(value: &DuplicatePolicy) -> Self {
        String::from(match value {
            DuplicatePolicy::ERROR => "ERROR",
            DuplicatePolicy::OVER => "OVER",
            DuplicatePolicy::KEEP => "KEEP",
        })
    }
}
