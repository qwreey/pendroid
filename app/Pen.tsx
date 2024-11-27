import {StyleSheet, View, ViewStyle} from 'react-native';
import React, {useMemo, useRef} from 'react';
import {
  GestureDetector,
  Gesture,
  PointerType,
} from 'react-native-gesture-handler';
import {type StylusData} from 'react-native-gesture-handler/lib/typescript/web/interfaces';

import {type TPS} from './libs/useTPS';
import {type Screen} from './libs/useScreen';

// Pen data type
export interface PenData {
  x: number;
  y: number;
  pressure: number;
  down: boolean;
  hover: boolean;
  tps: number;
  tiltX: number;
  tiltY: number;
}
export type onChange = (state: PenData) => void;

const PRESSURE_RESOLUTION = 4096; // FIXME: no hard coding

// Update pen data with gesture event
function updatePenData(
  pen: PenData,
  screen: Screen,
  event: {
    x: number;
    y: number;
    stylusData?: StylusData;
  },
) {
  const stylusData = event.stylusData;
  pen.x = Math.round(event.x * screen.scale);
  pen.y = Math.round(event.y * screen.scale);
  pen.pressure = Math.round((stylusData?.pressure ?? 0) * PRESSURE_RESOLUTION);
  pen.tiltX = event.stylusData?.tiltX ?? 0;
  pen.tiltY = event.stylusData?.tiltY ?? 0;
}

// Trigger onChange emit if pen data changed
function change(pen: PenData, penLast: PenData, tps: TPS, onChange: onChange) {
  const {
    hover: lastHover,
    pressure: lastPressure,
    x: lastX,
    y: lastY,
    down: lastDown,
  } = penLast;
  const {hover, pressure, x, y, down} = pen;

  // No Diff, ignore
  if (
    pressure == lastPressure &&
    x == lastX &&
    y == lastY &&
    lastDown == down &&
    lastHover == hover
  ) {
    return;
  }

  // Save last input for diff
  penLast.pressure = pressure;
  penLast.x = x;
  penLast.y = y;
  penLast.down = down;
  penLast.hover = hover;

  // Increase tps
  penLast.tps = pen.tps = tps[1];
  tps[0]++;

  // Emit
  onChange(pen);
}

// Pen handle view
export function PenHandle({
  tps,
  screen,
  onChange,
}: {
  tps: TPS;
  screen: Screen;
  onChange: onChange;
}) {
  const pen = useRef({
    down: false,
  } as PenData).current;
  const penLast = useRef({} as PenData).current;

  // Gesture handle
  const gesture = useMemo(() => {
    // Moving (Down)
    const pan = Gesture.Pan();
    pan.onUpdate(e => {
      if (!e.stylusData) return;
      updatePenData(pen, screen, e);
      change(pen, penLast, tps, onChange);
    });
    pan.onBegin(e => {
      if (!e.stylusData) return;
      updatePenData(pen, screen, e);
      pen.down = true;
      pen.hover = true;
      change(pen, penLast, tps, onChange);
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
      change(pen, penLast, tps, onChange);
    });
    hover.onTouchesDown(e => {
      if (e.pointerType !== PointerType.STYLUS) return;
      pen.down = true;
    });
    hover.onTouchesUp(e => {
      if (e.pointerType !== PointerType.STYLUS) return;
      pen.down = false;
    });
    hover.onTouchesCancelled(e => {
      if (e.pointerType !== PointerType.STYLUS) return;
      pen.down = false;
    });
    hover.onBegin(() => {
      pen.hover = true;
    });
    hover.onEnd(() => {
      pen.hover = false;
      change(pen, penLast, tps, onChange);
    });
    hover.runOnJS(true);

    return Gesture.Simultaneous(pan, hover);
  }, [onChange]);

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
