import {StatusBar} from 'react-native';
import SystemNavigationBar from 'react-native-system-navigation-bar';
import KeepAwake from '@sayem314/react-native-keep-awake';
import React, {useCallback, useEffect} from 'react';
import {GestureHandlerRootView} from 'react-native-gesture-handler';
import useWebSocket, {ReadyState} from 'react-native-use-websocket';

import {type PenData} from './PenData';
import {GestureHandle} from './GestureHandle';
import {useTPS, useScreen, useStorage, StorageProvider} from './libs';
import {TouchData} from './TouchData';

export default function App() {
  const tps = useTPS();
  const screen = useScreen();
  useStorage;

  // Open ws
  const {sendMessage, lastMessage, readyState} = useWebSocket(
    'ws://localhost:57362',
    {
      shouldReconnect: closeEvent => true,
      reconnectAttempts: Infinity,
      reconnectInterval: 2000,
    },
  );

  // Send pen input
  const onPenChange = useCallback(
    (pen: PenData) => {
      if (readyState == ReadyState.OPEN) {
        sendMessage(
          `${pen.hover ? (pen.down ? 'D' : 'U') : 'O'}${pen.pressure};${
            pen.tps
          };${pen.x};${pen.y};${pen.tiltX};${pen.tiltY}`,
        );
      }
    },
    [readyState],
  );

  // Send pen input
  const onTouchChange = useCallback(
    (touch: TouchData) => {
      if (readyState == ReadyState.OPEN) {
        sendMessage(`T${touch.pos.join(';')}`);
      }
    },
    [readyState],
  );

  // Send screen update
  useEffect(() => {
    console.log('try');
    if (readyState == ReadyState.OPEN) {
      console.log('open');
      sendMessage(`V${screen.width};${screen.height}`);
    }
  }, [readyState]);

  // Nav bar hide
  useEffect(() => {
    SystemNavigationBar.navigationHide();
  }, []);

  return (
    <StorageProvider>
      <GestureHandlerRootView onLayout={screen.bindLayout}>
        <StatusBar hidden={true} />
        <KeepAwake />
        <GestureHandle
          onTouchChange={onTouchChange}
          onPenChage={onPenChange}
          tps={tps}
          screen={screen}
        />
      </GestureHandlerRootView>
    </StorageProvider>
  );
}
