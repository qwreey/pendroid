use std::{iter::Peekable, str::Split};

use crate::utility::ErrToString;

pub type ActionElementSplit<'a, 'b> = Split<'a, &'b str>;

pub fn create_action_element_split(action: &str) -> Result<(char, Split<'_, &str>), String> {
    let Some(head) = action.chars().next() else {
        return Err(String::from("Unexpected header"));
    };
    let split = action[1..].split(";");
    Ok((head, split))
}

pub trait ActionElementSplitParser {
    fn parse_element<T: ActionElement>(&mut self, name: &'static str) -> Result<T, String>;
}
impl ActionElementSplitParser for ActionElementSplit<'_, '_> {
    fn parse_element<T: ActionElement>(&mut self, name: &'static str) -> Result<T, String> {
        T::from_element(
            self.next()
                .ok_or_else(|| format!("field {name} required"))?,
        )
    }
}
impl ActionElementSplitParser for Peekable<&mut ActionElementSplit<'_, '_>> {
    fn parse_element<T: ActionElement>(&mut self, name: &'static str) -> Result<T, String> {
        T::from_element(
            self.next()
                .ok_or_else(|| format!("field {name} required"))?,
        )
    }
}

pub trait ActionElement
where
    Self: Sized,
{
    fn from_element(text: &str) -> Result<Self, String>;
}
macro_rules! impl_num_action_element {
    ($target:ty) => {
        impl ActionElement for $target {
            fn from_element(text: &str) -> Result<Self, String> {
                text.parse::<$target>().err_tostring()
            }
        }
    };
}
impl ActionElement for bool {
    fn from_element(text: &str) -> Result<Self, String> {
        Ok(text == "T")
    }
}
impl_num_action_element!(i32);
impl_num_action_element!(u32);
