use evdev::{uinput::VirtualDeviceBuilder, EventType, InputEvent, Key, UinputAbsSetup};

use super::super::{
    parse::{action_parse, ActionType},
    utility::ErrToString,
};

mod pen;
mod touch;

use pen::PenBackend;
use touch::TouchBackend;

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

trait PushEvent {
    fn push_abs_event(&mut self, code: u16, value: i32);
    // fn push_rel_event(&mut self, code: u16, value: i32);
    fn push_key(&mut self, code: Key, value: i32);
}
impl PushEvent for Vec<InputEvent> {
    #[inline]
    fn push_abs_event(&mut self, code: u16, value: i32) {
        self.push(InputEvent::new(EventType::ABSOLUTE, code, value));
    }
    #[inline]
    fn push_key(&mut self, code: Key, value: i32) {
        self.push(InputEvent::new(EventType::KEY, code.code(), value));
    }
}

pub struct InputBackend {
    pen: PenBackend,
    touch: TouchBackend,
}
impl InputBackend {
    pub fn new() -> Result<Self, String> {
        Ok(Self {
            pen: PenBackend::new()?,
            touch: TouchBackend::new()?,
        })
    }

    pub fn execute_text(&mut self, text: String) -> Result<(), String> {
        let action = action_parse(text)?;
        match action {
            ActionType::Touch(touch_data) => self.touch.process_touch(&touch_data),
            ActionType::Pen(pen_data) => self.pen.process_pen(&pen_data),
            ActionType::ScreenUpdate(_screen) => Ok(()),
        }
    }
}
