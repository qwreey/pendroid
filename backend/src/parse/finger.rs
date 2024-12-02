use super::{ActionElementSplit, ActionElementSplitParser, ActionType, FromSplit};

#[derive(Debug, Clone, Copy)]
pub struct Touch {
    pub x: i32,
    pub y: i32,
    pub slot: i32,
    pub tracking_id: i32,
}
impl Default for Touch {
    fn default() -> Self {
        Self {
            x: 0,
            y: 0,
            slot: -1,
            tracking_id: 0,
        }
    }
}

#[derive(Debug)]
pub struct FingerData {
    pub length: i32,
    pub touchs: [Touch; 12],
}

impl FromSplit for FingerData {
    const KEY: char = 'F';
    fn from_split(split: &mut ActionElementSplit) -> Result<ActionType, String> {
        let mut split = split.peekable();
        let length = split.parse_element::<i32>("length")?;
        let mut touchs = [Touch::default(); 12];

        for touch in &mut touchs {
            if split.peek().is_none() {
                break;
            }
            touch.x = split.parse_element::<i32>("x")?;
            touch.y = split.parse_element::<i32>("y")?;
            touch.slot = split.parse_element::<i32>("slot")?;
            touch.tracking_id = split.parse_element::<i32>("tracking_id")?;
        }

        let finger_data = FingerData { length, touchs };
        Ok(ActionType::Finger(finger_data))
    }
}
