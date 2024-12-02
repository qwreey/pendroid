import {FingerEvent} from '../libs';

// F(length int);((x int);(y int);(slot int);(trackingId int))

export function PackFingerData(finger: FingerEvent): string {
  return `F${finger.length};${finger.touchs
    .flatMap(touch => {
      return [touch.x, touch.y, touch.slot, touch.trackingId];
    })
    .join(';')}`;
}
