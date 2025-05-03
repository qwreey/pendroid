import {AppState, StatusBar} from 'react-native';
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
  useAppState,
} from './libs';
// import { BtService } from './libs/BtService';
import BtHIDService from './libs/BtHIDService';
import { BtRFCOMMService } from './libs/BtRFCOMMService';

export function App() {
  const method = useStorage("method");
  const appState = useAppState();

  // Init HID service if using hid method
  useEffect(()=>{
    if (appState != 'active') return;
    if (method.read().running) return;
    if (method.read().result == 'bluetooth-hid') {
      BtHIDService.initService();
    } else {
      BtHIDService.dropService();
    }
  }, [appState, method.read().running, method.read().result]);

  // Send pen input
  const onStylus = useCallback(
    (stylus: StylusEvent) => {
      switch (method.read().result) {
        case 'bluetooth-hid':
          BtHIDService.writeStylus(stylus);
          break;
        case 'bluetooth-rfcomm':
          BtRFCOMMService.write(PackStylusData(stylus))
          break;
      }
    },
    [],
  );

  // Send stylus input
  const onFinger = useCallback(
    (finger: FingerEvent) => {
        // BtService.write(PackFingerData(finger));
    },
    [],
  );

  // // Open ws
  // const {
  //   sendMessage,
  //   lastMessage: _lastMessage,
  //   readyState,
  // } = useWebSocket('ws://localhost:57362', {
  //   shouldReconnect: _closeEvent => true,
  //   reconnectAttempts: Infinity,
  //   reconnectInterval: 2000,
  // });

  // // Send pen input
  // const onStylus = useCallback(
  //   (stylus: StylusEvent) => {
  //     if (readyState == ReadyState.OPEN) {
  //       sendMessage(PackStylusData(stylus));
  //     }
  //   },
  //   [readyState],
  // );

  // // Send stylus input
  // const onFinger = useCallback(
  //   (finger: FingerEvent) => {
  //     if (readyState == ReadyState.OPEN) {
  //       sendMessage(PackFingerData(finger));
  //     }
  //   },
  //   [readyState],
  // );

  // Send screen update
  // useEffect(() => {
  //   // console.log('try');
  //   if (readyState == ReadyState.OPEN) {
  //     // console.log('open');
  //     sendMessage(`V${screen.width};${screen.height}`);
  //   }
  // }, [readyState]);

  return (
    <MotionView
      style={{width: '100%', height: '100%', backgroundColor: 'black'}}
      onStylus={onStylus}
      onFinger={onFinger}
    />
  );
}

export default function Root() {
  // Nav bar hide
  useEffect(() => {
    SystemNavigationBar.navigationHide();
  }, []);

  return (
    <StorageProvider defaultValues={{
      method: "bluetooth-hid",
    }}>
      <StatusBar hidden={true} />
      <KeepAwake />
      <App/>
    </StorageProvider>
  );
}
