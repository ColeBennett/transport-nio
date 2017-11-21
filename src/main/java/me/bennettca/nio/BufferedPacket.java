package me.bennettca.nio;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import me.bennettca.nio.protocol.LengthAwarePacket;
import me.bennettca.nio.protocol.Medium;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

public class BufferedPacket extends LengthAwarePacket {

    public static final short PACKET_ID = -3;

    private static final Map<Class<?>, Key> KEYS;

    private enum Key {
        BYTES(Medium.SIZE, byte[].class),
        BYTE(1, Byte.class),
        FLOAT(4, Float.class),
        CHAR(2, Character.class),
        SHORT(2, Short.class),
        MEDIUM(Medium.SIZE, Medium.class),
        INT(4, Integer.class),
        DOUBLE(8, Double.class),
        LONG(8, Long.class),
        UTF_8(Medium.SIZE, String.class),
        BOOLEAN(1, Boolean.class);

        final byte value;
        final int length;
        final Class<?> clazz;

        Key(int length, Class<?> clazz) {
            value = (byte) ordinal();
            this.length = length;
            this.clazz = clazz;
        }
    }

    static {
        Key[] keys = Key.values();
        KEYS = new HashMap<>(keys.length);
        for (Key key : keys) {
            KEYS.put(key.clazz, key);
        }
    }

    /**
     * Data stored in this buffer.
     */
    private List<Object> data;

    /**
     * Construct with default capacity.
     */
    public BufferedPacket() {
        this(new ArrayList<>());
    }

    /**
     * Construct with initial capacity.
     *
     * @param initialCapacity initial size of the buffer
     */
    public BufferedPacket(int initialCapacity) {
        this(new ArrayList<>(initialCapacity));
    }

    /**
     * Construct with initial data.
     *
     * @param data initial object list of the buffer
     */
    public BufferedPacket(List<Object> data) {
        super(PACKET_ID);

        this.data = data;
    }

    /**
     * Construct by reading a buffer. This should
     * only be used for internal use in a decoder.
     *
     * @param buf construct using an existing buffer
     */
    public BufferedPacket(ByteBuf buf) {
        super(PACKET_ID);

        read(buf);
    }

    @Override
    public void read(final ByteBuf buf) {
        if (data != null) {
            throw new IllegalStateException("Packet data has already been read");
        }
        int len = buf.readUnsignedMedium();
        data = new ArrayList<>(len);
        int start = buf.readerIndex();
        buf.readerIndex(start + len);
        buf.forEachByte(start, len, value -> {
            Object obj;
            if (value == Key.BYTES.value) {
                obj = readBytes(buf);
            } else if (value == Key.BYTE.value) {
                obj = buf.readByte();
            } else if (value == Key.FLOAT.value) {
                obj = buf.readFloat();
            } else if (value == Key.CHAR.value) {
                obj = buf.readChar();
            } else if (value == Key.SHORT.value) {
                obj = buf.readShort();
            } else if (value == Key.MEDIUM.value) {
                obj = new Medium(buf.readMedium());
            } else if (value == Key.INT.value) {
                obj = buf.readInt();
            } else if (value == Key.DOUBLE.value) {
                obj = buf.readDouble();
            } else if (value == Key.LONG.value) {
                obj = buf.readLong();
            } else if (value == Key.UTF_8.value) {
                obj = readString(buf);
            } else if (value == Key.BOOLEAN.value) {
                obj = buf.readBoolean();
            } else {
                throw new NullPointerException("Invalid key: " + value);
            }
            data.add(obj);
            return true;
        });
    }

    @Override
    public void write(ByteBuf buf) {
        int len = size();
        byte[] key = new byte[len];
        for (int i = 0; i < len; i++) {
            key[i] = getKey(data.get(i)).value;
        }
        writeBytes(buf, key);
        for (int i = 0; i < len; i++) {
            byte value = key[i];
            Object obj = data.get(i);
            if (value == Key.BYTES.value) {
                writeBytes(buf, (byte[]) obj);
            } else if (value == Key.BYTE.value) {
                buf.writeByte((Byte) obj);
            } else if (value == Key.FLOAT.value) {
                buf.writeFloat((Float) obj);
            } else if (value == Key.CHAR.value) {
                buf.writeChar((Character) obj);
            } else if (value == Key.SHORT.value) {
                buf.writeShort((Short) obj);
            } else if (value == Key.MEDIUM.value) {
                buf.writeMedium(((Medium) obj).value());
            } else if (value == Key.INT.value) {
                buf.writeInt((Integer) obj);
            } else if (value == Key.DOUBLE.value) {
                buf.writeDouble((Double) obj);
            } else if (value == Key.LONG.value) {
                buf.writeLong((Long) obj);
            } else if (value == Key.UTF_8.value) {
                writeString(buf, (String) obj);
            } else if (value == Key.BOOLEAN.value) {
                buf.writeBoolean((Boolean) obj);
            }
        }
    }

    @Override
    public int length() {
        checkReleased();
        int len = Medium.SIZE + size();
        for (Object obj : data) {
            Key key = getKey(obj);
            len += key.length;
            switch (key) {
                case BYTES:
                    len += ((byte[]) obj).length;
                    break;
                case UTF_8:
                    len += utf8Length((String) obj);
                    break;
                default:
                    break;
            }
        }
        return len;
    }

    public Object getObject(int index) {
        return data.get(index);
    }

    public byte[] getBytes(int index) {
        return get(Key.BYTES, index);
    }

    public byte getByte(int index) {
        return (Byte) get(Key.BYTE, index);
    }

    public float getFloat(int index) {
        return (Float) get(Key.FLOAT, index);
    }

    public char getChar(int index) {
        return (Character) get(Key.CHAR, index);
    }

    public short getShort(int index) {
        return (Short) get(Key.SHORT, index);
    }

    public int getMedium(int index) {
        Medium med = get(Key.MEDIUM, index);
        return med.value();
    }

    public int getInt(int index) {
        return (Integer) get(Key.INT, index);
    }

    public double getDouble(int index) {
        return (Double) get(Key.DOUBLE, index);
    }

    public long getLong(int index) {
        return (Long) get(Key.LONG, index);
    }

    public String getString(int index) {
        String value = get(Key.UTF_8, index);
        return isNull(value) ? null : value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> T getEnum(int index, Class<T> enumType) {
        String value = getString(index);
        return value != null ? (T) Enum.valueOf(
                (Class<? extends Enum>) enumType, value) : null;
    }

    public boolean getBoolean(int index) {
        return (Boolean) get(Key.BOOLEAN, index);
    }

    public byte[] readBytes() {
        return read(Key.BYTES);
    }

    public byte readByte() {
        return (Byte) read(Key.BYTE);
    }

    public float readFloat() {
        return (Float) read(Key.FLOAT);
    }

    public char readChar() {
        return (Character) read(Key.CHAR);
    }

    public short readShort() {
        return (Short) read(Key.SHORT);
    }

    public int readMedium() {
        Medium med = read(Key.MEDIUM);
        return med.value();
    }

    public int readInt() {
        return (Integer) read(Key.INT);
    }

    public double readDouble() {
        return (Double) read(Key.DOUBLE);
    }

    public long readLong() {
        return (Long) read(Key.LONG);
    }

    public String readString() {
        String value = read(Key.UTF_8);
        return isNull(value) ? null : value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> T readEnum(Class<T> enumType) {
        String value = readString();
        return value != null ? (T) Enum.valueOf(
                (Class<? extends Enum>) enumType, value) : null;
    }

    public boolean readBoolean() {
        return (Boolean) read(Key.BOOLEAN);
    }

    public BufferedPacket writeBytes(byte[] value) {
        write(value);
        return this;
    }

    public BufferedPacket writeByte(byte value) {
        write(value);
        return this;
    }

    public BufferedPacket writeFloat(float value) {
        write(value);
        return this;
    }

    public BufferedPacket writeChar(char value) {
        write(value);
        return this;
    }

    public BufferedPacket writeShort(short value) {
        write(value);
        return this;
    }

    public BufferedPacket writeMedium(int value) {
        write(new Medium(value));
        return this;
    }

    public BufferedPacket writeInt(int value) {
        write(value);
        return this;
    }

    public BufferedPacket writeDouble(double value) {
        write(value);
        return this;
    }

    public BufferedPacket writeLong(long value) {
        write(value);
        return this;
    }

    public BufferedPacket writeString(String value) {
        write(value != null ? value : NULL_STRING);
        return this;
    }

    public <T extends Enum<T>> BufferedPacket writeEnum(Enum<T> value) {
        writeString(value != null ? value.name() : null);
        return this;
    }

    public BufferedPacket writeBoolean(boolean value) {
        write(value);
        return this;
    }

    public byte[] takeBytes(int index) {
        return remove(Key.BYTES, index);
    }

    public byte takeByte(int index) {
        return (Byte) remove(Key.BYTE, index);
    }

    public float takeFloat(int index) {
        return (Float) remove(Key.FLOAT, index);
    }

    public char takeChar(int index) {
        return (Character) remove(Key.CHAR, index);
    }

    public short takeShort(int index) {
        return (Short) remove(Key.SHORT, index);
    }

    public int takeMedium(int index) {
        Medium med = remove(Key.MEDIUM, index);
        return med.value();
    }

    public int takeInt(int index) {
        return (Integer) remove(Key.INT, index);
    }

    public double takeDouble(int index) {
        return (Double) remove(Key.DOUBLE, index);
    }

    public long takeLong(int index) {
        return (Long) remove(Key.LONG, index);
    }

    public String takeString(int index) {
        String value = remove(Key.UTF_8, index);
        if (value.equals(NULL_STRING)) {
            value = null;
        }
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> T takeEnum(int index, Class<T> enumType) {
        String value = takeString(index);
        return value != null ? (T) Enum.valueOf(
                (Class<? extends Enum>) enumType, value) : null;
    }

    public boolean takeBoolean(int index) {
        return (Boolean) remove(Key.BOOLEAN, index);
    }

    public boolean hasBytes(int index) {
        return has(Key.BYTES, index);
    }

    public boolean hasByte(int index) {
        return has(Key.BYTE, index);
    }

    public boolean hasFloat(int index) {
        return has(Key.FLOAT, index);
    }

    public boolean hasChar(int index) {
        return has(Key.CHAR, index);
    }

    public boolean hasShort(int index) {
        return has(Key.SHORT, index);
    }

    public boolean hasMedium(int index) {
        return has(Key.MEDIUM, index);
    }

    public boolean hasInt(int index) {
        return has(Key.INT, index);
    }

    public boolean hasDouble(int index) {
        return has(Key.DOUBLE, index);
    }

    public boolean hasLong(int index) {
        return has(Key.LONG, index);
    }

    public boolean hasString(int index) {
        return has(Key.UTF_8, index);
    }

    public boolean hasBoolean(int index) {
        return has(Key.BOOLEAN, index);
    }

    public BufferedPacket setBytes(int index, byte[] value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setByte(int index, byte value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setFloat(int index, float value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setChar(int index, char value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setShort(int index, short value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setMedium(int index, Medium value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setMedium(int index, int value) {
        set(index, new Medium(value));
        return this;
    }

    public BufferedPacket setInt(int index, int value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setDouble(int index, double value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setLong(int index, long value) {
        set(index, value);
        return this;
    }

    public BufferedPacket setString(int index, String value) {
        set(index, value != null ? value : NULL_STRING);
        return this;
    }

    public <T extends Enum<T>> BufferedPacket setEnum(int index, Enum<T> value) {
        setString(index, value != null ? value.name() : null);
        return this;
    }

    public BufferedPacket setBoolean(int index, boolean value) {
        set(index, value);
        return this;
    }

    public BufferedPacket writeAll(Object... values) {
        for (Object obj : values) {
            processWrite(obj);
        }
        return this;
    }

    public <T> BufferedPacket writeAll(Iterable<T> values) {
        for (T obj : values) {
            processWrite(obj);
        }
        return this;
    }

    public void copyTo(BufferedPacket buf) {
        buf.writeAll(data);
    }

    public <K, V> BufferedPacket writeMap(Map<K, V> map) {
        for (Entry<K, V> entry : map.entrySet()) {
            processWrite(entry.getKey());
            processWrite(entry.getValue());
        }
        return this;
    }

    public Object[] asArray() {
        checkReleased();
        Object[] result = new Object[size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = data.get(i);
        }
        return result;
    }

    public <T> T[] asArray(Class<T> clazz) {
        return asArray(0, size(), clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] asArray(int start, int end, Class<T> clazz) {
        checkReleased();
        int capacity = end - start;
        if (capacity < 0) {
            throw new NegativeArraySizeException();
        }
        T[] result = (T[]) Array.newInstance(clazz, capacity);
        int idx = 0;
        for (int i = start; i < end; i++) {
            Object obj = data.get(i);
            if (!obj.getClass().equals(clazz)) {
                throw new IllegalArgumentException(obj.getClass()
                        .getSimpleName() + " != " + clazz.getSimpleName());
            }
            result[idx++] = (T) obj;
        }
        if (result.length != capacity) {
            throw new IllegalStateException("result < expected capacity");
        }
        return result;
    }

    public List<Object> asList() {
        checkReleased();
        List<Object> result = new ArrayList<>(size());
        for (Object obj : data) {
            result.add(obj);
        }
        return result;
    }

    public <T> List<T> asList(Class<T> clazz) {
        return asList(0, size(), clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> asList(int start, int end, Class<T> clazz) {
        checkReleased();
        int capacity = end - start;
        if (capacity < 0) {
            throw new NegativeArraySizeException();
        }
        List<T> result = new ArrayList<>(capacity);
        for (int i = start; i < end; i++) {
            Object obj = data.get(i);
            if (!obj.getClass().equals(clazz)) {
                throw new IllegalArgumentException(obj.getClass()
                        .getSimpleName() + " != " + clazz.getSimpleName());
            }
            result.add((T) obj);
        }
        if (result.size() != capacity) {
            throw new IllegalStateException("result < expected capacity");
        }
        return result;
    }

    public <K, V> Map<K, V> asMap(Class<K> keyClass, Class<V> valueClass) {
        return asMap(0, size(), keyClass, valueClass);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> asMap(int start, int end, Class<K> keyClass, Class<V> valueClass) {
        checkReleased();
        int elements = end - start;
        if (elements < 0) {
            throw new NegativeArraySizeException();
        }
        if ((elements % 2) != 0) {
            throw new IllegalStateException("There must be enough keys for values");
        }
        int capacity = elements / 2;
        Map<K, V> result = new LinkedHashMap<>(capacity);
        for (int i = start; i < end; i += 2) {
            Object keyObj = data.get(i);
            if (!keyObj.getClass().equals(keyClass)) {
                throw new IllegalArgumentException(keyObj.getClass()
                        .getSimpleName() + " != " + keyClass.getSimpleName());
            }
            Object valueObj = data.get(i + 1);
            if (!valueObj.getClass().equals(valueClass)) {
                throw new IllegalArgumentException(valueObj.getClass()
                        .getSimpleName() + " != " + valueClass.getSimpleName());
            }
            result.put((K) keyObj, (V) valueObj);
        }
        if (result.size() != capacity) {
            throw new IllegalStateException("result < expected capacity");
        }
        return result;
    }

    public boolean contains(Object obj) {
        checkReleased();
        return data.contains(obj);
    }

    public int size() {
        checkReleased();
        return data.size();
    }

    public boolean isEmpty() {
        checkReleased();
        return data.isEmpty();
    }

    public boolean remove(int index) {
        checkReleased();
        return data.remove(index) != null;
    }

    public BufferedPacket clear() {
        checkReleased();
        data.clear();
        return this;
    }

    public BufferedPacket release() {
        clear();
        data = null;
        return this;
    }

    public BufferedPacket duplicate() {
        checkReleased();
        return new BufferedPacket(new ArrayList<>(data));
    }

    public boolean hasIndex(int index) {
        checkReleased();
        return index >= 0 && index < data.size();
    }

    @SuppressWarnings("unchecked")
    private <T> T get(Key key, int index) {
        checkReleased();
        Object value = data.get(index);
        if (!isEqual(key, value)) {
            throw new IllegalArgumentException(key.clazz.getSimpleName()
                    + " not found at index: " + index);
        }
        return (T) value;
    }

    private void set(int index, Object value) {
        checkReleased();
        if (value == null) {
            throw new NullPointerException("value");
        }
        data.set(index, value);
    }

    private <T> T read(Key key) {
        if (isEmpty()) {
            throw new IllegalStateException("No values to read");
        }
        return remove(key, 0);
    }

    private void write(Object value) {
        checkReleased();
        data.add(value);
    }

    private void processWrite(Object value) {
        if (value == null) {
            writeString(null);
        } else if (value instanceof Enum<?>) {
            writeEnum((Enum<?>) value);
        } else {
            getKey(value);
            write(value);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T remove(Key key, int index) {
        checkReleased();
        Object value = data.get(index);
        if (!isEqual(key, value)) {
            throw new IllegalArgumentException(key.clazz.getSimpleName()
                    + " not found at index: " + index);
        }
        return (T) data.remove(index);
    }

    private boolean isEqual(Key key, Object obj) {
        return key.clazz.equals(obj.getClass());
    }

    private boolean has(Key key, int index) {
        return hasIndex(index) && isEqual(key, data.get(index));
    }

    private Key getKey(Object obj) {
        if (obj == null) {
            throw new NullPointerException("object");
        }
        Key key = KEYS.get(obj.getClass());
        if (key == null) {
            throw new UnsupportedMessageTypeException(obj.getClass().getSimpleName());
        }
        return key;
    }

    private void checkReleased() {
        if (data == null) {
            throw new IllegalStateException("Packet data has already been released");
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (data != null) {
            Iterator<Object> itr = data.iterator();
            while (itr.hasNext()) {
                Object value = itr.next();
                if (value instanceof byte[]) {
                    byte[] arr = (byte[]) value;
                    if (arr.length <= 10) {
                        buf.append(Arrays.toString(arr));
                    } else {
                        buf.append("bytes(");
                        buf.append(arr.length);
                        buf.append(')');
                    }
                } else {
                    buf.append(value);
                }
                if (itr.hasNext()) {
                    buf.append(", ");
                }
            }
        } else {
            buf.append("released");
        }
        buf.append(']');
        return buf.toString();
    }

    public static boolean isNull(String string) {
        return string.equals(NULL_STRING);
    }

    public static final String NULL_STRING = "NULL";
}
