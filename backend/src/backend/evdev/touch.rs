use super::{
    super::super::{parse::TouchData, utility::ErrToString},
    PushEvent, WithAbs,
};

use evdev::{
    uinput::{VirtualDevice, VirtualDeviceBuilder},
    AbsInfo, AbsoluteAxisType, AttributeSet, BusType, InputEvent, InputId, Key, PropType,
    UinputAbsSetup,
};

const ABS_MT_SLOT: u16 = AbsoluteAxisType::ABS_MT_SLOT.0;
const ABS_MT_POSITION_X: u16 = AbsoluteAxisType::ABS_MT_POSITION_X.0;
const ABS_MT_POSITION_Y: u16 = AbsoluteAxisType::ABS_MT_POSITION_Y.0;
const ABS_MT_TRACKING_ID: u16 = AbsoluteAxisType::ABS_MT_TRACKING_ID.0;
const ABS_X: u16 = AbsoluteAxisType::ABS_X.0;
const ABS_Y: u16 = AbsoluteAxisType::ABS_Y.0;

pub struct TouchBackend {
    device: VirtualDevice,
    current_touch: bool,
    track: i32,
    inputs: Vec<InputEvent>,
    double: bool,
    triple: bool,
    quad: bool,
    quint: bool,
    single: bool,
}

impl TouchBackend {
    pub fn new() -> Result<Self, String> {
        let mut device = VirtualDeviceBuilder::new()
            .err_tostring()?
            .name("pendroid-touchpad")
            .input_id(InputId::new(BusType::BUS_USB, 0u16, 1333u16, 1u16))
            .with_abs(&[
                // TOOL INFO
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_TOOL_TYPE,
                    AbsInfo::new(2, 0, 0, 0, 0, 1),
                ),
                // ABS X / Y
                UinputAbsSetup::new(AbsoluteAxisType::ABS_X, AbsInfo::new(0, 0, 2800, 6, 10, 11)),
                UinputAbsSetup::new(AbsoluteAxisType::ABS_Y, AbsInfo::new(0, 0, 1752, 6, 10, 11)),
                // ABS MT X / Y
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_POSITION_X,
                    AbsInfo::new(0, 0, 2800, 6, 10, 11),
                ),
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_POSITION_Y,
                    AbsInfo::new(0, 0, 1752, 6, 10, 11),
                ),
                // ABS SLOT
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_SLOT,
                    AbsInfo::new(0, 0, 12, 0, 0, 1),
                ),
                // ABS_MT_TRACKING_ID
                UinputAbsSetup::new(
                    AbsoluteAxisType::ABS_MT_TRACKING_ID,
                    AbsInfo::new(0, 0, 65535, 0, 0, 1),
                ),
            ])?
            .with_keys(&AttributeSet::from_iter([
                Key::BTN_TOUCH,
                Key::BTN_TOOL_FINGER,
                Key::BTN_TOOL_DOUBLETAP,
                Key::BTN_TOOL_TRIPLETAP,
                Key::BTN_TOOL_QUADTAP,
                Key::BTN_TOOL_QUINTTAP,
                Key::BTN_LEFT,
            ]))
            .err_tostring()?
            .with_properties(&AttributeSet::from_iter([
                PropType::POINTER,
                PropType::BUTTONPAD,
            ]))
            .err_tostring()?
            .build()
            .err_tostring()?;

        for path in device.enumerate_dev_nodes_blocking().err_tostring()? {
            let path = path.err_tostring()?;
            println!("Available as {}", path.display());
        }

        Ok(Self {
            device,
            inputs: Vec::<InputEvent>::with_capacity(32),
            current_touch: false,
            track: 0,
            double: false,
            quad: false,
            triple: false,
            quint: false,
            single: false,
        })
    }

    fn increase_track(&mut self) {
        self.track += 1;
        if self.track == 65535 {
            self.track = 1;
        }
    }

    pub fn process_touch(&mut self, touch_data: &TouchData) -> Result<(), String> {
        self.inputs.clear();
        let down = !touch_data.pos.is_empty();
        let len = touch_data.pos.len();

        // MT event
        for (i, touch) in touch_data.pos.iter().enumerate() {
            self.inputs.push_abs_event(ABS_MT_SLOT, i as i32);
            self.inputs.push_abs_event(ABS_MT_POSITION_X, touch.0);
            self.inputs.push_abs_event(ABS_MT_POSITION_Y, touch.1);
        }

        // ABS event
        if down {
            let (x, y) = touch_data.pos.first().unwrap();
            self.inputs.push_abs_event(ABS_X, x.to_owned());
            self.inputs.push_abs_event(ABS_Y, y.to_owned());
        }

        // Count touch
        if self.single != (len == 1) {
            self.single = len == 1;
            self.inputs
                .push_key(Key::BTN_TOOL_FINGER, if self.single { 1 } else { 0 });
        }
        if self.double != (len == 2) {
            self.double = len == 2;
            self.inputs
                .push_key(Key::BTN_TOOL_DOUBLETAP, if self.double { 1 } else { 0 });
        }
        if self.triple != (len == 3) {
            self.triple = len == 3;
            self.inputs
                .push_key(Key::BTN_TOOL_TRIPLETAP, if self.triple { 1 } else { 0 });
        }
        if self.quad != (len == 4) {
            self.quad = len == 4;
            self.inputs
                .push_key(Key::BTN_TOOL_QUADTAP, if self.quad { 1 } else { 0 });
        }
        if self.quint != (len == 5) {
            self.quint = len == 5;
            self.inputs
                .push_key(Key::BTN_TOOL_QUINTTAP, if self.quint { 1 } else { 0 });
        }

        // Change on off
        if !down {
            // Off
            if self.current_touch {
                self.current_touch = false;
                self.inputs.push_abs_event(ABS_MT_TRACKING_ID, -1);
                self.inputs.push_key(Key::BTN_TOUCH, 0);
            }
        } else if !self.current_touch {
            // On
            self.increase_track();
            self.inputs.push_key(Key::BTN_TOUCH, 1);
            self.current_touch = true;
        }

        // Send track
        if self.current_touch {
            self.inputs.push_abs_event(ABS_MT_TRACKING_ID, self.track);
        }

        self.device.emit(self.inputs.as_slice()).err_tostring()?;
        Ok(())
    }
}
