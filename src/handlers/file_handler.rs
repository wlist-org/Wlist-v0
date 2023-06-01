use std::io;
use std::io::Read;

use crate::bytes::bytes_util::{read_bool, read_string, read_variable_u32, read_variable_u64, write_string, write_variable_u32, write_variable_u64};
use crate::bytes::vec_u8_reader::VecU8Reader;
use crate::handlers::common_handler::{handle_state, operate_with_token};
use crate::network::client::WListClient;
use crate::operations::states::State;
use crate::operations::types::Type;
use crate::operations::wrong_state_error::WrongStateError;
use crate::options::duplicate_policies::DuplicatePolicy;
use crate::options::order_directions::OrderDirection;
use crate::options::order_policies::OrderPolicy;
use crate::structures::file_information::FileInformation;

fn receive_file_information(receiver: &mut impl Read) -> Result<Result<Option<FileInformation>, WrongStateError>, io::Error> {
    Ok(match handle_state(receiver)? {
        Ok(true) => Ok(Some(FileInformation::parse(receiver)?)),
        Ok(false) => if read_string(receiver)? == "File" { Ok(None) } else {
            Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) },
        Err(e) => Err(e),
    })
}

pub fn list_files(client: &mut WListClient, token: &String, path: &String, limit: u32, page: u32, policy: &OrderPolicy, direction: &OrderDirection) -> Result<Result<Option<(u64, Vec<FileInformation>)>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::ListFiles, token)?;
    write_string(&mut sender, path)?;
    write_variable_u32(&mut sender, limit)?;
    write_variable_u32(&mut sender, page)?;
    write_string(&mut sender, &policy.to_string())?;
    write_string(&mut sender, &direction.to_string())?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => {
            let total = read_variable_u64(&mut receiver)?;
            let count = read_variable_u32(&mut receiver)?;
            let mut infos = Vec::new();
            for _ in 0..count {
                infos.push(FileInformation::parse(&mut receiver)?);
            }
            Ok(Some((total, infos)))
        },
        Ok(false) => if read_string(&mut receiver)? == "File" { Ok(None) } else {
            Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) },
        Err(e) => return Ok(Err(e)),
    })
}

pub fn make_directories(client: &mut WListClient, token: &String, path: &String, policy: &DuplicatePolicy) -> Result<Result<Option<FileInformation>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::MakeDirectories, token)?;
    write_string(&mut sender, path)?;
    write_string(&mut sender, &policy.to_string())?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    receive_file_information(&mut receiver)
}

pub fn delete_file(client: &mut WListClient, token: &String, path: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::DeleteFile, token)?;
    write_string(&mut sender, path)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn rename_file(client: &mut WListClient, token: &String, path: &String, name: &String, policy: &DuplicatePolicy) -> Result<Result<Option<FileInformation>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::RenameFile, token)?;
    write_string(&mut sender, path)?;
    write_string(&mut sender, name)?;
    write_string(&mut sender, &policy.to_string())?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    receive_file_information(&mut receiver)
}

pub fn request_download_file(client: &mut WListClient, token: &String, path: &String, from: u64, to: u64) -> Result<Result<Option<(u64, u32, String)>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::RequestDownloadFile, token)?;
    write_string(&mut sender, path)?;
    write_variable_u64(&mut sender, from)?;
    write_variable_u64(&mut sender, to)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some((read_variable_u64(&mut receiver)?, read_variable_u32(&mut receiver)?, read_string(&mut receiver)?))),
        Ok(false) => if read_string(&mut receiver)? == "File" { Ok(None) } else {
            Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) },
        Err(e) => Err(e),
    })
}

pub fn download_file(client: &mut WListClient, token: &String, id: &String) -> Result<Result<Option<(u32, VecU8Reader)>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::DownloadFile, token)?;
    write_string(&mut sender, id)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some((read_variable_u32(&mut receiver)?, receiver))),
        Ok(false) => Ok(None),
        Err(e) => Err(e),
    })
}

pub fn cancel_download_file(client: &mut WListClient, token: &String, id: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::CancelDownloadFile, token)?;
    write_string(&mut sender, id)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn request_upload_file(client: &mut WListClient, token: &String, path: &String, size: u64, md5: &String, policy: &DuplicatePolicy) -> Result<Result<Option<Result<FileInformation, String>>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::RequestUploadFile, token)?;
    write_string(&mut sender, path)?;
    write_variable_u64(&mut sender, size)?;
    write_string(&mut sender, md5)?;
    write_string(&mut sender, &policy.to_string())?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(if read_bool(&mut receiver)? { Ok(FileInformation::parse(&mut receiver)?) } else {
            Err(read_string(&mut receiver)?) })),
        Ok(false) => if read_string(&mut receiver)? == "File" { Ok(None) } else {
            Err(WrongStateError::new(State::DataError, "Illegal argument.".to_string())) },
        Err(e) => Err(e),
    })
}

pub fn upload_file(client: &mut WListClient, token: &String, id: &String, chunk: u32, file: &[u8]) -> Result<Result<Option<Option<FileInformation>>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::DownloadFile, token)?;
    write_string(&mut sender, id)?;
    write_variable_u32(&mut sender, chunk)?;
    sender.extend_from_slice(file);
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    Ok(match handle_state(&mut receiver)? {
        Ok(true) => Ok(Some(if read_bool(&mut receiver)? { None } else { Some(FileInformation::parse(&mut receiver)?) })),
        Ok(false) => Ok(None),
        Err(e) => Err(e),
    })
}

pub fn cancel_upload_file(client: &mut WListClient, token: &String, id: &String) -> Result<Result<bool, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::CancelUploadFile, token)?;
    write_string(&mut sender, id)?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    handle_state(&mut receiver)
}

pub fn copy_file(client: &mut WListClient, token: &String, source: &String, target: &String, policy: &DuplicatePolicy) -> Result<Result<Option<FileInformation>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::CopyFile, token)?;
    write_string(&mut sender, source)?;
    write_string(&mut sender, target)?;
    write_string(&mut sender, &policy.to_string())?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    receive_file_information(&mut receiver)
}

pub fn move_file(client: &mut WListClient, token: &String, source: &String, target: &String, policy: &DuplicatePolicy) -> Result<Result<Option<FileInformation>, WrongStateError>, io::Error> {
    let mut sender = operate_with_token(&Type::MoveFile, token)?;
    write_string(&mut sender, source)?;
    write_string(&mut sender, target)?;
    write_string(&mut sender, &policy.to_string())?;
    let mut receiver = VecU8Reader::new(client.send(&sender)?);
    receive_file_information(&mut receiver)
}
