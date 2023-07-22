package io.github.stuff_stuffs.river_net_gen.geo;

import io.github.stuff_stuffs.river_net_gen.util.ImageOut;
import io.github.stuff_stuffs.river_net_gen.util.SubSampler;
import io.github.stuff_stuffs.river_net_gen.util.Tri;
import it.unimi.dsi.fastutil.HashCommon;

import java.util.function.IntConsumer;

public class GeoTest {
    public static void main(final String[] args) {
        final int seed = 2113;
        final int resolution = 2048;
        final int samplerSizeLog2 = 4;
        final int samplerRateLog2 = 2;
        final int colorCount = 32;
        final int layerCount = 32;
        final int samplerWidth = (resolution - 1 + (1 << samplerSizeLog2)) / (1 << samplerSizeLog2);
        final SubSampler[][] samplers = new SubSampler[samplerWidth][samplerWidth];
        final double scale = 0.005;
        final SubSampler.XZSampler xzSampler = new SubSampler.AbstractSampler() {
            private GeoColumnInterpolator3d interpolator;
            private Tri.Coordinate last = null;

            @Override
            protected void setupColumn(final int x, final int z) {
                final Tri.Coordinate coordinate = Tri.fromCartesian(x * scale, z * scale);
                if (coordinate.equals(last)) {
                    return;
                }
                final Tri.Coordinate[] corners = Tri.corners(coordinate);
                interpolator = new GeoColumnInterpolator3d(corners[0].x(), corners[0].y(), random(corners[0], layerCount, colorCount, seed), corners[1].x(), corners[1].y(), random(corners[1], layerCount, colorCount, seed), corners[2].x(), corners[2].y(), random(corners[2], layerCount, colorCount, seed));
                last = coordinate;
            }

            @Override
            public int sample(final int y) {
                return interpolator.interpolate(getX() * scale, y, getZ() * scale);
            }
        };
        final int samplerSize = 1 << samplerSizeLog2;
        for (int i = 0; i < samplerWidth; i++) {
            for (int j = 0; j < samplerWidth; j++) {
                final SubSampler sampler = new SubSampler(samplerSizeLog2, samplerRateLog2, xzSampler, HashCommon.murmurHash3(1 + i ^ HashCommon.murmurHash3(j)));
                sampler.setup(i * samplerSize, j * samplerSize, 0);
                samplers[i][j] = sampler;
            }
        }
        ImageOut.draw(new ImageOut.Drawer() {
            @Override
            public void draw(final int x, final int y, final IntConsumer[] painters) {
                final int sample = samplers[x / samplerSize][y / samplerSize].sample(x, y, 0);
                painters[0].accept(HashCommon.murmurHash3(sample ^ HashCommon.murmurHash3(sample + 1)));
            }
        }, resolution, resolution, "tGeoTest.png");
    }

    private static GeoColumn random(final Tri.Coordinate coordinate, final int layerCount, final int colorCount, final int seed) {
        return random(HashCommon.murmurHash3(coordinate.a() ^ HashCommon.murmurHash3(coordinate.b() ^ HashCommon.murmurHash3(coordinate.c() + seed)) + seed + seed), layerCount, colorCount);
    }

    private static GeoColumn random(final int seed, final int layerCount, final int colorCount) {
        final int[] data = new int[layerCount];
        final int[] thickness = new int[layerCount];
        int randomState = HashCommon.murmurHash3(1234567 + HashCommon.murmurHash3(1234567 ^ HashCommon.murmurHash3(seed + 1234567)));
        for (int i = 0; i < layerCount; i++) {
            data[i] = (randomState & 0x7FFF_FFFF) % colorCount;
            randomState = HashCommon.murmurHash3(1234567 + HashCommon.murmurHash3(1234567 ^ HashCommon.murmurHash3(randomState + 1234567)));
            thickness[i] = ((randomState & 0x7FFF_FFFF) % 16) + 16;
        }
        return new GeoColumn(data, thickness);
    }
}
