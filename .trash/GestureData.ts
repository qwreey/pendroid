import {type TPS} from './libs/useTPS';
import {type Screen} from './libs/useScreen';

// Touch data
export interface TouchData {
  x: number;
  y: number;
  tps: number;
  touchCount: number;
  maxTouchCount: number;
  startX: number;
  startY: number;
  isStart: boolean;
  isEnd: boolean;
  currentGesture: number; // 2 : scroll, 3: (undefined)
  upgradeFrom: number;
  velocityX: number;
  velocityY: number;
  down: boolean;
}

export type onTouchChange = (state: TouchData) => void;

function getRadius(x: number, y: number) {
  return Math.sqrt(x * x + y * y);
}

const MIN_MOVES = [-1, 0, 16];

// Update touch data with gesture event
export function updateTouchData(
  touch: TouchData,
  screen: Screen,
  event: {
    numberOfPointers: number;
    velocityX: number;
    velocityY: number;
    x: number;
    y: number;
  },
  down: boolean,
) {
  const x = (touch.x = screen.applyScale(event.x));
  const y = (touch.y = screen.applyScale(event.y));
  const touchCount = down ? (touch.touchCount = event.numberOfPointers) : 0;
  touch.isStart = false;
  touch.isEnd = false;
  touch.upgradeFrom = -1;
  if (touchCount === 0) {
    touch.maxTouchCount = 0;
    touch.isEnd = true;
    touch.currentGesture = 0;
  } else if (touch.maxTouchCount < touchCount) {
    touch.maxTouchCount = touchCount;
    touch.startX = x;
    touch.startY = y;
  }
  // Check gesture upgrade
  let minMove = MIN_MOVES[touchCount];
  if (
    minMove !== undefined &&
    touchCount !== 0 &&
    touchCount > touch.currentGesture &&
    getRadius(Math.abs(touch.startX - x), Math.abs(touch.startY - y)) > minMove
  ) {
    touch.upgradeFrom = touch.currentGesture;
    touch.currentGesture = touchCount;
    touch.isStart = true;
    console.log('up');
  }
  touch.velocityX = Math.round(event.velocityX);
  touch.velocityY = Math.round(event.velocityY);
}

// Trigger onChange emit if pen data changed
export function triggerTouchChange(
  touch: TouchData,
  touchLast: TouchData,
  tps: TPS,
  onTouchChange: onTouchChange,
) {
  const {x: lastX, y: lastY, currentGesture: lastGesture} = touch;
  const {touchCount, x, y, currentGesture} = touchLast;

  // No Diff, ignore
  if (currentGesture == lastGesture && x == lastX && y == lastY) {
    return;
  }

  // Save last input for diff
  touchLast.touchCount = touchCount;
  touchLast.x = x;
  touchLast.y = y;

  // Increase tps
  touch.tps = tps[1];
  tps[0]++;

  // Emit
  onTouchChange(touch);
}
