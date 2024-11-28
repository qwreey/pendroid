import {type TPS} from './libs/useTPS';
import {type Screen} from './libs/useScreen';

// Touch data
export interface TouchData {
  down: boolean;
  pos: number[];
  tps: number;
}

export type onTouchChange = (state: TouchData) => void;

// Update touch data with gesture event
export function updateTouchData(
  touch: TouchData,
  screen: Screen,
  event: {
    numberOfTouches: number;
    allTouches: {x: number; y: number}[];
  },
) {
  touch.pos = touch.down
    ? event.allTouches.flatMap(p => [
        screen.applyScale(p.x),
        screen.applyScale(p.y),
      ])
    : [];
}

// Trigger onChange emit if pen data changed
export function triggerTouchChange(
  touch: TouchData,
  // touchLast: TouchData,
  tps: TPS,
  onTouchChange: onTouchChange,
) {
  // Increase tps
  touch.tps = tps[1];
  tps[0]++;

  // Emit
  onTouchChange(touch);
}
