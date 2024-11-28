mod pen;
mod screen;
mod touch;

use self::{pen::pen_parse, screen::screen_parse, touch::touch_parse};
pub use self::{pen::PenData, screen::ScreenData, touch::TouchData};

pub enum ActionType {
    Pen(PenData),
    Touch(TouchData),
    ScreenUpdate(ScreenData),
}

pub fn action_parse(action: String) -> Result<ActionType, String> {
    let Some(head) = action.chars().next() else {
        return Err(String::from("Unexpected header"));
    };

    let mut split = action.as_str()[1..].split(";");

    match head {
        // Pen Down Up Out
        'D' => pen_parse(true, true, &mut split),
        'U' => pen_parse(false, true, &mut split),
        'O' => pen_parse(false, false, &mut split),

        // Gesture End Start Continued
        // 'E' => touch_parse(TouchState::End, &mut split),
        // 'S' => touch_parse(TouchState::Start, &mut split),
        // 'C' => touch_parse(TouchState::Continued, &mut split),

        // Touch
        'T' => touch_parse(&mut split),

        // View update
        'V' => screen_parse(&mut split),
        _ => Err(String::from("Unexpected header")),
    }
}
