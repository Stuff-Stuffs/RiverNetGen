package io.github.stuff_stuffs.river_net_gen.api.util;

import it.unimi.dsi.fastutil.HashCommon;

import java.util.ArrayList;
import java.util.List;

public class RandomCollection<T> {
    private final List<T> data = new ArrayList<>();
    private long state;

    public RandomCollection(final long seed) {
        state = seed;
    }

    public void add(final T val) {
        data.add(val);
    }

    public T pop() {
        if (data.size() == 1) {
            return data.remove(0);
        }
        final int idx = (int) ((state >>> 16) % data.size());
        state = HashCommon.mix(state) ^ HashCommon.murmurHash3(state * 127 + 1);
        return data.remove(idx);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }
}
