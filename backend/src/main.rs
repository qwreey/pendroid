use std::{cell::RefCell, rc::Rc};

use ws::{Error as WsError, Factory, Handler, Message, Sender, WebSocket};

mod parse;
mod udev_backend;
mod utility;

use parse::{action_parse, ActionType};
use udev_backend::PenBackend;
use utility::ErrToString;

// use std::thread;
// use std::time::Duration;
// use uinput::event::absolute::Absolute::Position;
// use uinput::event::absolute::Position::{X, Y};
// use uinput::event::controller::Controller::Mouse;
// use uinput::event::controller::Mouse::Left;
// use uinput::event::Event::{Absolute, Controller};

struct PenWsHandle {
    backend: Rc<RefCell<PenBackend>>,
}
impl Handler for PenWsHandle {
    fn on_message(&mut self, msg: Message) -> Result<(), WsError> {
        // Ok(self.backend)
        let Message::Text(text) = msg else {
            println!("Got unexpected client data");
            return Ok(());
        };
        if let Err(err) = (*self.backend).borrow_mut().execute_text(text) {
            println!("{err}");
        };
        Ok(())
    }
}

struct PenWs {
    backend: Rc<RefCell<PenBackend>>,
}
impl Factory for PenWs {
    type Handler = PenWsHandle;

    fn connection_made(&mut self, _: Sender) -> Self::Handler {
        PenWsHandle {
            backend: self.backend.clone(),
        }
    }
}

fn main() -> Result<(), String> {
    let ws = WebSocket::new(PenWs {
        backend: Rc::new(RefCell::new(PenBackend::new()?)),
    })
    .err_tostring()?;
    ws.listen("localhost:57362").err_tostring()?;

    Ok(())
}
