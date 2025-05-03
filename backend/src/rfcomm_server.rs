use bluer::{
    agent::{Agent, AgentHandle},
    rfcomm::{ConnectRequest, Profile, ProfileHandle, Role, Stream},
    Session, Uuid,
};
use qwreey_utility_rs::ErrToString;
use tokio_stream::StreamExt;

pub trait RfcommHandle {
    async fn can_accept(&self, conn: &ConnectRequest) -> bool;
    async fn handle_stream(&mut self, stream: Stream);
}

pub struct RfcommServer {
    session: Session,
    profile: ProfileHandle,
    agent: AgentHandle,
}
impl RfcommServer {
    pub async fn new() -> Result<RfcommServer, String> {
        let session = Session::new().await.err_tostring()?;

        let agent = session
            .register_agent(Agent::default())
            .await
            .err_tostring()?;

        let profile = session
            .register_profile(Profile {
                uuid: Uuid::parse_str("09c8e685-c2ca-49f3-98d5-2bacd8670a16").unwrap(),
                name: Some(String::from("pendroid")),
                role: Some(Role::Server),
                channel: Some(21),
                require_authentication: Some(true),
                require_authorization: Some(true),
                auto_connect: Some(true),
                ..Default::default()
            })
            .await
            .err_tostring()?;

        Ok(RfcommServer {
            session,
            profile,
            agent,
        })
    }

    pub async fn listen(&mut self, handle: &mut impl RfcommHandle) -> Result<(), String> {
        while let Some(conn) = self.profile.next().await {
            if !handle.can_accept(&conn).await {
                continue;
            }

            match conn.accept().err_tostring() {
                Err(err) => {
                    println!("Connection Accept Error: {}", err);
                }
                Ok(stream) => {
                    handle.handle_stream(stream).await;
                }
            }
        }

        Ok(())
    }
}
