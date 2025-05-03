mod event_list;
mod finger;
mod stylus;
mod with_abs;

use super::super::parse::ActionType;
use finger::FingerBackend;
use stylus::StylusBackend;

pub struct InputBackend {
    stylus: StylusBackend,
    finger: FingerBackend,
}
impl InputBackend {
    pub fn new() -> Result<Self, String> {
        Ok(Self {
            stylus: StylusBackend::new()?,
            finger: FingerBackend::new()?,
        })
    }

    pub fn execute(&mut self, action: ActionType) -> Result<(), String> {
        match action {
            ActionType::Finger(finger_data) => self.finger.process(&finger_data),
            ActionType::Stylus(stylus_data) => self.stylus.process(&stylus_data),
            ActionType::Screen(_screen) => Ok(()),
            ActionType::Len(_len) => Ok(()),
        }
    }
}
