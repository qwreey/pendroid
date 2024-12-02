import {StatusBar} from 'react-native';
import SystemNavigationBar from 'react-native-system-navigation-bar';
import KeepAwake from '@sayem314/react-native-keep-awake';
import React, {useCallback, useEffect} from 'react';
import useWebSocket, {ReadyState} from 'react-native-use-websocket';

import {PackFingerData, PackStylusData} from './data';
import {
  useScreen,
  useStorage,
  StorageProvider,
  MotionView,
  StylusEvent,
  FingerEvent,
} from './libs';

export default function App() {
  const screen = useScreen();
  useStorage;

  // Open ws
  const {
    sendMessage,
    lastMessage: _lastMessage,
    readyState,
  } = useWebSocket('ws://localhost:57362', {
    shouldReconnect: _closeEvent => true,
    reconnectAttempts: Infinity,
    reconnectInterval: 2000,
  });

  // Send pen input
  const onStylus = useCallback(
    (stylus: StylusEvent) => {
      if (readyState == ReadyState.OPEN) {
        sendMessage(PackStylusData(stylus));
      }
    },
    [readyState],
  );

  // Send stylus input
  const onFinger = useCallback(
    (finger: FingerEvent) => {
      if (readyState == ReadyState.OPEN) {
        sendMessage(PackFingerData(finger));
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
      <StatusBar hidden={true} />
      <KeepAwake />
      <MotionView
        style={{width: '100%', height: '100%', backgroundColor: 'black'}}
        onStylus={onStylus}
        onFinger={onFinger}
      />
    </StorageProvider>
  );
}
