#![allow(non_snake_case)]

mod global_configuration;
pub mod print_table;
pub mod file_lock;

pub extern crate serde_yaml;
pub extern crate fs2;
pub extern crate memmap;
pub extern crate md5;

use std::cmp::min;
use std::env::{set_var, var};
use std::fs::OpenOptions;
use std::{io, thread};
use std::io::{copy, Read, Seek, stdin, stdout, Write};
use std::path::Path;
use std::sync::{Arc, Mutex};
use std::sync::atomic::{AtomicU32, AtomicU64, Ordering};
use std::sync::mpsc::sync_channel;
use log::{debug, error, info, trace};
use md5::Context;
use memmap::MmapOptions;
use wlist_client_library::bytes::vec_u8_reader::VecU8Reader;
use wlist_client_library::chrono::Local;
use wlist_client_library::handlers::{file_handler, server_handler, user_handler};
use wlist_client_library::handlers::failure_reason::FailureReason;
use wlist_client_library::network::client::WListClient;
use wlist_client_library::network::FILE_TRANSFER_BUFFER_SIZE;
use wlist_client_library::operations::permissions::Permission;
use wlist_client_library::operations::states::State;
use wlist_client_library::operations::wrong_state_error::WrongStateError;
use wlist_client_library::options::duplicate_policies::DuplicatePolicy;
use wlist_client_library::options::order_directions::OrderDirection;
use wlist_client_library::options::order_policies::OrderPolicy;
use wlist_client_library::structures::file_information::FileInformation;
use crate::file_lock::FileLock;
use crate::global_configuration::GlobalConfiguration;
use crate::print_table::{from_slice, PrintTable, PrintTableCached};

fn read_line() -> Result<String, io::Error> {
    let mut str = String::new();
    stdin().read_line(&mut str)?;
    while str.as_bytes()[str.len() - 1] == b'\n' || str.as_bytes()[str.len() - 1] == b'\r' {
        str.pop();
        if str.is_empty() {
            return Ok(str);
        }
    }
    Ok(str)
}
fn enter_to_continue() -> Result<bool, io::Error> {
    print!("Enter to continue, or other key to cancel: "); stdout().flush()?;
    Ok(!read_line()?.trim().is_empty())
}

fn main() -> Result<(), io::Error> {
    if var("RUST_LOG").is_err() {
        set_var("RUST_LOG", "debug, wlist_client_library=debug");
    }
    env_logger::init();
    info!("Hello WList Client (Console Version) v0.1.2! Initializing...");
    let menu = PrintTable::create(from_slice(&vec!["ID", "Operation", "Permission", "Detail"]))
        .add_body(from_slice(&vec!["0", "Exit", "any", "Logoff and shutdown client."]))
        .add_body(from_slice(&vec!["1", "Close server", "admin", "Try close server, and then shutdown client."]))
        .add_body(from_slice(&vec!["10", "Register", "any", "Register a new user."]))
        .add_body(from_slice(&vec!["11", "Login", "any", "Login with username and password."]))
        .add_body(from_slice(&vec!["12", "Get permissions", "user", "Display the current user group."]))
        .add_body(from_slice(&vec!["13", "Change username", "user", "Change username. Don't need login again."]))
        .add_body(from_slice(&vec!["14", "Change password", "user", "Change password after verifying the old one. All accounts need to be logged in again."]))
        .add_body(from_slice(&vec!["15", "Logoff", "user", "Logoff after verifying old password. WARNING: Please operate with caution."]))
        .add_body(from_slice(&vec!["20", "List users", "admin", "Get registered users list."]))
        .add_body(from_slice(&vec!["21", "Delete user", "admin", "Delete any of registered user."]))
        .add_body(from_slice(&vec!["22", "List groups", "admin", "Get user groups list."]))
        .add_body(from_slice(&vec!["23", "Add group", "admin", "Add a new empty user group."]))
        .add_body(from_slice(&vec!["24", "Delete group", "admin", "Delete a user group without users in it."]))
        .add_body(from_slice(&vec!["25", "Change group", "admin", "Change user into explicit user group."]))
        .add_body(from_slice(&vec!["26", "Add permissions", "admin", "Add permissions to user group."]))
        .add_body(from_slice(&vec!["27", "Remove permissions", "admin", "Remove permissions from user group."]))
        .add_body(from_slice(&vec!["30", "Build index", "admin", "Build file index for explicit driver in background."]))
        .add_body(from_slice(&vec!["40", "List files", "user", "Get files list in explicit directory."]))
        .add_body(from_slice(&vec!["41", "Make directory", "user", "Making new directories recursively."]))
        .add_body(from_slice(&vec!["42", "Delete file", "user", "Delete a file or directory recursively."]))
        .add_body(from_slice(&vec!["43", "Rename file", "user", "Rename a file or directory."]))
        .add_body(from_slice(&vec!["44", "Copy file", "user", "Copy a file to another path."]))
        .add_body(from_slice(&vec!["45", "Move file", "user", "Move a file or a directory to another directory."]))
        .add_body(from_slice(&vec!["46", "Download file", "user", "Download file to the local path."]))
        .add_body(from_slice(&vec!["47", "Upload file", "user", "Upload local file to the path."]))
        .finish();
    let permissions_table: PrintTableCached = PrintTable::create(from_slice(&vec!["id", "policy", "detail"]))
        .add_body(from_slice(&vec!["1", "ServerOperate", "Operate server state. DANGEROUS!"]))
        .add_body(from_slice(&vec!["2", "Broadcast", "Send broadcast to other connections."]))
        .add_body(from_slice(&vec!["3", "UsersList", "Get users and user groups list."]))
        .add_body(from_slice(&vec!["4", "UsersOperate", "Modify users and user groups. DANGEROUS!"]))
        .add_body(from_slice(&vec!["5", "DriverOperate", "Operate web drivers. DANGEROUS!"]))
        .add_body(from_slice(&vec!["6", "FilesList", "Get files list."]))
        .add_body(from_slice(&vec!["7", "FileDownload", "Download explicit file."]))
        .add_body(from_slice(&vec!["8", "FileUpload", "Upload file to explicit path."]))
        .add_body(from_slice(&vec!["9", "FileDelete", "Delete explicit file."]))
        .finish();
    let duplicate_policy_table: PrintTableCached = PrintTable::create(from_slice(&vec!["id", "policy", "detail"]))
        .add_body(from_slice(&vec!["1", "ERROR", "Only attempt to response the same file."]))
        .add_body(from_slice(&vec!["2", "OVER", "Force replace existing file."]))
        .add_body(from_slice(&vec!["3", "KEEP", "Automatically rename and retry."]))
        .finish();
    let mut token: Option<(String, String)> = None;
    let configuration = Path::new("client.yaml");
    debug!("Initializing global configuration. file: {:?}", configuration);
    GlobalConfiguration::init(configuration)?;
    trace!("Initialized global configuration.");
    let address = String::from(&GlobalConfiguration::get().host) + ":" + &GlobalConfiguration::get().port.to_string();
    debug!("Connecting to WList Server (address: {}) ...", address);
    let mut client = WListClient::new(&address)?;
    trace!("Initialized WList clients.");
    loop {
        println!("Current login status: {}", match &token {
            Some(t) => String::from("true") + "  username: '" + t.1.as_str() + "'",
            None => String::from("false"),
        });
        menu.print();
        print!("Please enter operation id: "); stdout().flush()?;
        match match match read_line()?.parse::<u32>() {
            Ok(i) => i,
            Err(e) => {
                println!("Invalid number format. {} \nEnter to continue...", e);
                read_line()?;
                continue
            }
        } {
            0 => console_exit(&token)?,
            1 => console_close_server(&mut client, &token)?,
            10 => console_register(&mut client)?,
            11 => console_login(&mut client, &mut token)?,
            12 => console_get_permissions(&mut client, &token)?,
            13 => console_change_username(&mut client, &mut token)?,
            14 => console_change_password(&mut client, &mut token)?,
            15 => console_logoff(&mut client, &mut token)?,
            20 => console_list_users(&mut client, &token)?,
            21 => console_delete_user(&mut client, &mut token)?,
            22 => console_list_groups(&mut client, &token)?,
            23 => console_add_group(&mut client, &token)?,
            24 => console_delete_group(&mut client, &token)?,
            25 => console_change_group(&mut client, &token)?,
            26 => console_add_permissions(&mut client, &token, &permissions_table)?,
            27 => console_remove_permissions(&mut client, &token, &permissions_table)?,
            30 => console_build_index(&mut client, &token)?,
            40 => console_list_files(&mut client, &token)?,
            41 => console_make_directories(&mut client, &token, &duplicate_policy_table)?,
            42 => console_delete_file(&mut client, &token)?,
            43 => console_rename_file(&mut client, &token, &duplicate_policy_table)?,
            44 => console_copy_file(&mut client, &token, &duplicate_policy_table)?,
            45 => console_move_file(&mut client, &token, &duplicate_policy_table)?,
            46 => console_download_directly(&mut client, &token, &address)?,
            47 => console_upload_directly(&mut client, &token, &duplicate_policy_table, &address)?,
            _ => Ok(3),
        } {
            Ok(0) => (),
            Ok(1) => println!("Currently not logged in, please login and try again."),
            Ok(2) => break,
            Ok(3) => println!("Unsupported operation id."),
            Ok(_) => unreachable!(),
            Err(e) => match e.state {
                State::NoPermission => println!("No permission! It is also possible that the password or permissions have been changed and a new login is required."),
                _ => println!("Wrong response state. Please check if the client version matches. {}", e),
            }
        }
        println!("Enter to continue...");
        read_line()?;
    }
    info!("Thanks to use WList Client (Console Version).");
    Ok(())
}

fn console_exit(t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Exiting client... (log out) WARNING!");
    if t.is_none() {
        return Ok(Ok(2));
    }
    Ok(if enter_to_continue()? { Ok(0) } else { Ok(2) })
}
fn console_close_server(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Closing server... WARNING!");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    if enter_to_continue()? { return Ok(Ok(0)); }
    if match server_handler::close_server(client, token)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
        return Ok(Ok(2));
    }
    println!("Failure, unknown reason.");
    Ok(Ok(0))
}

fn console_register(client: &mut WListClient) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Registering...");
    print!("Please enter username: "); stdout().flush()?;
    let username = read_line()?;
    print!("Please enter password: "); stdout().flush()?;
    let password = read_line()?;
    if match user_handler::register(client, &username, &password)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success, then login again!");
    } else {
        println!("Username already exists.");
    }
    Ok(Ok(0))
}
fn console_login(client: &mut WListClient, t: &mut Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Logging in...");
    print!("Please enter username: "); stdout().flush()?;
    let username = read_line()?;
    print!("Please enter password: "); stdout().flush()?;
    let password = read_line()?;
    match match user_handler::login(client, &username, &password)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        Some(token) => {
            println!("Success!");
            *t = Some((token, username));
        }
        None => { println!("Wrong username or password."); }
    }
    Ok(Ok(0))
}
fn console_get_permissions(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Getting permissions...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    match match user_handler::get_permissions(client, token)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        Some(info) => {
            println!("Success!");
            println!("Group name: {}", info.name());
            let mut name = Vec::new();
            let mut value = Vec::new();
            for i in 1..10 {
                let p = Permission::from(1 << i);
                name.push(String::from(&p));
                value.push("false");
            }
            for permission in info.permissions() {
                let mut index = 0;
                let mut id = u64::from(permission) >> 2;
                while id > 0 {
                    index += 1;
                    id >>= 1;
                }
                if index < 10 {
                    value[index] = "true";
                }
            }
            PrintTable::create(name).add_body(from_slice(&value)).print();
        }
        None => { println!("Failure, unknown reason."); }
    }
    Ok(Ok(0))
}
fn console_change_username(client: &mut WListClient, t: &mut Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Changing username...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter new username: "); stdout().flush()?;
    let new_username = read_line()?;
    if match user_handler::change_username(client, token, &new_username)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
        *t = Some((t.as_ref().unwrap().0.clone(), new_username));
    } else {
        println!("Denied operation.");
    }
    Ok(Ok(0))
}
fn console_change_password(client: &mut WListClient, t: &mut Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Changing password...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter old password: "); stdout().flush()?;
    let old_password = read_line()?;
    print!("Please enter new password: "); stdout().flush()?;
    let new_password = read_line()?;
    if enter_to_continue()? { return Ok(Ok(0)); }
    if match user_handler::change_password(client, token, &old_password, &new_password)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success, then login again!");
        *t = None;
    } else {
        println!("Wrong password or expired token.");
    }
    Ok(Ok(0))
}
fn console_logoff(client: &mut WListClient, t: &mut Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Logging off... WARNING!");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please confirm password: "); stdout().flush()?;
    let password = read_line()?;
    if enter_to_continue()? { return Ok(Ok(0)); }
    if match user_handler::logoff(client, token, &password)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
        *t = None;
    } else {
        println!("Denied operation.");
    }
    Ok(Ok(0))
}

fn console_list_users(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Listing users...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    let limit = GlobalConfiguration::get().limit;
    print!("Please enter page number, or enter to the first page (limit: {}): ", limit); stdout().flush()?;
    let chose = read_line()?;
    let mut page_count = chose.trim().parse().unwrap_or(1) - 1;
    loop {
        let page = match user_handler::list_users(client, token, limit, page_count, &OrderDirection::ASCEND)? {
            Ok(p) => p, Err(e) => return Ok(Err(e)),
        };
        if page.1.is_empty() && page_count > 0 {
            break
        }
        println!("Total: {}, Page: {}", page.0, page_count);
        let mut table = PrintTable::create(from_slice(&vec!["id", "username", "group"]));
        for user in &page.1 {
            table = table.add_body(from_slice(&vec![&user.id().to_string(), user.username(), user.group()]));
        }
        table.print();
        if (page_count + 1) as u64 * limit as u64 >= page.0 {
            break
        }
        if enter_to_continue()? {
            break
        }
        page_count += 1;
    }
    Ok(Ok(0))
}
fn console_delete_user(client: &mut WListClient, t: &mut Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Deleting user...");
    let token = match t { Some(p) => (&p.0, &p.1), None => return Ok(Ok(1)) };
    print!("Please enter username to delete: "); stdout().flush()?;
    let username = read_line()?;
    let logoff = &username == token.1;
    if logoff {
        println!("WARNING! Deleting yourself. (LOGOFF)");
        if enter_to_continue()? {
            return Ok(Ok(0));
        }
    }
    if match user_handler::delete_user(client, token.0, &username)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
        if logoff {
            *t = None;
        }
    } else {
        println!("No such user or denied operation.");
    }
    Ok(Ok(0))
}
fn console_list_groups(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Listing groups...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    let limit = GlobalConfiguration::get().limit;
    print!("Please enter page number, or enter to the first page (limit: {}): ", limit); stdout().flush()?;
    let chose = read_line()?;
    let mut page_count = chose.trim().parse().unwrap_or(1) - 1;
    loop {
        let page = match user_handler::list_groups(client, token, limit, page_count, &OrderDirection::ASCEND)? {
            Ok(p) => p, Err(e) => return Ok(Err(e)),
        };
        if page.1.is_empty() && page_count > 0 {
            break
        }
        println!("Total: {}, Page: {}", page.0, page_count);
        let mut table = PrintTable::create(from_slice(&vec!["id", "name", "permissions"]));
        for user in &page.1 {
            table = table.add_body(from_slice(&vec![&user.id().to_string(), user.name(), &format!("{:?}", user.permissions())]));
        }
        table.print();
        if (page_count + 1) as u64 * limit as u64 >= page.0 {
            break
        }
        if enter_to_continue()? {
            break
        }
        page_count += 1;
    }
    Ok(Ok(0))
}
fn console_add_group(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Adding user group...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter new group name: "); stdout().flush()?;
    let group_name = read_line()?;
    if match user_handler::add_group(client, token, &group_name)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
    } else {
        println!("Group name already exists.");
    }
    Ok(Ok(0))
}
fn console_delete_group(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Deleting user group...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter group name to delete: "); stdout().flush()?;
    let group_name = read_line()?;
    match match user_handler::delete_group(client, token, &group_name)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        Some(true) => println!("Success!"),
        Some(false) => println!("Some users are still in this group."),
        None => println!("No such group or denied operation."),
    }
    Ok(Ok(0))
}
fn console_change_group(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Changing user group...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter username: "); stdout().flush()?;
    let username = read_line()?;
    print!("Please enter group name: "); stdout().flush()?;
    let group_name = read_line()?;
    match match user_handler::change_group(client, token, &username, &group_name)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        Some(true) => println!("Success!"),
        Some(false) => println!("No such user or denied operation."),
        None => println!("No such group."),
    }
    Ok(Ok(0))
}
fn console_add_permissions(client: &mut WListClient, t: &Option<(String, String)>, permissions_table: &PrintTableCached) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Adding permissions for user group...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter group name: "); stdout().flush()?;
    let group_name = read_line()?;
    let permissions = read_permissions(permissions_table)?;
    if match user_handler::change_permission(client, token, &group_name, true, &permissions)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
    } else {
        println!("No such group.");
    }
    Ok(Ok(0))
}
fn console_remove_permissions(client: &mut WListClient, t: &Option<(String, String)>, permissions_table: &PrintTableCached) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Removing permissions for user group...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter group name: "); stdout().flush()?;
    let group_name = read_line()?;
    let permissions = read_permissions(permissions_table)?;
    if match user_handler::change_permission(client, token, &group_name, false, &permissions)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
    } else {
        println!("No such group or denied operation.");
    }
    Ok(Ok(0))
}

fn console_build_index(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Building index...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter driver name: "); stdout().flush()?;
    let driver = read_line()?;
    if match file_handler::build_index(client, token, &driver)? {
        Ok(t) => t, Err(e) => return Ok(Err(e)),
    } {
        println!("Success, task is running in background!");
    } else {
        println!("No such driver.");
    }
    Ok(Ok(0))
}

fn console_list_files(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Listing files...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter directory path: "); stdout().flush()?;
    let path = read_line()?;
    print!("Enter to use cache, otherwise force refresh: "); stdout().flush()?;
    let mut refresh = !read_line()?.trim().is_empty();
    let limit = GlobalConfiguration::get().limit;
    print!("Please enter page number, or enter to the first page (limit: {}): ", limit); stdout().flush()?;
    let chose = read_line()?;
    let mut page_count = chose.trim().parse().unwrap_or(1) - 1;
    loop {
        let page = match file_handler::list_files(client, token, &path, limit, page_count, &OrderPolicy::FileName, &OrderDirection::ASCEND, refresh)? {
            Ok(p) => p, Err(e) => return Ok(Err(e)),
        };
        refresh = false;
        if page.is_none() {
            println!("No such directory.");
            break
        }
        let page = page.unwrap();
        if page.1.is_empty() && page_count > 0 {
            break
        }
        println!("Total: {}, Page: {}", page.0, page_count);
        let mut table = write_file_print_table();
        for file in &page.1 {
            table = write_file_information(table, file);
        }
        table.print();
        if (page_count + 1) as u64 * limit as u64 >= page.0 {
            break
        }
        if enter_to_continue()? {
            break
        }
        page_count += 1;
    }
    Ok(Ok(0))
}
fn console_make_directories(client: &mut WListClient, t: &Option<(String, String)>, duplicate_policy_table: &PrintTableCached) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Making directory...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter directory path: "); stdout().flush()?;
    let filepath = read_line()?;
    println!("It will try to response the original one.");
    let duplicate_policy = read_duplicate_policy(duplicate_policy_table)?;
    handle_file_reason(&match file_handler::make_directories(client, token, &filepath, &duplicate_policy)? {
        Ok(t) => t, Err(e) => return Ok(Err(e)),
    });
    Ok(Ok(0))
}
fn console_delete_file(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Deleting file...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter file path: "); stdout().flush()?;
    let filepath = read_line()?;
    if match file_handler::delete_file(client, token, &filepath)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
    } else {
        println!("Failure, unknown reason.");
    }
    Ok(Ok(0))
}
fn console_rename_file(client: &mut WListClient, t: &Option<(String, String)>, duplicate_policy_table: &PrintTableCached) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Renaming file...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter file path: "); stdout().flush()?;
    let filepath = read_line()?;
    print!("Please enter new filename: "); stdout().flush()?;
    let filename = read_line()?;
    let duplicate_policy = read_duplicate_policy(duplicate_policy_table)?;
    handle_file_reason(&match file_handler::rename_file(client, token, &filepath, &filename, &duplicate_policy)? {
        Ok(t) => t, Err(e) => return Ok(Err(e)),
    });
    Ok(Ok(0))
}
fn console_copy_file(client: &mut WListClient, t: &Option<(String, String)>, duplicate_policy_table: &PrintTableCached) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Copying file...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter source file path: "); stdout().flush()?;
    let source = read_line()?;
    print!("Please enter target file path: "); stdout().flush()?;
    let target = read_line()?;
    let duplicate_policy = read_duplicate_policy(duplicate_policy_table)?;
    handle_file_reason(&match file_handler::copy_file(client, token, &source, &target, &duplicate_policy)? {
        Ok(t) => t, Err(e) => return Ok(Err(e)),
    });
    Ok(Ok(0))
}
fn console_move_file(client: &mut WListClient, t: &Option<(String, String)>, duplicate_policy_table: &PrintTableCached) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Moving file...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter source file path: "); stdout().flush()?;
    let source = read_line()?;
    print!("Please enter target directory path: "); stdout().flush()?;
    let target = read_line()?;
    let duplicate_policy = read_duplicate_policy(duplicate_policy_table)?;
    handle_file_reason(&match file_handler::move_file(client, token, &source, &target, &duplicate_policy)? {
        Ok(t) => t, Err(e) => return Ok(Err(e))
    });
    Ok(Ok(0))
}

fn read_permissions(permissions_table: &PrintTableCached) -> Result<Vec<Permission>, io::Error> {
    permissions_table.print();
    print!("Please enter the selected permission ids (separate with spaces): "); stdout().flush()?;
    loop {
        let mut permissions = Vec::new();
        let mut flag = false;
        for chose in read_line()?.split_whitespace() {
            let id: u8 = chose.parse().unwrap_or(0);
            let permission = Permission::from(1 << id);
            if &permission.to_string() == "Undefined" {
                print!("Invalid id ({}). Please enter valid permission id again: ", chose); stdout().flush()?;
                flag = true;
            } else {
                permissions.push(permission);
            }
        }
        if flag {
            continue
        }
        return Ok(permissions)
    }
}
fn read_duplicate_policy(duplicate_policy_table: &PrintTableCached) -> Result<DuplicatePolicy, io::Error> {
    duplicate_policy_table.print();
    print!("Please enter duplicate policy id: "); stdout().flush()?;
    loop {
        let id: u8 = read_line()?.trim().parse().unwrap_or(0);
        return Ok(match id {
            1 => DuplicatePolicy::ERROR,
            2 => DuplicatePolicy::OVER,
            3 => DuplicatePolicy::KEEP,
            _ => {
                print!("Invalid id. Please enter valid duplicate policy id again: "); stdout().flush()?;
                continue
            }
        })
    }

}
fn write_file_print_table() -> PrintTable {
    PrintTable::create(from_slice(&vec!["name", "dir", "size", "create_time", "update_time", "md5"]))
}
fn write_file_information(table: PrintTable, information: &FileInformation) -> PrintTable {
    let index = information.path().rfind('/').unwrap() + 1;
    table.add_body(from_slice(&vec![&information.path()[index..], &information.is_dir().to_string(), &information.size().to_string(),
                                match information.create_time() { Some(t) => t, None => "Unknown"},
                                match information.update_time() { Some(t) => t, None => "Unknown"},
                                information.md5()]))
}
fn handle_file_reason(result: &Result<FileInformation, FailureReason>) {
    match result {
        Ok(information) => {
            println!("Success!");
            write_file_information(write_file_print_table(), information).print();
        }
        Err(reason) => {
            println!("{}", reason);
        }
    }
}

fn read_local_file_path(line: &String) -> &str {
    if line.len() > 2 && line.as_bytes()[0] == b'"' && line.as_bytes()[line.len() - 1] == b'"' { &line[1..line.len() - 1] } else { line }
}
fn get_chunk_len(i: u32, count: u32, size: u64) -> usize {
    if i == count - 1 {
        let m = (size % FILE_TRANSFER_BUFFER_SIZE as u64) as usize;
        if m == 0 { FILE_TRANSFER_BUFFER_SIZE } else { m }
    } else {
        FILE_TRANSFER_BUFFER_SIZE
    }
}

fn console_download_directly(client: &mut WListClient, t: &Option<(String, String)>, address: &str) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Downloading file...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter web file path: "); stdout().flush()?;
    let web = read_line()?;
    print!("Please enter local file path (or drag in): "); stdout().flush()?;
    let local = read_line()?;
    let local = match OpenOptions::new().read(true).write(true).create(true).open(read_local_file_path(&local)) {
        Ok(f) => f, Err(e) => { println!("Failed to create writable local file. {}", e); return Ok(Ok(0)) }
    };
    let (size, id) = match match file_handler::request_download_file(client, token, &web, 0, i64::max_value() as u64)? {
        Ok(t) => t, Err(e) => return Ok(Err(e)),
    } {
        Some(i) => i, None => { println!("No such file."); return Ok(Ok(0)) }
    };
    let count = (size as f32 / FILE_TRANSFER_BUFFER_SIZE as f32).ceil() as u32;
    let _guard = local.try_clone()?;
    let _guard = FileLock::lock_exclusive(&_guard)?;
    local.set_len(size)?;
    let address = Arc::new(address.to_string());
    let token = Arc::new(token.clone());
    let id = Arc::new(id);
    let local = Arc::new(local);
    let web = Arc::new(web);
    let index = Arc::new(Mutex::new(AtomicU32::new(0)));
    let remaining = Arc::new(Mutex::new(AtomicU64::new(size)));
    let (sender, receiver) = sync_channel(count as usize);
    let mut futures = Vec::new();
    for c in 0..min(count as usize, GlobalConfiguration::get().thread_count) {
        let address = address.clone();
        let token = token.clone();
        let id = id.clone();
        let local = local.clone();
        let web = web.clone();
        let index = index.clone();
        let remaining = remaining.clone();
        let sender = sender.clone();
        futures.push(thread::Builder::new().name("Downloader-".to_string() + &c.to_string()).spawn(move || {
            let mut client = WListClient::new(&address).unwrap();
            loop {
                let i = index.lock().unwrap().fetch_add(1, Ordering::AcqRel);
                if i >= count {
                    break
                }
                let mut times = 0;
                while times < 3 {
                    times += 1;
                    let mut reader = match match match file_handler::download_file(&mut client, &token, &id, i) {
                        Ok(t) => t,
                        Err(e) => {
                            error!("Downloading file. ({:?} from '{}'). retry time: {}. Network error. {}", local, web, times, e);
                            continue
                        }
                    } {
                        Ok(t) => t,
                        Err(e) => {
                            sender.send(e).unwrap();
                            break
                        }
                    } {
                        Some(r) => r,
                        None => {
                            error!("Invalid download id. Unknown reason. chunk id: {}", i);
                            break
                        }
                    };
                    let len = get_chunk_len(i, count, size);
                    assert_eq!(reader.readable(), len);
                    let mut mmap = unsafe {
                        MmapOptions::new()
                            .offset(i as u64 * FILE_TRANSFER_BUFFER_SIZE as u64).len(len)
                            .map_mut(&local).unwrap()
                    };
                    reader.read_exact(mmap.as_mut()).unwrap();
                    remaining.lock().unwrap().fetch_sub(len as u64, Ordering::AcqRel);
                    break
                }
            }
        })?);
    }
    for future in futures {
        future.join().unwrap();
    }
    let remaining = remaining.lock().unwrap().load(Ordering::Acquire);
    if remaining != 0 {
        error!("Downloading file ({:?} from '{}'). Unexpected remaining: {}. Unknown reason.", local, web, remaining);
    }
    if let Ok(e) = receiver.try_recv() {
        return Ok(Err(e));
    }
    println!("Success!");
    Ok(Ok(0))
}
fn console_upload_directly(client: &mut WListClient, t: &Option<(String, String)>, duplicate_policy_table: &PrintTableCached, address: &str) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Uploading file...");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    print!("Please enter local file path (or drag in): "); stdout().flush()?;
    let local = read_line()?;
    let mut local = match OpenOptions::new().read(true).write(false).open(read_local_file_path(&local)) {
        Ok(f) => f, Err(e) => { println!("No such local file. {}", e); return Ok(Ok(0)) }
    };
    print!("Please enter web file path: "); stdout().flush()?;
    let web = read_line()?;
    let duplicate_policy = read_duplicate_policy(duplicate_policy_table)?;
    let _guard = local.try_clone()?;
    let _guard = FileLock::lock_shared(&_guard)?;
    println!("Calculating file md5...");
    let mut md5 = Context::new();
    copy(&mut local, &mut md5)?;
    let size = local.stream_position()?;
    let md5 = format!("{:x}", md5.compute());
    println!("File size: {}, md5: {}", size, md5);
    let id = match match match file_handler::request_upload_file(client, token, &web, size, &md5, &duplicate_policy)? {
        Ok(t) => t, Err(e) => return Ok(Err(e)),
    } {
        Ok(t) => t, Err(e) => { println!("{}", e); return Ok(Ok(0)) },
    } {
        Ok(information) => {
            println!("Success! (reuse)");
            write_file_information(write_file_print_table(), &information).print();
            return Ok(Ok(0))
        }, Err(i) => i,
    };
    let count = (size as f32 / FILE_TRANSFER_BUFFER_SIZE as f32).ceil() as u32;
    let address = Arc::new(address.to_string());
    let token = Arc::new(token.clone());
    let id = Arc::new(id);
    let local = Arc::new(local);
    let web = Arc::new(web);
    let index = Arc::new(Mutex::new(AtomicU32::new(0)));
    let remaining = Arc::new(Mutex::new(AtomicU64::new(size)));
    let (sender, receiver) = sync_channel(count as usize);
    let mut futures = Vec::new();
    for c in 0..min(count as usize, GlobalConfiguration::get().thread_count) {
        let address = address.clone();
        let token = token.clone();
        let id = id.clone();
        let local = local.clone();
        let web = web.clone();
        let index = index.clone();
        let remaining = remaining.clone();
        let sender = sender.clone();
        futures.push(thread::Builder::new().name("Uploader-".to_string() + &c.to_string()).spawn(move || {
            let mut client = WListClient::new(&address).unwrap();
            loop {
                let i = index.lock().unwrap().fetch_add(1, Ordering::AcqRel);
                if i >= count {
                    break
                }
                let mut times = 0;
                while times < 3 {
                    times += 1;
                    debug!("{} {}: Uploading chunk {}. ",
                        Local::now().format("%.9f"),
                        thread::current().name().unwrap_or("Unknown"),
                        i);
                    let len = get_chunk_len(i, count, size);
                    let mut buffer = Vec::new();
                    let mmap = unsafe {
                        MmapOptions::new()
                            .offset(i as u64 * FILE_TRANSFER_BUFFER_SIZE as u64).len(len)
                            .map(&local).unwrap()
                    };
                    buffer.extend_from_slice(&mmap);
                    let info = match match match file_handler::upload_file(&mut client, &token, &id, i, Box::new(VecU8Reader::new(buffer))) {
                        Ok(t) => t,
                        Err(e) => {
                            error!("Uploading file. ({:?} to '{}'). retry time: {}. Network error. {}", local, web, times, e);
                            continue
                        }
                    } {
                        Ok(t) => t,
                        Err(e) => {
                            sender.send(e).unwrap();
                            break
                        }
                    } {
                        Some(r) => r,
                        None => {
                            error!("Invalid upload id. Unknown reason. chunk id: {}", i);
                            break
                        }
                    };
                    remaining.lock().unwrap().fetch_sub(len as u64, Ordering::AcqRel);
                    match info {
                        Ok(information) => {
                            println!("Success!");
                            write_file_information(write_file_print_table(), &information).print();
                        }
                        Err(true) => (),
                        Err(false) => {
                            error!("Mismatching file content. Unknown reason.");
                        },
                    };
                    break
                }
            }
        })?);
    }
    for future in futures {
        future.join().unwrap();
    }
    let remaining = remaining.lock().unwrap().load(Ordering::Acquire);
    if remaining != 0 {
        error!("Uploading file ({:?} to '{}'). Unexpected remaining {}. Unknown reason.", local, web, remaining);
    }
    if let Ok(e) = receiver.try_recv() {
        return Ok(Err(e));
    }
    Ok(Ok(0))
}
