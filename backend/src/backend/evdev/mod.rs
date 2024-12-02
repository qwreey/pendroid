use evdev::{uinput::VirtualDeviceBuilder, EventType, InputEvent, Key, UinputAbsSetup};

use super::super::{
    parse::{action_parse, ActionType},
    utility::ErrToString,
};

mod finger;
mod stylus;

use finger::FingerBackend;
use stylus::StylusBackend;

trait WithAbs<'a> {
    fn with_abs(self, abs_list: &[UinputAbsSetup]) -> Result<VirtualDeviceBuilder<'a>, String>;
}
impl<'a> WithAbs<'a> for VirtualDeviceBuilder<'a> {
    fn with_abs(self, abs_list: &[UinputAbsSetup]) -> Result<VirtualDeviceBuilder<'a>, String> {
        let mut ret = self;
        for item in abs_list {
            ret = ret.with_absolute_axis(item).err_tostring()?;
        }
        Ok(ret)
    }
}

pub type EventList = Vec<InputEvent>;
pub trait PushEvent {
    fn push_abs_event(&mut self, code: u16, value: i32);
    // fn push_rel_event(&mut self, code: u16, value: i32);
    fn push_key(&mut self, code: &Key, value: i32);
}
impl PushEvent for EventList {
    #[inline]
    fn push_abs_event(&mut self, code: u16, value: i32) {
        self.push(InputEvent::new(EventType::ABSOLUTE, code, value));
    }
    #[inline]
    fn push_key(&mut self, code: &Key, value: i32) {
        self.push(InputEvent::new(EventType::KEY, code.code(), value));
    }
}
pub trait GetInputs {
    fn get_inputs(&mut self) -> &mut EventList;
}
impl<T> PushEvent for T
where
    T: GetInputs,
{
    fn push_abs_event(&mut self, code: u16, value: i32) {
        self.get_inputs().push_abs_event(code, value);
    }
    fn push_key(&mut self, code: &Key, value: i32) {
        self.get_inputs().push_key(code, value);
    }
}

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

    pub fn execute_text(&mut self, text: String) -> Result<(), String> {
        let action = action_parse(text)?;
        match action {
            ActionType::Finger(finger_data) => self.finger.process(&finger_data),
            ActionType::Stylus(stylus_data) => self.stylus.process(&stylus_data),
            ActionType::Screen(_screen) => Ok(()),
        }
    }
}
