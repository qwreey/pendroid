import {BoolToStr, StylusEvent} from '../libs';

// S(hover TF);(down TF);(button TF);(x int);(y int);(tiltX int);(tiltY int);(pressure int)

export function PackStylusData(pen: StylusEvent): string {
  return `S${BoolToStr(pen.hover)};${BoolToStr(pen.down)};${BoolToStr(
    pen.button,
  )};${pen.x};${pen.y};${pen.tiltX};${pen.tiltY};${pen.pressure}`;
}
