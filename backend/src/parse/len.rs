use bytebuffer::ByteBuffer;

use super::{ActionReader, ActionType, FieldParser, FieldType};

pub struct LenReader {
    reader: FieldParser,
}
impl LenReader {
    pub fn new() -> LenReader {
        LenReader {
            reader: FieldParser::new(114, vec![FieldType::UInt8]),
        }
    }
}
impl ActionReader for LenReader {
    fn get_field_parser(&self) -> &FieldParser {
        &self.reader
    }
    fn read(&self, buf: &[u8]) -> ActionType {
        let mut bytes = ByteBuffer::from_bytes(buf);
        bytes.read_u8().unwrap();
        bytes.read_u8().unwrap();
        ActionType::Len(bytes.read_u8().unwrap())
    }
}
