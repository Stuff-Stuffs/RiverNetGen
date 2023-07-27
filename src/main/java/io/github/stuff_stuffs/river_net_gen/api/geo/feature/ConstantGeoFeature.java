package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

public class ConstantGeoFeature implements GeoFeature {
    private final double age;
    private final String material;

    public ConstantGeoFeature(double age, String material) {
        this.age = age;
        this.material = material;
    }

    @Override
    public double age() {
        return age;
    }

    @Override
    public Instance setup(Registry registry) {
        int materialGeoId = registry.getGeoId(material);
        return context -> context.set(materialGeoId);
    }
}
