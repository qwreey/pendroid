pub trait ErrToString<T> {
    fn err_tostring(self) -> Result<T, String>;
}
impl<T, U: ToString> ErrToString<T> for Result<T, U> {
    fn err_tostring(self) -> Result<T, String> {
        self.map_err(|e| e.to_string())
    }
}
