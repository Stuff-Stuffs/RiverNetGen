package io.github.stuff_stuffs.river_net_gen.util;

import it.unimi.dsi.fastutil.HashCommon;

public class SubSampler {
    private final int sizeLog2;
    private final int rateLog2;
    private final int count;
    private final int seed;
    private final XZSampler sampler;
    private final int[] samples;

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
        final int offset = 1 << rateLog2;
        final int halfRate = offset >>> 1;
        final int[] samples = this.samples;
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                final ColumnSampler sampler = this.sampler.sampler(x + i * offset - halfRate, z + j * offset - halfRate);
                for (int k = 0; k < count; k++) {
                    final int sample = sampler.sample(y + k * offset - halfRate);
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
        final int rateMask = (1 << rateLog2) - 1;
        final int xSampleCoord = x >> rateLog2;
        final int ySampleCoord = y >> rateLog2;
        final int zSampleCoord = z >> rateLog2;
        final int xSampleUpper = xSampleCoord + 1;
        final int ySampleUpper = ySampleCoord + 1;
        final int zSampleUpper = zSampleCoord + 1;
        final int mixed = seed + (HashCommon.mix(x + 1234235) ^ HashCommon.mix(y + 214235) ^ HashCommon.mix(z));
        int randomState = HashCommon.murmurHash3(mixed);
        final int xChosen = (randomState & rateMask) < x - (xSampleCoord << rateLog2) ? xSampleUpper : xSampleCoord;
        randomState = HashCommon.murmurHash3(randomState + mixed);
        final int yChosen = (randomState & rateMask) < y - (ySampleCoord << rateLog2) ? ySampleUpper : ySampleCoord;
        randomState = HashCommon.murmurHash3(randomState + mixed);
        final int zChosen = (randomState & rateMask) < z - (zSampleCoord << rateLog2) ? zSampleUpper : zSampleCoord;
        return samples[(xChosen * count + zChosen) * count + yChosen];
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
