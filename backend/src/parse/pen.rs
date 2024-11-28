use std::str::Split;

use super::ActionType;

pub struct PenData {
    pub x: i32,
    pub y: i32,
    pub tilt_x: i32,
    pub tilt_y: i32,
    pub tps: u32,
    pub pressure: i32,
    pub down: bool,
    pub hover: bool,
}

pub fn pen_parse(down: bool, hover: bool, split: &mut Split<&str>) -> Result<ActionType, String> {
    let pressure = split
        .next()
        .ok_or_else(|| String::from("field 0 pressure required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 0 pressure parse failed"))?;
    let tps = split
        .next()
        .ok_or_else(|| String::from("field 1 tps required"))?
        .parse::<u32>()
        .map_err(|_| String::from("field 1 tps parse failed"))?;
    let x = split
        .next()
        .ok_or_else(|| String::from("field 2 x required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 2 x parse failed"))?;
    let y = split
        .next()
        .ok_or_else(|| String::from("field 3 y required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 3 y parse failed"))?;
    let tilt_x = split
        .next()
        .ok_or_else(|| String::from("field 4 tilt_x required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 4 tilt_x parse failed"))?;
    let tilt_y = split
        .next()
        .ok_or_else(|| String::from("field 5 tilt_y required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 5 tilt_y parse failed"))?;

    let pen_data = PenData {
        pressure,
        tps,
        x,
        y,
        tilt_x,
        tilt_y,
        down,
        hover,
    };

    Ok(ActionType::Pen(pen_data))
}
