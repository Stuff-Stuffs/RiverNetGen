package io.github.stuff_stuffs.river_net_gen.api.geo;

import io.github.stuff_stuffs.river_net_gen.api.geo.column.GeoColumn;
import io.github.stuff_stuffs.river_net_gen.api.geo.column.GeoColumnInterpolator3d;
import io.github.stuff_stuffs.river_net_gen.api.geo.feature.ConstantGeoFeature;
import io.github.stuff_stuffs.river_net_gen.api.geo.feature.DikeGeoFeature;
import io.github.stuff_stuffs.river_net_gen.api.geo.feature.GeoFeatureApplicator;
import io.github.stuff_stuffs.river_net_gen.api.util.ImageOut;
import io.github.stuff_stuffs.river_net_gen.api.util.SubSampler;
import io.github.stuff_stuffs.river_net_gen.api.util.Tri;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public class GeoTest {
    public static void main(final String[] args) {
        final int seed = 2113;
        final int resolution = 2048;
        final int samplerSizeLog2 = 4;
        final int samplerRateLog2 = 2;
        final int layerCount = 32;
        final int samplerWidth = (resolution - 1 + (1 << samplerSizeLog2)) / (1 << samplerSizeLog2);
        final SubSampler[][] samplers = new SubSampler[samplerWidth][samplerWidth];
        final double scale = 0.005;
        final DikeGeoFeature feature = new DikeGeoFeature(0, "rock", 200, 0, 0, 1, -1, 0, 4, 2000);
        final GeoFeatureApplicator applicator = GeoFeatureApplicator.create(new ConstantGeoFeature(0, "none"), 4);
        final List<String> rocks = List.of("base0", "base1", "base2", "base3", "base4", "base5", "base6", "base7");
        applicator.setFeatures(Collections.singleton(feature), rocks);
        final int noneMaterial = applicator.getGeoId("none").getAsInt();
        final Object2IntMap<String> cache = new Object2IntOpenHashMap<>(rocks.size() * 4);
        for (final String rock : rocks) {
            cache.put(rock, applicator.getGeoId(rock).getAsInt());
        }
        final SubSampler.XZSampler xzSampler = new SubSampler.AbstractSampler() {
            private GeoColumnInterpolator3d interpolator;
            private Tri.Coordinate last = null;

            @Override
            protected void setupColumn(final int x, final int z) {
                final double scaledX = x * scale;
                final double scaledZ = z * scale;
                final Tri.Coordinate coordinate = Tri.fromCartesian(scaledX, scaledZ);
                if (coordinate.equals(last)) {
                    return;
                }
                final Tri.Coordinate[] corners = Tri.corners(coordinate);
                interpolator = new GeoColumnInterpolator3d(corners[0].x(), corners[0].y(), random(corners[0], layerCount, rocks, seed), corners[1].x(), corners[1].y(), random(corners[1], layerCount, rocks, seed), corners[2].x(), corners[2].y(), random(corners[2], layerCount, rocks, seed));
                last = coordinate;
            }

            @Override
            public int sample(final int y) {
                final double scaledX = getX() * scale;
                final double scaledZ = getZ() * scale;
                final int apply = applicator.apply(getX(), y, getZ());
                if (apply == noneMaterial) {
                    return cache.getInt(interpolator.interpolate(scaledX, y, scaledZ));
                }
                return apply;
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
        }, resolution, resolution, "tGeoTestSq.png");
    }

    private static GeoColumn random(final Tri.Coordinate coordinate, final int layerCount, final List<String> rocks, final int seed) {
        return random(HashCommon.murmurHash3(coordinate.a() ^ HashCommon.murmurHash3(coordinate.b() ^ HashCommon.murmurHash3(coordinate.c() + seed)) + seed + seed), layerCount, rocks);
    }

    private static GeoColumn random(final int seed, final int layerCount, final List<String> rocks) {
        final String[] data = new String[layerCount];
        final int[] thickness = new int[layerCount];
        int randomState = HashCommon.murmurHash3(1234567 + HashCommon.murmurHash3(1234567 ^ HashCommon.murmurHash3(seed + 1234567)));
        final int count = rocks.size();
        for (int i = 0; i < layerCount; i++) {
            data[i] = rocks.get((randomState & 0x7FFF_FFFF) % count);
            randomState = HashCommon.murmurHash3(1234567 + HashCommon.murmurHash3(1234567 ^ HashCommon.murmurHash3(randomState + 1234567)));
            thickness[i] = ((randomState & 0x7FFF_FFFF) % 16) + 16;
        }
        return new GeoColumn(data, thickness);
    }
}
