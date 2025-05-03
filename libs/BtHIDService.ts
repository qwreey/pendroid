import {NativeModules, NativeEventEmitter, EmitterSubscription} from "react-native";
const {BtHIDService: HIDServiceRaw} = NativeModules;
import { FingerEvent, StylusEvent, Touch } from "./MotionView";
import { Buffer } from "react-native-buffer";

const eventEmitter = new NativeEventEmitter(HIDServiceRaw);

export enum InputId {
    ID_STYLUS = 1,
    ID_TOUCHPAD = 2,
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

    private static lastTouchs: (Touch|null)[] = [ null, null, null, null ];
    private static writeTouch(touch: Touch, len: number, button: number) {
        const buf = Buffer.alloc(7);
        let offset = 0;

        // tip and slot
        offset = buf.writeUInt8(
            1 | (touch.x != -1 ? 0b0010 : 0) | touch.slot << 2,
            offset
        );

        // if up, use last pos
        if (touch.x == -1) {
            offset = buf.writeInt16LE(this.lastTouchs[touch.slot]?.x ?? 0, offset);
            offset = buf.writeInt16LE(this.lastTouchs[touch.slot]?.y ?? 0, offset);
        } else {
            offset = buf.writeInt16LE(touch.x, offset);
            offset = buf.writeInt16LE(touch.y, offset);
        }

        // press button
        offset = buf.writeUInt8(1 << (Math.max(button, 3) - 1), offset);

        // update length
        offset = buf.writeUInt8(len, offset);


        this.lastTouchs[touch.slot] = touch;

        this.writeReport(buf, InputId.ID_TOUCHPAD);
    }
    public static writeFinger(data: FingerEvent) {
        let button = 0;
        for (const touch of data.touchs) {
            if (touch.x == -1) continue;
            button++;
        }

        for (const touch of data.touchs) {
            if (touch.slot > 4) continue;
            this.writeTouch(touch, data.length, button);
        }
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
