mod action_parser;
mod finger;
mod len;
mod stylus;
mod view;

use len::LenReader;
use stylus::StylusReader;

pub use self::{
    action_parser::{ActionReader, FieldParser, FieldType},
    finger::FingerData,
    stylus::StylusData,
    view::ViewData,
};

#[derive(Debug)]
pub enum ActionType {
    Stylus(StylusData),
    Screen(ViewData),
    Finger(FingerData),
    Len(u8),
}

pub struct ActionParser {
    stylus_reader: StylusReader,
    len_reader: LenReader,
}
impl ActionParser {
    pub fn new() -> ActionParser {
        ActionParser {
            stylus_reader: StylusReader::new(),
            len_reader: LenReader::new(),
        }
    }

    pub fn should_pop(&self, raw: u8) -> bool {
        raw != 0xFF
    }

    pub fn parse(&self, raw: &mut Vec<u8>) -> Option<ActionType> {
        if self.stylus_reader.can_read(raw) {
            let action = self.stylus_reader.read(raw);
            raw.drain(0..self.stylus_reader.get_len(raw));
            return Some(action);
        }
        if self.len_reader.can_read(raw) {
            let len = self.len_reader.read(raw);
            raw.drain(0..self.len_reader.get_len(raw));
            return Some(len);
        }
        None
    }
}
