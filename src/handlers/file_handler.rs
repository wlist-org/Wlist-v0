use std::io::{Error, Read};
use crate::bytes::bytes_util;
use crate::bytes::composite_reader::CompositeReader;
use crate::bytes::index_reader::IndexReader;
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::handlers::common_handler::{handle_state, operate_with_token};
use crate::handlers::failure_reason::FailureReason;
use crate::network::client::WListClient;
use crate::network::DEFAULT_DO_GZIP;
use crate::operations::states::State;
use crate::operations::types::Type;
use crate::operations::wrong_state_error::WrongStateError;
use crate::options::duplicate_policies::DuplicatePolicy;
use crate::options::order_directions::OrderDirection;
use crate::options::order_policies::OrderPolicy;
use crate::structures::file_information::FileInformation;

fn handle_state_failure(receiver: &mut impl Read) -> Result<Result<Result<(), FailureReason>, WrongStateError>, Error> {
    Ok(match handle_state(receiver)? {
        Ok(true) => Ok(Ok(())),
        Ok(false) => match bytes_util::read_string(receiver)? {
                v if v == "Parameters" => Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())),
                v if v == "Filename" => Ok(Err(FailureReason::InvalidFilename)),
                v if v == "Duplicate" => Ok(Err(FailureReason::DuplicatePolicyError)),
                v if v == "Size" => Ok(Err(FailureReason::ExceedMaxSize)),
                v if v == "File" => Ok(Err(FailureReason::NoSuchFile)),
                _ => Ok(Err(FailureReason::Others)),
            },
        Err(e) => Err(e),
    })
}

fn handle_state_information(receiver: &mut impl Read) -> Result<Result<Result<FileInformation, FailureReason>, WrongStateError>, Error> {
    Ok(match handle_state_failure(receiver)? {
        Ok(r) => Ok(match r {
            Ok(_) => Ok(FileInformation::parse(receiver)?),
            Err(e) => Err(e),
        }),
        Err(e) => Err(e),
    })
}

pub fn list_files(client: &mut WListClient, token: &String, path: &String, limit: u32, page: u32, policy: &OrderPolicy, direction: &OrderDirection, refresh: bool) -> Result<Result<Option<(u64, Vec<FileInformation>)>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::ListFiles, token)?;
    bytes_util::write_string(&mut sender, path)?;
    bytes_util::write_variable_u32(&mut sender, limit)?;
    bytes_util::write_variable_u32(&mut sender, page)?;
    bytes_util::write_string(&mut sender, &String::from(policy))?;
    bytes_util::write_string(&mut sender, &String::from(direction))?;
    bytes_util::write_bool(&mut sender, refresh)?;
    let mut receiver = client.send_vec(sender)?;
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => {
            let total = bytes_util::read_variable_u64(&mut receiver)?;
            let count = bytes_util::read_variable_u32(&mut receiver)?;
            let mut infos = Vec::new();
            for _ in 0..count {
                infos.push(FileInformation::parse(&mut receiver)?);
            }
            Ok(Some((total, infos)))
        },
        Ok(false) => if bytes_util::read_string(&mut receiver)? == "File" { Ok(None) } else {
            Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) },
        Err(e) => return Ok(Err(e)),
    })
}

pub fn make_directories(client: &mut WListClient, token: &String, path: &String, policy: &DuplicatePolicy) -> Result<Result<Result<FileInformation, FailureReason>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::MakeDirectories, token)?;
    bytes_util::write_string(&mut sender, path)?;
    bytes_util::write_string(&mut sender, &String::from(policy))?;
    let mut receiver = client.send_vec(sender)?;
    handle_state_information(&mut receiver)
}

pub fn delete_file(client: &mut WListClient, token: &String, path: &String) -> Result<Result<bool, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::DeleteFile, token)?;
    bytes_util::write_string(&mut sender, path)?;
    let mut receiver = client.send_vec(sender)?;
    handle_state(&mut receiver)
}

pub fn rename_file(client: &mut WListClient, token: &String, path: &String, name: &String, policy: &DuplicatePolicy) -> Result<Result<Result<FileInformation, FailureReason>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::RenameFile, token)?;
    bytes_util::write_string(&mut sender, path)?;
    bytes_util::write_string(&mut sender, name)?;
    bytes_util::write_string(&mut sender, &String::from(policy))?;
    let mut receiver = client.send_vec(sender)?;
    handle_state_information(&mut receiver)
}

pub fn request_download_file(client: &mut WListClient, token: &String, path: &String, from: u64, to: u64) -> Result<Result<Option<(u64, String)>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::RequestDownloadFile, token)?;
    bytes_util::write_string(&mut sender, path)?;
    bytes_util::write_variable_u64(&mut sender, from)?;
    bytes_util::write_variable2_u64_be(&mut sender, to)?;
    let mut receiver = client.send_vec(sender)?;
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some((bytes_util::read_variable2_u64_be(&mut receiver)?, bytes_util::read_string(&mut receiver)?))),
        Ok(false) => if bytes_util::read_string(&mut receiver)? == "File" { Ok(None) } else {
            Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) },
        Err(e) => Err(e),
    })
}

pub fn download_file(client: &mut WListClient, token: &String, id: &String, chunk: u32) -> Result<Result<Option<Box<dyn IndexReader>>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::DownloadFile, token)?;
    bytes_util::write_string(&mut sender, id)?;
    bytes_util::write_variable_u32(&mut sender, chunk)?;
    let mut receiver = client.send_vec(sender)?;
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(receiver)),
        Ok(false) => Ok(None),
        Err(e) => Err(e),
    })
}

pub fn cancel_download_file(client: &mut WListClient, token: &String, id: &String) -> Result<Result<bool, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::CancelDownloadFile, token)?;
    bytes_util::write_string(&mut sender, id)?;
    let mut receiver = client.send_vec(sender)?;
    handle_state(&mut receiver)
}

pub fn request_upload_file(client: &mut WListClient, token: &String, path: &String, size: u64, md5: &String, policy: &DuplicatePolicy) -> Result<Result<Result<Result<FileInformation, String>, FailureReason>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::RequestUploadFile, token)?;
    bytes_util::write_string(&mut sender, path)?;
    bytes_util::write_variable2_u64_be(&mut sender, size)?;
    bytes_util::write_string(&mut sender, md5)?;
    bytes_util::write_string(&mut sender, &String::from(policy))?;
    let mut receiver = client.send_vec(sender)?;
    Ok(match handle_state_failure(&mut receiver)? {
        Ok(r) => Ok(match r {
            Ok(_) => Ok(if bytes_util::read_bool(&mut receiver)? {
                Ok(FileInformation::parse(&mut receiver)?) } else { Err(bytes_util::read_string(&mut receiver)?) }),
            Err(e) => Err(e),
        }),
        Err(e) => Err(e),
    })
}

pub fn upload_file(client: &mut WListClient, token: &String, id: &String, chunk: u32, file: Box<dyn IndexReader>) -> Result<Result<Option<Result<FileInformation, bool>>, WrongStateError>, Error> {
    let mut sender = Vec::new();
    bytes_util::write_u8(&mut sender, DEFAULT_DO_GZIP)?;
    bytes_util::write_string(&mut sender, &String::from(&Type::UploadFile))?;
    bytes_util::write_string(&mut sender, token)?;
    bytes_util::write_string(&mut sender, id)?;
    bytes_util::write_variable_u32(&mut sender, chunk)?;
    let mut receiver = VecU8Reader::new(client.send(Box::new(CompositeReader::composite(Box::new(VecU8Reader::new(sender)), file)))?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(if bytes_util::read_bool(&mut receiver)? {
            Err(true) } else { Ok(FileInformation::parse(&mut receiver)?) })),
        Ok(false) => Ok(if bytes_util::read_string(&mut receiver)? == "Content" { Some(Err(false)) } else { None }),
        Err(e) => Err(e),
    })
}

pub fn cancel_upload_file(client: &mut WListClient, token: &String, id: &String) -> Result<Result<bool, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::CancelUploadFile, token)?;
    bytes_util::write_string(&mut sender, id)?;
    let mut receiver = client.send_vec(sender)?;
    handle_state(&mut receiver)
}

pub fn copy_file(client: &mut WListClient, token: &String, source: &String, target: &String, policy: &DuplicatePolicy) -> Result<Result<Result<FileInformation, FailureReason>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::CopyFile, token)?;
    bytes_util::write_string(&mut sender, source)?;
    bytes_util::write_string(&mut sender, target)?;
    bytes_util::write_string(&mut sender, &String::from(policy))?;
    let mut receiver = client.send_vec(sender)?;
    handle_state_information(&mut receiver)
}

pub fn move_file(client: &mut WListClient, token: &String, source: &String, target: &String, policy: &DuplicatePolicy) -> Result<Result<Result<FileInformation, FailureReason>, WrongStateError>, Error> {
    let mut sender = operate_with_token(&Type::MoveFile, token)?;
    bytes_util::write_string(&mut sender, source)?;
    bytes_util::write_string(&mut sender, target)?;
    bytes_util::write_string(&mut sender, &String::from(policy))?;
    let mut receiver = client.send_vec(sender)?;
    handle_state_information(&mut receiver)
}
