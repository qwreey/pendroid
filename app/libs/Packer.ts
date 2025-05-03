import { Buffer } from "react-native-buffer";

export enum FieldType {
    UInt8,
    Int8,
    UInt16,
    Int32,
    UInt32,
}

export class Packer {
    private len: number;
    private struct: FieldType[];
    private id: number;
    constructor(id: number, struct: FieldType[]) {
        this.len = struct.reduce((prev: number, item: FieldType): number => {
            switch (item) {
                case FieldType.UInt16:
                    return prev + 2;
                case FieldType.UInt8:
                case FieldType.Int8:
                    return prev + 1;
                case FieldType.Int32:
                case FieldType.UInt32:
                    return prev + 4;
            }
        }, 2);
        this.id = id;
        this.struct = struct;
    }

    public pack(values: any[]): string {
        const buf = Buffer.alloc(this.len);
        let offset = buf.writeUInt8(0b11111111, 0);
        offset = buf.writeUInt8(this.id, offset);
        for (const index in values) {
            const fieldType = this.struct[index];
            switch (fieldType) {
                case FieldType.UInt16:
                    offset = buf.writeUInt16BE(values[index], offset);
                    break;
                case FieldType.Int8:
                    offset = buf.writeInt8(values[index], offset);
                    break;
                case FieldType.UInt8:
                    offset = buf.writeUInt8(values[index], offset);
                    break;
                case FieldType.Int32:
                    offset = buf.writeInt32BE(values[index], offset);
                    break;
                case FieldType.UInt32:
                    offset = buf.writeUInt32BE(values[index], offset);
                    break;
            }
        }
        return buf.toString('hex', 0, offset);
    }
}
