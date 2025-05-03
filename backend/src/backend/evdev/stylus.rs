use super::{
    super::super::parse::StylusData,
    event_list::{EventList, GetInputs, PushEvent},
    with_abs::WithAbs,
};
use evdev::{
    uinput::VirtualDevice, AbsInfo, AbsoluteAxisCode, AttributeSet, BusType, InputEvent, InputId,
    KeyCode, MiscCode, PropType, UinputAbsSetup,
};
use qwreey_utility_rs::ErrToString;
use std::sync::LazyLock;

const ABS_X: u16 = AbsoluteAxisCode::ABS_X.0;
const ABS_Y: u16 = AbsoluteAxisCode::ABS_Y.0;
const ABS_PRESSURE: u16 = AbsoluteAxisCode::ABS_PRESSURE.0;
const ABS_TILT_X: u16 = AbsoluteAxisCode::ABS_TILT_X.0;
const ABS_TILT_Y: u16 = AbsoluteAxisCode::ABS_TILT_Y.0;

pub struct StylusBackend {
    device: VirtualDevice,
    current_down: bool,
    current_hover: bool,
    current_button: bool,
    inputs: EventList,
}

static RUBBER_OFF: LazyLock<EventList> = LazyLock::new(|| {
    let mut events = EventList::with_capacity(1);
    events.push_key(&KeyCode::BTN_TOOL_RUBBER, 0);
    events
});
static PENCIL_OFF: LazyLock<EventList> = LazyLock::new(|| {
    let mut events = EventList::with_capacity(1);
    events.push_key(&KeyCode::BTN_TOOL_PENCIL, 0);
    events
});

impl GetInputs for StylusBackend {
    fn get_inputs(&mut self) -> &mut EventList {
        &mut self.inputs
    }
}

impl StylusBackend {
    // Create new evdev device
    pub fn new() -> Result<Self, String> {
        let mut device = VirtualDevice::builder()
            .err_tostring()?
            .name("pendroid-stylus")
            .input_id(InputId::new(BusType::BUS_USB, 0u16, 1332u16, 1u16))
            .with_abs(&[
                // ABS PRESSURE
                UinputAbsSetup::new(
                    AbsoluteAxisCode::ABS_PRESSURE,
                    AbsInfo::new(0, 0, 4096, 0, 0, 1),
                ),
                // TOOL INFO
                UinputAbsSetup::new(
                    AbsoluteAxisCode::ABS_MT_TOOL_TYPE,
                    AbsInfo::new(1, 0, 0, 0, 0, 1),
                ),
                // ABS TILT X / Y
                UinputAbsSetup::new(
                    AbsoluteAxisCode::ABS_TILT_X,
                    AbsInfo::new(0, -90, 90, 0, 0, 1),
                ),
                UinputAbsSetup::new(
                    AbsoluteAxisCode::ABS_TILT_Y,
                    AbsInfo::new(0, -90, 90, 0, 0, 1),
                ),
                // ABS X / Y
                UinputAbsSetup::new(AbsoluteAxisCode::ABS_X, AbsInfo::new(0, 0, 2800, 0, 0, 1)),
                UinputAbsSetup::new(AbsoluteAxisCode::ABS_Y, AbsInfo::new(0, 0, 1752, 0, 0, 1)),
            ])?
            .with_keys(&AttributeSet::from_iter([
                KeyCode::BTN_TOOL_PEN,
                KeyCode::BTN_TOOL_RUBBER,
                KeyCode::BTN_TOOL_PENCIL,
                KeyCode::BTN_STYLUS,
                KeyCode::BTN_STYLUS2,
            ]))
            .err_tostring()?
            .with_msc(&AttributeSet::from_iter([MiscCode::MSC_TIMESTAMP]))
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
            inputs: Vec::<InputEvent>::with_capacity(32),
            current_down: false,
            current_hover: false,
            current_button: false,
        })
    }

    pub fn process(&mut self, pen_data: &StylusData) -> Result<(), String> {
        let hover_changed = pen_data.hover != self.current_hover;
        let button_changed = pen_data.button != self.current_button;
        self.inputs.clear();

        // Report position and pressure
        self.push_abs_event(ABS_X, pen_data.x);
        self.push_abs_event(ABS_Y, pen_data.y);
        self.push_abs_event(ABS_PRESSURE, pen_data.pressure);
        self.push_abs_event(ABS_TILT_X, pen_data.tilt_x);
        self.push_abs_event(ABS_TILT_Y, pen_data.tilt_y);

        // Process tool (eraser, pencil)
        if (hover_changed || button_changed) && pen_data.hover && !pen_data.down {
            if pen_data.button {
                if !hover_changed {
                    // Disable old tool
                    self.device.emit(&PENCIL_OFF).err_tostring()?;
                }
                self.push_key(&KeyCode::BTN_TOOL_RUBBER, 1);
            } else {
                if !hover_changed {
                    // Disable old tool
                    self.device.emit(&RUBBER_OFF).err_tostring()?;
                }
                self.push_key(&KeyCode::BTN_TOOL_PENCIL, 1);
            }
            self.current_button = pen_data.button;
        }

        // Process hover
        if hover_changed && !pen_data.hover {
            self.push_key(
                if self.current_button {
                    &KeyCode::BTN_TOOL_RUBBER
                } else {
                    &KeyCode::BTN_TOOL_PENCIL
                },
                0,
            );
        }
        self.current_hover = pen_data.hover;

        // Process pen down (touch)
        if pen_data.down != self.current_down {
            self.push_key(
                if self.current_button {
                    &KeyCode::BTN_STYLUS2
                } else {
                    &KeyCode::BTN_STYLUS
                },
                if pen_data.down { 1 } else { 0 },
            );
            self.current_down = pen_data.down;
        }
        self.push_msc(MiscCode::MSC_TIMESTAMP.0, pen_data.timestamp);

        self.device.emit(&self.inputs).err_tostring()?;
        Ok(())
    }
}
