use evdev::{uinput::VirtualDeviceBuilder, UinputAbsSetup};
use qwreey_utility_rs::ErrToString;

// VirtualDeviceBuilder 에 with_keys 와 같은 방식으로 AbsSetup 레코드를 추가할 수 있게합니다.
pub trait WithAbs<'a> {
    fn with_abs(self, abs_list: &[UinputAbsSetup]) -> Result<VirtualDeviceBuilder<'a>, String>;
}
impl<'a> WithAbs<'a> for VirtualDeviceBuilder<'a> {
    fn with_abs(self, abs_list: &[UinputAbsSetup]) -> Result<VirtualDeviceBuilder<'a>, String> {
        let mut ret = self;
        for item in abs_list {
            ret = ret.with_absolute_axis(item).err_tostring()?;
        }
        Ok(ret)
    }
}
