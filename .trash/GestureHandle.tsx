import {StyleSheet, View, ViewStyle} from 'react-native';
import React, {useMemo, useRef} from 'react';
import {
  GestureDetector,
  Gesture,
  PointerType,
} from 'react-native-gesture-handler';

import {
  triggerPenChange,
  updatePenData,
  PenData,
  type onPenChange,
} from './data/StylusData';
import {
  triggerTouchChange,
  updateTouchData,
  TouchData,
  type onTouchChange,
} from './data/FingerData';
import {type Screen, type TPS} from './libs';

// Pen handle view
export function GestureHandle({
  tps,
  screen,
  onPenChage,
  onTouchChange,
}: {
  tps: TPS;
  screen: Screen;
  onPenChage: onPenChange;
  onTouchChange: onTouchChange;
}) {
  const {pen, penLast, touch} = useRef({
    pen: {down: false} as PenData,
    penLast: {} as PenData,
    touch: {} as TouchData,
  }).current;

  // Gesture handle
  const gesture = useMemo(() => {
    // Moving (Down)
    const pan = Gesture.Pan();
    pan.onUpdate(e => {
      if (!e.stylusData) return;
      updatePenData(pen, screen, e);
      triggerPenChange(pen, penLast, tps, onPenChage);
    });
    pan.onBegin(e => {
      if (!e.stylusData) return;
      pen.down = true;
      pen.hover = true;
      updatePenData(pen, screen, e);
      triggerPenChange(pen, penLast, tps, onPenChage);
    });
    pan.activeOffsetX(0);
    pan.activeOffsetY(0);
    pan.minVelocityX(0);
    pan.minVelocityY(0);
    pan.runOnJS(true);

    // Moving (Hover)
    const hover = Gesture.Hover();
    hover.onUpdate(e => {
      if (pen.down) return;
      updatePenData(pen, screen, e);
      triggerPenChange(pen, penLast, tps, onPenChage);
    });
    hover.onTouchesDown(e => {
      if (e.pointerType === PointerType.STYLUS) {
        pen.down = true;
        return;
      }
      touch.down = true;
      updateTouchData(touch, screen, e);
      triggerTouchChange(touch, tps, onTouchChange);
    });
    hover.onTouchesUp(e => {
      if (e.pointerType === PointerType.STYLUS) {
        pen.down = false;
        return;
      }
      touch.down = false;
      updateTouchData(touch, screen, e);
      triggerTouchChange(touch, tps, onTouchChange);
    });
    hover.onTouchesMove(e => {
      if (e.pointerType === PointerType.STYLUS) {
        return;
      }
      updateTouchData(touch, screen, e);
      triggerTouchChange(touch, tps, onTouchChange);
    });
    hover.onBegin(() => {
      pen.hover = true;
    });
    hover.onEnd(() => {
      pen.hover = false;
      triggerPenChange(pen, penLast, tps, onPenChage);
    });
    hover.runOnJS(true);

    return Gesture.Simultaneous(pan, hover);
  }, [onPenChage, onTouchChange]);

  return (
    <GestureDetector gesture={gesture}>
      <View onLayout={screen.bindLayout} style={styles.penView} />
    </GestureDetector>
  );
}

const styles = StyleSheet.create({
  penView: {
    backgroundColor: 'black',
    height: '100%',
    width: '100%',
  } as ViewStyle,
});
