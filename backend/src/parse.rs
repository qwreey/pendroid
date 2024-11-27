use std::str::Split;

pub struct PenData {
    pub x: i32,
    pub y: i32,
    pub tilt_x: i32,
    pub tilt_y: i32,
    pub tps: i32,
    pub pressure: i32,
    pub down: bool,
    pub hover: bool,
}

pub struct ScreenData {
    pub width: u32,
    pub height: u32,
}

pub enum ActionType {
    Pen(PenData),
    ScreenUpdate(ScreenData),
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
        .parse::<i32>()
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

fn screen_parse(split: &mut Split<&str>) -> Result<ActionType, String> {
    let width = split
        .next()
        .ok_or_else(|| String::from("field 0 width required"))?
        .parse::<u32>()
        .map_err(|_| String::from("field 0 width parse failed"))?;
    let height = split
        .next()
        .ok_or_else(|| String::from("field 1 height required"))?
        .parse::<u32>()
        .map_err(|_| String::from("field 1 height parse failed"))?;

    Ok(ActionType::ScreenUpdate(ScreenData { width, height }))
}

pub fn action_parse(action: String) -> Result<ActionType, String> {
    let Some(head) = action.chars().next() else {
        return Err(String::from("Unexpected header"));
    };

    let mut split = action.as_str()[1..].split(";");

    match head {
        'D' => pen_parse(true, true, &mut split),
        'U' => pen_parse(false, true, &mut split),
        'O' => pen_parse(false, false, &mut split),
        'S' => screen_parse(&mut split),
        _ => Err(String::from("Unexpected header")),
    }
}
