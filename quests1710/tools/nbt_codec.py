"""Bounded Java-edition NBT codec for offline migration; no Minecraft dependencies."""
import gzip
import io
import math
import struct
import zlib

LIMIT = 32 * 1024 * 1024

class IntTag(int):
    def __new__(cls, value, suffix=''):
        result = super().__new__(cls, value)
        bits = {'b': 8, 's': 16, 'l': 64}.get(suffix.lower(), 32)
        if not -(2 ** (bits - 1)) <= result < 2 ** (bits - 1):
            raise ValueError('NBT integer outside range')
        result.suffix = suffix
        return result

class FloatTag(float):
    def __new__(cls, value, suffix=''):
        result = super().__new__(cls, value)
        if not math.isfinite(result):
            raise ValueError('Non-finite NBT number')
        result.suffix = suffix
        return result

class TypedArray(list):
    def __init__(self, values=(), kind='I'):
        super().__init__(values)
        self.kind = kind.upper()

def mutf_encode(value):
    data = value.encode('utf-16-be', errors='surrogatepass')
    result = bytearray()
    for at in range(0, len(data), 2):
        unit = int.from_bytes(data[at:at + 2], 'big')
        if 0 < unit < 128:
            result.append(unit)
        elif unit < 2048:
            result.extend((0xc0 | unit >> 6, 0x80 | unit & 63))
        else:
            result.extend((0xe0 | unit >> 12, 0x80 | unit >> 6 & 63, 0x80 | unit & 63))
    if len(result) > 65535:
        raise ValueError('NBT string exceeds Java UTF limit')
    return struct.pack('>H', len(result)) + result

def mutf_decode(data):
    result = bytearray()
    at = 0
    while at < len(data):
        first = data[at]; at += 1
        if 0 < first < 128:
            unit = first
        elif first & 0xe0 == 0xc0:
            if at >= len(data) or data[at] & 0xc0 != 0x80:
                raise ValueError('Invalid Java UTF')
            unit = (first & 31) << 6 | data[at] & 63; at += 1
            if unit != 0 and unit < 128:
                raise ValueError('Overlong Java UTF')
        elif first & 0xf0 == 0xe0:
            if at + 1 >= len(data) or any(b & 0xc0 != 0x80 for b in data[at:at + 2]):
                raise ValueError('Invalid Java UTF')
            unit = (first & 15) << 12 | (data[at] & 63) << 6 | data[at + 1] & 63; at += 2
            if unit < 2048:
                raise ValueError('Overlong Java UTF')
        else:
            raise ValueError('Invalid Java UTF')
        result.extend(struct.pack('>H', unit))
    return result.decode('utf-16-be', errors='surrogatepass')

class Reader:
    def __init__(self, data):
        self.stream = io.BytesIO(data)
        self.nodes = 0
    def take(self, count):
        if count < 0 or count > LIMIT:
            raise ValueError('NBT length limit')
        data = self.stream.read(count)
        if len(data) != count:
            raise ValueError('Truncated NBT')
        return data
    def number(self, fmt):
        return struct.unpack('>' + fmt, self.take(struct.calcsize('>' + fmt)))[0]
    def string(self):
        return mutf_decode(self.take(self.number('H')))
    def value(self, tag, depth=0):
        self.nodes += 1
        if depth > 64 or self.nodes > 100000:
            raise ValueError('NBT complexity limit')
        if tag in range(1, 7):
            fmt, suffix = {1: ('b', 'b'), 2: ('h', 's'), 3: ('i', ''), 4: ('q', 'L'), 5: ('f', 'f'), 6: ('d', 'd')}[tag]
            value = self.number(fmt)
            if tag >= 5 and not math.isfinite(value):
                raise ValueError('Non-finite NBT value')
            return (IntTag if tag < 5 else FloatTag)(value, suffix)
        if tag == 8:
            return self.string()
        if tag == 10:
            result = {}
            while True:
                child = self.number('B')
                if not child:
                    return result
                key = self.string()
                if key in result:
                    raise ValueError('Duplicate NBT key')
                result[key] = self.value(child, depth + 1)
        if tag in (7, 9, 11, 12):
            child = self.number('B') if tag == 9 else {7: 1, 11: 3, 12: 4}[tag]
            count = self.number('i')
            if count < 0 or count > 100000 or child == 0 and count:
                raise ValueError('NBT list length limit')
            values = [self.value(child, depth + 1) for _ in range(count)]
            return values if tag == 9 else TypedArray(values, {7: 'B', 11: 'I', 12: 'L'}[tag])
        raise ValueError('Unknown NBT tag: ' + str(tag))

def read_nbt(path):
    if path.stat().st_size > LIMIT:
        raise ValueError('NBT file size limit')
    data = path.read_bytes()
    try:
        if data.startswith(b'\x1f\x8b'):
            with gzip.GzipFile(fileobj=io.BytesIO(data)) as stream:
                data = stream.read(LIMIT + 1)
        elif data[:1] == b'x':
            decoder = zlib.decompressobj(); data = decoder.decompress(data, LIMIT + 1)
            if not decoder.eof:
                raise ValueError('Oversized or incomplete zlib NBT')
    except (OSError, EOFError, zlib.error) as error:
        raise ValueError("Invalid compressed NBT") from error
    if len(data) > LIMIT:
        raise ValueError('Inflated NBT size limit')
    reader = Reader(data)
    if reader.number('B') != 10:
        raise ValueError('NBT root must be compound')
    reader.string(); result = reader.value(10)
    if reader.stream.read(1):
        raise ValueError('Trailing NBT data')
    return result

def tag_type(value):
    if isinstance(value, TypedArray):
        return {'B': 7, 'I': 11, 'L': 12}[value.kind]
    if isinstance(value, bool):
        return 1
    if isinstance(value, int):
        suffix = getattr(value, 'suffix', '').lower()
        return {'b': 1, 's': 2, 'l': 4}.get(suffix, 3)
    if isinstance(value, float):
        return 5 if getattr(value, 'suffix', '').lower() == 'f' else 6
    if isinstance(value, str):
        return 8
    if isinstance(value, list):
        return 9
    if isinstance(value, dict):
        return 10
    raise ValueError('Unsupported NBT value')

def payload(value, tag=None, depth=0):
    if depth > 64:
        raise ValueError('NBT nesting limit')
    tag = tag_type(value) if tag is None else tag
    if tag in range(1, 7):
        return struct.pack('>' + {1: 'b', 2: 'h', 3: 'i', 4: 'q', 5: 'f', 6: 'd'}[tag], value)
    if tag == 8:
        return mutf_encode(value)
    if tag == 10:
        return b''.join(bytes([tag_type(v)]) + mutf_encode(k) + payload(v, depth=depth + 1) for k, v in value.items()) + b'\0'
    if tag in (7, 9, 11, 12):
        if len(value) > 100000:
            raise ValueError('NBT array size limit')
        child = tag_type(value[0]) if tag == 9 and value else {7: 1, 11: 3, 12: 4}.get(tag, 0)
        if tag == 9 and any(tag_type(v) != child for v in value):
            raise ValueError('Mixed NBT list types')
        return (bytes([child]) if tag == 9 else b'') + struct.pack('>i', len(value)) + b''.join(payload(v, child, depth + 1) for v in value)
    raise ValueError('Unsupported NBT tag')

def write_nbt(path, data):
    try:
        raw = b'\x0a\0\0' + payload(data)
    except struct.error as error:
        raise ValueError('NBT numeric value outside range') from error
    if len(raw) > LIMIT:
        raise ValueError('NBT output size limit')
    encoded = gzip.compress(raw, mtime=0)
    with path.open('xb') as stream:
        stream.write(encoded)
