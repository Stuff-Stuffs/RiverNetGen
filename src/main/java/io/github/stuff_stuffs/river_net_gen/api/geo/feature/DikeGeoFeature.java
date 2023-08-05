package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

public class DikeGeoFeature implements GeoFeature {
    private final double timeStamp;
    private final String material;
    private final double x, y, z;
    private final double tx, ty, tz;
    private final double thickness;
    private final double radius;

    public DikeGeoFeature(final double timeStamp, final String material, final double x, final double y, final double z, final double tx, final double ty, final double tz, final double thickness, final double radius) {
        this.timeStamp = timeStamp;
        this.material = material;
        this.x = x;
        this.y = y;
        this.z = z;
        final double scale = 1 / Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (!Double.isFinite(scale) || scale == 0) {
            throw new IllegalArgumentException();
        }
        this.tx = tx * scale;
        this.ty = ty * scale;
        this.tz = tz * scale;
        this.thickness = thickness;
        this.radius = radius;
    }

    @Override
    public double timeStamp() {
        return timeStamp;
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
            } else if (Math.abs(tx * dx + ty * dy + tz * dz) * 2 <= thickness) {
                context.set(materialGeoId);
            } else {
                context.setQuery();
            }
        };
    }
}
