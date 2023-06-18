#![allow(non_snake_case)]

pub mod print_table;
mod global_configuration;

use std::env::set_var;
use std::io;
use std::io::{stdin, stdout, Write};
use std::path::Path;

use log::{debug, info, trace, warn};
use wlist_client_library::handlers::server_handler;
use wlist_client_library::handlers::user_handler;
use wlist_client_library::network::client::WListClient;
use wlist_client_library::operations::permissions::Permission;
use wlist_client_library::operations::states::State;
use wlist_client_library::operations::wrong_state_error::WrongStateError;
use crate::global_configuration::GlobalConfiguration;
use crate::print_table::PrintTable;

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
fn enter_to_confirm() -> Result<bool, io::Error> {
    print!("Enter to confirm: "); stdout().flush()?;
    let mut str = String::new();
    stdin().read_line(&mut str)?;
    Ok(!str.trim().is_empty())
}

fn main() -> Result<(), io::Error> {
    set_var("RUST_LOG", "debug, wlist_client_library=debug");
    env_logger::init();
    info!("Hello WList Client (Console Version)! Initializing...");
    let configuration = Path::new("client.yaml");
    debug!("Initializing global configuration. file: {:?}", configuration);
    GlobalConfiguration::init(configuration)?;
    trace!("Initialized global configuration.");
    let address = String::from(&GlobalConfiguration::get().host) + ":" + &GlobalConfiguration::get().port.to_string();
    debug!("Connecting to WList Server (address: {}) ...", address);
    let mut client = vec![WListClient::new(&address)?];
    trace!("Initialized WList clients.");
    let menu = PrintTable::createFromSlice(vec!["ID", "Operation", "Permission", "Detail"])
        .addBodyFromSlice(vec!["0", "Exit", "any", "Exit client directly."])
        .addBodyFromSlice(vec!["1", "Close server", "admin", "Try close server, and then exit client."])
        .addBodyFromSlice(vec!["10", "Register", "any", "Register a new user."])
        .addBodyFromSlice(vec!["11", "Login", "any", "Login with username and password."])
        .addBodyFromSlice(vec!["12", "Get permissions", "user", "Display the current user group."])
        .addBodyFromSlice(vec!["13", "Change username", "user", "Change username. Don't need login again."])
        .addBodyFromSlice(vec!["14", "Change password", "user", "Change password after verifying the old one. All accounts need to be logged in again."])
        .addBodyFromSlice(vec!["15", "Logoff", "user", "Logoff after verifying old password. WARNING: Please operate with caution."])
        .addBodyFromSlice(vec!["20", "List users", "admin", "Get registered users list."])
        .addBodyFromSlice(vec!["21", "Delete user", "admin", "Delete any of registered user."])
        .addBodyFromSlice(vec!["22", "List groups", "admin", "Get user groups list."])
        .addBodyFromSlice(vec!["23", "Add group", "admin", "Add a new empty user group."])
        .addBodyFromSlice(vec!["24", "Delete group", "admin", "Delete a user group without users in it."])
        .addBodyFromSlice(vec!["25", "Change group", "admin", "Move the user to the specified user group."])
        .addBodyFromSlice(vec!["26", "Add permissions", "admin", "Add permissions to user group."])
        .addBodyFromSlice(vec!["27", "Remove permissions", "admin", "Remove permissions from user group."])
        .addBodyFromSlice(vec!["40", "List files", "user", "Get files list in explicit directory."])
        .addBodyFromSlice(vec!["41", "Make directory", "user", "Making new directories recursively."])
        .addBodyFromSlice(vec!["42", "Delete file", "user", "Delete a file or directory recursively."])
        .addBodyFromSlice(vec!["43", "Rename file", "user", "Rename a file or directory."])
        .addBodyFromSlice(vec!["44", "Copy file", "user", "Copy a file to another path."])
        .addBodyFromSlice(vec!["45", "Move file", "user", "Move a file or a directory to another directory."])
        .addBodyFromSlice(vec!["46", "Download file", "user", "Download file to the local path."])
        .addBodyFromSlice(vec!["47", "Upload file", "user", "Upload local file to the path."])
        .finish();
    let mut token: Option<(String, String)> = None;
    loop {
        println!("Current login status: {}", match &token {
            Some(t) => String::from("true") + "  username: '" + t.1.as_str() + "'",
            None => String::from("false"),
        });
        menu.print();
        print!("Please enter operation id: ");
        stdout().flush()?;
        match match match read_line()?.parse::<u32>() {
            Ok(i) => i,
            Err(e) => {
                println!("Invalid number format. {} \nEnter to continue...", e);
                read_line()?;
                continue;
            }
        } {
            0 => console_exit()?,
            1 => console_close_server(&mut client[0], &token)?,
            10 => console_register(&mut client[0])?,
            11 => console_login(&mut client[0], &mut token)?,
            12 => console_get_permissions(&mut client[0], &token)?,
            13 => console_change_username(&mut client[0], &mut token)?,
            14 => console_change_password(&mut client[0], &mut token)?,
            15 => console_logoff(&mut client[0], &mut token)?,

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
    warn!("Thanks to use WList Client (Console Version).");
    Ok(())
}

fn console_exit() -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Exiting client... WARNING!");
    Ok(if enter_to_confirm()? { Ok(0) } else { Ok(2) })
}
fn console_close_server(client: &mut WListClient, t: &Option<(String, String)>) -> Result<Result<u8, WrongStateError>, io::Error> {
    println!("Closing server... WARNING!");
    let token = match t { Some(p) => &p.0, None => return Ok(Ok(1)) };
    if enter_to_confirm()? { return Ok(Ok(0)); }
    if match server_handler::close_server(client, token)? { Ok(s) => s, Err(e) => return Ok(Err(e)) } {
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
    if match user_handler::register(client, &username, &password)? { Ok(s) => s, Err(e) => return Ok(Err(e)) } {
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
            PrintTable::create(name).addBodyFromSlice(value).print();
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
        println!("Expired token or denied operation.");
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
    if match user_handler::logoff(client, token, &password)? { Ok(t) => t, Err(e) => return Ok(Err(e)) } {
        println!("Success!");
        *t = None;
    } else {
        println!("Expired token.");
    }
    Ok(Ok(0))
}
