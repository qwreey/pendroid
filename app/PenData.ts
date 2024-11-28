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
export type onPenChange = (state: PenData) => void;

const PRESSURE_RESOLUTION = 4096; // FIXME: no hard coding

// Update pen data with gesture event
export function updatePenData(
  pen: PenData,
  screen: Screen,
  event: {
    x: number;
    y: number;
    stylusData?: StylusData;
  },
) {
  const stylusData = event.stylusData;
  pen.x = screen.applyScale(event.x);
  pen.y = screen.applyScale(event.y);
  pen.pressure = Math.round((stylusData?.pressure ?? 0) * PRESSURE_RESOLUTION);
  pen.tiltX = event.stylusData?.tiltX ?? 0;
  pen.tiltY = event.stylusData?.tiltY ?? 0;
}

// Trigger onChange emit if pen data changed
export function triggerPenChange(
  pen: PenData,
  penLast: PenData,
  tps: TPS,
  onPenChange: onPenChange,
) {
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
  pen.tps = tps[1];
  tps[0]++;

  // Emit
  onPenChange(pen);
}
