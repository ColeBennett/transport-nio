package net.thearchon.nio.protocol;

public final class Medium extends Number {

    public static final int SIZE = 3;
    public static final int MAX_UNSIGNED_VALUE = 16777215;
    public static final int MIN_UNSIGNED_VALUE = 0;
    public static final int MAX_SIGNED_VALUE = 8388607;
    public static final int MIN_SIGNED_VALUE = -8388608;

    private final int value;

    public Medium(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Medium && ((Medium) obj).value == value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
