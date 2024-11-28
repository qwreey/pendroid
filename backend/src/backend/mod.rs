mod evdev;

#[cfg(target_os = "linux")]
pub use evdev::InputBackend;
