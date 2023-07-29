package io.github.stuff_stuffs.river_net_gen.api.util;

import it.unimi.dsi.fastutil.HashCommon;

public class SubSampler {
    private final int sizeLog2;
    private final int rateLog2;
    private final int count;
    private final int seed;
    private final XZSampler sampler;
    private final int[] samples;
    private int sampleSeed;

    public SubSampler(final int sizeLog2, final int rateLog2, final XZSampler sampler, final int seed) {
        this.sizeLog2 = sizeLog2;
        if (rateLog2 < 1) {
            throw new IllegalArgumentException();
        }
        this.rateLog2 = rateLog2;
        count = (1 << sizeLog2) / (1 << rateLog2) + 2;
        this.seed = seed;
        this.sampler = sampler;
        if (count < 2) {
            throw new IllegalArgumentException();
        }
        samples = new int[count * count * count];
    }

    public void setup(int x, int y, int z) {
        final int mask = (1 << sizeLog2) - 1;
        x = x & ~mask;
        y = y & ~mask;
        z = z & ~mask;
        sampleSeed = HashCommon.mix((x + seed) ^ HashCommon.mix((y + seed) ^ HashCommon.mix(z + seed)));
        final int offset = 1 << rateLog2;
        final int[] samples = this.samples;
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                final ColumnSampler sampler = this.sampler.sampler(x + i * offset - offset, z + j * offset - offset);
                for (int k = 0; k < count; k++) {
                    final int sample = sampler.sample(y + k * offset - offset);
                    final int index = (i * count + j) * count + k;
                    samples[index] = sample;
                }
            }
        }
    }

    public int sample(int x, int y, int z) {
        final int mask = (1 << sizeLog2) - 1;
        x = x & mask;
        y = y & mask;
        z = z & mask;
        final int rateMaskSq = (1 << (rateLog2 * 2)) - 1;
        final int xSampleLower = (x >> rateLog2) + 1;
        final int ySampleLower = (y >> rateLog2) + 1;
        final int zSampleLower = (z >> rateLog2) + 1;
        final int xSampleUpper = xSampleLower + 1;
        final int ySampleUpper = ySampleLower + 1;
        final int zSampleUpper = zSampleLower + 1;
        final int mixed = sampleSeed + (HashCommon.mix(x ^ HashCommon.mix(y ^ HashCommon.mix(z))));
        int randomState = HashCommon.murmurHash3(mixed);
        final int xChosen = choose(x, xSampleLower << rateLog2, xSampleLower, xSampleUpper, randomState & rateMaskSq);
        randomState = mix(randomState, mixed);
        final int yChosen = choose(y, ySampleLower << rateLog2, ySampleLower, ySampleUpper, randomState & rateMaskSq);
        randomState = mix(randomState, mixed);
        final int zChosen = choose(z, zSampleLower << rateLog2, zSampleLower, zSampleUpper, randomState & rateMaskSq);
        return samples[(xChosen * count + zChosen) * count + yChosen];
    }

    private static int mix(final int state, final int mixer) {
        return HashCommon.mix(state + mixer);
    }

    private int choose(final int pos, final int cutoff, final int lowerIndex, final int upperIndex, final int seed) {
        final int diffSq = pos - cutoff;
        return seed > diffSq * diffSq ? upperIndex : lowerIndex;
    }

    public interface ColumnSampler {
        int sample(int y);
    }

    public interface XZSampler {
        ColumnSampler sampler(int x, int z);
    }

    public static abstract class AbstractSampler implements ColumnSampler, XZSampler {
        private int x;
        private int z;

        @Override
        public ColumnSampler sampler(final int x, final int z) {
            this.x = x;
            this.z = z;
            setupColumn(x, z);
            return this;
        }

        protected abstract void setupColumn(int x, int z);

        protected int getX() {
            return x;
        }

        protected int getZ() {
            return z;
        }
    }
}
