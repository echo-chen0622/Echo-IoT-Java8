import Long from 'long';
import uuidParse from 'uuid-parse';

export function toUUIDString(mostSigBits: string, leastSigBits: string): string {
    const msbBytes = Long.fromValue(mostSigBits, false).toBytes(false);
    const lsbBytes = Long.fromValue(leastSigBits, false).toBytes(false);
    const uuidBytes = msbBytes.concat(lsbBytes);
    return uuidParse.unparse(uuidBytes as any);
}

export function UUIDFromBuffer(buf: Buffer): string {
    return uuidParse.unparse(buf);
}

export function UUIDToBits(uuidString: string): [string, string] {
    const bytes = Array.from(uuidParse.parse(uuidString));
    const msb = Long.fromBytes(bytes.slice(0, 8), false, false).toString();
    const lsb = Long.fromBytes(bytes.slice(-8), false, false).toString();
    return [msb, lsb];
}

export function isString(value: any): boolean {
    return typeof value === 'string';
}

export function parseJsErrorDetails(err: any): string | undefined {
    if (!err) {
        return undefined;
    }
    let details = err.name + ': ' + err.message;
    if (err.stack) {
        const lines = err.stack.split('\n');
        if (lines && lines.length) {
            const line = lines[0];
            const split = line.split(':');
            if (split && split.length === 2) {
                if (!isNaN(split[1])) {
                    details += ' in at line number ' + split[1];
                }
            }
        }
    }
    return details;
}

export function isNotUUID(candidate: string) {
    return candidate.length != 36 || !candidate.includes('-');
}
