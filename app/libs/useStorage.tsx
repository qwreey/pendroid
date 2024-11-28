import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  useState,
  useEffect,
  createContext,
  useContext,
  useRef,
  type PropsWithChildren,
} from 'react';

export type TransactionState<U> = {
  running: boolean;
  result?: U;
  errored?: boolean;
};

export type ValueHandler = {
  prefetch(): undefined;
  read(): TransactionState<any>;
  write(
    value: any,
    errorHandler?: (error: string) => undefined,
  ): TransactionState<string | undefined>;
};

export type Storage = {
  cachedValues: {
    [key: string]: TransactionState<any>;
  };
  updateTriggers: {
    [key: string]: Array<React.Dispatch<React.SetStateAction<any>>>;
  };
  waitingReadPromises: {
    [key: string]: Promise<object> | undefined;
  };
  defaultValues: {
    [key: string]: any;
  };
};

export const StorageContext = createContext({
  cachedValues: {},
  updateTriggers: {},
  waitingReadPromises: {},
} as Storage);

interface StorageProviderProps extends PropsWithChildren {
  defaultValues?: {
    [key: string]: any;
  };
}

export function StorageProvider({
  defaultValues,
  children,
}: StorageProviderProps) {
  const storage = useRef({
    cachedValues: {},
    updateTriggers: {},
    waitingReadPromises: {},
    defaultValues: defaultValues ?? {},
  } as Storage);
  return (
    <StorageContext.Provider value={storage.current}>
      {children}
    </StorageContext.Provider>
  );
}

export function useStorage(key: string) {
  const storageCache = useContext(StorageContext);
  const [_, updateTrigger] = useState({});

  // Add update trigger
  useEffect(() => {
    let updateTriggers = storageCache.updateTriggers[key];
    if (updateTriggers === undefined)
      updateTriggers = storageCache.updateTriggers[key] = [];
    updateTriggers.push(updateTrigger);
    return () => {
      updateTriggers.splice(updateTriggers.indexOf(updateTrigger), 1);
    };
  }, []);

  // Create value handler
  const valueHandler = useRef(null as unknown as ValueHandler);
  if (!valueHandler.current)
    valueHandler.current = {
      read() {
        // load cached value first
        let value = storageCache.cachedValues[key];
        if (value) return value;

        // if not loaded, call getItem
        if (storageCache.waitingReadPromises[key]) return {running: true};
        AsyncStorage.getItem(key)
          .then(value => {
            // successfully loaded
            storageCache.cachedValues[key] = {
              running: false,
              result: value
                ? JSON.parse(value)
                : storageCache.defaultValues[key],
              errored: false,
            };
          })
          .catch(error => {
            // failed
            storageCache.cachedValues[key] = {
              running: false,
              result: error,
              errored: true,
            };
          })
          .finally(() => {
            // trigger all renders
            if (storageCache.updateTriggers[key])
              for (const trigger of storageCache.updateTriggers[key])
                trigger({});
          });

        return {running: true};
      },
      write(value, errorHandler) {
        // call setItem to save
        AsyncStorage.setItem(key, JSON.stringify(value))
          .then(() => {
            // successfully loaded update value and trigger all renders
            storageCache.cachedValues[key] = {
              running: false,
              result: value,
              errored: false,
            };
            if (storageCache.updateTriggers[key])
              for (const trigger of storageCache.updateTriggers[key])
                trigger({});
          })
          .catch(
            // failed
            errorHandler ||
              (error => {
                console.error(error);
              }),
          );
      },
    } as ValueHandler;

  return valueHandler.current;
}
