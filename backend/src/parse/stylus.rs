use bytebuffer::ByteBuffer;

use super::{ActionReader, ActionType, FieldParser, FieldType};

#[derive(Debug)]
pub struct StylusData {
    pub x: i32,
    pub y: i32,
    pub tilt_x: i32,
    pub tilt_y: i32,
    pub pressure: i32,
    pub down: bool,
    pub hover: bool,
    pub button: bool,
    pub timestamp: i32,
}

pub struct StylusReader {
    reader: FieldParser,
}
impl StylusReader {
    pub fn new() -> StylusReader {
        StylusReader {
            reader: FieldParser::new(
                132,
                vec![
                    FieldType::UInt8,
                    FieldType::UInt16,
                    FieldType::UInt16,
                    FieldType::Int8,
                    FieldType::Int8,
                    FieldType::UInt16,
                    FieldType::Int32,
                ],
            ),
        }
    }
}
impl ActionReader for StylusReader {
    fn get_field_parser(&self) -> &FieldParser {
        &self.reader
    }
    fn read(&self, buf: &[u8]) -> ActionType {
        let mut bytes = ByteBuffer::from_bytes(buf);

        bytes.read_u8().unwrap();
        bytes.read_u8().unwrap();

        let flags = bytes.read_u8().unwrap();
        let x = bytes.read_u16().unwrap() as i32;
        let y = bytes.read_u16().unwrap() as i32;
        let tilt_x = bytes.read_i8().unwrap() as i32;
        let tilt_y = bytes.read_i8().unwrap() as i32;
        let pressure = bytes.read_u16().unwrap() as i32;
        let timestamp = bytes.read_i32().unwrap();

        ActionType::Stylus(StylusData {
            hover: (flags & 0b0001) != 0,
            down: (flags & 0b0010) != 0,
            button: (flags & 0b0100) != 0,
            x,
            y,
            tilt_x,
            tilt_y,
            pressure,
            timestamp,
        })
    }
}
