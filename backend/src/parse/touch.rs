use std::str::Split;

use crate::utility::ErrToString;

use super::ActionType;

pub struct TouchData {
    pub pos: Vec<(i32, i32)>,
}

pub fn touch_parse(split: &mut Split<&str>) -> Result<ActionType, String> {
    let mut first = 0i32;
    let mut list = Vec::<(i32, i32)>::with_capacity(12);

    for (i, num) in split.enumerate() {
        if num.is_empty() {
            break;
        }
        let num = num.parse::<i32>().err_tostring()?;
        if i % 2 == 0 {
            first = num;
            continue;
        }
        list.push((first, num));
        if i > 2 * 12 {
            break;
        }
    }

    Ok(ActionType::Touch(TouchData { pos: list }))
}
