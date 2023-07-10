package io.github.stuff_stuffs.river_net_gen.api.neighbour.base;

import io.github.stuff_stuffs.river_net_gen.api.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import it.unimi.dsi.fastutil.HashCommon;
import org.jetbrains.annotations.Nullable;

public abstract class NeighbourChooser<T> {
    private static final Hex.Direction[] DIRECTIONS = Hex.Direction.values();

    protected abstract double weight(T val, T center, Hex.Direction direction, Neighbourhood<T> neighbourhood, long seed);

    public Hex.@Nullable Direction choose(final Neighbourhood<T> neighbourhood, final long seed) {
        Hex.Direction[] neighbours = new Hex.Direction[6];
        double[] weights = new double[6];
        final int center = neighbourhood.center();
        final T centerVal = neighbourhood.get(center);
        int count = 0;
        double weightSum = 0;
        long state = seed;
        for (final Hex.Direction direction : DIRECTIONS) {
            final T val = neighbourhood.get(center, direction);
            final double weight = weight(val, centerVal, direction, neighbourhood, state);
            state = HashCommon.mix(state + 12345 ^ seed) ^ state;
            if (weight > 0) {
                neighbours[count] = direction;
                weights[count++] = weight;
                weightSum += weight;
            }
        }
        final double val = ((HashCommon.murmurHash3(state + 12345) ^ HashCommon.mix(state - 12345)) >>> 11) * (weightSum * 0x1.0p-53);
        double s = 0;
        for (int i = 0; i < count; i++) {
            final double nextWeight = weights[i];
            s = s + nextWeight;
            if (s > val) {
                return neighbours[i];
            }
        }
        if (count != 0) {
            return neighbours[count - 1];
        }
        return null;
    }
}
