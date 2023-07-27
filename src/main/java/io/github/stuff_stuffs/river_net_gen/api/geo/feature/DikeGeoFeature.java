package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

public class DikeGeoFeature implements GeoFeature {
    private final double age;
    private final String material;
    private final double x, y, z;
    private final double nx, ny, nz;
    private final double thickness;
    private final double radius;

    public DikeGeoFeature(final double age, final String material, final double x, final double y, final double z, final double nx, final double ny, final double nz, final double thickness, final double radius) {
        this.age = age;
        this.material = material;
        this.x = x;
        this.y = y;
        this.z = z;
        final double scale = 1 / Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (!Double.isFinite(scale)) {
            throw new IllegalArgumentException();
        }
        this.nx = nx * scale;
        this.ny = ny * scale;
        this.nz = nz * scale;
        this.thickness = thickness;
        this.radius = radius;
    }

    @Override
    public double age() {
        return age;
    }

    @Override
    public Instance setup(final Registry registry) {
        final int materialGeoId = registry.getGeoId(material);
        return context -> {
            final double dx = context.x() - x;
            final double dy = context.y() - y;
            final double dz = context.z() - z;
            if (dx * dx + dy * dy + dz * dz >= radius * radius) {
                context.setQuery();
            } else if (Math.abs(nx * dx + ny * dy + nz * dz) <= thickness) {
                context.set(materialGeoId);
            } else {
                context.setQuery();
            }
        };
    }
}
