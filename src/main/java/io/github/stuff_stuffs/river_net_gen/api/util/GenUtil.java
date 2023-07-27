package io.github.stuff_stuffs.river_net_gen.api.util;

import it.unimi.dsi.fastutil.HashCommon;

import java.util.function.ToIntFunction;

public final class GenUtil {
    public static <T> T choose(final T[] values, final int length, final ToIntFunction<T> weighter, final int seed) {
        int weight = 0;
        for (int i = 0; i < length; i++) {
            weight = weight + weighter.applyAsInt(values[i]);
        }
        final int index = (HashCommon.murmurHash3(HashCommon.mix(seed) + seed) & 0x7FFFFFFF) % weight;
        int s = 0;
        for (int i = 0; i < length; i++) {
            final int w = weighter.applyAsInt(values[i]);
            if (s + w >= index) {
                return values[i];
            }
            s = s + w;
        }
        throw new RuntimeException();
    }

    public static double randomDoubleFromLong(final long state) {
        return (state >>> 11) * 0x1.0p-53;
    }

    private GenUtil() {
    }
}
