pub struct TouchData {
    pub x: i32,
    pub y: i32,
    pub tps: u32,
    pub state: TouchState,
    pub upgrade_from: i32,
    pub current_gesture: i32,
    pub velocity_x: i32,
    pub velocity_y: i32,
}
#[derive(PartialEq)]
pub enum TouchState {
    End,
    Start,
    Continued,
}
pub fn touch_parse(state: TouchState, split: &mut Split<&str>) -> Result<ActionType, String> {
    let upgrade_from = split
        .next()
        .ok_or_else(|| String::from("field 0 upgrade_from required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 0 upgrade_from parse failed"))?;
    let current_gesture = split
        .next()
        .ok_or_else(|| String::from("field 1 current_gesture required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 1 current_gesture parse failed"))?;
    let tps = split
        .next()
        .ok_or_else(|| String::from("field 2 tps required"))?
        .parse::<u32>()
        .map_err(|_| String::from("field 2 tps parse failed"))?;
    let x = split
        .next()
        .ok_or_else(|| String::from("field 3 x required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 3 x parse failed"))?;
    let y = split
        .next()
        .ok_or_else(|| String::from("field 4 y required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 4 y parse failed"))?;
    let velocity_x = split
        .next()
        .ok_or_else(|| String::from("field 5 velocity_x required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 5 velocity_x parse failed"))?;
    let velocity_y = split
        .next()
        .ok_or_else(|| String::from("field 6 velocity_y required"))?
        .parse::<i32>()
        .map_err(|_| String::from("field 6 velocity_y parse failed"))?;

    Ok(ActionType::Touch(TouchData {
        state,
        upgrade_from,
        current_gesture,
        tps,
        x,
        y,
        velocity_x,
        velocity_y,
    }))
}
