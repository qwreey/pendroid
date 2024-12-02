use super::{ActionElementSplit, ActionElementSplitParser, ActionType, FromSplit};

#[derive(Debug)]
pub struct ViewData {
    pub width: u32,
    pub height: u32,
}

impl FromSplit for ViewData {
    const KEY: char = 'V';
    fn from_split(split: &mut ActionElementSplit) -> Result<ActionType, String> {
        let width = split.parse_element::<u32>("width")?;
        let height = split.parse_element::<u32>("height")?;
        Ok(ActionType::Screen(ViewData { width, height }))
    }
}
