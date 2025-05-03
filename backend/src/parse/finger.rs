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
