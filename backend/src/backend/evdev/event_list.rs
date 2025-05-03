use evdev::{EventType, InputEvent, KeyCode};

// commit 하기 위한 event 를 담아주는 event 리스트

pub type EventList = Vec<InputEvent>;
pub trait PushEvent {
    fn push_abs_event(&mut self, code: u16, value: i32);
    // fn push_rel_event(&mut self, code: u16, value: i32);
    fn push_key(&mut self, code: &KeyCode, value: i32);
    fn push_msc(&mut self, code: u16, value: i32);
}
impl PushEvent for EventList {
    #[inline]
    fn push_abs_event(&mut self, code: u16, value: i32) {
        self.push(InputEvent::new(EventType::ABSOLUTE.0, code, value));
    }
    #[inline]
    fn push_key(&mut self, code: &KeyCode, value: i32) {
        self.push(InputEvent::new(EventType::KEY.0, code.code(), value));
    }
    #[inline]
    fn push_msc(&mut self, code: u16, value: i32) {
        self.push(InputEvent::new(EventType::MISC.0, code, value));
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
    fn push_key(&mut self, code: &KeyCode, value: i32) {
        self.get_inputs().push_key(code, value);
    }
    fn push_msc(&mut self, code: u16, value: i32) {
        self.get_inputs().push_msc(code, value);
    }
}
