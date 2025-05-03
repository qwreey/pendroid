use super::ActionType;

pub enum FieldType {
    UInt8,
    Int8,
    UInt16,
    Int32,
    UInt32,
}
impl FieldType {
    const fn get_len(&self) -> usize {
        match self {
            FieldType::Int8 => 1,
            FieldType::UInt8 => 1,
            FieldType::UInt16 => 2,
            FieldType::Int32 => 4,
            FieldType::UInt32 => 4,
        }
    }
}

pub struct FieldParser {
    fields: Vec<FieldType>,
    len: usize,
    id: u8,
}

impl FieldParser {
    pub fn new(id: u8, fields: Vec<FieldType>) -> FieldParser {
        let mut len = 2;

        for item in &fields {
            len += item.get_len();
        }

        FieldParser { fields, len, id }
    }
    pub fn get_id(&self) -> u8 {
        self.id
    }
    pub fn get_len(&self) -> usize {
        self.len
    }
}

pub trait ActionReader {
    fn get_field_parser(&self) -> &FieldParser;
    fn get_id(&self) -> u8 {
        self.get_field_parser().get_id()
    }
    fn get_len(&self, _buf: &[u8]) -> usize {
        self.get_field_parser().get_len()
    }
    fn can_read(&self, buf: &[u8]) -> bool {
        buf.len() >= self.get_len(buf) && *buf.get(1).unwrap() == self.get_id()
    }
    fn read(&self, buf: &[u8]) -> ActionType;
}
