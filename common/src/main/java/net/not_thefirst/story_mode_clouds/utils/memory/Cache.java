package net.not_thefirst.story_mode_clouds.utils.memory;

import java.util.Arrays;
import java.util.function.IntFunction;

public final class Cache<T> {
    private Object[] data;

    public Cache() {
        this(16);
    }

    public Cache(int initialCapacity) {
        this.data = new Object[Math.max(1, initialCapacity)];
    }

    private void ensureCapacity(int index) {
        if (index < data.length) {
            return;
        }

        int newSize = data.length;
        while (newSize <= index) {
            newSize <<= 1;
        }

        data = Arrays.copyOf(data, newSize);
    }

    public boolean has(int index) {
        return index >= 0
            && index < data.length
            && data[index] != null;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        return (index >= 0 && index < data.length)
            ? (T)data[index]
            : null;
    }

    public T getOrCreate(int index, IntFunction<T> factory) {
        if (has(index)) {
            return get(index);
        }

        T value = factory.apply(index);
        put(index, value);
        return value;
    }

    public void put(int index, T value) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative cache index");
        }

        ensureCapacity(index);
        data[index] = value;
    }

    public void remove(int index) {
        if (index >= 0 && index < data.length) {
            data[index] = null;
        }
    }

    public void clear() {
        Arrays.fill(data, null);
    }
}

