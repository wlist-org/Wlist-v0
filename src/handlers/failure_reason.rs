use std::fmt::{Display, Formatter};

pub enum FailureReason {
    InvalidFilename,
    DuplicatePolicyError,
    ExceedMaxSize,
    NoSuchFile,
    Others,
}

impl Display for FailureReason {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", String::from(self))
    }
}

impl From<&FailureReason> for String {
    fn from(value: &FailureReason) -> Self {
        String::from(match value {
            FailureReason::InvalidFilename => "Invalid filename.",
            FailureReason::DuplicatePolicyError => "ERROR by duplicate policy.",
            FailureReason::ExceedMaxSize => "Exceed max size per file.",
            FailureReason::NoSuchFile => "No such file.",
            FailureReason::Others => "Failure, unknown reason.",
        })
    }
}
