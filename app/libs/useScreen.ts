import {useWindowDimensions, type LayoutChangeEvent} from 'react-native';
import {useRef} from 'react';

export interface Screen {
  width: number;
  height: number;
  windowWidth: number;
  windowHeight: number;
  scale: number;
  bindLayout: (event: LayoutChangeEvent) => void;
}

export function useScreen(): Screen {
  const screen = useRef({
    bindLayout: (event: LayoutChangeEvent) => {
      const {width, height} = event.nativeEvent.layout;
      screen.height = Math.floor(height * screen.scale);
      screen.width = Math.floor(width * screen.scale);
    },
  } as Screen).current;

  const {
    height: windowHeight,
    width: windowWidth,
    scale,
  } = useWindowDimensions();
  screen.windowHeight = windowHeight;
  screen.windowWidth = windowWidth;
  screen.scale = scale;

  return screen;
}
