mod action_parser;
mod finger;
mod stylus;
mod view;

pub use self::{
    action_parser::{create_action_element_split, ActionElementSplit, ActionElementSplitParser},
    finger::FingerData,
    stylus::StylusData,
    view::ViewData,
};

pub enum ActionType {
    Stylus(StylusData),
    Screen(ViewData),
    Finger(FingerData),
}

pub trait FromSplit {
    const KEY: char;
    fn from_split(split: &mut ActionElementSplit) -> Result<ActionType, String>;
}

pub fn action_parse(text: String) -> Result<ActionType, String> {
    let (head, mut split) = create_action_element_split(&text)?;

    match head {
        // Pen Down Up Out
        StylusData::KEY => StylusData::from_split(&mut split),

        // Gesture End Start Continued
        FingerData::KEY => FingerData::from_split(&mut split),

        // View update
        ViewData::KEY => ViewData::from_split(&mut split),

        _ => Err(String::from("Unexpected header")),
    }
    .map_err(|err| format!("{text}: {err}"))
}
