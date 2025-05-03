import {StatusBar} from 'react-native';
import SystemNavigationBar from 'react-native-system-navigation-bar';
import KeepAwake from '@sayem314/react-native-keep-awake';
import React, {useCallback, useEffect} from 'react';

import { BtService } from './libs/BtService';
import BtHIDService from './libs/BtHIDService';
import { StorageProvider, useStorage } from './libs/useStorage';
import { useAppState } from './libs/useAppState';
import { FingerEvent, MotionView, StylusEvent } from './libs/MotionView';

export function App() {
  const appState = useAppState();
  const bluetoothGranted = BtService.useBluetoothPermission();

  // Init HID service
  useEffect(()=>{
    if (appState != 'active') return;
    if (!bluetoothGranted) return;
    BtHIDService.initService();
  }, [appState, bluetoothGranted]);

  // Send stylus input
  const onStylus = useCallback((stylus: StylusEvent) => {
    BtHIDService.writeStylus(stylus);
  }, []);

  // Send finger input
  const onFinger = useCallback((finger: FingerEvent) => {
    BtHIDService.writeFinger(finger);
  }, []);

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
