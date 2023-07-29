package io.github.stuff_stuffs.river_net_gen.api.util;

import it.unimi.dsi.fastutil.HashCommon;

public abstract class SpreadGrid<T> {
    private final int seed;

    public SpreadGrid(final int seed) {
        this.seed = seed;
    }

    public T get(final double x, final double y) {
        final int cellX = (int) Math.floor(x);
        final int cellY = (int) Math.floor(y);
        long best = 0;
        double bestDist = Double.POSITIVE_INFINITY;
        for (int i = -1; i <= 1; i++) {
            final int centerX = cellX + i;
            for (int j = -1; j <= 1; j++) {
                final int centerY = cellY + j;
                long state = HashCommon.mix(seed + HashCommon.mix(((long) centerX << 32) | (((long) centerY) & 0xFFFF_FFFFL)));
                final double pointX = centerX + GenUtil.randomDoubleFromLong(state) * 0.75 + 0.125;
                state = HashCommon.mix(state + 1) ^ state;
                final double pointY = centerY + GenUtil.randomDoubleFromLong(state) * 0.75 + 0.125;
                final double dx = x - pointX;
                final double dy = y - pointY;
                final double dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = state;
                }
            }
        }
        return create(HashCommon.murmurHash3(best));
    }

    protected abstract T create(long seed);
}
