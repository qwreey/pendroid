mod backend;
mod parse;
mod rfcomm_server;
// mod ws_server;

use std::{cell::UnsafeCell, sync::Arc};

use backend::InputBackend;
use parse::{ActionParser, ActionType};
use qwreey_utility_rs::ErrToString;
use rfcomm_server::{RfcommHandle, RfcommServer};
use tokio::io::AsyncReadExt;
// use ws::WebSocket;
// use ws_server::PenWsFactory;

struct Handle {
    backend: Arc<UnsafeCell<InputBackend>>,
    parser: ActionParser,

    sleep: u64, // 쉬어야하는 시간
    nth_item: u64,
    taken: u64, // 실행에 걸린시간
}
impl Handle {
    fn remove_dirty(&self, readbuf: &mut Vec<u8>) {
        while !readbuf.is_empty() && self.parser.should_pop(*readbuf.first().unwrap()) {
            readbuf.remove(0);
        }
    }

    async fn execute(&mut self, action: ActionType) {
        if let ActionType::Len(len) = action {
            self.sleep = if len > 1 {
                1000000u64 / 28u64 / (len as u64 + 1)
            } else {
                0
            } * 2u64;
            self.nth_item = 0u64;
            self.taken = 0u64;
        } else {
            let before = tokio::time::Instant::now();
            unsafe {
                self.backend
                    .get()
                    .as_mut()
                    .unwrap()
                    .execute(action)
                    .unwrap();
            }
            let execute_dur = tokio::time::Instant::now().duration_since(before);
            self.taken += execute_dur.as_micros() as u64;
            if self.nth_item % 2 == 0 {
                // tokio::time::sleep(tokio::time::Duration::from_micros(
                //     self.sleep.saturating_sub(self.taken),
                // ))
                // .await;
                self.taken = 0;
            }
            self.nth_item += 1;
        }
    }
}
impl RfcommHandle for Handle {
    async fn can_accept(&self, conn: &bluer::rfcomm::ConnectRequest) -> bool {
        true
    }
    async fn handle_stream(&mut self, mut stream: bluer::rfcomm::Stream) {
        let mtu = 8192;
        let mut readbuf = vec![0u8; mtu];

        loop {
            // Read incoming
            match stream.read_buf(&mut readbuf).await {
                Ok(0) => {
                    break;
                }
                Ok(n) => n,
                Err(err) => {
                    println!("{}", err);
                    break;
                }
            };

            while let Some(action) = self.parser.parse(&mut readbuf) {
                self.execute(action).await;
            }
            self.remove_dirty(&mut readbuf);
        }
    }
}
impl Handle {
    pub fn new(backend: Arc<UnsafeCell<InputBackend>>) -> Handle {
        Handle {
            backend,
            parser: ActionParser::new(),
            nth_item: 0,
            sleep: 0,
            taken: 0,
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), String> {
    let input_backend = Arc::new(UnsafeCell::new(InputBackend::new().err_tostring()?));
    let mut rfcomm_server = RfcommServer::new().await?;
    let mut handle = Handle::new(input_backend);

    // let rfcomm_task = tokio::spawn(async move {
    let result = rfcomm_server.listen(&mut handle).await;
    if let Err(err) = result {
        println!("Listen Error: {}", err);
    }
    // });
    // rfcomm_task.await.err_tostring()?;

    Ok(())
}
