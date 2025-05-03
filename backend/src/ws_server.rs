use std::{cell::RefCell, rc::Rc};

use ws::{Error as WsError, Factory, Handler, Message, Sender};

use super::backend::InputBackend;

// Connection
pub struct PenWsConnection {
    backend: Rc<RefCell<InputBackend>>,
}
impl Handler for PenWsConnection {
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

pub struct PenWsFactory {
    backend: Rc<RefCell<InputBackend>>,
}
impl Factory for PenWsFactory {
    type Handler = PenWsConnection;

    fn connection_made(&mut self, _: Sender) -> Self::Handler {
        PenWsConnection {
            backend: self.backend.clone(),
        }
    }
}
impl PenWsFactory {
    pub fn new(backend: InputBackend) -> PenWsFactory {
        PenWsFactory {
            backend: Rc::new(RefCell::new(backend)),
        }
    }
}
