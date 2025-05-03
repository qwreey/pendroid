import { useEffect, useState } from "react";
import { AppState } from "react-native";

export function useAppState(): typeof AppState.currentState {
    const [appState, setAppState] = useState<typeof AppState.currentState>(AppState.currentState);
    useEffect(() => {
        const subscription = AppState.addEventListener('change', nextAppState => {
            setAppState(nextAppState);
        });
        
        return () => {
            subscription.remove();
        };
    }, []);

    return appState;
}
