use std::io;
use std::io::{ErrorKind, Read, Write};

pub fn check_index(len: usize, offset: usize, require: usize) -> Result<(), io::Error> {
    if len < offset + require {
        Err(io::Error::new(ErrorKind::UnexpectedEof, format!("Array len ({}) < offset ({}) + require ({}).", len, offset, require)))
    } else {
        Ok(())
    }
}


pub fn read_u8(source: &mut impl Read) -> Result<u8, io::Error> {
    let mut bytes = [0; 1];
    source.read_exact(&mut bytes)?;
    Ok(bytes[0])
}
pub fn write_u8(target: &mut impl Write, message: u8) -> Result<usize, io::Error> {
    target.write(&[message])
}
pub fn read_u8_buf(buffer: &[u8], offset: &mut usize) -> Result<u8, io::Error> {
    check_index(buffer.len(), *offset, 1)?;
    let byte = buffer[*offset];
    *offset += 1;
    Ok(byte)
}
pub fn write_u8_buf(message: u8) -> Vec<u8> {
    Vec::from([message])
}

pub fn read_bool(source: &mut impl Read) -> Result<bool, io::Error> {
    Ok(read_u8(source)? != 0)
}
pub fn write_bool(target: &mut impl Write, message: bool) -> Result<usize, io::Error> {
    write_u8(target, if message { 1 } else { 0 })
}
pub fn read_bool_buf(buffer: &[u8], offset: &mut usize) -> Result<bool, io::Error> {
    Ok(read_u8_buf(buffer, offset)? != 0)
}
pub fn write_bool_buf(message: bool) -> Vec<u8> {
    if message { vec![1] } else { vec![0] }
}

macro_rules! primitive_util {
    ($primitive: ident, $length: literal, $read: ident, $read_be: ident, $write: ident, $write_be: ident,
            $read_buf: ident, $read_be_buf: ident, $write_buf: ident, $write_be_buf: ident) => {
        pub fn $read(source: &mut impl Read) -> Result<$primitive, io::Error> {
            let mut bytes = [0; $length];
            source.read_exact(&mut bytes)?;
            Ok($primitive::from_le_bytes(bytes))
        }
        pub fn $read_be(source: &mut impl Read) -> Result<$primitive, io::Error> {
            let mut bytes = [0; $length];
            source.read_exact(&mut bytes)?;
            Ok($primitive::from_be_bytes(bytes))
        }
        pub fn $write(target: &mut impl Write, message: $primitive) -> Result<usize, io::Error> {
            target.write(&$primitive::to_le_bytes(message))
        }
        pub fn $write_be(target: &mut impl Write, message: $primitive) -> Result<usize, io::Error> {
            target.write(&$primitive::to_be_bytes(message))
        }

        pub fn $read_buf(buffer: &[u8], offset: &mut usize) -> Result<$primitive, io::Error> {
            check_index(buffer.len(), *offset, $length)?;
            let mut bytes = [0; $length];
            for i in 0..$length {
                bytes[i] = buffer[*offset + i];
            }
            *offset += $length;
            Ok($primitive::from_le_bytes(bytes))
        }
        pub fn $read_be_buf(buffer: &[u8], offset: &mut usize) -> Result<$primitive, io::Error> {
            check_index(buffer.len(), *offset, $length)?;
            let mut bytes = [0; $length];
            for i in 0..$length {
                bytes[i] = buffer[*offset + i];
            }
            *offset += $length;
            Ok($primitive::from_be_bytes(bytes))
        }
        pub fn $write_buf(message: $primitive) -> Vec<u8> {
            Vec::from($primitive::to_le_bytes(message))
        }
        pub fn $write_be_buf(message: $primitive) -> Vec<u8> {
            Vec::from($primitive::to_be_bytes(message))
        }
    };
}
primitive_util!(i8, 1, read_i8, read_i8_be, write_i8, write_i8_be, read_i8_buf, read_i8_be_buf, write_i8_buf, write_i8_be_buf);
primitive_util!(u16, 2, read_u16, read_u16_be, write_u16, write_u16_be, read_u16_buf, read_u16_be_buf, write_u16_buf, write_u16_be_buf);
primitive_util!(i16, 2, read_i16, read_i16_be, write_i16, write_i16_be, read_i16_buf, read_i16_be_buf, write_i16_buf, write_i16_be_buf);
primitive_util!(u32, 4, read_u32, read_u32_be, write_u32, write_u32_be, read_u32_buf, read_u32_be_buf, write_u32_buf, write_u32_be_buf);
primitive_util!(i32, 4, read_i32, read_i32_be, write_i32, write_i32_be, read_i32_buf, read_i32_be_buf, write_i32_buf, write_i32_be_buf);
primitive_util!(u64, 8, read_u64, read_u64_be, write_u64, write_u64_be, read_u64_buf, read_u64_be_buf, write_u64_buf, write_u64_be_buf);
primitive_util!(i64, 8, read_i64, read_i64_be, write_i64, write_i64_be, read_i64_buf, read_i64_be_buf, write_i64_buf, write_i64_be_buf);
primitive_util!(u128, 16, read_u128, read_u128_be, write_u128, write_u128_be, read_u128_buf, read_u128_be_buf, write_u128_buf, write_u128_be_buf);
primitive_util!(i128, 16, read_i128, read_i128_be, write_i128, write_i128_be, read_i128_buf, read_i128_be_buf, write_i128_buf, write_i128_be_buf);


macro_rules! variable_len_util {
    ($primitive: ident, $length: literal, $read_variable: ident, $write_variable: ident,
            $read_variable_buf: ident, $write_variable_buf: ident, $cause: literal,
            $read: ident, $write: ident, $read_buf: ident, $write_buf: ident, $inside_type: ident,
            $num_bits: literal, $next_bit: literal, $offset_position: literal) => {
        pub fn $read_variable(source: &mut impl Read) -> Result<$primitive, io::Error> {
            let mut value = 0;
            let mut position = 0;
            loop {
                let current = $read(source)?;
                value |= ((current & $num_bits) as $primitive) << position;
                if current & $next_bit == 0 {
                    break;
                }
                position += $offset_position;
                if position >= $length {
                    return Err(io::Error::new(ErrorKind::InvalidData, format!("Variable {} in stream is too long.", $cause)));
                }
            }
            Ok(value)
        }
        pub fn $write_variable(target: &mut impl Write, message: $primitive) -> Result<usize, io::Error> {
            let mut size = 0;
            let mut value = message;
            while value >> $offset_position > 0 {
                size += $write(target, ((value & $num_bits) as $inside_type) | $next_bit)?;
                value >>= $offset_position;
            }
            size += $write(target, (value & $num_bits) as $inside_type)?;
            Ok(size)
        }
        
        pub fn $read_variable_buf(buffer: &[u8], offset: &mut usize) -> Result<$primitive, io::Error> {
            let mut value = 0;
            let mut position = 0;
            let mut length = 0;
            loop {
                let current = $read_buf(&buffer[*offset..], &mut length)?;
                value |= ((current & $num_bits) as $primitive) << position;
                if current & $next_bit == 0 {
                    break;
                }
                position += $offset_position;
                if position >= $length {
                    return Err(io::Error::new(ErrorKind::InvalidData, format!("Variable {} in stream is too long.", $cause)));
                }
            }
            *offset += length;
            Ok(value)
        }
        pub fn $write_variable_buf(message: $primitive) -> Vec<u8> {
            let mut bytes = Vec::new();
            $write_variable(&mut bytes, message).unwrap();
            bytes
        }
    };
}
variable_len_util!(u16, 16, read_variable_u16, write_variable_u16,read_variable_u16_buf, write_variable_u16_buf, "u16",
    read_u8, write_u8, read_u8_buf, write_u8_buf, u8, 0x7f, 0x80, 7);
variable_len_util!(u32, 32, read_variable_u32, write_variable_u32,read_variable_u32_buf, write_variable_u32_buf, "u32",
    read_u8, write_u8, read_u8_buf, write_u8_buf, u8, 0x7f, 0x80, 7);
variable_len_util!(u32, 32, read_variable2_u32, write_variable2_u32, read_variable2_u32_buf, write_variable2_u32_buf, "2 u32",
    read_u16, write_u16, read_u16_buf, write_u16_buf, u16, 0x7fff, 0x8000, 15);
variable_len_util!(u32, 32, read_variable2_u32_be, write_variable2_u32_be, read_variable2_u32_be_buf, write_variable2_u32_be_buf, "2 u32",
    read_u16_be, write_u16_be, read_u16_be_buf, write_u16_be_buf, u16, 0x7fff, 0x8000, 15);
variable_len_util!(u64, 64, read_variable_u64, write_variable_u64, read_variable_u64_buf, write_variable_u64_buf, "u64",
    read_u8, write_u8, read_u8_buf, write_u8_buf, u8, 0x7f, 0x80, 7);
variable_len_util!(u64, 64, read_variable2_u64, write_variable2_u64, read_variable2_u64_buf, write_variable2_u64_buf, "2 u64",
    read_u16, write_u16, read_u16_buf, write_u16_buf, u16, 0x7fff, 0x8000, 15);
variable_len_util!(u64, 64, read_variable2_u64_be, write_variable2_u64_be, read_variable2_u64_be_buf, write_variable2_u64_be_buf, "2 u64",
    read_u16_be, write_u16_be, read_u16_be_buf, write_u16_be_buf, u16, 0x7fff, 0x8000, 15);
variable_len_util!(u128, 128, read_variable_u128, write_variable_u128, read_variable_u128_buf, write_variable_u128_buf, "u128",
    read_u8, write_u8, read_u8_buf, write_u8_buf, u8, 0x7f, 0x80, 7);
variable_len_util!(u128, 128, read_variable2_u128, write_variable2_u128, read_variable2_u128_buf, write_variable2_u128_buf, "2 u128",
    read_u16, write_u16, read_u16_buf, write_u16_buf, u16, 0x7fff, 0x8000, 15);
variable_len_util!(u128, 128, read_variable2_u128_be, write_variable2_u128_be, read_variable2_u128_be_buf, write_variable2_u128_be_buf, "2 u128",
    read_u16_be, write_u16_be, read_u16_be_buf, write_u16_be_buf, u16, 0x7fff, 0x8000, 15);
variable_len_util!(u128, 128, read_variable4_u128, write_variable4_u128, read_variable4_u128_buf, write_variable4_u128_buf, "4 u128",
    read_u32, write_u32, read_u32_buf, write_u32_buf, u32, 0x7fffffff, 0x80000000, 31);
variable_len_util!(u128, 128, read_variable4_u128_be, write_variable4_u128_be, read_variable4_u128_be_buf, write_variable4_u128_be_buf, "4 u128",
    read_u32_be, write_u32_be, read_u32_be_buf, write_u32_be_buf, u32, 0x7fffffff, 0x80000000, 31);


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

pub fn read_string_nullable(source: &mut impl Read) -> Result<Option<String>, io::Error> {
    Ok(if read_bool(source)? { None } else { Some(read_string(source)?) })
}

pub fn write_string_nullable(target: &mut impl Write, message: &Option<String>) -> Result<usize, io::Error> {
    Ok(match message {
        Some(s) => write_bool(target, true)? + write_string(target, s)?,
        None => write_bool(target, false)?,
    })
}
