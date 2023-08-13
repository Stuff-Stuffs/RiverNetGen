package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

public class ConstantGeoFeature<T> implements GeoFeature<T> {
    private final double age;
    private final T material;

    public ConstantGeoFeature(final double age, final T material) {
        this.age = age;
        this.material = material;
    }

    @Override
    public double timeStamp() {
        return age;
    }

    @Override
    public Instance setup(final Registry<T> registry) {
        final int materialGeoId = registry.getGeoId(material);
        return context -> materialGeoId;
    }
}
