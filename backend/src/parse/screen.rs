use std::str::Split;

use super::ActionType;

pub struct ScreenData {
    pub width: u32,
    pub height: u32,
}

pub fn screen_parse(split: &mut Split<&str>) -> Result<ActionType, String> {
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
