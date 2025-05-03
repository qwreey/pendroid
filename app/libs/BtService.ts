import {NativeModules, PermissionsAndroid} from "react-native";
const {BtService: BtServiceRaw} = NativeModules;

export class BtService {
    public static ensureBluetooth() {
        BtServiceRaw.ensureBluetooth()
    }

    public static searchPaired(): { address: string, name: string }[] {
        return BtServiceRaw.searchPaired();
    }

    public static async requestPermission() {
        try {
            const granted = await PermissionsAndroid.requestMultiple([
                PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT
            ]).then((result)=>{
                console.log(result['android.permission.BLUETOOTH_CONNECT'])
            })
        } catch (err) {
            console.warn(err);
        }
    }
}
