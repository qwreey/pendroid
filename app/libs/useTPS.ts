import {useEffect, useRef} from 'react';

export type TPS = [number, number];

export function useTPS(): TPS {
  const tps = useRef([0, 0]).current as TPS;

  // TPS
  useEffect(() => {
    const timer = setInterval(() => {
      tps[1] = tps[0];
      tps[0] = 0;
    }, 1000);

    return () => {
      clearInterval(timer);
    };
  }, []);

  return tps;
}
