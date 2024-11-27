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
trait PushAbsEvent {
    fn push_abs_event(&mut self, code: u16, value: i32);
}
impl PushAbsEvent for Vec<InputEvent> {
    fn push_abs_event(&mut self, code: u16, value: i32) {
        self.push(InputEvent::new(EventType::ABSOLUTE, code, value));
    }
}

const ABS_X: u16 = AbsoluteAxisType::ABS_X.0;
const ABS_Y: u16 = AbsoluteAxisType::ABS_Y.0;
const ABS_PRESSURE: u16 = AbsoluteAxisType::ABS_PRESSURE.0;
const ABS_TILT_X: u16 = AbsoluteAxisType::ABS_TILT_X.0;
const ABS_TILT_Y: u16 = AbsoluteAxisType::ABS_TILT_Y.0;

pub struct PenBackend {
    device: VirtualDevice,
    current_down: bool,
    current_hover: bool,
    inputs: Vec<InputEvent>,
}
impl PenBackend {
    pub fn new() -> Result<Self, String> {
        // let x_info =
        //     ;

        // AbsoluteAxisType::ABS_TILT_X
        // AbsoluteAxisType::ABS_TILT_Y

        let mut device = VirtualDeviceBuilder::new()
            .err_tostring()?
            .name("pendroid")
            .input_id(InputId::new(BusType::BUS_USB, 0u16, 1332u16, 1u16))
            .with_relative_axes(&AttributeSet::from_iter([
                RelativeAxisType::REL_HWHEEL_HI_RES,
                RelativeAxisType::REL_WHEEL_HI_RES,
            ]))
            .err_tostring()?
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
            ])?
            .with_keys(&AttributeSet::from_iter([
                Key::BTN_TOOL_PEN,
                Key::BTN_TOUCH,
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
            inputs: Vec::<InputEvent>::with_capacity(10),
            current_down: false,
            current_hover: false,
        })
    }

    pub fn execute_text(&mut self, text: String) -> Result<(), String> {
        let action = action_parse(text)?;

        match action {
            ActionType::Pen(pen_data) => {
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
                    inputs.push(InputEvent::new(
                        EventType::KEY,
                        Key::BTN_TOOL_PEN.code(),
                        if pen_data.hover { 1 } else { 0 },
                    ));
                    self.current_hover = pen_data.hover;
                }

                // Process pen down
                if pen_data.down != self.current_down {
                    inputs.push(InputEvent::new(
                        EventType::KEY,
                        Key::BTN_TOUCH.code(),
                        if pen_data.down { 1 } else { 0 },
                    ));
                    self.current_down = pen_data.down;
                }

                self.device.emit(inputs.as_slice()).err_tostring()?;
                Ok(())
            }
            ActionType::ScreenUpdate(_screen) => Ok(()),
        }
    }
}
