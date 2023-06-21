use std::collections::BTreeMap;
use std::fmt::{Display, Formatter};
use std::fs::File;
use std::io;
use std::io::ErrorKind;
use std::mem::MaybeUninit;
use std::path::Path;
use std::str::FromStr;
use std::sync::RwLock;

pub struct GlobalConfiguration {
    pub host: String,
    pub port: u32,
    pub limit: u32,
    pub thread_count: usize,
}

impl Display for GlobalConfiguration {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "GlobalConfiguration(host='{}', port={}, limit={}, thread_count={})",
               self.host, self.port, self.limit, self.thread_count)
    }
}


static mut INSTANCE: MaybeUninit<GlobalConfiguration> = MaybeUninit::uninit();
static LOCK: RwLock<bool> = RwLock::new(false);

impl GlobalConfiguration {
    pub fn init(file: &Path) -> Result<(), io::Error> {
        let mut guard = LOCK.write().unwrap();
        if *guard {
            panic!("Global configuration is initialized.");
        }
        *guard = true;
        let mut configuration = GlobalConfiguration {
            host: String::from("localhost"),
            port: 5212,
            limit: 20,
            thread_count: 4,
        };
        if file.is_file() {
            let map: BTreeMap<String, String> = match serde_yaml::from_reader(File::open(file)?) {
                Ok(m) => m,
                Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, e.to_string())),
            };
            configuration.host = match map.get("host") {
                Some(v) => v.clone(),
                None => configuration.host,
            };
            configuration.port = match map.get("port") {
                Some(v) => match u32::from_str(v) {
                    Ok(v) if v < 1 => return Err(io::Error::new(ErrorKind::InvalidData, format!("Getting 'port'. Exceed minimum limit. {} < {}", v, 1))),
                    Ok(v) if v > 65535 => return Err(io::Error::new(ErrorKind::InvalidData, format!("Getting 'port'. Exceed maximum limit. {} > {}", v, 65535))),
                    Ok(v) => v,
                    Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Getting 'port'. {}", e))),
                },
                None => configuration.port,
            };
            configuration.limit = match map.get("limit") {
                Some(v) => match u32::from_str(v) {
                    Ok(v) if v < 1 => return Err(io::Error::new(ErrorKind::InvalidData, format!("Getting 'limit'. Exceed minimum limit. {} < {}", v, 1))),
                    Ok(v) if v > 200 => return Err(io::Error::new(ErrorKind::InvalidData, format!("Getting 'limit'. Exceed maximum limit. {} > {}", v, 200))),
                    Ok(v) => v,
                    Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Getting 'limit'. {}", e))),
                },
                None => configuration.limit,
            };
            configuration.thread_count = match map.get("thread_count") {
                Some(v) => match usize::from_str(v) {
                    Ok(v) => v,
                    Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, format!("Getting 'thread_count'. {}", e))),
                },
                None => configuration.thread_count,
            };
        } else {
            let mut map: BTreeMap<String, String> = BTreeMap::new();
            map.insert(String::from("host"), configuration.host.to_string());
            map.insert(String::from("port"), configuration.port.to_string());
            map.insert(String::from("limit"), configuration.limit.to_string());
            map.insert(String::from("thread_count"), configuration.thread_count.to_string());
            match serde_yaml::to_writer(File::create(file)?, &map) {
                Ok(_) => (),
                Err(e) => return Err(io::Error::new(ErrorKind::InvalidData, e.to_string())),
            };
        }
        unsafe {
            INSTANCE.write(configuration);
        }
        Ok(())
    }

    pub fn get() -> &'static GlobalConfiguration {
        let guard = LOCK.read().unwrap();
        if !*guard {
            panic!("Global configuration is not initialized.");
        }
        unsafe {
            INSTANCE.assume_init_ref()
        }
    }
}
