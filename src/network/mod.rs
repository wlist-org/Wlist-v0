mod codecs;
pub mod client;

pub static FILE_TRANSFER_BUFFER_SIZE: usize = 4 << 20;
pub static MAX_SIZE_PER_PACKET: usize = (64 << 10) + FILE_TRANSFER_BUFFER_SIZE;

pub static DO_AES: u8 = 1;
pub static DEFAULT_DO_AES: u8 = DO_AES;
pub static DO_GZIP: u8 = 1 << 1;
pub static DEFAULT_DO_GZIP: u8 = DO_GZIP;
pub static DEFAULT_CIPHER: u8 = DEFAULT_DO_AES | DEFAULT_DO_GZIP;
