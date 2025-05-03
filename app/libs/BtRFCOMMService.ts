import {NativeModules, NativeEventEmitter} from "react-native";
const {BtService: BtServiceRaw} = NativeModules;
import { FieldType, Packer } from "./Packer";

const LenPacker = new Packer(114, [ FieldType.UInt8 ]);

const eventEmitter = new NativeEventEmitter(BtServiceRaw);
let state: number = 0;

const STATE_CONNECTING = 2;
const STATE_CONNECTED = 3;
const STATE_FAILED = 4;
const STATE_DATA = 5;
const STATE_READY = 6;
const STATE_DISCONNECTED = 7;

eventEmitter.addListener("onStateChanged", (data)=>{
    state = data;
});

let buffer: string[] = [];
setInterval(()=>{
    const len = buffer.length;
    if (len == 0) return;
    buffer.push(LenPacker.pack([ len ]));
    const take = buffer.join("");
    buffer = [];
    if (state == STATE_READY) BtServiceRaw.write(take);
}, 1000 / 28);

export class BtRFCOMMService {
    public static connectTo(address: string) {
        BtServiceRaw.connectTo(address);
    }

    public static write(text: string) {
        buffer.push(text)
    }
}
