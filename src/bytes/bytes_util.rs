use std::io;
use std::io::{ErrorKind, Read, Write};

macro_rules! primitive_util {
    ($primitive: ident, $read: ident, $read_be: ident, $write: ident, $write_be: ident, $length: literal) => {
        pub fn $read(source: &mut impl Read) -> Result<$primitive, io::Error> {
            let mut bytes = [0;$length];
            source.read_exact(&mut bytes)?;
            Ok($primitive::from_le_bytes(bytes))
        }
        pub fn $read_be(source: &mut impl Read) -> Result<$primitive, io::Error> {
            let mut bytes = [0;$length];
            source.read_exact(&mut bytes)?;
            Ok($primitive::from_be_bytes(bytes))
        }
        pub fn $write(target: &mut impl Write, message: $primitive) -> Result<usize, io::Error> {
            target.write(&$primitive::to_le_bytes(message))
        }
        pub fn $write_be(target: &mut impl Write, message: $primitive) -> Result<usize, io::Error> {
            target.write(&$primitive::to_be_bytes(message))
        }
    };
}
primitive_util!(u8, read_u8, read_u8_be, write_u8, write_u8_be, 1);
primitive_util!(i8, read_i8, read_i8_be, write_i8, write_i8_be, 1);
primitive_util!(u16, read_u16, read_u16_be, write_u16, write_u16_be, 2);
primitive_util!(i16, read_i16, read_i16_be, write_i16, write_i16_be, 2);
primitive_util!(u32, read_u32, read_u32_be, write_u32, write_u32_be, 4);
primitive_util!(i32, read_i32, read_i32_be, write_i32, write_i32_be, 4);
primitive_util!(u64, read_u64, read_u64_be, write_u64, write_u64_be, 8);
primitive_util!(i64, read_i64, read_i64_be, write_i64, write_i64_be, 8);
primitive_util!(u128, read_u128, read_u128_be, write_u128, write_u64_128, 16);
primitive_util!(i128, read_i128, read_i128_be, write_i128, write_i64_128, 16);

pub fn read_bool(source: &mut impl Read) -> Result<bool, io::Error> {
    Ok(read_u8(source)? != 0)
}
pub fn read_bool_be(source: &mut impl Read) -> Result<bool, io::Error> {
    Ok(read_u8_be(source)? != 0)
}
pub fn write_bool(target: &mut impl Write, message: bool) -> Result<usize, io::Error> {
    write_u8(target, if message { 1 } else { 0 })
}
pub fn write_bool_be(target: &mut impl Write, message: bool) -> Result<usize, io::Error> {
    write_u8_be(target, if message { 1 } else { 0 })
}


fn create_length_error(name: &str) -> io::Error {
    io::Error::new(ErrorKind::InvalidData, format!("Variable {} in stream is too long.", name))
}

macro_rules! variable_len_util {
    ($name: ident, $read_variable: ident, $write_variable: ident, $length: literal, $cause: literal) => {
        pub fn $read_variable(source: &mut impl Read) -> Result<$name, io::Error> {
            let mut value = 0;
            let mut position = 0;
            loop {
                let current = read_u8(source)?;
                value |= ((current & 0x7f) as $name) << position;
                if current & 0x80 == 0 {
                    break;
                }
                position += 7;
                if position >= $length {
                    return Err(create_length_error($cause));
                }
            }
            Ok(value)
        }
        pub fn $write_variable(target: &mut impl Write, message: $name) -> Result<usize, io::Error> {
            let mut size = 0;
            let mut value = message;
            while value >> 7 > 0 {
                size += write_u8(target, ((value & 0x7f) as u8) | 0x80)?;
                value >>= 7;
            }
            size += write_u8(target, (value & 0x7f) as u8)?;
            Ok(size)
        }
    };
}
variable_len_util!(u16, read_variable_u16, write_variable_u16, 16, "u16");
variable_len_util!(u32, read_variable_u32, write_variable_u32, 32, "u32");
variable_len_util!(u64, read_variable_u64, write_variable_u64, 64, "u64");
variable_len_util!(u128, read_variable_u128, write_variable_u128, 128, "u128");

macro_rules! variable_len_2_util {
    ($name: ident, $read_variable: ident, $write_variable: ident, $length: literal, $cause: literal) => {
        pub fn $read_variable(source: &mut impl Read) -> Result<$name, io::Error> {
            let mut value = 0;
            let mut position = 0;
            loop {
                let current = read_u16(source)?;
                value |= ((current & 0x7fff) as $name) << position;
                if current & 0x8000 == 0 {
                    break;
                }
                position += 15;
                if position >= $length {
                    return Err(create_length_error($cause));
                }
            }
            Ok(value)
        }
        pub fn $write_variable(target: &mut impl Write, message: $name) -> Result<usize, io::Error> {
            let mut size = 0;
            let mut value = message;
            while value >> 15 > 0 {
                size += write_u16(target, ((value & 0x7fff) as u16) | 0x8000)?;
                value >>= 15;
            }
            size += write_u16(target, (value & 0x7fff) as u16)?;
            Ok(size)
        }
    };
}
variable_len_2_util!(u32, read_variable2_u32, write_variable2_u32, 32, "2 u32");
variable_len_2_util!(u64, read_variable2_u64, write_variable2_u64, 64, "2 u64");
variable_len_2_util!(u128, read_variable2_u128, write_variable2_u128, 128, "2 u128");


pub fn read_u8_vec(source: &mut impl Read) -> Result<Vec<u8>, io::Error> {
    let length = read_variable_u32(source)? as usize;
    let mut bytes = vec![0; length];
    source.read_exact(&mut bytes)?;
    Ok(bytes)
}
pub fn write_u8_vec(target: &mut impl Write, message: &Vec<u8>) -> Result<usize, io::Error> {
    let mut size = write_variable_u32(target, message.len() as u32)?;
    size += target.write(message)?;
    Ok(size)
}
pub fn write_u8_array(target: &mut impl Write, message: &[u8]) -> Result<usize, io::Error> {
    let mut size = write_variable_u32(target, message.len() as u32)?;
    size += target.write(message)?;
    Ok(size)
}

pub fn read_string(source: &mut impl Read) -> Result<String, io::Error> {
    match String::from_utf8(read_u8_vec(source)?) {
        Ok(s) => Ok(s),
        Err(e) => Err(io::Error::new(ErrorKind::InvalidData, e.to_string())),
    }
}
pub fn write_string(target: &mut impl Write, message: &String) -> Result<usize, io::Error> {
    write_u8_array(target, message.as_bytes())
}
