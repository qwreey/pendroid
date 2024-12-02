use super::{
    super::super::{parse::FingerData, utility::ErrToString},
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
const TOUCHS: [Key; 5] = [
    Key::BTN_TOOL_FINGER,
    Key::BTN_TOOL_DOUBLETAP,
    Key::BTN_TOOL_TRIPLETAP,
    Key::BTN_TOOL_QUADTAP,
    Key::BTN_TOOL_QUINTTAP,
];

pub struct FingerBackend {
    device: VirtualDevice,
    current_slot: i32,
    current_down: bool,
    inputs: Vec<InputEvent>,
    touch_trackings: [i32; 12],
    touch_active: [bool; 12],
    touch_pos: [(i32, i32); 12],
}

impl FingerBackend {
    // Create new evdev device
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
                    AbsInfo::new(0, -1, 65535, 0, 0, 1),
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
            current_slot: -1,
            current_down: false,
            touch_active: [false; 12],
            touch_trackings: [-1i32; 12],
            touch_pos: [(0, 0); 12],
        })
    }

    // Update slot
    pub fn update_slot(&mut self, new_slot: i32) {
        if new_slot != self.current_slot {
            self.current_slot = new_slot;
            self.inputs.push_abs_event(ABS_MT_SLOT, new_slot);
        }
    }

    pub fn process(&mut self, finger_data: &FingerData) -> Result<(), String> {
        self.inputs.clear();

        // MT event
        for (index, touch) in finger_data.touchs.iter().enumerate() {
            if touch.slot == -1 {
                break;
            }

            // Update ABS_MT_POSITION XY
            if touch.x != -1 {
                self.update_slot(touch.slot);
                self.inputs.push_abs_event(ABS_MT_POSITION_X, touch.x);
            }
            if touch.y != -1 {
                self.update_slot(touch.slot);
                self.inputs.push_abs_event(ABS_MT_POSITION_Y, touch.y);
            }
            self.touch_pos[index] = (touch.x, touch.y);

            // Update ABS_MT_TRACKING_ID
            if self.touch_trackings[index] != touch.tracking_id {
                self.update_slot(touch.slot);
                self.touch_trackings[index] = touch.tracking_id;
                self.inputs
                    .push_abs_event(ABS_MT_TRACKING_ID, touch.tracking_id);
            }
        }

        // Count touch (Finger / Double / ...)
        for (index, key) in TOUCHS.iter().enumerate() {
            let active = finger_data.length == (index + 1) as i32;
            if self.touch_active[index] != active {
                self.touch_active[index] = active;
                self.inputs.push_key(key, active as i32);
            }
        }

        // Touch event (BTN_TOUCH)
        let down = finger_data.length != 0;
        if self.current_down != down {
            self.current_down = down;
            self.inputs.push_key(&Key::BTN_TOUCH, down as i32);
        }

        // ABS event (ABS_X, ABS_Y)
        for (index, active) in self.touch_active.iter().enumerate() {
            if !active {
                continue;
            }
            let (x, y) = self.touch_pos[index];
            self.inputs.push_abs_event(ABS_X, x);
            self.inputs.push_abs_event(ABS_Y, y);
            break;
        }

        self.device.emit(self.inputs.as_slice()).err_tostring()?;
        Ok(())
    }
}
