package io.github.stuff_stuffs.river_net_gen.geo;

import io.github.stuff_stuffs.river_net_gen.util.ImageOut;
import it.unimi.dsi.fastutil.HashCommon;

import java.util.function.IntConsumer;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class GeoTest {
    public static void main(final String[] args) {
        final int resolution = 512;
        System.out.println(RandomGeneratorFactory.all().map(f -> f.name()).toList());
        final RandomGeneratorFactory<RandomGenerator> factory = RandomGeneratorFactory.of("Xoroshiro128PlusPlus");
        final int count = 5;
        final int seed = 1;
        final GeoColumn[] geoColumns = new GeoColumn[count + 1];
        for (int i = 0; i < count + 1; i++) {
            final int s = HashCommon.murmurHash3(i ^ HashCommon.murmurHash3(seed + HashCommon.mix(i)));
            geoColumns[i] = random(factory.create(s));
        }
        final GeoColumnInterpolator2d[] interpolators = new GeoColumnInterpolator2d[count];
        for (int i = 0; i < count; i++) {
            interpolators[i] = new GeoColumnInterpolator2d(i, geoColumns[i], geoColumns[i+1]);
        }
        final double factor = count / (double) resolution;
        ImageOut.draw(new ImageOut.Drawer() {
            @Override
            public void draw(final int x, final int y, final IntConsumer[] painters) {
                final double start = x * factor;
                final GeoColumnInterpolator2d interpolator = interpolators[Math.min((int) Math.floor(start), count - 1)];
                final int color = interpolator.interpolate(start, y/(double)2, 0);
                painters[0].accept(HashCommon.murmurHash3(color ^ HashCommon.murmurHash3(color + 123456789)));
            }
        }, resolution, resolution, "tgeo.png");
    }

    private static GeoColumn random(final RandomGenerator random) {
        final int layerCount = 13;
        final int[] thickness = new int[layerCount];
        final int[] data = new int[layerCount];
        int last = -1;
        for (int i = 0; i < layerCount; i++) {
            thickness[i] = random.nextInt(8 + random.nextInt(8)) + 8;
            int r;
            do {
                r = random.nextInt(8);
            } while (r == last);
            last = r;
            data[i] = r;
        }
        return new GeoColumn(data, thickness);
    }

    private record GeoData(int color) {

    }
}
