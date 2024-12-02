use super::{ActionElementSplit, ActionElementSplitParser, ActionType, FromSplit};

#[derive(Debug)]
pub struct StylusData {
    pub x: i32,
    pub y: i32,
    pub tilt_x: i32,
    pub tilt_y: i32,
    pub pressure: i32,
    pub down: bool,
    pub hover: bool,
    pub button: bool,
}

impl FromSplit for StylusData {
    const KEY: char = 'S';
    fn from_split(split: &mut ActionElementSplit) -> Result<ActionType, String> {
        let hover = split.parse_element::<bool>("hover")?;
        let down = split.parse_element::<bool>("down")?;
        let button = split.parse_element::<bool>("button")?;
        let x = split.parse_element::<i32>("x")?;
        let y = split.parse_element::<i32>("y")?;
        let tilt_x = split.parse_element::<i32>("tilt_x")?;
        let tilt_y = split.parse_element::<i32>("tilt_y")?;
        let pressure = split.parse_element::<i32>("pressure")?;

        let stylus_data = StylusData {
            pressure,
            button,
            x,
            y,
            tilt_x,
            tilt_y,
            down,
            hover,
        };

        Ok(ActionType::Stylus(stylus_data))
    }
}
