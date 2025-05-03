import {NativeModules, NativeEventEmitter, EmitterSubscription} from "react-native";
const {BtHIDService: HIDServiceRaw} = NativeModules;
import { StylusEvent } from "./MotionView";
import { Buffer } from "react-native-buffer";

const eventEmitter = new NativeEventEmitter(HIDServiceRaw);

export enum InputId {
    ID_STYLUS = 1,
}

export default class BtHIDService {
    public static connectTo(address: string) {
        HIDServiceRaw.connectTo(address);
    }

    // SEE: https://learn.microsoft.com/en-us/windows-hardware/design/component-guidelines/windows-pen-states
    private static buttonActivated: boolean = false;
    private static writeDropStylusState(data: StylusEvent) {
        const buf = Buffer.alloc(11);
        let offset = 0;

        offset = buf.writeUInt8(
            0, offset
        );
        offset = buf.writeInt16LE(data.pressure, offset);
        offset = buf.writeInt16LE(data.tiltX * 100, offset);
        offset = buf.writeInt16LE(data.tiltY * 100, offset);
        offset = buf.writeInt16LE(data.x, offset);
        offset = buf.writeInt16LE(data.y, offset);

        this.writeReport(buf, InputId.ID_STYLUS);
    }
    public static writeStylus(data: StylusEvent) {
        const buf = Buffer.alloc(11);
        let offset = 0;

        // Update side button state
        if (data.button) {
            if (!data.down && !this.buttonActivated) {
                this.buttonActivated = true;
                this.writeDropStylusState(data);
            }
        } else {
            if (!data.down && this.buttonActivated) {
                this.buttonActivated = false;
                this.writeDropStylusState(data);
            }
        }

        // Write tip / eraser / range state
        offset = buf.writeUInt8(
            (data.down ? 0b0000_0001 : 0) // (Tip Switch)
            | (this.buttonActivated ? 0b0000_0010 : 0) // (Eraser Switch)
            | ((!this.buttonActivated && data.hover) ? 0b0000_0100 : 0), // (In Range)
            offset
        );

        // Write abs datas
        offset = buf.writeInt16LE(data.pressure, offset);
        offset = buf.writeInt16LE(data.tiltX * 100, offset);
        offset = buf.writeInt16LE(data.tiltY * 100, offset);
        offset = buf.writeInt16LE(data.x, offset);
        offset = buf.writeInt16LE(data.y, offset);

        this.writeReport(buf, InputId.ID_STYLUS);
    }

    public static writeReport(data: Buffer, inputId: InputId) {
        HIDServiceRaw.writeReport(data.toString('hex'), inputId);
    }

    public static initService() {
        HIDServiceRaw.initService();
    }

    public static dropService() {
        HIDServiceRaw.dropService();
    }
}
