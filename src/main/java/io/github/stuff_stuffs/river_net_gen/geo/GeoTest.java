package io.github.stuff_stuffs.river_net_gen.geo;

import io.github.stuff_stuffs.river_net_gen.util.ImageOut;
import it.unimi.dsi.fastutil.HashCommon;

import java.util.function.IntConsumer;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class GeoTest {
    public static void main(final String[] args) {
        final int resolution = 512;
        final RandomGenerator generator = RandomGeneratorFactory.getDefault().create(1);
        final GeoColumn<GeoData> first = random(generator);
        final GeoColumn<GeoData> second = random(generator);
        final GeoColumnInterpolator2d<GeoData> interpolator = new GeoColumnInterpolator2d<>(first, second);
        ImageOut.draw(new ImageOut.Drawer() {
            @Override
            public void draw(final int x, final int y, final IntConsumer[] painters) {
                final int color = interpolator.interpolate(x / (double) resolution, resolution-y).color;
                painters[0].accept(HashCommon.murmurHash3(color ^ HashCommon.murmurHash3(color + 123456789)));
            }
        }, resolution, resolution, "tgeo.png");
    }

    private static GeoColumn<GeoData> random(final RandomGenerator random) {
        final int layerCount = 512;
        final int[] thickness = new int[layerCount];
        final GeoData[] data = new GeoData[layerCount];
        for (int i = 0; i < layerCount; i++) {
            thickness[i] = random.nextInt(16) + 4;
            data[i] = new GeoData(random.nextInt(8));
        }
        return new GeoColumn<>(data, thickness);
    }

    private record GeoData(int color) {

    }
}
