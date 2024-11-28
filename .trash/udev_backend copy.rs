use crate::parse::{PenData, TouchData, TouchState};

use super::{action_parse, ActionType, ErrToString};

use evdev::{
    uinput::{VirtualDevice, VirtualDeviceBuilder},
    AbsInfo, AbsoluteAxisType, AttributeSet, BusType, EventType, InputEvent, InputId, Key,
    PropType, RelativeAxisType, UinputAbsSetup,
};

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
    // #[inline]
    // fn push_rel_event(&mut self, code: u16, value: i32) {
    //     self.push(InputEvent::new(EventType::RELATIVE, code, value));
    // }
    #[inline]
    fn push_key(&mut self, code: Key, value: i32) {
        self.push(InputEvent::new(EventType::KEY, code.code(), value));
    }
}

const ABS_X: u16 = AbsoluteAxisType::ABS_X.0;
const ABS_Y: u16 = AbsoluteAxisType::ABS_Y.0;
const ABS_PRESSURE: u16 = AbsoluteAxisType::ABS_PRESSURE.0;
const ABS_TILT_X: u16 = AbsoluteAxisType::ABS_TILT_X.0;
const ABS_TILT_Y: u16 = AbsoluteAxisType::ABS_TILT_Y.0;
// const REL_HWHEEL_HI_RES: u16 = RelativeAxisType::REL_HWHEEL_HI_RES.0;
// const REL_WHEEL_HI_RES: u16 = RelativeAxisType::REL_WHEEL_HI_RES.0;
// const REL_X: u16 = RelativeAxisType::REL_X.0;
// const REL_Y: u16 = RelativeAxisType::REL_Y.0;

pub struct PenBackend {
    device: VirtualDevice,
    current_down: bool,
    current_hover: bool,
    inputs: Vec<InputEvent>,
    // current_touch_x: i32,
    // current_touch_y: i32,
    current_touch: bool,
}
impl PenBackend {
    pub fn new() -> Result<Self, String> {
        let mut device = VirtualDeviceBuilder::new()
            .err_tostring()?
            .name("pendroid")
            .input_id(InputId::new(BusType::BUS_USB, 0u16, 1332u16, 1u16))
            // .with_relative_axes(&AttributeSet::from_iter([
            //     RelativeAxisType::REL_HWHEEL_HI_RES,
            //     RelativeAxisType::REL_WHEEL_HI_RES,
            //     RelativeAxisType::REL_X,
            //     RelativeAxisType::REL_Y,
            // ]))
            // .err_tostring()?
            .with_abs(&[
                // ABS PRESSURE
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_PRESSURE,
                    AbsInfo::new(0, 0, 4096, 0, 0, 1),
                ),
                // TOOL INFO
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_TOOL_TYPE,
                    AbsInfo::new(1, 0, 1, 0, 0, 0),
                ),
                // ABS TILT X / Y
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_TILT_X,
                    AbsInfo::new(0, -90, 90, 0, 0, 1),
                ),
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_TILT_Y,
                    AbsInfo::new(0, -90, 90, 0, 0, 1),
                ),
                // ABS X / Y
                UinputAbsSetup::new(AbsoluteAxisType::ABS_X, AbsInfo::new(0, 0, 2800, 0, 0, 1)),
                UinputAbsSetup::new(AbsoluteAxisType::ABS_Y, AbsInfo::new(0, 0, 1752, 0, 0, 1)),
                // ABS MT X / Y
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_POSITION_X,
                    AbsInfo::new(0, 0, 2800, 0, 0, 1),
                ),
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_POSITION_Y,
                    AbsInfo::new(0, 0, 1752, 0, 0, 1),
                ),
            ])?
            .with_keys(&AttributeSet::from_iter([
                Key::BTN_TOOL_PEN,
                Key::BTN_TOUCH,
                // Key::BTN_TOOL_MOUSE,
                Key::BTN_TOOL_FINGER,
            ]))
            .err_tostring()?
            .with_properties(&AttributeSet::from_iter([PropType::POINTER]))
            .err_tostring()?
            .build()
            .err_tostring()?;

        for path in device.enumerate_dev_nodes_blocking().err_tostring()? {
            let path = path.err_tostring()?;
            println!("Available as {}", path.display());
        }

        Ok(Self {
            device,
            inputs: Vec::<InputEvent>::with_capacity(16),
            current_down: false,
            current_hover: false,
            current_touch: false,
            // current_touch_x: 0,
            // current_touch_y: 0,
        })
    }

    // fn calc_touch_rel(&mut self, touch_data: &TouchData) -> (i32, i32) {
    //     let result = match touch_data.state {
    //         TouchState::Start => (0, 0),
    //         TouchState::End => (0, 0),
    //         TouchState::Continued => (
    //             touch_data.x - self.current_touch_x,
    //             touch_data.y - self.current_touch_y,
    //         ),
    //     };
    //     self.current_touch_x = touch_data.x;
    //     self.current_touch_y = touch_data.y;
    //     result
    // }

    fn process_pen(&mut self, pen_data: &PenData) -> Result<(), String> {
        let inputs = &mut self.inputs;
        inputs.clear();

        // Report position and pressure
        inputs.push_abs_event(ABS_X, pen_data.x);
        inputs.push_abs_event(ABS_Y, pen_data.y);
        inputs.push_abs_event(ABS_PRESSURE, pen_data.pressure);
        inputs.push_abs_event(ABS_TILT_X, pen_data.tilt_x);
        inputs.push_abs_event(ABS_TILT_Y, pen_data.tilt_y);

        // Process pen hover
        if pen_data.hover != self.current_hover {
            inputs.push_key(Key::BTN_TOOL_PEN, if pen_data.hover { 1 } else { 0 });
            self.current_hover = pen_data.hover;
        }

        // Process pen down
        if pen_data.down != self.current_down {
            inputs.push_key(Key::BTN_TOUCH, if pen_data.down { 1 } else { 0 });
            self.current_down = pen_data.down;
        }

        self.device.emit(inputs.as_slice()).err_tostring()?;
        Ok(())
    }

    fn process_touch(&mut self, touch_data: &TouchData) -> Result<(), String> {
        self.inputs.clear();
        let current_gesture = touch_data.current_gesture;
        // let upgrade_from = touch_data.upgrade_from;

        // if current_gesture == 2 || upgrade_from == 2 {
        //     let (x, y) = if current_gesture == 2 {
        //         self.calc_touch_rel(touch_data)
        //     } else {
        //         (0i32, 0i32)
        //     };
        //     // self.inputs.push_rel_event(REL_WHEEL_HI_RES, x);
        //     // self.inputs.push_rel_event(REL_HWHEEL_HI_RES, y);
        //     // self.inputs.push_key(Key::BTN_TOOL_MOUSE, 1);
        //     self.device.emit(self.inputs.as_slice()).err_tostring()?;
        // } else if current_gesture == 1 {
        //     let (x, y) = if current_gesture == 1 {
        //         self.calc_touch_rel(touch_data)
        //     } else {
        //         (0i32, 0i32)
        //     };
        //     dbg!(x, y);
        //     // self.inputs.push_rel_event(REL_X, x);
        //     // self.inputs.push_rel_event(REL_Y, y);
        //     self.inputs.push_key(Key::BTN_TOOL_MOUSE, 1);
        //     self.device.emit(self.inputs.as_slice()).err_tostring()?;
        // }
        if current_gesture == 0 {
            if self.current_touch {
                self.current_touch = false;
                self.inputs.push_key(Key::BTN_TOOL_FINGER, 0);
                self.inputs.push_key(Key::BTN_TOUCH, 0);
                dbg!("end");
            }
        } else if !self.current_touch {
            self.inputs.push_key(Key::BTN_TOOL_FINGER, 1);
            self.inputs.push_key(Key::BTN_TOUCH, 1);
            self.current_touch = true;
            dbg!("start");
        }

        Ok(())
    }

    pub fn execute_text(&mut self, text: String) -> Result<(), String> {
        let action = action_parse(text)?;
        match action {
            ActionType::Touch(touch_data) => self.process_touch(&touch_data),
            ActionType::Pen(pen_data) => self.process_pen(&pen_data),
            ActionType::ScreenUpdate(_screen) => Ok(()),
        }
    }
}
