import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  useState,
  useEffect,
  createContext,
  useContext,
  useRef,
  type PropsWithChildren,
  useMemo,
} from 'react';

// #region Provider
export type Storage = {
  cachedValues: {
    [key: string]: TransactionState<any> | undefined,
  },
  rerenderTriggers: {
    [key: string]: Array<React.Dispatch<React.SetStateAction<any>>> | undefined,
  },
  waitingReadPromises: {
    [key: string]: Promise<void> | undefined,
  },
  defaultValues: {
    [key: string]: any | undefined,
  },
};

export function StorageProvider({
  defaultValues,
  children,
}: StorageProvider.Params) {
  const storage = useRef({
    cachedValues: {},
    rerenderTriggers: {},
    waitingReadPromises: {},
    defaultValues: defaultValues ?? {},
  } as Storage);
  return (
    <StorageProvider.Context.Provider value={storage.current}>
      {children}
    </StorageProvider.Context.Provider>
  );
}
export namespace StorageProvider {
  export const Context = createContext({
    cachedValues: {},
    rerenderTriggers: {},
    waitingReadPromises: {},
  } as Storage);

  export type Params = PropsWithChildren<{
    defaultValues?: {
      [key: string]: any,
    },
  }>;
}
// #endregion Provider

// #region Transaction
export type TransactionState<Value> = {
  running: boolean,
  result?: Value,
  errored?: boolean,
};

export class TransactionHandler<Value> {
  private storage: Storage;
  private key: string;
  constructor(storage: Storage, key: string) {
    this.storage = storage;
    this.key = key;
  }

  private triggerRerender() {
    const triggers = this.storage.rerenderTriggers[this.key];
    if (!triggers) return;
    for (const rerender of triggers) rerender({});
  }

  private updateCache(transaction: TransactionState<Value>) {
    this.storage.cachedValues[this.key] = transaction;
  }

  public read(): TransactionState<Value> {
    // Check cached value first
    let cachedValue = this.storage.cachedValues[this.key];
    if (cachedValue) return cachedValue;

    // If not loaded, call getItem
    this.updateCache(cachedValue = {running: true});
    this.storage.waitingReadPromises[this.key] =
      AsyncStorage.getItem(this.key)
      .then(value => {
        // Successfully loaded
        this.updateCache({
          running: false,
          result: value
            ? JSON.parse(value)
            : this.storage.defaultValues[this.key],
          errored: false,
        });
      })
      .catch(error => {
        // Failed
        this.updateCache({
          running: false,
          result: error,
          errored: true,
        });
      })
      .finally(() => {
        // Trigger all renders
        delete this.storage.waitingReadPromises[this.key];
        this.triggerRerender();
      });

    return cachedValue;
  }

  public write(value: Value, errorHandler: (error: string) => void) {
    // call setItem to save
    AsyncStorage.setItem(this.key, JSON.stringify(value))
      .then(() => {
        // Successfully loaded update value and trigger all renders
        this.updateCache({
          running: false,
          result: value,
          errored: false,
        });
        this.triggerRerender();
      })
      .catch(
        // Failed
        errorHandler
        || (error => {
          console.error(error);
        }),
      );
  }
};
// #endregion Transaction

export function useStorage<Value>(key: string): TransactionHandler<Value> {
  const storage = useContext(StorageProvider.Context);
  const [_, updateTrigger] = useState({});

  // Add rerender trigger
  useEffect(() => {
    let updateTriggers = storage.rerenderTriggers[key];
    if (updateTriggers === undefined) {
      updateTriggers = storage.rerenderTriggers[key] = [];
    }
    updateTriggers.push(updateTrigger);
    return () => {
      updateTriggers.splice(updateTriggers.indexOf(updateTrigger), 1);
    };
  }, []);

  // Create transaction handler
  const handler = useMemo(() => new TransactionHandler<Value>(storage, key), []);

  return handler;
}
