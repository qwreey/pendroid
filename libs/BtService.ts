import { useEffect, useState } from "react";
import {NativeModules, PermissionsAndroid} from "react-native";
const {BtService: BtServiceRaw} = NativeModules;

export class BtService {
    public static ensureBluetooth() {
        BtServiceRaw.ensureBluetooth()
    }

    public static searchPaired(): { address: string, name: string }[] {
        return BtServiceRaw.searchPaired();
    }

    public static useBluetoothPermission(): boolean {
        const [granted, setGranted] = useState(false);

        useEffect(()=>{
            try {
                PermissionsAndroid.requestMultiple([
                    PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT
                ]).then((result)=>{
                    if (result['android.permission.BLUETOOTH_CONNECT'] == 'granted') {
                        setGranted(true);
                    }
                });
            } catch (err) {
                console.warn(err);
            }
        }, []);

        return granted
    }
}
