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
    private static eraserActivated: boolean = false;
    private static barrelActivated: boolean = false;
    private static lastButtonState: boolean = false;
    private static lastButtonStartTimestamp: number = -1;
    private static barrelTimeout = 420;
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
        this.dropAllTouchs()

        const buf = Buffer.alloc(11);
        let offset = 0;

        // Update barrel state
        if (!this.lastButtonState && data.button) {
            if (this.lastButtonStartTimestamp == -1) {
                // First clicking
                this.lastButtonStartTimestamp = data.timestamp;
            } else if (data.timestamp - this.lastButtonStartTimestamp < this.barrelTimeout) {
                // Second clicking
                this.barrelActivated = true;
                this.lastButtonStartTimestamp = -1;
            } else {
                // Timeout
                this.lastButtonStartTimestamp = data.timestamp;
            }
        }
        this.lastButtonState = data.button;
        if (!data.hover) {
            this.barrelActivated = false;
            this.lastButtonStartTimestamp = -1;
            this.lastButtonState = false;
        }
        if (!data.button) {
            this.barrelActivated = false;
        }
        if (data.down) {
            this.lastButtonStartTimestamp = -1;
        }

        // Update eraser state when only not down
        if (data.button) {
            if (!data.down && !this.eraserActivated && !this.barrelActivated) {
                this.eraserActivated = true;
                this.writeDropStylusState(data);
            }
        } else {
            if (!data.down && this.eraserActivated) {
                this.eraserActivated = false;
                this.writeDropStylusState(data);
            }
        }

        // Write tip / eraser / range state
        offset = buf.writeUInt8(
            (data.down ? 0b0000_0001 : 0) // (Tip Switch)
            | (this.eraserActivated ? 0b0000_0010 : 0) // (Eraser Switch)
            | ((!this.eraserActivated && data.hover) ? 0b0000_0100 : 0) // (In Range)
            | (this.barrelActivated ? 0b0000_1000 : 0), // (Barrel Button)
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
    private static dropAllTouchs() {
        this.lastTouchs.forEach((touch, slot)=>{
            if (touch == null) return;

            const buf = Buffer.alloc(6);
            let offset = 0;

            offset = buf.writeUInt8(
                0 | slot << 2,
                offset
            );

            offset = buf.writeInt16LE(touch.x, offset);
            offset = buf.writeInt16LE(touch.y, offset);
            offset = buf.writeUInt8(0, offset);

            this.lastTouchs[slot] = null;

            this.writeReport(buf, InputId.ID_TOUCHPAD);
        })
    }
    private static writeTouch(touch: Touch, len: number, button: number) {
        const buf = Buffer.alloc(6);
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
            // no diff
            const old = this.lastTouchs[touch.slot];
            if (old?.x == touch.x && old?.y == touch.y) {
                return;
            }
            offset = buf.writeInt16LE(touch.x, offset);
            offset = buf.writeInt16LE(touch.y, offset);
        }

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
