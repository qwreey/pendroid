import {type ReactNode} from 'react';
import {requireNativeComponent, type ViewProps} from 'react-native';

export interface StylusEvent {
  button: boolean;
  tiltX: number;
  tiltY: number;
  pressure: number;
  down: boolean;
  x: number;
  y: number;
  hover: boolean;
}

export interface Touch {
  x: number;
  y: number;
  trackingId: number;
  slot: number;
}

export interface FingerEvent {
  touchs: Touch[];
  length: number;
}

export type onStylus = (event: StylusEvent) => void;
export type onFinger = (event: FingerEvent) => void;

export interface MotionViewProps extends ViewProps {
  onStylus?: onStylus;
  onFinger?: onFinger;
  children?: ReactNode;
}

const NativeMotionView = requireNativeComponent('NativeMotionView') as any;

export function MotionView(arg0: MotionViewProps): ReactNode {
  return (
    <NativeMotionView
      {...arg0}
      onFinger={
        arg0.onFinger
          ? (e: any) => {
              arg0.onFinger!(e.nativeEvent);
            }
          : undefined
      }
      onStylus={
        arg0.onStylus
          ? (e: any) => {
              arg0.onStylus!(e.nativeEvent);
            }
          : undefined
      }>
      {arg0.children}
    </NativeMotionView>
  );
}
